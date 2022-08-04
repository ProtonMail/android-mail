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

package ch.protonmail.android.mailmailbox.domain.model

import ch.protonmail.android.mailconversation.domain.entity.Conversation
import ch.protonmail.android.mailconversation.domain.entity.ConversationId
import ch.protonmail.android.mailconversation.domain.entity.Recipient
import ch.protonmail.android.mailmessage.domain.entity.Message
import ch.protonmail.android.mailpagination.domain.entity.PageItem
import me.proton.core.domain.entity.UserId
import me.proton.core.label.domain.entity.Label
import me.proton.core.label.domain.entity.LabelId
import me.proton.core.mailsettings.domain.entity.ViewMode

enum class MailboxItemType {
    Message,
    Conversation
}

/**
 * @property isReplied always `false` if [type] is [MailboxItemType.Conversation]
 * @property isRepliedAll always `false` if [type] is [MailboxItemType.Conversation]
 * @property isForwarded always `false` if [type] is [MailboxItemType.Conversation]
 */
data class MailboxItem(
    val type: MailboxItemType,
    override val id: String,
    override val userId: UserId,
    override val time: Long,
    override val size: Long,
    override val order: Long,
    override val read: Boolean,
    override val labelIds: List<LabelId>,
    val conversationId: ConversationId,
    val labels: List<Label>,
    val subject: String,
    val senders: List<Recipient>,
    val recipients: List<Recipient>,
    val isReplied: Boolean,
    val isRepliedAll: Boolean,
    val isForwarded: Boolean,
    val numMessages: Int,
    val hasAttachments: Boolean
) : PageItem {
    override val keywords: String by lazy { subject + senders + recipients }
}

fun Message.toMailboxItem(labels: Map<LabelId, Label>) = MailboxItem(
    type = MailboxItemType.Message,
    id = messageId.id,
    userId = userId,
    time = time,
    size = size,
    order = order,
    read = read,
    labelIds = labelIds,
    conversationId = conversationId,
    labels = labelIds.mapNotNull { labels[it] },
    subject = subject,
    senders = listOf(sender),
    recipients = toList + ccList + bccList,
    isReplied = isReplied,
    isRepliedAll = isRepliedAll,
    isForwarded = isForwarded,
    numMessages = 1,
    hasAttachments = numAttachments > 0
)

fun Conversation.toMailboxItem(labels: Map<LabelId, Label>) = MailboxItem(
    type = MailboxItemType.Conversation,
    id = conversationId.id,
    userId = userId,
    time = time,
    size = size,
    order = order,
    read = read,
    labelIds = labelIds,
    conversationId = conversationId,
    labels = labelIds.mapNotNull { labels[it] },
    subject = subject,
    senders = senders,
    recipients = recipients,
    isReplied = false,
    isRepliedAll = false,
    isForwarded = false,
    numMessages = numMessages,
    hasAttachments = numAttachments > 0
)

fun ViewMode.toMailboxItemType() = when (this) {
    ViewMode.ConversationGrouping -> MailboxItemType.Conversation
    ViewMode.NoConversationGrouping -> MailboxItemType.Message
}
