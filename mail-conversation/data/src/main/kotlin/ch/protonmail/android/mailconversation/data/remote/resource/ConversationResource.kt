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

package ch.protonmail.android.mailconversation.data.remote.resource

import ch.protonmail.android.mailcommon.domain.model.ConversationId
import ch.protonmail.android.mailconversation.domain.entity.Conversation
import ch.protonmail.android.mailconversation.domain.entity.ConversationWithContext
import ch.protonmail.android.mailmessage.data.remote.resource.AttachmentsInfoResource
import ch.protonmail.android.mailmessage.data.remote.resource.RecipientResource
import ch.protonmail.android.mailmessage.data.remote.resource.toAttachmentsCount
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import me.proton.core.domain.entity.UserId
import me.proton.core.label.domain.entity.LabelId

@Serializable
data class ConversationResource(
    @SerialName("ID")
    val id: String,
    @SerialName("Order")
    val order: Long,
    @SerialName("Subject")
    val subject: String,
    @SerialName("NumMessages")
    val numMessages: Int,
    @SerialName("NumUnread")
    val numUnread: Int,
    @SerialName("NumAttachments")
    val numAttachments: Int,
    @SerialName("Senders")
    val senders: List<RecipientResource>,
    @SerialName("Recipients")
    val recipients: List<RecipientResource>,
    @SerialName("ExpirationTime")
    val expirationTime: Long,
    @SerialName("Labels")
    val labels: List<ConversationLabelResource>,
    @SerialName("AttachmentInfo")
    val attachmentsInfo: AttachmentsInfoResource? = null
) {

    fun toConversation(userId: UserId) = Conversation(
        userId = userId,
        conversationId = ConversationId(id),
        order = order,
        labels = labels.map { it.toConversationLabel(ConversationId(id)) },
        subject = subject,
        senders = senders.map { it.toRecipient() },
        recipients = recipients.map { it.toRecipient() },
        expirationTime = expirationTime,
        numMessages = numMessages,
        numUnread = numUnread,
        numAttachments = numAttachments,
        attachmentCount = attachmentsInfo.toAttachmentsCount()
    )

    fun toConversationWithContext(userId: UserId, contextLabelId: LabelId) = ConversationWithContext(
        toConversation(userId),
        contextLabelId = contextLabelId
    )
}
