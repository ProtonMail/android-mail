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
import arrow.core.continuations.either
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.composer.data.remote.MessageRemoteDataSource
import ch.protonmail.android.composer.data.remote.resource.SendMessageBody
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcomposer.domain.usecase.FindLocalDraft
import ch.protonmail.android.mailcomposer.domain.usecase.ResolveUserAddress
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailsettings.domain.usecase.ObserveMailSettings
import kotlinx.coroutines.flow.first
import me.proton.core.domain.entity.UserId
import me.proton.core.mailmessage.domain.entity.Email
import me.proton.core.mailsendpreferences.domain.model.SendPreferences
import me.proton.core.mailsendpreferences.domain.usecase.ObtainSendPreferences
import me.proton.core.util.kotlin.filterValues
import me.proton.core.util.kotlin.toInt
import timber.log.Timber
import javax.inject.Inject

internal class SendMessage @Inject constructor(
    private val messageRemoteDataSource: MessageRemoteDataSource,
    private val resolveUserAddress: ResolveUserAddress,
    private val generateMessagePackages: GenerateMessagePackages,
    private val findLocalDraft: FindLocalDraft,
    private val obtainSendPreferences: ObtainSendPreferences,
    private val observeMailSettings: ObserveMailSettings
) {

    /**
     * Because we ignore versioning conflicts between different clients, we assume that by the time this is called,
     * local draft has been correctly uploaded to backend and we will get the final version from DB here. Draft
     * should also be locked for editing by now.
     */
    suspend operator fun invoke(userId: UserId, messageId: MessageId): Either<DataError, Unit> = either {

        val localDraft = findLocalDraft(userId, messageId) ?: shift(DataError.MessageSending.DraftNotFound)

        val senderAddress = resolveUserAddress(userId, localDraft.message.addressId)
            .mapLeft { DataError.MessageSending.SenderAddressNotFound }
            .bind()

        val autoSaveContacts = observeMailSettings(userId).first()?.autoSaveContacts ?: false

        val recipients = localDraft.message.toList + localDraft.message.ccList + localDraft.message.bccList

        val sendPreferences = getSendPreferences(userId, recipients.map { it.address }).bind()

        val messagePackages = generateMessagePackages(senderAddress, localDraft, sendPreferences).bind()

        val response = messageRemoteDataSource.send(
            userId,
            localDraft.message.messageId.id,
            SendMessageBody(
                autoSaveContacts = autoSaveContacts.toInt(),
                packages = messagePackages
            )
        )

        response.onLeft {
            Timber.e("API error sending message ID: $messageId", it)
        }.onRight {
            Timber.d("Success sending message ID: $messageId")
        }
    }

    private suspend fun getSendPreferences(
        userId: UserId,
        emails: List<Email>
    ): Either<DataError.MessageSending.SendPreferences, Map<Email, SendPreferences>> {

        val sendPreferences = obtainSendPreferences(userId, emails)
            .filterValues(ObtainSendPreferences.Result.Success::class.java)
            .mapValues { it.value.sendPreferences }

        // we failed getting send preferences for all recipients
        return if (sendPreferences.size != emails.size) {
            DataError.MessageSending.SendPreferences.left()
        } else sendPreferences.right()

    }

}
