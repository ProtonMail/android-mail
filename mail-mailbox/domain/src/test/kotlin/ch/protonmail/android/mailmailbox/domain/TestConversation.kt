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

package ch.protonmail.android.mailmailbox.domain

import ch.protonmail.android.mailconversation.domain.entity.Conversation
import ch.protonmail.android.mailconversation.domain.entity.ConversationId
import ch.protonmail.android.mailconversation.domain.entity.Recipient
import me.proton.core.domain.entity.UserId
import me.proton.core.label.domain.entity.LabelId

fun getConversation(
    userId: UserId,
    id: String,
    order: Long = 1000,
    time: Long = 1000,
    labelIds: List<String> = listOf("0"),
) = Conversation(
    userId = userId,
    conversationId = ConversationId(id),
    contextLabelId = labelIds.firstOrNull()?.let { LabelId(it) } ?: LabelId("0"),
    labels = labelIds.map { getConversationLabel(id, it, time) },
    order = order,
    subject = "subject",
    expirationTime = 1000,
    recipients = emptyList(),
    senders = listOf(Recipient("address", "name")),
    numAttachments = 0,
    numMessages = 0,
    numUnread = 0,
)
