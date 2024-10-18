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

import ch.protonmail.android.mailmessage.domain.model.MessageAttachment
import ch.protonmail.android.mailmessage.domain.model.MessageBody
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailmessage.domain.model.MimeType
import ch.protonmail.android.mailmessage.domain.model.Recipient
import ch.protonmail.android.mailmessage.domain.model.UnsubscribeMethods
import ch.protonmail.android.mailmessage.domain.sample.MessageAttachmentSample
import ch.protonmail.android.mailmessage.domain.sample.RecipientSample
import ch.protonmail.android.testdata.message.MessageTestData.RAW_MESSAGE_ID
import ch.protonmail.android.testdata.user.UserIdTestData
import me.proton.core.domain.entity.UserId

object MessageBodyTestData {

    const val RAW_ENCRYPTED_MESSAGE_BODY = "This is a raw encrypted message body."

    val messageBody = buildMessageBody()

    val messageBodyWithAttachment = buildMessageBody(
        attachments = listOf(
            MessageAttachmentSample.invoice
        )
    )

    val messageBodyWithEmbeddedImage = buildMessageBody(
        attachments = listOf(
            MessageAttachmentSample.embeddedImageAttachment
        )
    )

    val messageBodyWithEmbeddedOctetStream = buildMessageBody(
        attachments = listOf(
            MessageAttachmentSample.embeddedOctetStreamAttachment
        )
    )

    val messageBodyWithInvalidEmbeddedAttachment = buildMessageBody(
        attachments = listOf(
            MessageAttachmentSample.invalidEmbeddedImageAttachment
        )
    )

    val messageBodyWithContentIdList = buildMessageBody(
        attachments = listOf(
            MessageAttachmentSample.embeddedImageAttachmentAsList
        )
    )

    val htmlMessageBody = buildMessageBody(
        mimeType = MimeType.Html
    )

    val multipartMixedMessageBody = buildMessageBody(
        mimeType = MimeType.MultipartMixed
    )

    fun buildMessageBody(
        userId: UserId = UserIdTestData.userId,
        messageId: MessageId = MessageId(RAW_MESSAGE_ID),
        body: String = RAW_ENCRYPTED_MESSAGE_BODY,
        header: String = "",
        attachments: List<MessageAttachment> = emptyList(),
        mimeType: MimeType = MimeType.PlainText,
        spamScore: String = "",
        replyTo: Recipient = RecipientSample.John,
        replyTos: List<Recipient> = emptyList(),
        unsubscribeMethods: UnsubscribeMethods? = UnsubscribeMethods(null, null, null)
    ) = MessageBody(
        userId = userId,
        messageId = messageId,
        body = body,
        header = header,
        attachments = attachments,
        mimeType = mimeType,
        spamScore = spamScore,
        replyTo = replyTo,
        replyTos = replyTos,
        unsubscribeMethods = unsubscribeMethods
    )
}
