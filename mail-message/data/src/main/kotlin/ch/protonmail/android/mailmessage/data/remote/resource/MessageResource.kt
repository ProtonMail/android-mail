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
import ch.protonmail.android.mailmessage.domain.model.Message
import ch.protonmail.android.mailmessage.domain.model.MessageId
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import me.proton.core.domain.entity.UserId
import me.proton.core.label.domain.entity.LabelId
import me.proton.core.user.domain.entity.AddressId
import me.proton.core.util.kotlin.toBooleanOrFalse

@Serializable
data class MessageResource(
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
    @SerialName("AttachmentInfo")
    val attachmentsInfo: AttachmentsInfoResource? = null
) {
    fun toMessage(userId: UserId) = Message(
        userId = userId,
        messageId = MessageId(id),
        conversationId = ConversationId(conversationId),
        order = order,
        subject = subject,
        unread = unread.toBooleanOrFalse(),
        sender = sender.toRecipient(),
        toList = toList.map { it.toRecipient() },
        ccList = ccList.map { it.toRecipient() },
        bccList = bccList.map { it.toRecipient() },
        time = time,
        size = size,
        expirationTime = expirationTime,
        isReplied = isReplied.toBooleanOrFalse(),
        isRepliedAll = isRepliedAll.toBooleanOrFalse(),
        isForwarded = isForwarded.toBooleanOrFalse(),
        addressId = AddressId(addressId),
        labelIds = labelIds.map { LabelId(it) },
        externalId = externalId,
        numAttachments = numAttachments,
        flags = flags,
        attachmentCount = attachmentsInfo.toAttachmentsCount()
    )
}
