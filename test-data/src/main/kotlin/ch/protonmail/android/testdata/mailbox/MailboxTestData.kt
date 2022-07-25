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

    val mailboxItem1 = MailboxItem(
        type = MailboxItemType.Message,
        id = "1",
        conversationId = ConversationId("2"),
        userId = UserId("0"),
        senders = listOf(Recipient("address", "name")),
        recipients = emptyList(),
        subject = "First message",
        time = 0,
        size = 0,
        order = 0,
        read = false
    )

    val mailboxItem2 = MailboxItem(
        type = MailboxItemType.Message,
        id = "2",
        conversationId = ConversationId("2"),
        userId = UserId("0"),
        senders = listOf(Recipient("address", "name")),
        recipients = emptyList(),
        subject = "Second message",
        time = 0,
        size = 0,
        order = 0,
        read = true
    )


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
        labels = labelIds.map { buildLabel(userId = userId, id = it) }
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
        read = false
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
        read = true
    )
}
