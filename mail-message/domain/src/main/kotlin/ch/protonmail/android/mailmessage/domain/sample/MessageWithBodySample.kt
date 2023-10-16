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

import ch.protonmail.android.mailmessage.domain.model.Message
import ch.protonmail.android.mailmessage.domain.model.MessageAttachment
import ch.protonmail.android.mailmessage.domain.model.MessageBody
import ch.protonmail.android.mailmessage.domain.model.MessageWithBody
import ch.protonmail.android.mailmessage.domain.model.MimeType
import ch.protonmail.android.mailmessage.domain.model.Recipient

object MessageWithBodySample {

    val EmptyDraft = build()

    val HtmlInvoice = build(
        message = MessageSample.HtmlInvoice,
        body = "This is the ENCRYPTED body of this html invoice message",
        mimeType = MimeType.Html
    )

    val Invoice = build(
        message = MessageSample.Invoice,
        body = "This is the ENCRYPTED body of this invoice message"
    )

    val NewDraftWithSubject = build(
        message = MessageSample.NewDraftWithSubject
    )

    val NewDraftWithSubjectAndBody = build(
        message = MessageSample.NewDraftWithSubject,
        body = "This is the body typed from the user, ENCRYPTED"
    )

    val RemoteDraft = build(
        message = MessageSample.RemoteDraft
    )

    val RemoteDraftWith4RecipientTypes = build(
        message = MessageSample.RemoteDraftWith4RecipientTypes
    )

    val MessageWithAttachments = build(
        message = MessageSample.MessageWithAttachments,
        attachments = listOf(
            MessageAttachmentSample.document,
            MessageAttachmentSample.documentWithReallyLongFileName,
            MessageAttachmentSample.embeddedImageAttachment
        )
    )

    val MessageWithEncryptedAttachments = build(
        message = MessageSample.MessageWithAttachments,
        attachments = listOf(
            MessageAttachmentSample.document.copy(keyPackets = "encryptedKeyPackets"),
            MessageAttachmentSample.documentWithReallyLongFileName.copy(keyPackets = "encryptedKeyPackets"),
            MessageAttachmentSample.embeddedImageAttachment.copy(keyPackets = "encryptedKeyPackets")
        )
    )

    val MessageWithSignedAttachments = build(
        message = MessageSample.MessageWithAttachments,
        attachments = listOf(MessageAttachmentSample.signedDocument)
    )

    val MessageWithInvoiceAttachment = build(
        message = MessageSample.MessageWithAttachments,
        attachments = listOf(MessageAttachmentSample.invoice),
        body = "non-empty-body"
    )

    private fun build(
        message: Message = MessageSample.EmptyDraft,
        replyTo: Recipient = RecipientSample.John,
        body: String = "",
        mimeType: MimeType = MimeType.PlainText,
        attachments: List<MessageAttachment> = emptyList()
    ) = MessageWithBody(
        message = message,
        messageBody = MessageBody(
            userId = message.userId,
            messageId = message.messageId,
            body = body,
            header = "",
            attachments = attachments,
            mimeType = mimeType,
            spamScore = "",
            replyTo = replyTo,
            replyTos = emptyList(),
            unsubscribeMethods = null
        )
    )
}
