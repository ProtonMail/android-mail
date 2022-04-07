/*
 * Copyright (c) 2021 Proton Technologies AG
 * This file is part of Proton Technologies AG and ProtonMail.
 *
 * ProtonMail is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ProtonMail is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with ProtonMail.  If not, see <https://www.gnu.org/licenses/>.
 */

package ch.protonmail.android.mailconversation.data.local

import ch.protonmail.android.mailconversation.data.local.entity.ConversationEntity
import ch.protonmail.android.mailconversation.data.local.relation.ConversationWithLabels
import ch.protonmail.android.mailconversation.domain.entity.Conversation
import me.proton.core.label.domain.entity.LabelId

fun ConversationWithLabels.toConversation(contextLabelId: LabelId) = Conversation(
    userId = conversation.userId,
    conversationId = conversation.conversationId,
    contextLabelId = contextLabelId,
    order = conversation.order,
    subject = conversation.subject,
    senders = conversation.senders,
    recipients = conversation.recipients,
    expirationTime = conversation.expirationTime,
    labels = labels,
    numMessages = conversation.numMessages,
    numUnread = conversation.numUnread,
    numAttachments = conversation.numAttachments,
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
)
