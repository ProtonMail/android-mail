/*
 * Copyright (c) 2021 Proton Technologies AG
 * This file is part of Proton Technologies AG and ProtonCore.
 *
 * ProtonCore is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ProtonCore is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with ProtonCore.  If not, see <https://www.gnu.org/licenses/>.
 */

package ch.protonmail.android.mailconversation.data.remote.resource

import ch.protonmail.android.mailconversation.domain.entity.Conversation
import ch.protonmail.android.mailconversation.domain.entity.ConversationId
import ch.protonmail.android.mailconversation.domain.entity.ConversationLabel
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
) {
    fun toConversation(userId: UserId, contextLabelId: LabelId = LabelId("0")) = Conversation(
        userId = userId,
        conversationId = ConversationId(id),
        contextLabelId = contextLabelId,
        order = order,
        subject = subject,
        senders = senders.map { it.toRecipient() },
        recipients = recipients.map { it.toRecipient() },
        expirationTime = expirationTime,
        labels = labels.map { it.toConversationLabel(ConversationId(id)) },
        numMessages = numMessages,
        numUnread = numUnread,
        numAttachments = numAttachments,
    )
}

@Serializable
data class ConversationLabelResource(
    @SerialName("ID")
    val id: String,
    @SerialName("ContextNumUnread")
    val contextNumUnread: Int,
    @SerialName("ContextNumMessages")
    val contextNumMessages: Int,
    @SerialName("ContextTime")
    val contextTime: Long,
    @SerialName("ContextSize")
    val contextSize: Long,
    @SerialName("ContextNumAttachments")
    val contextNumAttachments: Int,
) {
    fun toConversationLabel(conversationId: ConversationId) = ConversationLabel(
        conversationId = conversationId,
        labelId = LabelId(id),
        contextTime = contextTime,
        contextSize = contextSize,
        contextNumMessages = contextNumMessages,
        contextNumUnread = contextNumUnread,
        contextNumAttachments = contextNumAttachments
    )
}
