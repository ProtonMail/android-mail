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

package ch.protonmail.android.mailmessage.domain.sample

import ch.protonmail.android.mailmessage.domain.model.AttachmentId
import ch.protonmail.android.mailmessage.domain.model.MessageAttachment
import ch.protonmail.android.mailmessage.domain.model.attachments.header.HeaderValue

object MessageAttachmentSample {

    val invoice = MessageAttachment(
        attachmentId = AttachmentId("invoice"),
        name = "invoice.pdf",
        size = 5678,
        mimeType = "application/pdf",
        disposition = null,
        keyPackets = "keyPackets",
        signature = null,
        encSignature = null,
        headers = emptyMap()
    )

    val invoiceWithBinaryContentType = MessageAttachment(
        attachmentId = AttachmentId("invoice_binary_content_type"),
        name = "invoice.pdf",
        size = 5678,
        mimeType = "application/octet-stream",
        disposition = null,
        keyPackets = "keyPackets",
        signature = null,
        encSignature = null,
        headers = emptyMap()
    )

    val publicKey = MessageAttachment(
        attachmentId = AttachmentId("publicKey"),
        name = "publickey - example@protonmail.com - 0x61DD734E.asc",
        size = 666,
        mimeType = "application/pgp-keys",
        disposition = "attachment",
        keyPackets = null,
        signature = null,
        encSignature = null,
        headers = emptyMap()
    )

    val document = MessageAttachment(
        attachmentId = AttachmentId("document"),
        name = "document.pdf",
        size = 1234,
        mimeType = "application/doc",
        disposition = null,
        keyPackets = null,
        signature = null,
        encSignature = null,
        headers = emptyMap()
    )

    val documentWithMultipleDots = MessageAttachment(
        attachmentId = AttachmentId("complicated.document.name"),
        name = "complicated.document.pdf",
        size = 1234,
        mimeType = "application/doc",
        disposition = null,
        keyPackets = null,
        signature = null,
        encSignature = null,
        headers = emptyMap()
    )

    val documentWithReallyLongFileName = MessageAttachment(
        attachmentId = AttachmentId("document"),
        name = "document-with-really-long-and-unnecessary-file-name-that-should-be-truncated.pdf",
        size = 1234,
        mimeType = "application/doc",
        disposition = null,
        keyPackets = null,
        signature = null,
        encSignature = null,
        headers = emptyMap()
    )

    val image = MessageAttachment(
        attachmentId = AttachmentId("image"),
        name = "image.png",
        size = 1234,
        mimeType = "image/png",
        disposition = "attachment",
        keyPackets = null,
        signature = null,
        encSignature = null,
        headers = emptyMap()
    )

    val embeddedImageAttachment = MessageAttachment(
        attachmentId = AttachmentId("embeddedImageId"),
        name = "embeddedImage.png",
        size = 1234,
        mimeType = "image/png",
        disposition = "inline",
        keyPackets = null,
        signature = null,
        encSignature = null,
        headers = createHeaderMap("content-id" to "embeddedImageContentId", "content-disposition" to "inline")
    )

    val embeddedImageAttachmentAsList = MessageAttachment(
        attachmentId = AttachmentId("embeddedImageId"),
        name = "embeddedImage.png",
        size = 1234,
        mimeType = "image/png",
        disposition = "inline",
        keyPackets = null,
        signature = null,
        encSignature = null,
        headers = createHeaderMap("content-id" to listOf("embeddedImageId"), "content-disposition" to "inline")
    )

    val embeddedOctetStreamAttachment = MessageAttachment(
        attachmentId = AttachmentId("embeddedImageId"),
        name = "embeddedOctet.png",
        size = 1234,
        mimeType = "application/octet-stream",
        disposition = "inline",
        keyPackets = null,
        signature = null,
        encSignature = null,
        headers = createHeaderMap("content-id" to "embeddedImageContentId", "content-disposition" to "inline")
    )

    val invalidEmbeddedImageAttachment = MessageAttachment(
        attachmentId = AttachmentId("embeddedImageId"),
        name = "embeddedImage.png",
        size = 1234,
        mimeType = "application/pdf",
        disposition = "inline",
        keyPackets = null,
        signature = null,
        encSignature = null,
        headers = createHeaderMap("content-id" to "embeddedImageContentId", "content-disposition" to "inline")
    )

    val signedDocument = MessageAttachment(
        attachmentId = AttachmentId("signed_document"),
        name = "document_signed.pdf",
        size = 1234,
        mimeType = "application/doc",
        disposition = null,
        keyPackets = null,
        signature = "attachment_signature",
        encSignature = "PGPSIGN----test----PGPSIGN",
        headers = emptyMap()
    )

    val calendar = MessageAttachment(
        attachmentId = AttachmentId("calendar"),
        name = "invite.ics",
        size = 1234,
        mimeType = "text/calendar",
        disposition = null,
        keyPackets = null,
        signature = null,
        encSignature = null,
        headers = emptyMap()
    )

    private fun createHeaderMap(vararg pairs: Pair<String, Any>): Map<String, HeaderValue> =
        pairs.toMap().mapValues { (_, value) ->
            when (value) {
                is String -> HeaderValue.StringValue(value)
                is List<*> -> HeaderValue.ListValue(value.filterIsInstance<String>())
                else -> throw IllegalArgumentException("Unsupported header value type: ${value::class.java}")
            }
        }
}
