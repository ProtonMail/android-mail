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

import ch.protonmail.android.mailattachments.domain.model.AttachmentMetadata
import ch.protonmail.android.mailcommon.domain.model.AvatarInformation
import ch.protonmail.android.mailcommon.domain.model.ConversationId
import ch.protonmail.android.maillabel.domain.model.ExclusiveLocation
import ch.protonmail.android.maillabel.domain.model.Label
import ch.protonmail.android.maillabel.domain.model.ViewMode
import ch.protonmail.android.mailmessage.domain.model.Recipient
import ch.protonmail.android.mailmessage.domain.model.Sender
import ch.protonmail.android.mailsnooze.domain.model.ConversationSnoozeStatus

enum class MailboxItemType {
    Message,
    Conversation
}

/**
 * @property isReplied always `false` if [type] is [MailboxItemType.Conversation]
 * @property isRepliedAll always `false` if [type] is [MailboxItemType.Conversation]
 * @property isForwarded always `false` if [type] is [MailboxItemType.Conversation]
 * @property searchMatchedTerms the text a search matched in the subject and addresses, fuzzy matches
 *  included. Empty outside search results.
 */
data class MailboxItem(
    val type: MailboxItemType,
    val id: String,
    val time: Long,
    val size: Long,
    val order: Long,
    val read: Boolean,
    val conversationId: ConversationId,
    val labels: List<Label>,
    val subject: String,
    val senders: List<Sender>,
    val recipients: List<Recipient>,
    val isReplied: Boolean,
    val isRepliedAll: Boolean,
    val isForwarded: Boolean,
    val isStarred: Boolean,
    val numMessages: Int,
    val hasNonCalendarAttachments: Boolean,
    val expirationTime: Long,
    val calendarAttachmentCount: Int,
    val avatarInformation: AvatarInformation,
    val exclusiveLocations: List<ExclusiveLocation>,
    val attachments: List<AttachmentMetadata>,
    val isDraft: Boolean,
    val isScheduled: Boolean,
    val snoozeStatus: ConversationSnoozeStatus,
    val searchMatchedTerms: List<String> = emptyList()
)

fun ViewMode.toMailboxItemType() = when (this) {
    ViewMode.ConversationGrouping -> MailboxItemType.Conversation
    ViewMode.NoConversationGrouping -> MailboxItemType.Message
}
