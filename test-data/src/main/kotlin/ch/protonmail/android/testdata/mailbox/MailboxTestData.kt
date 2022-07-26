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

import ch.protonmail.android.mailconversation.domain.entity.ConversationId
import ch.protonmail.android.mailconversation.domain.entity.Recipient
import ch.protonmail.android.mailmailbox.domain.model.MailboxItem
import ch.protonmail.android.mailmailbox.domain.model.MailboxItemType
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxItemUiModel
import ch.protonmail.android.testdata.label.LabelTestData.buildLabel
import me.proton.core.domain.entity.UserId

object MailboxTestData {

    val mailboxItem1 = buildMessageMailboxItem("1", isRead = false)
    val mailboxItem2 = buildMessageMailboxItem("2", isRead = true)

    val repliedMailboxItem = buildMessageMailboxItem("3", isReplied = true)
    val repliedAllMailboxItem = buildMessageMailboxItem("4", isRepliedAll = true)
    val allActionsMailboxItem = buildMessageMailboxItem(
        "5", isReplied = true, isRepliedAll = true, isForwarded = true
    )

    val mailboxConversationItem = buildConversationMailboxItem("6")

    fun buildMailboxItem(
        userId: UserId,
        id: String,
        time: Long,
        labelIds: List<String> = listOf("0"),
        type: MailboxItemType = MailboxItemType.Message,
    ) = MailboxItem(
        type = type,
        id = id,
        conversationId = ConversationId(id),
        userId = userId,
        time = time,
        size = 1000,
        order = 1000,
        read = true,
        subject = "subject",
        senders = listOf(Recipient("address", "name")),
        recipients = emptyList(),
        labels = labelIds.map { buildLabel(userId = userId, id = it) },
        isReplied = false,
        isRepliedAll = false,
        isForwarded = false
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
        conversationId = ConversationId("2"),
        userId = UserId("0"),
        senders = listOf(Recipient("address", "name")),
        recipients = emptyList(),
        subject = "First message",
        time = 0,
        size = 0,
        order = 0,
        read = isRead,
        isReplied = isReplied,
        isRepliedAll = isRepliedAll,
        isForwarded = isForwarded
    )

    private fun buildConversationMailboxItem(id: String) = MailboxItem(
        type = MailboxItemType.Message,
        id = id,
        conversationId = ConversationId("2"),
        userId = UserId("0"),
        senders = listOf(Recipient("address", "name")),
        recipients = emptyList(),
        subject = "First message",
        time = 0,
        size = 0,
        order = 0,
        read = false,
        isReplied = false,
        isRepliedAll = false,
        isForwarded = false
    )

}

object MailboxItemUiModelTestData {

    val mailboxItemUiModel1 = MailboxItemUiModel(
        type = MailboxItemType.Message,
        id = "1",
        conversationId = ConversationId("2"),
        userId = UserId("0"),
        senders = listOf(Recipient("address", "name")),
        recipients = emptyList(),
        subject = "First message",
        time = 0,
        read = false,
        showRepliedIcon = false,
    )

    val mailboxItemUiModel2 = MailboxItemUiModel(
        type = MailboxItemType.Message,
        id = "2",
        conversationId = ConversationId("2"),
        userId = UserId("0"),
        senders = listOf(Recipient("address", "name")),
        recipients = emptyList(),
        subject = "Second message",
        time = 0,
        read = true,
        showRepliedIcon = false,
    )
}
