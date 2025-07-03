/*
 * Copyright (c) 2022 Proton Technologies AG
 * This file is part of Proton Technologies AG and Proton Mail.
 *
 * Proton Mail is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Proton Mail is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Proton Mail. If not, see <https://www.gnu.org/licenses/>.
 */

package ch.protonmail.android.composer.data.usecase

import arrow.core.Either
import arrow.core.left
import arrow.core.raise.either
import arrow.core.right
import ch.protonmail.android.composer.data.remote.MessageRemoteDataSource
import ch.protonmail.android.composer.data.remote.resource.SendMessageBody
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcommon.domain.model.asExternalAddressSendDisabledError
import ch.protonmail.android.mailcommon.domain.model.asLocalizedApiError
import ch.protonmail.android.mailcommon.domain.model.isMessageAlreadySentSendingError
import ch.protonmail.android.mailcommon.domain.usecase.ResolveUserAddress
import ch.protonmail.android.mailcomposer.domain.usecase.FindLocalDraft
import ch.protonmail.android.mailcomposer.domain.usecase.ObserveMessageExpirationTime
import ch.protonmail.android.mailcomposer.domain.usecase.ObserveMessagePassword
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailmessage.domain.model.SendingError
import ch.protonmail.android.mailsettings.domain.usecase.ObserveMailSettings
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import me.proton.core.account.domain.repository.AccountRepository
import me.proton.core.auth.domain.repository.AuthRepository
import me.proton.core.domain.entity.UserId
import me.proton.core.mailmessage.domain.entity.Email
import me.proton.core.mailsendpreferences.domain.model.SendPreferences
import me.proton.core.mailsendpreferences.domain.usecase.ObtainSendPreferences
import me.proton.core.mailsettings.domain.entity.PackageType
import me.proton.core.util.kotlin.filterNullValues
import me.proton.core.util.kotlin.filterValues
import me.proton.core.util.kotlin.toInt
import timber.log.Timber
import javax.inject.Inject

class SendMessage @Inject constructor(
    private val accountRepository: AccountRepository,
    private val authRepository: AuthRepository,
    private val messageRemoteDataSource: MessageRemoteDataSource,
    private val resolveUserAddress: ResolveUserAddress,
    private val generateMessagePackages: GenerateMessagePackages,
    private val findLocalDraft: FindLocalDraft,
    private val obtainSendPreferences: ObtainSendPreferences,
    private val observeMailSettings: ObserveMailSettings,
    private val getAttachmentFiles: GetAttachmentFiles,
    private val observeMessagePassword: ObserveMessagePassword,
    private val observeMessageExpirationTime: ObserveMessageExpirationTime
) {

    /**
     * Because we ignore versioning conflicts between different clients, we assume that by the time this is called,
     * local draft has been correctly uploaded to backend and we will get the final version from DB here. Draft
     * should also be locked for editing by now.
     */
    suspend operator fun invoke(userId: UserId, messageId: MessageId): Either<Error, Unit> = either {

        val localDraft = findLocalDraft(userId, messageId) ?: raise(Error.DraftNotFound)

        val senderAddress = resolveUserAddress(userId, localDraft.message.addressId)
            .mapLeft { Error.SenderAddressNotFound }
            .bind()

        val autoSaveContacts = observeMailSettings(userId).firstOrNull()?.autoSaveContacts ?: false

        val recipients = localDraft.message.toList + localDraft.message.ccList + localDraft.message.bccList

        val sendPreferences = getSendPreferences(userId, recipients.map { it.address }).bind()

        val attachmentFiles = if (sendPreferences.containsMimeSchemePreferences()) {
            val attachmentIds = localDraft.messageBody.attachments.map { it.attachmentId }
            getAttachmentFiles(userId, messageId, attachmentIds).mapLeft { Error.DownloadingAttachments }.bind()
        } else {
            emptyMap()
        }

        val messagePassword = observeMessagePassword(userId, messageId).first()
        val modulus = if (messagePassword != null) {
            val sessionId = accountRepository.getSessionIdOrNull(userId)
            authRepository.randomModulus(sessionId)
        } else null

        val messagePackages = generateMessagePackages(
            senderAddress = senderAddress,
            localDraft = localDraft,
            sendPreferences = sendPreferences,
            attachmentFiles = attachmentFiles,
            messagePassword = messagePassword,
            modulus = modulus
        )
            .mapLeft {
                Timber.tag("SendMessage").e("Error generating packages: $it")
                Error.GeneratingPackages
            }
            .bind()

        val expiresInSeconds = observeMessageExpirationTime(userId, messageId).first()?.expiresIn?.inWholeSeconds ?: 0

        val response = messageRemoteDataSource.send(
            userId,
            localDraft.message.messageId.id,
            SendMessageBody(
                expiresIn = expiresInSeconds,
                autoSaveContacts = autoSaveContacts.toInt(),
                packages = messagePackages
            )
        ).mapLeft { Error.SendingToApi(it) }

        response.onLeft {
            Timber.tag("SendMessage").e("API error sending - error: %s - messageId: %s", it, messageId)
        }.onRight {
            Timber.tag("SendMessage").d("Success sending message ID: $messageId")
        }.bind()
    }

    private suspend fun getSendPreferences(
        userId: UserId,
        emails: List<Email>
    ): Either<Error.SendPreferences, Map<Email, SendPreferences>> {

        val sendPreferencesResults = runCatching {
            obtainSendPreferences(userId, emails)
        }.getOrElse {
            Timber.tag("SendMessage").e(
                "Unexpected exception ${it.message} while obtaining send preferences for $userId"
            )
            return Error.SendPreferences(emptyMap()).left()
        }

        val sendPreferencesSuccesses = sendPreferencesResults
            .filterValues<Email, ObtainSendPreferences.Result.Success>()

        val sendPreferencesErrors = sendPreferencesResults.mapValues {
            if (it.value is ObtainSendPreferences.Result.Error) {
                it.value as ObtainSendPreferences.Result.Error
            } else null
        }.filterNullValues()

        if (sendPreferencesErrors.isNotEmpty()) return Error.SendPreferences(sendPreferencesErrors).left()

        val sendPreferences = sendPreferencesSuccesses
            .mapValues { it.value.sendPreferences }

        val uniqueEmails = emails.distinctBy { it.lowercase() }
        // we failed getting send preferences for all recipients
        return if (sendPreferences.size != uniqueEmails.size) {
            Error.SendPreferences(emptyMap()).left()
        } else sendPreferences.right()
    }

    private fun Map<Email, SendPreferences>.containsMimeSchemePreferences() = values.any {
        it.encrypt && it.pgpScheme != PackageType.ProtonMail ||
            it.encrypt.not() && it.sign
    }

    sealed interface Error {

        object DraftNotFound : Error

        object SenderAddressNotFound : Error

        data class SendPreferences(
            /**
             * Detail mapping of what went wrong with SendPreferences for recipient emails.
             */
            val errors: Map<Email, ObtainSendPreferences.Result.Error>
        ) : Error

        object GeneratingPackages : Error

        data class SendingToApi(val remoteDataError: DataError.Remote) : Error

        object DownloadingAttachments : Error

        fun toSendingError(isApiSendingErrorsEnabled: Boolean): SendingError {
            return when (this) {
                is SendPreferences -> {
                    SendingError.SendPreferences(
                        this.errors.mapValues {
                            when (it.value) {
                                ObtainSendPreferences.Result.Error.AddressDisabled -> {
                                    SendingError.SendPreferencesError.AddressDisabled
                                }
                                ObtainSendPreferences.Result.Error.GettingContactPreferences -> {
                                    SendingError.SendPreferencesError.GettingContactPreferences
                                }
                                ObtainSendPreferences.Result.Error.NoCorrectlySignedTrustedKeys -> {
                                    SendingError.SendPreferencesError.NoCorrectlySignedTrustedKeys
                                }
                                ObtainSendPreferences.Result.Error.PublicKeysInvalid -> {
                                    SendingError.SendPreferencesError.PublicKeysInvalid
                                }
                                ObtainSendPreferences.Result.Error.TrustedKeysInvalid -> {
                                    SendingError.SendPreferencesError.TrustedKeysInvalid
                                }
                            }
                        }
                    )
                }

                is SendingToApi -> {
                    val localizedApiErrorMsg = if (isApiSendingErrorsEnabled) {
                        remoteDataError.asLocalizedApiError()?.apiMessage
                    } else {
                        null
                    }
                    val externalAddressDisabledError = remoteDataError.asExternalAddressSendDisabledError()
                    when {
                        externalAddressDisabledError != null -> {
                            SendingError.ExternalAddressSendDisabled(externalAddressDisabledError.apiMessage)
                        }

                        this.remoteDataError.isMessageAlreadySentSendingError() -> {
                            SendingError.MessageAlreadySent
                        }

                        localizedApiErrorMsg != null -> {
                            SendingError.GenericLocalized(localizedApiErrorMsg)
                        }

                        else -> {
                            SendingError.Other
                        }
                    }
                }

                else -> SendingError.Other
            }
        }
    }
}
