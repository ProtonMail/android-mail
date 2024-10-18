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

package ch.protonmail.android.mailmessage.data.remote.resource

import ch.protonmail.android.mailcommon.domain.model.ConversationId
import ch.protonmail.android.mailmessage.domain.model.AttachmentId
import ch.protonmail.android.mailmessage.domain.model.MailTo
import ch.protonmail.android.mailmessage.domain.model.Message
import ch.protonmail.android.mailmessage.domain.model.MessageAttachment
import ch.protonmail.android.mailmessage.domain.model.MessageBody
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailmessage.domain.model.MessageWithBody
import ch.protonmail.android.mailmessage.domain.model.MimeType
import ch.protonmail.android.mailmessage.domain.model.UnsubscribeMethods
import ch.protonmail.android.mailmessage.domain.model.attachments.header.HeaderValue
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import me.proton.core.domain.entity.UserId
import me.proton.core.label.domain.entity.LabelId
import me.proton.core.user.domain.entity.AddressId
import me.proton.core.util.kotlin.toBooleanOrFalse

@Serializable
data class MessageWithBodyResource(
    @SerialName("ID")
    val id: String,
    @SerialName("Order")
    val order: Long,
    @SerialName("ConversationID")
    val conversationId: String,
    @SerialName("Subject")
    val subject: String,
    @SerialName("Unread")
    val unread: Int,
    @SerialName("Sender")
    val sender: RecipientResource,
    @SerialName("ToList")
    val toList: List<RecipientResource>,
    @SerialName("CCList")
    val ccList: List<RecipientResource>,
    @SerialName("BCCList")
    val bccList: List<RecipientResource>,
    @SerialName("Time")
    val time: Long,
    @SerialName("Size")
    val size: Long,
    @SerialName("ExpirationTime")
    val expirationTime: Long,
    @SerialName("IsReplied")
    val isReplied: Int,
    @SerialName("IsRepliedAll")
    val isRepliedAll: Int,
    @SerialName("IsForwarded")
    val isForwarded: Int,
    @SerialName("AddressID")
    val addressId: String,
    @SerialName("LabelIDs")
    val labelIds: List<String>,
    @SerialName("ExternalID")
    val externalId: String?,
    @SerialName("NumAttachments")
    val numAttachments: Int,
    @SerialName("Flags")
    val flags: Long,
    // --- Extended properties ---
    @SerialName("Body")
    val body: String,
    @SerialName("Header")
    val header: String,
    @SerialName("ParsedHeaders")
    val parsedHeaders: Map<String, JsonElement>,
    @SerialName("Attachments")
    val attachments: List<AttachmentResource>,
    @SerialName("MIMEType")
    val mimeType: String,
    @SerialName("SpamScore")
    val spamScore: String,
    @SerialName("ReplyTo")
    val replyTo: RecipientResource,
    @SerialName("ReplyTos")
    val replyTos: List<RecipientResource>,
    @SerialName("UnsubscribeMethods")
    val unsubscribeMethods: UnsubscribeMethodResource? = null,
    @SerialName("AttachmentInfo")
    val attachmentsInfo: AttachmentsInfoResource? = null
) {
    fun toMessageWithBody(userId: UserId) = MessageWithBody(
        message = Message(
            userId = userId,
            messageId = MessageId(id),
            conversationId = ConversationId(conversationId),
            time = time,
            size = size,
            order = order,
            labelIds = labelIds.map { LabelId(it) },
            subject = subject,
            unread = unread.toBooleanOrFalse(),
            sender = sender.toRecipient(),
            toList = toList.map { it.toRecipient() },
            ccList = ccList.map { it.toRecipient() },
            bccList = bccList.map { it.toRecipient() },
            expirationTime = expirationTime,
            isReplied = isReplied.toBooleanOrFalse(),
            isRepliedAll = isRepliedAll.toBooleanOrFalse(),
            isForwarded = isForwarded.toBooleanOrFalse(),
            addressId = AddressId(addressId),
            externalId = externalId,
            numAttachments = numAttachments,
            flags = flags,
            attachmentCount = attachmentsInfo.toAttachmentsCount()
        ),
        messageBody = MessageBody(
            userId = userId,
            messageId = MessageId(id),
            body = body,
            header = header,
            attachments = attachments.map { it.toMessageAttachment() },
            mimeType = MimeType.from(mimeType),
            spamScore = spamScore,
            replyTo = replyTo.toRecipient(),
            replyTos = replyTos.map { it.toRecipient() },
            unsubscribeMethods = unsubscribeMethods?.toUnsubscribeMethods()
        )
    )
}

@Serializable
data class AttachmentResource(
    @SerialName("ID")
    val id: String,
    @SerialName("Name")
    val name: String,
    @SerialName("Size")
    val size: Long,
    @SerialName("MIMEType")
    val mimeType: String,
    @SerialName("Disposition")
    val disposition: String? = null,
    @SerialName("KeyPackets")
    val keyPackets: String? = null,
    @SerialName("Signature")
    val signature: String? = null,
    @SerialName("EncSignature")
    val encSignature: String? = null,
    @SerialName("Headers")
    val headers: Map<String, HeaderValue>
) {
    fun toMessageAttachment() = MessageAttachment(
        attachmentId = AttachmentId(id),
        name = name,
        size = size,
        mimeType = mimeType,
        disposition = disposition,
        keyPackets = keyPackets,
        signature = signature,
        encSignature = encSignature,
        headers = headers
    )
}

@Serializable
data class UnsubscribeMethodResource(
    @SerialName("HttpClient")
    val httpClient: String? = null,
    @SerialName("OneClick")
    val oneClick: String? = null,
    @SerialName("Mailto")
    val mailTo: MailToResource? = null
) {
    fun toUnsubscribeMethods() = UnsubscribeMethods(
        httpClient = httpClient,
        oneClick = oneClick,
        mailTo = mailTo?.toMailTo()
    )
}

@Serializable
data class MailToResource(
    @SerialName("ToList")
    val toList: List<String>,
    @SerialName("Subject")
    val subject: String? = null,
    @SerialName("Body")
    val body: String? = null
) {
    fun toMailTo() = MailTo(
        toList = toList,
        subject = subject,
        body = body
    )
}
