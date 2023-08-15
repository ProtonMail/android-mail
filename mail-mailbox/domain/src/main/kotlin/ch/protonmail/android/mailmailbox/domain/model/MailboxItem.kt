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

import ch.protonmail.android.mailcommon.domain.model.ConversationId
import ch.protonmail.android.mailmessage.domain.model.Recipient
import ch.protonmail.android.mailmessage.domain.model.Sender
import ch.protonmail.android.mailpagination.domain.model.PageItem
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
    val senders: List<Sender>,
    val recipients: List<Recipient>,
    val isReplied: Boolean,
    val isRepliedAll: Boolean,
    val isForwarded: Boolean,
    val numMessages: Int,
    val hasNonCalendarAttachments: Boolean,
    val expirationTime: Long,
    val calendarAttachmentCount: Int
) : PageItem {
    override val keywords: String by lazy { subject + senders + recipients }
}

fun ViewMode.toMailboxItemType() = when (this) {
    ViewMode.ConversationGrouping -> MailboxItemType.Conversation
    ViewMode.NoConversationGrouping -> MailboxItemType.Message
}
