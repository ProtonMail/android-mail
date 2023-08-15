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

import ch.protonmail.android.mailmailbox.domain.model.MailboxItem
import ch.protonmail.android.mailmailbox.domain.model.MailboxItemType
import ch.protonmail.android.mailmessage.domain.model.Message
import me.proton.core.domain.arch.Mapper
import me.proton.core.label.domain.entity.Label
import me.proton.core.label.domain.entity.LabelId
import javax.inject.Inject

class MessageMailboxItemMapper @Inject constructor() : Mapper<Message, MailboxItem> {

    fun toMailboxItem(message: Message, labels: Map<LabelId, Label>) = MailboxItem(
        type = MailboxItemType.Message,
        id = message.messageId.id,
        userId = message.userId,
        time = message.time,
        size = message.size,
        order = message.order,
        read = message.read,
        labelIds = message.labelIds,
        conversationId = message.conversationId,
        labels = message.labelIds.mapNotNull { labels[it] }.sortedBy { it.order },
        subject = message.subject,
        senders = listOf(message.sender),
        recipients = message.toList + message.ccList + message.bccList,
        isReplied = message.isReplied,
        isRepliedAll = message.isRepliedAll,
        isForwarded = message.isForwarded,
        numMessages = 1,
        hasNonCalendarAttachments = message.numAttachments > message.attachmentCount.calendar,
        expirationTime = message.expirationTime,
        calendarAttachmentCount = message.attachmentCount.calendar
    )

}
