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

import java.io.File
import java.io.StringWriter
import arrow.core.Either
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.composer.data.extension.encryptAndSignText
import ch.protonmail.android.composer.data.remote.resource.SendMessagePackage
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailmessage.domain.model.AttachmentId
import ch.protonmail.android.mailmessage.domain.model.MessageAttachment
import ch.protonmail.android.mailmessage.domain.model.MessageWithBody
import ch.protonmail.android.mailmessage.domain.model.MimeType
import com.github.mangstadt.vinnie.io.FoldedLineWriter
import me.proton.core.crypto.common.context.CryptoContext
import me.proton.core.crypto.common.pgp.DataPacket
import me.proton.core.crypto.common.pgp.KeyPacket
import me.proton.core.crypto.common.pgp.SessionKey
import me.proton.core.crypto.common.pgp.dataPacket
import me.proton.core.crypto.common.pgp.keyPacket
import me.proton.core.crypto.common.pgp.split
import me.proton.core.key.domain.decryptMimeMessage
import me.proton.core.key.domain.decryptSessionKey
import me.proton.core.key.domain.decryptText
import me.proton.core.key.domain.encryptAndSignText
import me.proton.core.key.domain.entity.keyholder.KeyHolderContext
import me.proton.core.key.domain.useKeys
import me.proton.core.mailmessage.domain.entity.Email
import me.proton.core.mailsendpreferences.domain.model.SendPreferences
import me.proton.core.mailsettings.domain.entity.PackageType
import me.proton.core.user.domain.entity.UserAddress
import me.proton.core.util.kotlin.filterNullValues
import javax.inject.Inject
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.random.Random

@OptIn(ExperimentalEncodingApi::class)
@Suppress("LongParameterList")
class GenerateMessagePackages @Inject constructor(
    private val cryptoContext: CryptoContext,
    private val generateSendMessagePackage: GenerateSendMessagePackage
) {

    @Suppress("LongMethod")
    operator fun invoke(
        senderAddress: UserAddress,
        localDraft: MessageWithBody,
        sendPreferences: Map<Email, SendPreferences>,
        attachmentFiles: Map<AttachmentId, File>
    ): Either<DataError.MessageSending.GeneratingPackages, List<SendMessagePackage>> {
        lateinit var decryptedPlaintextBodySessionKey: SessionKey
        lateinit var encryptedPlaintextBodyDataPacket: DataPacket

        lateinit var decryptedMimeBodySessionKey: SessionKey
        lateinit var encryptedMimeBodyDataPacket: DataPacket

        lateinit var signedAndEncryptedMimeBodyForRecipients: Map<Email, Pair<KeyPacket, DataPacket>>

        lateinit var decryptedAttachmentSessionKeys: Map<String, SessionKey>

        senderAddress.useKeys(cryptoContext) {

            // Decrypt session keys of all attachments for later creation of packages for plaintext recipients.
            decryptedAttachmentSessionKeys = localDraft.messageBody.attachments.associate { attachment ->
                attachment.keyPackets?.let {
                    attachment.attachmentId.id to decryptSessionKey(Base64.decode(it))
                } ?: return DataError.MessageSending.GeneratingPackages.left()
            }

            val encryptedBodyPgpMessage = localDraft.messageBody.body

            // Decrypt body's session key to send it for plaintext recipients.
            val encryptedPlaintextBodySplit = encryptedBodyPgpMessage.split(cryptoContext.pgpCrypto)
            decryptedPlaintextBodySessionKey = decryptSessionKey(encryptedPlaintextBodySplit.keyPacket())
            encryptedPlaintextBodyDataPacket = encryptedPlaintextBodySplit.dataPacket()

            // generate MIME version of the email
            val decryptedBody =
                if (localDraft.messageBody.mimeType == MimeType.MultipartMixed) {
                    decryptMimeMessage(encryptedBodyPgpMessage).body.content
                } else {
                    decryptText(encryptedBodyPgpMessage)
                }

            val mimeBody = generateMimeBody(
                decryptedBody,
                localDraft.messageBody.mimeType,
                localDraft.messageBody.attachments,
                attachmentFiles
            )

            // Encrypt and sign, then decrypt MIME body's session key to send it for plaintext recipients.
            val encryptedMimeBodySplit = encryptAndSignText(mimeBody).split(cryptoContext.pgpCrypto)
            decryptedMimeBodySessionKey = decryptSessionKey(encryptedMimeBodySplit.keyPacket())
            encryptedMimeBodyDataPacket = encryptedMimeBodySplit.dataPacket()

            signedAndEncryptedMimeBodyForRecipients = sendPreferences.mapValues { entry ->
                signAndEncryptMimeBody(entry, mimeBody, this, cryptoContext)
            }.filterNullValues()
        }

        val areAllAttachmentsSigned = localDraft.messageBody.attachments.all { it.signature != null }

        val packages = sendPreferences.map { entry ->
            generateSendMessagePackage(
                entry.key,
                entry.value,
                decryptedPlaintextBodySessionKey,
                encryptedPlaintextBodyDataPacket,
                decryptedMimeBodySessionKey,
                encryptedMimeBodyDataPacket,
                localDraft.messageBody.mimeType,
                signedAndEncryptedMimeBodyForRecipients[entry.key],
                decryptedAttachmentSessionKeys,
                areAllAttachmentsSigned
            )
        }.filterNotNull()

        return if (packages.size == sendPreferences.size) {
            packages.right()
        } else DataError.MessageSending.GeneratingPackages.left()
    }

    private fun signAndEncryptMimeBody(
        entry: Map.Entry<Email, SendPreferences>,
        plaintextMimeBody: String,
        keyHolderContext: KeyHolderContext,
        cryptoContext: CryptoContext
    ): Pair<KeyPacket, DataPacket>? {
        return with(entry.value) {
            if (encrypt && pgpScheme != PackageType.ProtonMail) {
                publicKey?.let { publicKey ->
                    keyHolderContext.encryptAndSignText(plaintextMimeBody, publicKey)
                        ?.split(cryptoContext.pgpCrypto)
                        ?.let { Pair(it.keyPacket(), it.dataPacket()) }
                }
            } else null
        }
    }

    /**
     * Correctly encodes and formats Message body in multipart/mixed content type.
     */
    @Suppress("ImplicitDefaultLocale")
    private fun generateMimeBody(
        body: String,
        bodyContentType: MimeType,
        attachments: List<MessageAttachment>,
        attachmentFiles: Map<AttachmentId, File>
    ): String {

        val bytes = ByteArray(16)
        Random.nextBytes(bytes)
        val boundaryHex = bytes.joinToString("") {
            String.format("%02x", it)
        }

        val boundary = "---------------------$boundaryHex"

        val stringWriter = StringWriter()
        FoldedLineWriter(stringWriter).use {
            it.write(body, true, Charsets.UTF_8)
        }
        val quotedPrintableBody = stringWriter.toString()

        val mimeAttachments = attachments.joinToString(separator = "\n") { attachment ->
            attachmentFiles[attachment.attachmentId]?.let { attachmentFile ->
                "${boundary}\n${generateMimeAttachment(attachment, attachmentFile)}"
            } ?: ""
        }

        return """
            |Content-Type: multipart/mixed; boundary=${boundary.substring(2)}
            |
            |$boundary
            |Content-Transfer-Encoding: quoted-printable
            |Content-Type: ${bodyContentType.value}; charset=utf-8
            |
            |$quotedPrintableBody
            |$mimeAttachments
            |$boundary--
        """.trimMargin()
    }

    private fun generateMimeAttachment(attachment: MessageAttachment, attachmentFile: File): String {

        val stringWriter = StringWriter()
        FoldedLineWriter(stringWriter).use {
            it.write(Base64.encode(attachmentFile.readBytes()))
        }
        val foldedAttachment = stringWriter.toString()

        return """
            |Content-Type: ${attachment.mimeType}; filename="${attachment.name}"; name="${attachment.name}"
            |Content-Transfer-Encoding: base64
            |Content-Disposition: attachment; filename="${attachment.name}"; name="${attachment.name}"
            |
            |$foldedAttachment
        """.trimMargin()
    }
}
