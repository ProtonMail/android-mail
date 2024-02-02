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
@Suppress("LongParameterList")
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
    ): List<SendMessagePackage> {

        val sendPreferencesBySubpackageType = groupBySubpackageType(
            sendPreferences, isEncryptOutside = messagePassword != null
        )

        val protonMailAndCleartextSendPreferences =
            sendPreferencesBySubpackageType.getOrDefault(
                PackageType.ProtonMail, emptyList()
            ) + sendPreferencesBySubpackageType.getOrDefault(
                PackageType.Cleartext, emptyList()
            )

        val protonMailAndCleartextPackage =
            generateProtonMailAndCleartext(
                protonMailAndCleartextSendPreferences,
                decryptedBodySessionKey,
                decryptedAttachmentSessionKeys,
                encryptedBodyDataPacket,
                areAllAttachmentsSigned,
                bodyContentType
            )

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
            )

        val pgpMimeSendPreferences = sendPreferencesBySubpackageType.getOrDefault(
            PackageType.PgpMime, emptyList()
        )

        val pgpMimePackages =
            generatePgpMime(
                pgpMimeSendPreferences,
                signedEncryptedMimeBodies,
                areAllAttachmentsSigned
            )

        return listOfNotNull(
            protonMailAndCleartextPackage.takeIf { it.addresses.isNotEmpty() },
            clearMimePackage.takeIf { it.addresses.isNotEmpty() },
            encryptedOutsidePackage.takeIf { it.addresses.isNotEmpty() }
        ) + pgpMimePackages
    }


    private fun generateProtonMailAndCleartext(
        sendPreferences: List<Map.Entry<Email, SendPreferences>>,
        decryptedBodySessionKey: SessionKey,
        decryptedAttachmentSessionKeys: Map<String, SessionKey>,
        encryptedBodyDataPacket: ByteArray,
        areAllAttachmentsSigned: Boolean,
        bodyContentType: MimeType
    ): SendMessagePackage {

        val addresses = sendPreferences.mapNotNull { (recipientEmail, sendPreference) ->
            when (sendPreference.pgpScheme) {
                PackageType.ProtonMail -> {

                    val recipientPublicKey = sendPreference.publicKey

                    if (recipientPublicKey == null) {
                        Timber.e("GenerateSendMessagePackages: publicKey for ${sendPreference.pgpScheme.name} was null")
                        return@mapNotNull null
                    }

                    val recipientBodyKeyPacket = recipientPublicKey.encryptSessionKey(
                        cryptoContext,
                        decryptedBodySessionKey
                    )

                    val encryptedAttachmentKeyPackets = decryptedAttachmentSessionKeys.mapValues {
                        Base64.encode(recipientPublicKey.encryptSessionKey(cryptoContext, it.value))
                    }

                    recipientEmail to SendMessagePackage.Address.Internal(
                        signature = areAllAttachmentsSigned.toInt(),
                        bodyKeyPacket = Base64.encode(recipientBodyKeyPacket),
                        attachmentKeyPackets = encryptedAttachmentKeyPackets
                    )

                }

                PackageType.Cleartext -> {

                    recipientEmail to SendMessagePackage.Address.ExternalCleartext(signature = false.toInt())

                }

                else -> null
            }
        }.toMap()

        val (bodyKey, attachmentKeys) = if (addresses.any { it.value.type == PackageType.Cleartext.type }) {

            val bodyKey = SendMessagePackage.Key(
                Base64.encode(decryptedBodySessionKey.key),
                SessionKeyAlgorithm
            )

            val packageAttachmentKeys = decryptedAttachmentSessionKeys.mapValues {
                SendMessagePackage.Key(Base64.encode(it.value.key), SessionKeyAlgorithm)
            }

            Pair(bodyKey, packageAttachmentKeys)

        } else Pair(null, null)

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
        )
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
            PackageType.PgpMime -> when {
                it.value.encrypt -> PackageType.PgpMime
                isEncryptOutside -> PackageType.EncryptedOutside
                else -> PackageType.ClearMime
            }
            PackageType.ClearMime -> if (isEncryptOutside) PackageType.EncryptedOutside else PackageType.ClearMime
            else -> null
        }
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

    private suspend fun generateEncryptedOutside(
        sendPreferences: List<Map.Entry<Email, SendPreferences>>,
        decryptedBodySessionKey: SessionKey,
        decryptedAttachmentSessionKeys: Map<String, SessionKey>,
        encryptedBodyDataPacket: ByteArray,
        bodyContentType: MimeType,
        messagePassword: MessagePassword?,
        modulus: Modulus?,
        areAllAttachmentsSigned: Boolean
    ): SendMessagePackage {

        val addresses = if (messagePassword != null && modulus != null) {

            sendPreferences.associate { (recipientEmail, _) ->

                val passwordByteArray = messagePassword.password.toByteArray()

                val bodyKeyPacket = Base64.encode(
                    cryptoContext.pgpCrypto.encryptSessionKeyWithPassword(decryptedBodySessionKey, passwordByteArray)
                )

                val attachmentKeyPackets = decryptedAttachmentSessionKeys.mapValues {
                    Base64.encode(cryptoContext.pgpCrypto.encryptSessionKeyWithPassword(it.value, passwordByteArray))
                }

                val token = Base64.encode(cryptoContext.pgpCrypto.generateRandomBytes(size = 32))
                val encryptedToken = cryptoContext.pgpCrypto.encryptTextWithPassword(token, passwordByteArray)

                val passwordVerifier = cryptoContext.srpCrypto.calculatePasswordVerifier(
                    username = "", // required for legacy reasons, can be empty
                    password = passwordByteArray,
                    modulusId = modulus.modulusId,
                    modulus = modulus.modulus
                )
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
        )
    }

    companion object {

        const val SessionKeyAlgorithm = "aes256"
    }
}
