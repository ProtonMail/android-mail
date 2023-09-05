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
import me.proton.core.crypto.common.context.CryptoContext
import me.proton.core.crypto.common.pgp.EncryptedPacket
import me.proton.core.crypto.common.pgp.SessionKey
import me.proton.core.key.domain.encryptSessionKey
import me.proton.core.mailmessage.domain.entity.Email
import me.proton.core.mailsendpreferences.domain.model.SendPreferences
import me.proton.core.mailsettings.domain.entity.MimeType
import me.proton.core.mailsettings.domain.entity.PackageType
import me.proton.core.util.kotlin.toInt
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
        signedEncryptedMimeBody: Pair<EncryptedPacket, EncryptedPacket>?,
        decryptedAttachmentSessionKeys: List<SessionKey>
    ): SendMessagePackage? {

        return if (sendPreferences.encrypt) {

            if (sendPreferences.pgpScheme == PackageType.ProtonMail) { // Internal Proton

                val publicKey = sendPreferences.publicKey ?: return null

                val recipientBodyKeyPacket = publicKey.encryptSessionKey(cryptoContext, decryptedBodySessionKey)

                val encryptedAttachmentKeyPackets = decryptedAttachmentSessionKeys.map {
                    Base64.encode(publicKey.encryptSessionKey(cryptoContext, it))
                }

                SendMessagePackage(
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

            } else { // PgpMime

                if (signedEncryptedMimeBody == null) return null

                SendMessagePackage(
                    addresses = mapOf(
                        recipientEmail to SendMessagePackage.Address.ExternalEncrypted(
                            signature = true.toInt(),
                            bodyKeyPacket = Base64.encode(signedEncryptedMimeBody.first.packet)
                        )
                    ),
                    mimeType = MimeType.Mixed.value,
                    body = Base64.encode(signedEncryptedMimeBody.second.packet),
                    type = PackageType.PgpMime.type
                )

            }

        } else {

            if (sendPreferences.sign) { // ClearMime

                SendMessagePackage(
                    addresses = mapOf(
                        recipientEmail to SendMessagePackage.Address.ExternalSigned(signature = true.toInt())
                    ),
                    mimeType = MimeType.Mixed.value,
                    body = Base64.encode(encryptedMimeBodyDataPacket),
                    type = PackageType.ClearMime.type,
                    bodyKey = SendMessagePackage.Key(
                        Base64.encode(decryptedMimeBodySessionKey.key),
                        SessionKeyAlgorithm
                    )
                )

            } else { // Cleartext

                val packageAttachmentKeys = decryptedAttachmentSessionKeys.map {
                    SendMessagePackage.Key(Base64.encode(it.key), SessionKeyAlgorithm)
                }

                SendMessagePackage(
                    addresses = mapOf(
                        recipientEmail to SendMessagePackage.Address.ExternalCleartext(signature = false.toInt())
                    ),
                    mimeType = sendPreferences.mimeType.value,
                    body = Base64.encode(encryptedBodyDataPacket),
                    type = PackageType.Cleartext.type,
                    bodyKey = SendMessagePackage.Key(Base64.encode(decryptedBodySessionKey.key), SessionKeyAlgorithm),
                    attachmentKeys = packageAttachmentKeys
                )

            }

        }

    }

    private companion object {

        const val SessionKeyAlgorithm = "aes256"
    }
}
