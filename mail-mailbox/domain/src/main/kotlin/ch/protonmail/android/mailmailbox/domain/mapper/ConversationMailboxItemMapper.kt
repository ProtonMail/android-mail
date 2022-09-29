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

package ch.protonmail.android.mailmailbox.domain.mapper

import ch.protonmail.android.mailconversation.domain.entity.ConversationWithContext
import ch.protonmail.android.mailmailbox.domain.model.MailboxItem
import ch.protonmail.android.mailmailbox.domain.model.MailboxItemType
import me.proton.core.domain.arch.Mapper
import me.proton.core.label.domain.entity.Label
import me.proton.core.label.domain.entity.LabelId
import javax.inject.Inject

class ConversationMailboxItemMapper @Inject constructor() : Mapper<ConversationWithContext, MailboxItem> {

    fun toMailboxItem(conversationWithContext: ConversationWithContext, labels: Map<LabelId, Label>) =
        with(conversationWithContext.conversation) {
            MailboxItem(
                type = MailboxItemType.Conversation,
                id = conversationId.id,
                userId = userId,
                time = conversationWithContext.time,
                size = conversationWithContext.size,
                order = order,
                read = conversationWithContext.read,
                labelIds = conversationWithContext.labelIds,
                conversationId = conversationId,
                labels = conversationWithContext.labelIds.mapNotNull { labels[it] }.sortedBy { it.order },
                subject = subject,
                senders = senders,
                recipients = recipients,
                isReplied = false,
                isRepliedAll = false,
                isForwarded = false,
                numMessages = numMessages,
                hasNonCalendarAttachments = numAttachments > attachmentCount.calendar,
                expirationTime = expirationTime,
                calendarAttachmentCount = attachmentCount.calendar
            )
        }

}
