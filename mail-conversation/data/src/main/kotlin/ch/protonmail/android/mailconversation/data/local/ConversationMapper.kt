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

package ch.protonmail.android.mailconversation.data.local

import ch.protonmail.android.mailconversation.data.local.entity.ConversationEntity
import ch.protonmail.android.mailconversation.data.local.relation.ConversationWithLabels
import ch.protonmail.android.mailconversation.domain.entity.Conversation
import ch.protonmail.android.mailconversation.domain.entity.ConversationWithContext
import ch.protonmail.android.mailmessage.data.mapper.toDomainModel
import ch.protonmail.android.mailmessage.data.mapper.toEntity
import me.proton.core.label.domain.entity.LabelId

fun ConversationWithLabels.toConversationWithContext(contextLabelId: LabelId) = ConversationWithContext(
    conversation = this.toConversation(),
    contextLabelId = contextLabelId
)

fun ConversationWithLabels.toConversation() = Conversation(
    userId = conversation.userId,
    conversationId = conversation.conversationId,
    order = conversation.order,
    labels = labels,
    subject = conversation.subject,
    senders = conversation.senders,
    recipients = conversation.recipients,
    expirationTime = conversation.expirationTime,
    numMessages = conversation.numMessages,
    numUnread = conversation.numUnread,
    numAttachments = conversation.numAttachments,
    attachmentCount = conversation.attachmentCount.toDomainModel()
)

fun Conversation.toEntity() = ConversationEntity(
    userId = userId,
    conversationId = conversationId,
    order = order,
    subject = subject,
    senders = senders,
    recipients = recipients,
    expirationTime = expirationTime,
    numMessages = numMessages,
    numUnread = numUnread,
    numAttachments = numAttachments,
    attachmentCount = attachmentCount.toEntity()
)
