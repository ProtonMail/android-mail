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

package ch.protonmail.android.testdata.mailbox

import ch.protonmail.android.mailcommon.domain.model.ConversationId
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.maillabel.domain.model.MailLabelId
import ch.protonmail.android.mailmailbox.domain.model.MailboxItem
import ch.protonmail.android.mailmailbox.domain.model.MailboxItemType
import ch.protonmail.android.mailcommon.presentation.model.AvatarUiModel
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxItemUiModel
import ch.protonmail.android.mailmessage.domain.entity.Recipient
import ch.protonmail.android.testdata.label.LabelTestData.buildLabel
import ch.protonmail.android.testdata.user.UserIdTestData
import ch.protonmail.android.testdata.user.UserIdTestData.userId
import kotlinx.collections.immutable.persistentListOf
import me.proton.core.domain.entity.UserId
import me.proton.core.label.domain.entity.Label
import me.proton.core.label.domain.entity.LabelId

object MailboxTestData {

    val unreadMailboxItem = buildMessageMailboxItem("1", isRead = false)
    val readMailboxItem = buildMessageMailboxItem("2", isRead = true)

    val repliedMailboxItem = buildMessageMailboxItem("3", isReplied = true)
    val repliedAllMailboxItem = buildMessageMailboxItem("4", isReplied = true, isRepliedAll = true)
    val allActionsMailboxItem = buildMessageMailboxItem(
        "5", isReplied = true, isRepliedAll = true, isForwarded = true
    )

    val mailboxConversationItem = buildConversationMailboxItem("6")

    fun buildMailboxItem(
        userId: UserId = UserIdTestData.userId,
        id: String = "itemId",
        time: Long = 0,
        labelIds: List<LabelId> = listOf(MailLabelId.System.Inbox.labelId),
        labels: List<Label> = labelIds.map { buildLabel(userId = userId, id = it.id) },
        type: MailboxItemType = MailboxItemType.Message,
        senders: List<Recipient> = emptyList(),
        recipients: List<Recipient> = emptyList(),
        numMessages: Int = 1,
        hasAttachments: Boolean = false,
        expirationTime: Long = 0,
        calendarAttachmentCount: Int = 0
    ) = MailboxItem(
        type = type,
        id = id,
        userId = userId,
        time = time,
        size = 0,
        order = 1000,
        read = true,
        labelIds = labelIds,
        conversationId = ConversationId(id),
        labels = labels,
        subject = "subject",
        senders = senders,
        recipients = recipients,
        isReplied = false,
        isRepliedAll = false,
        isForwarded = false,
        numMessages = numMessages,
        hasNonCalendarAttachments = hasAttachments,
        expirationTime = expirationTime,
        calendarAttachmentCount = calendarAttachmentCount
    )

    private fun buildMessageMailboxItem(
        id: String,
        isRead: Boolean = true,
        isReplied: Boolean = false,
        isRepliedAll: Boolean = false,
        isForwarded: Boolean = false
    ) = MailboxItem(
        type = MailboxItemType.Message,
        id = id,
        userId = UserId("0"),
        time = 0,
        size = 0,
        order = 0,
        read = isRead,
        labelIds = emptyList(),
        conversationId = ConversationId("2"),
        labels = emptyList(),
        subject = "First message",
        senders = listOf(Recipient("address", "name")),
        recipients = emptyList(),
        isReplied = isReplied,
        isRepliedAll = isRepliedAll,
        isForwarded = isForwarded,
        numMessages = 1,
        hasNonCalendarAttachments = false,
        expirationTime = 0,
        calendarAttachmentCount = 0
    )

    private fun buildConversationMailboxItem(id: String) = MailboxItem(
        type = MailboxItemType.Conversation,
        id = id,
        userId = UserId("0"),
        time = 0,
        size = 0,
        order = 0,
        read = false,
        labelIds = emptyList(),
        conversationId = ConversationId("2"),
        labels = emptyList(),
        subject = "First message",
        senders = listOf(Recipient("address", "name")),
        recipients = emptyList(),
        isReplied = false,
        isRepliedAll = false,
        isForwarded = false,
        numMessages = 3,
        hasNonCalendarAttachments = false,
        expirationTime = 0,
        calendarAttachmentCount = 0
    )

}

object MailboxItemUiModelTestData {

    val unreadMailboxItemUiModel = buildMailboxUiModelItem(
        id = "1",
        type = MailboxItemType.Message,
        subject = "First message",
        isRead = false
    )

    val readMailboxItemUiModel = buildMailboxUiModelItem(
        id = "2",
        type = MailboxItemType.Message,
        subject = "Second message",
        isRead = true
    )

    fun buildMailboxUiModelItem(
        id: String,
        type: MailboxItemType,
        subject: String = id,
        isRead: Boolean = true
    ) = MailboxItemUiModel(
        avatar = AvatarUiModel.ParticipantInitial("T"),
        type = type,
        id = id,
        userId = userId,
        conversationId = ConversationId(id),
        time = TextUiModel.Text("10:42"),
        isRead = isRead,
        labels = persistentListOf(),
        subject = subject,
        participants = persistentListOf(),
        shouldShowRepliedIcon = false,
        shouldShowRepliedAllIcon = false,
        shouldShowForwardedIcon = false,
        numMessages = null,
        showStar = false,
        locationIconResIds = persistentListOf(),
        shouldShowAttachmentIcon = false,
        shouldShowExpirationLabel = false,
        shouldShowCalendarIcon = false
    )
}
