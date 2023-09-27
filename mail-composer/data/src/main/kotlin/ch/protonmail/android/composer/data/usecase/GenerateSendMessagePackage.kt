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
import ch.protonmail.android.mailmessage.domain.model.MimeType
import me.proton.core.crypto.common.context.CryptoContext
import me.proton.core.crypto.common.pgp.DataPacket
import me.proton.core.crypto.common.pgp.KeyPacket
import me.proton.core.crypto.common.pgp.SessionKey
import me.proton.core.key.domain.encryptSessionKey
import me.proton.core.key.domain.entity.key.PublicKey
import me.proton.core.mailmessage.domain.entity.Email
import me.proton.core.mailsendpreferences.domain.model.SendPreferences
import me.proton.core.mailsettings.domain.entity.PackageType
import me.proton.core.util.kotlin.toInt
import timber.log.Timber
import javax.inject.Inject
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@OptIn(ExperimentalEncodingApi::class)
@Suppress("LongParameterList")
class GenerateSendMessagePackage @Inject constructor(
    private val cryptoContext: CryptoContext
) {

    @Suppress("LongMethod")
    operator fun invoke(
        recipientEmail: Email,
        sendPreferences: SendPreferences,
        decryptedBodySessionKey: SessionKey,
        encryptedBodyDataPacket: ByteArray,
        decryptedMimeBodySessionKey: SessionKey,
        encryptedMimeBodyDataPacket: ByteArray,
        bodyContentType: MimeType,
        signedEncryptedMimeBody: Pair<KeyPacket, DataPacket>?,
        decryptedAttachmentSessionKeys: Map<String, SessionKey>
    ): SendMessagePackage? {

        return if (sendPreferences.encrypt) {

            if (sendPreferences.pgpScheme == PackageType.ProtonMail) {

                val publicKey = sendPreferences.publicKey ?: return null.also {
                    Timber.e("GenerateSendMessagePackage: publicKey for ${sendPreferences.pgpScheme.name} was null")
                }

                generateProtonMail(
                    publicKey,
                    decryptedBodySessionKey,
                    decryptedAttachmentSessionKeys,
                    recipientEmail,
                    sendPreferences,
                    encryptedBodyDataPacket
                )

            } else {

                if (signedEncryptedMimeBody == null) return null.also {
                    Timber.e("GenerateSendMessagePackage: signedEncryptedMimeBody was null")
                }

                generatePgpMime(recipientEmail, signedEncryptedMimeBody)

            }

        } else {

            if (sendPreferences.sign) {

                generateClearMime(recipientEmail, encryptedMimeBodyDataPacket, decryptedMimeBodySessionKey)

            } else {

                generateCleartext(
                    decryptedAttachmentSessionKeys,
                    recipientEmail,
                    bodyContentType,
                    encryptedBodyDataPacket,
                    decryptedBodySessionKey
                )

            }

        }

    }

    private fun generateCleartext(
        decryptedAttachmentSessionKeys: Map<String, SessionKey>,
        recipientEmail: Email,
        bodyMimeType: MimeType,
        encryptedBodyDataPacket: ByteArray,
        decryptedBodySessionKey: SessionKey
    ): SendMessagePackage {
        val packageAttachmentKeys = decryptedAttachmentSessionKeys.mapValues {
            SendMessagePackage.Key(Base64.encode(it.value.key), SessionKeyAlgorithm)
        }

        return SendMessagePackage(
            addresses = mapOf(
                recipientEmail to SendMessagePackage.Address.ExternalCleartext(signature = false.toInt())
            ),
            mimeType = bodyMimeType.value,
            body = Base64.encode(encryptedBodyDataPacket),
            type = PackageType.Cleartext.type,
            bodyKey = SendMessagePackage.Key(Base64.encode(decryptedBodySessionKey.key), SessionKeyAlgorithm),
            attachmentKeys = packageAttachmentKeys
        )
    }

    private fun generateClearMime(
        recipientEmail: Email,
        encryptedMimeBodyDataPacket: ByteArray,
        decryptedMimeBodySessionKey: SessionKey
    ) = SendMessagePackage(
        addresses = mapOf(
            recipientEmail to SendMessagePackage.Address.ExternalSigned(signature = true.toInt())
        ),
        mimeType = MimeType.MultipartMixed.value,
        body = Base64.encode(encryptedMimeBodyDataPacket),
        type = PackageType.ClearMime.type,
        bodyKey = SendMessagePackage.Key(
            Base64.encode(decryptedMimeBodySessionKey.key),
            SessionKeyAlgorithm
        )
    )

    private fun generatePgpMime(recipientEmail: Email, signedEncryptedMimeBody: Pair<KeyPacket, DataPacket>) =
        SendMessagePackage(
            addresses = mapOf(
                recipientEmail to SendMessagePackage.Address.ExternalEncrypted(
                    signature = true.toInt(),
                    bodyKeyPacket = Base64.encode(signedEncryptedMimeBody.first)
                )
            ),
            mimeType = MimeType.MultipartMixed.value,
            body = Base64.encode(signedEncryptedMimeBody.second),
            type = PackageType.PgpMime.type
        )

    private fun generateProtonMail(
        publicKey: PublicKey,
        decryptedBodySessionKey: SessionKey,
        decryptedAttachmentSessionKeys: Map<String, SessionKey>,
        recipientEmail: Email,
        sendPreferences: SendPreferences,
        encryptedBodyDataPacket: ByteArray
    ): SendMessagePackage {
        val recipientBodyKeyPacket = publicKey.encryptSessionKey(cryptoContext, decryptedBodySessionKey)

        val encryptedAttachmentKeyPackets = decryptedAttachmentSessionKeys.mapValues {
            Base64.encode(publicKey.encryptSessionKey(cryptoContext, it.value))
        }

        return SendMessagePackage(
            addresses = mapOf(
                recipientEmail to SendMessagePackage.Address.Internal(
                    signature = true.toInt(),
                    bodyKeyPacket = Base64.encode(recipientBodyKeyPacket),
                    attachmentKeyPackets = encryptedAttachmentKeyPackets
                )
            ),
            mimeType = sendPreferences.mimeType.value,
            body = Base64.encode(encryptedBodyDataPacket),
            type = PackageType.ProtonMail.type
        )
    }

    companion object {

        const val SessionKeyAlgorithm = "aes256"
    }
}
