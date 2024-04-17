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
import ch.protonmail.android.mailmessage.domain.model.AttachmentId
import ch.protonmail.android.mailmessage.domain.model.MessageAttachment
import ch.protonmail.android.mailmessage.domain.model.MimeType
import com.github.mangstadt.vinnie.io.FoldedLineWriter
import javax.inject.Inject
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.random.Random

/**
 * Correctly encodes and formats Message body in multipart/mixed content type.
 */
@OptIn(ExperimentalEncodingApi::class)
class GenerateMimeBody @Inject constructor() {

    @Suppress("ImplicitDefaultLocale")
    operator fun invoke(
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

        val fileName = generateEncodedFilename(attachment.name)

        val stringWriter = StringWriter()
        FoldedLineWriter(stringWriter).use {
            it.write("Content-Transfer-Encoding: base64")
            it.writeln()
            it.write("Content-Type: ${attachment.mimeType}; filename=\"$fileName\"; name=\"$fileName\"")
            it.writeln()
            it.write("Content-Disposition: attachment; filename=\"$fileName\"; name=\"$fileName\"")
            it.writeln()
            it.writeln()
            it.write(Base64.encode(attachmentFile.readBytes()))
        }

        return stringWriter.toString()
    }

    private fun generateEncodedFilename(filename: String): String {
        // special way of encoding Base64 used in MIME: https://en.wikipedia.org/wiki/MIME#Encoded-Word
        val base64Filename = Base64.encode(filename.toByteArray())
        return "=?UTF-8?B?$base64Filename?="
    }
}
