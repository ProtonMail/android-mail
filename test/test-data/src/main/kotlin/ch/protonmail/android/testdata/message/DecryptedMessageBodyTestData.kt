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

package ch.protonmail.android.testdata.message

import ch.protonmail.android.mailmessage.domain.model.DecryptedMessageBody
import ch.protonmail.android.mailmessage.domain.model.MessageAttachment
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailmessage.domain.model.MimeType
import ch.protonmail.android.mailmessage.domain.sample.MessageIdSample

object DecryptedMessageBodyTestData {

    const val DECRYPTED_MESSAGE_BODY = "This is a decrypted message body"

    val messageBodyWithAttachment = buildDecryptedMessageBody(
        attachments = listOf(
            MessageAttachmentTestData.invoice
        )
    )

    val messageBodyWithEmbeddedImage = buildDecryptedMessageBody(
        attachments = listOf(
            MessageAttachmentTestData.embeddedImageAttachment
        )
    )

    val messageBodyWithEmbeddedOctetStream = buildDecryptedMessageBody(
        attachments = listOf(
            MessageAttachmentTestData.embeddedOctetStreamAttachment
        )
    )

    val messageBodyWithInvalidEmbeddedAttachment = buildDecryptedMessageBody(
        attachments = listOf(
            MessageAttachmentTestData.invalidEmbeddedImageAttachment
        )
    )

    val htmlInvoice = buildDecryptedMessageBody(
        messageId = MessageIdSample.HtmlInvoice,
        value = "<div>Decrypted invoice message HTML body</div>",
        mimeType = MimeType.Html,
        attachments = emptyList()
    )

    val PlainTextDecryptedBody = buildDecryptedMessageBody(
        messageId = MessageIdSample.PlainTextMessage,
        value = "Plain text message",
        mimeType = MimeType.PlainText,
        attachments = emptyList()
    )

    fun buildDecryptedMessageBody(
        messageId: MessageId = MessageIdSample.build(),
        value: String = DECRYPTED_MESSAGE_BODY,
        mimeType: MimeType = MimeType.Html,
        attachments: List<MessageAttachment> = emptyList()
    ) = DecryptedMessageBody(
        messageId = messageId,
        value = value,
        mimeType = mimeType,
        attachments = attachments
    )
}
