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
import arrow.core.raise.either
import arrow.core.right
import ch.protonmail.android.composer.data.remote.resource.SendMessagePackage
import ch.protonmail.android.mailcomposer.domain.model.MessagePassword
import ch.protonmail.android.mailmessage.domain.model.MimeType
import me.proton.core.auth.domain.entity.Modulus
import me.proton.core.crypto.common.context.CryptoContext
import me.proton.core.crypto.common.pgp.DataPacket
import me.proton.core.crypto.common.pgp.KeyPacket
import me.proton.core.crypto.common.pgp.SessionKey
import me.proton.core.key.domain.encryptSessionKey
import me.proton.core.mailmessage.domain.entity.Email
import me.proton.core.mailsendpreferences.domain.model.SendPreferences
import me.proton.core.mailsettings.domain.entity.PackageType
import me.proton.core.util.kotlin.takeIfNotEmpty
import me.proton.core.util.kotlin.toInt
import timber.log.Timber
import javax.inject.Inject
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@OptIn(ExperimentalEncodingApi::class)
@Suppress("LongParameterList", "MaxLineLength")
class GenerateSendMessagePackages @Inject constructor(
    private val cryptoContext: CryptoContext
) {

    @Suppress("LongMethod")
    suspend operator fun invoke(
        sendPreferences: Map<Email, SendPreferences>,
        decryptedBodySessionKey: SessionKey,
        encryptedBodyDataPacket: ByteArray,
        decryptedMimeBodySessionKey: SessionKey,
        encryptedMimeBodyDataPacket: ByteArray,
        bodyContentType: MimeType,
        signedEncryptedMimeBodies: Map<Email, Pair<KeyPacket, DataPacket>>,
        decryptedAttachmentSessionKeys: Map<String, SessionKey>,
        areAllAttachmentsSigned: Boolean,
        messagePassword: MessagePassword?,
        modulus: Modulus?
    ): Either<Error, List<SendMessagePackage>> = either {

        val sendPreferencesBySubpackageType = groupBySubpackageType(
            sendPreferences, isEncryptOutside = messagePassword != null
        )

        val protonMailSendPreferences = sendPreferencesBySubpackageType.getOrDefault(
            PackageType.ProtonMail, emptyList()
        )

        val protonMailPackage = generateProtonMail(
            protonMailSendPreferences,
            decryptedBodySessionKey,
            decryptedAttachmentSessionKeys,
            encryptedBodyDataPacket,
            areAllAttachmentsSigned,
            bodyContentType
        ).bind()

        val cleartextSendPreferences = sendPreferencesBySubpackageType.getOrDefault(
            PackageType.Cleartext, emptyList()
        )

        val cleartextPackage = generateCleartext(
            cleartextSendPreferences,
            decryptedBodySessionKey,
            decryptedAttachmentSessionKeys,
            encryptedBodyDataPacket,
            bodyContentType
        ).bind()

        val clearMimeSendPreferences = sendPreferencesBySubpackageType.getOrDefault(
            PackageType.ClearMime, emptyList()
        )

        val clearMimePackage =
            generateClearMime(
                clearMimeSendPreferences,
                encryptedMimeBodyDataPacket,
                decryptedMimeBodySessionKey
            )

        val encryptedOutsideSendPreferences = sendPreferencesBySubpackageType.getOrDefault(
            PackageType.EncryptedOutside, emptyList()
        )

        val encryptedOutsidePackage =
            generateEncryptedOutside(
                encryptedOutsideSendPreferences,
                decryptedBodySessionKey,
                decryptedAttachmentSessionKeys,
                encryptedBodyDataPacket,
                bodyContentType,
                messagePassword,
                modulus,
                areAllAttachmentsSigned
            ).bind()

        val pgpMimeSendPreferences = sendPreferencesBySubpackageType.getOrDefault(
            PackageType.PgpMime, emptyList()
        )

        val pgpMimePackages =
            generatePgpMime(
                pgpMimeSendPreferences,
                signedEncryptedMimeBodies,
                areAllAttachmentsSigned
            )

        return (
            listOfNotNull(
                protonMailPackage.takeIf { it.addresses.isNotEmpty() },
                cleartextPackage.takeIf { it.addresses.isNotEmpty() },
                clearMimePackage.takeIf { it.addresses.isNotEmpty() },
                encryptedOutsidePackage.takeIf { it.addresses.isNotEmpty() }
            ) + pgpMimePackages
            ).right()
    }

    private fun groupBySubpackageType(
        allSendPreferences: Map<Email, SendPreferences>,
        isEncryptOutside: Boolean
    ): Map<PackageType?, List<Map.Entry<Email, SendPreferences>>> = allSendPreferences.entries.groupBy {
        when (it.value.pgpScheme) {
            PackageType.ProtonMail -> PackageType.ProtonMail
            PackageType.Cleartext -> when {
                isEncryptOutside -> PackageType.EncryptedOutside
                it.value.sign -> PackageType.ClearMime
                else -> PackageType.Cleartext
            }
            PackageType.PgpInline,
            PackageType.PgpMime -> when {
                isEncryptOutside -> PackageType.EncryptedOutside
                it.value.encrypt -> PackageType.PgpMime
                it.value.sign -> PackageType.ClearMime
                else -> PackageType.Cleartext
            }
            PackageType.ClearMime -> if (isEncryptOutside) PackageType.EncryptedOutside else PackageType.ClearMime
            else -> null
        }
    }

    private fun generateProtonMail(
        sendPreferences: List<Map.Entry<Email, SendPreferences>>,
        decryptedBodySessionKey: SessionKey,
        decryptedAttachmentSessionKeys: Map<String, SessionKey>,
        encryptedBodyDataPacket: ByteArray,
        areAllAttachmentsSigned: Boolean,
        bodyContentType: MimeType
    ): Either<Error, SendMessagePackage> = either {
        val addresses = sendPreferences.mapNotNull { (recipientEmail, sendPreference) ->

            val recipientPublicKey = sendPreference.publicKey

            if (recipientPublicKey == null) {
                Timber.e("GenerateSendMessagePackages: publicKey for ${sendPreference.pgpScheme.name} was null")
                return@mapNotNull null
            }

            val recipientBodyKeyPacket = runCatching {
                recipientPublicKey.encryptSessionKey(
                    cryptoContext,
                    decryptedBodySessionKey
                )
            }.getOrElse {
                raise(Error.ProtonMailAndCleartext("generateProtonMailAndCleartext: error encrypting SessionKey for recipientBodyKeyPacket"))
            }

            val encryptedAttachmentKeyPackets = runCatching {
                decryptedAttachmentSessionKeys.mapValues {
                    Base64.encode(recipientPublicKey.encryptSessionKey(cryptoContext, it.value))
                }
            }.getOrElse {
                raise(Error.ProtonMailAndCleartext("generateProtonMailAndCleartext: error encrypting SessionKey for encryptedAttachmentKeyPackets"))
            }

            recipientEmail to SendMessagePackage.Address.Internal(
                signature = areAllAttachmentsSigned.toInt(),
                bodyKeyPacket = Base64.encode(recipientBodyKeyPacket),
                attachmentKeyPackets = encryptedAttachmentKeyPackets
            )
        }.toMap()

        val globalPackageType = addresses.map { it.value.type }.takeIfNotEmpty()?.reduce { a, b ->
            a.or(b) // logical OR of package types
        } ?: -1

        return SendMessagePackage(
            addresses = addresses,
            mimeType = bodyContentType.value,
            body = Base64.encode(encryptedBodyDataPacket),
            type = globalPackageType
        ).right()
    }

    private fun generateCleartext(
        sendPreferences: List<Map.Entry<Email, SendPreferences>>,
        decryptedBodySessionKey: SessionKey,
        decryptedAttachmentSessionKeys: Map<String, SessionKey>,
        encryptedBodyDataPacket: ByteArray,
        bodyContentType: MimeType
    ): Either<Error, SendMessagePackage> = either {

        val addresses = sendPreferences.associate { (recipientEmail, _) ->
            recipientEmail to SendMessagePackage.Address.ExternalCleartext(signature = false.toInt())
        }

        val bodyKey = SendMessagePackage.Key(
            Base64.encode(decryptedBodySessionKey.key),
            SessionKeyAlgorithm
        )

        val attachmentKeys = decryptedAttachmentSessionKeys.mapValues {
            SendMessagePackage.Key(Base64.encode(it.value.key), SessionKeyAlgorithm)
        }

        val globalPackageType = addresses.map { it.value.type }.takeIfNotEmpty()?.reduce { a, b ->
            a.or(b) // logical OR of package types
        } ?: -1

        return SendMessagePackage(
            addresses = addresses,
            mimeType = bodyContentType.value,
            body = Base64.encode(encryptedBodyDataPacket),
            type = globalPackageType,
            bodyKey = bodyKey,
            attachmentKeys = attachmentKeys
        ).right()
    }

    private fun generateClearMime(
        sendPreferences: List<Map.Entry<Email, SendPreferences>>,
        encryptedMimeBodyDataPacket: ByteArray,
        decryptedMimeBodySessionKey: SessionKey
    ): SendMessagePackage {

        val addresses = sendPreferences.associate { (recipientEmail, _) ->
            recipientEmail to SendMessagePackage.Address.ExternalSigned(signature = true.toInt())
        }.toMap()

        return SendMessagePackage(
            addresses = addresses,
            mimeType = MimeType.MultipartMixed.value,
            body = Base64.encode(encryptedMimeBodyDataPacket),
            type = PackageType.ClearMime.type,
            bodyKey = SendMessagePackage.Key(
                Base64.encode(decryptedMimeBodySessionKey.key),
                SessionKeyAlgorithm
            )
        )
    }

    private fun generatePgpMime(
        sendPreferences: List<Map.Entry<Email, SendPreferences>>,
        signedEncryptedMimeBodies: Map<Email, Pair<KeyPacket, DataPacket>>,
        areAllAttachmentsSigned: Boolean
    ): List<SendMessagePackage> = sendPreferences.mapNotNull { (recipientEmail, _) ->
        signedEncryptedMimeBodies[recipientEmail]?.let {
            SendMessagePackage(
                addresses = mapOf(
                    recipientEmail to SendMessagePackage.Address.ExternalEncrypted(
                        signature = areAllAttachmentsSigned.toInt(),
                        bodyKeyPacket = Base64.encode(it.first)
                    )
                ),
                mimeType = MimeType.MultipartMixed.value,
                body = Base64.encode(it.second),
                type = PackageType.PgpMime.type
            )
        } ?: null.also {
            Timber.e("GenerateSendMessagePackages: signedEncryptedMimeBody was null")
        }
    }

    @Suppress("LongMethod")
    private suspend fun generateEncryptedOutside(
        sendPreferences: List<Map.Entry<Email, SendPreferences>>,
        decryptedBodySessionKey: SessionKey,
        decryptedAttachmentSessionKeys: Map<String, SessionKey>,
        encryptedBodyDataPacket: ByteArray,
        bodyContentType: MimeType,
        messagePassword: MessagePassword?,
        modulus: Modulus?,
        areAllAttachmentsSigned: Boolean
    ): Either<Error, SendMessagePackage> = either {

        val addresses = if (messagePassword != null && modulus != null) {

            sendPreferences.associate { (recipientEmail, _) ->

                val passwordByteArray = messagePassword.password.toByteArray()

                val bodyKeyPacket = runCatching {
                    Base64.encode(
                        cryptoContext.pgpCrypto.encryptSessionKeyWithPassword(decryptedBodySessionKey, passwordByteArray)
                    )
                }.getOrElse {
                    raise(Error.EncryptedOutside("generateEncryptedOutside: error encrypting SessionKey with password for bodyKeyPacket"))
                }

                val attachmentKeyPackets = runCatching {
                    decryptedAttachmentSessionKeys.mapValues {
                        Base64.encode(cryptoContext.pgpCrypto.encryptSessionKeyWithPassword(it.value, passwordByteArray))
                    }
                }.getOrElse {
                    raise(Error.EncryptedOutside("generateEncryptedOutside: error encrypting SessionKey with password for attachmentKeyPackets"))
                }

                val token = runCatching {
                    Base64.encode(cryptoContext.pgpCrypto.generateRandomBytes(size = 32))
                }.getOrElse {
                    raise(Error.EncryptedOutside("generateEncryptedOutside: error generating random bytes"))
                }

                val encryptedToken = runCatching {
                    cryptoContext.pgpCrypto.encryptTextWithPassword(token, passwordByteArray)
                }.getOrElse {
                    raise(Error.EncryptedOutside("generateEncryptedOutside: error encrypting token"))
                }

                val passwordVerifier = runCatching {
                    cryptoContext.srpCrypto.calculatePasswordVerifier(
                        username = "", // required for legacy reasons, can be empty
                        password = passwordByteArray,
                        modulusId = modulus.modulusId,
                        modulus = modulus.modulus
                    )
                }.getOrElse {
                    raise(Error.EncryptedOutside("generateEncryptedOutside: error calculating password verifier"))
                }

                val auth = SendMessagePackage.Auth(
                    modulusId = passwordVerifier.modulusId,
                    version = passwordVerifier.version,
                    salt = passwordVerifier.salt,
                    verifier = passwordVerifier.verifier
                )

                recipientEmail to SendMessagePackage.Address.EncryptedOutside(
                    bodyKeyPacket = bodyKeyPacket,
                    attachmentKeyPackets = attachmentKeyPackets,
                    token = token,
                    encToken = encryptedToken,
                    auth = auth,
                    passwordHint = messagePassword.passwordHint,
                    signature = areAllAttachmentsSigned.toInt()
                )
            }.toMap()
        } else emptyMap()

        val globalPackageType = addresses.map { it.value.type }.takeIfNotEmpty()?.reduce { a, b ->
            a.or(b) // logical OR of package types
        } ?: -1

        return SendMessagePackage(
            addresses = addresses,
            mimeType = bodyContentType.value,
            body = Base64.encode(encryptedBodyDataPacket),
            type = globalPackageType
        ).right()
    }


    companion object {

        const val SessionKeyAlgorithm = "aes256"

    }

    sealed class Error(open val message: String) {
        data class ProtonMailAndCleartext(override val message: String) : Error(message)
        data class EncryptedOutside(override val message: String) : Error(message)
    }
}
