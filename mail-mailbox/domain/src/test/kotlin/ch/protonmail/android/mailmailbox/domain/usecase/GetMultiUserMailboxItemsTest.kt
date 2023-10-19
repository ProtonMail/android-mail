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

package ch.protonmail.android.mailmailbox.domain.usecase

import arrow.core.getOrHandle
import arrow.core.right
import ch.protonmail.android.mailconversation.domain.repository.ConversationRepository
import ch.protonmail.android.maillabel.domain.usecase.GetLabels
import ch.protonmail.android.mailmailbox.domain.mapper.ConversationMailboxItemMapper
import ch.protonmail.android.mailmailbox.domain.mapper.MessageMailboxItemMapper
import ch.protonmail.android.mailmailbox.domain.model.MailboxItemType.Conversation
import ch.protonmail.android.mailmailbox.domain.model.MailboxItemType.Message
import ch.protonmail.android.mailmailbox.domain.model.MailboxPageKey
import ch.protonmail.android.mailmessage.domain.model.Sender
import ch.protonmail.android.mailmessage.domain.repository.MessageRepository
import ch.protonmail.android.mailpagination.domain.model.OrderDirection
import ch.protonmail.android.mailpagination.domain.model.PageKey
import ch.protonmail.android.testdata.conversation.ConversationWithContextTestData
import ch.protonmail.android.testdata.label.LabelTestData.buildLabel
import ch.protonmail.android.testdata.mailbox.MailboxTestData.buildMailboxItem
import ch.protonmail.android.testdata.message.MessageTestData.buildMessage
import ch.protonmail.android.testdata.user.UserIdTestData.userId
import ch.protonmail.android.testdata.user.UserIdTestData.userId1
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import me.proton.core.label.domain.entity.LabelId
import me.proton.core.label.domain.entity.LabelType
import me.proton.core.label.domain.entity.LabelType.MessageLabel
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

class GetMultiUserMailboxItemsTest {

    private val messageRepository = mockk<MessageRepository> {
        coEvery { this@mockk.getLocalMessages(userId, any<PageKey>()) } returns listOf(
            // userId
            buildMessage(userId, "1", time = 1000, labelIds = emptyList()),
            buildMessage(userId, "2", time = 2000, labelIds = listOf("4")),
            buildMessage(userId, "3", time = 3000, labelIds = listOf("0", "1"))
        )
        coEvery { this@mockk.getLocalMessages(userId1, any<PageKey>()) } returns listOf(
            // userId
            buildMessage(userId1, "1", time = 1000, labelIds = emptyList()),
            buildMessage(userId1, "2", time = 2000, labelIds = listOf("4")),
            buildMessage(userId1, "3", time = 3000, labelIds = listOf("0", "1"))
        )
    }
    private val conversationRepository = mockk<ConversationRepository> {
        coEvery { getLocalConversations(userId, any()) } returns listOf(
            // userId
            ConversationWithContextTestData.conversation1Labeled,
            ConversationWithContextTestData.conversation2Labeled,
            ConversationWithContextTestData.conversation3Labeled
        )
        coEvery { getLocalConversations(userId1, any()) } returns listOf(
            // userId
            ConversationWithContextTestData.User2.conversation1Labeled,
            ConversationWithContextTestData.User2.conversation2Labeled,
            ConversationWithContextTestData.User2.conversation3Labeled
        )
    }
    private val getLabels = mockk<GetLabels> {
        coEvery { this@mockk(userId, any()) } returns listOf(
            buildLabel(userId = userId, type = MessageLabel, id = "0"),
            buildLabel(userId = userId, type = MessageLabel, id = "1"),
            buildLabel(userId = userId, type = MessageLabel, id = "2"),
            buildLabel(userId = userId, type = MessageLabel, id = "3"),
            buildLabel(userId = userId, type = MessageLabel, id = "4")
        ).right()
        coEvery { this@mockk(userId, any()) } returns listOf(
            buildLabel(userId = userId, type = MessageLabel, id = "0"),
            buildLabel(userId = userId, type = MessageLabel, id = "1"),
            buildLabel(userId = userId, type = MessageLabel, id = "2"),
            buildLabel(userId = userId, type = MessageLabel, id = "3"),
            buildLabel(userId = userId, type = MessageLabel, id = "4")
        ).right()
        coEvery { this@mockk(userId1, any()) } returns listOf(
            buildLabel(userId = userId1, type = MessageLabel, id = "0"),
            buildLabel(userId = userId1, type = MessageLabel, id = "1"),
            buildLabel(userId = userId1, type = MessageLabel, id = "2"),
            buildLabel(userId = userId1, type = MessageLabel, id = "3"),
            buildLabel(userId = userId1, type = MessageLabel, id = "4")
        ).right()
    }

    private val messageMailboxItemMapper = MessageMailboxItemMapper()
    private val conversationMailboxItemMapper = ConversationMailboxItemMapper()

    private lateinit var usecase: GetMultiUserMailboxItems

    @Before
    fun setUp() {
        usecase = GetMultiUserMailboxItems(
            GetMailboxItems(
                getLabels,
                messageRepository,
                conversationRepository,
                messageMailboxItemMapper,
                conversationMailboxItemMapper
            )
        )
    }

    @Test
    fun `invoke for Message, getLabels and loadMessages`() = runTest {
        // Given
        val pageKey = PageKey(orderDirection = OrderDirection.Ascending, size = 6)
        val mailboxPageKey = MailboxPageKey(listOf(userId, userId1), pageKey)

        // When
        val mailboxItems = usecase.invoke(Message, mailboxPageKey)
            .getOrHandle(::error)

        // Then
        coVerify { getLabels(userId, MessageLabel) }
        coVerify { getLabels(userId1, MessageLabel) }
        coVerify { getLabels(userId, LabelType.MessageFolder) }
        coVerify { getLabels(userId1, LabelType.MessageFolder) }
        coVerify { messageRepository.getLocalMessages(userId, pageKey) }
        coVerify { messageRepository.getLocalMessages(userId1, pageKey) }
        val senders = listOf(Sender("address", "name"))
        val mailboxItemsOrderedByTimeAscending = listOf(
            buildMailboxItem(
                userId = userId,
                id = "1",
                time = 1000,
                labelIds = emptyList(),
                type = Message,
                senders = senders
            ),
            buildMailboxItem(
                userId = userId1,
                id = "1",
                time = 1000,
                labelIds = emptyList(),
                type = Message,
                senders = senders
            ),
            buildMailboxItem(
                userId = userId,
                id = "2",
                time = 2000,
                labelIds = listOf(LabelId("4")),
                type = Message,
                senders = senders
            ),
            buildMailboxItem(
                userId = userId1,
                id = "2",
                time = 2000,
                labelIds = listOf(LabelId("4")),
                type = Message,
                senders = senders
            ),
            buildMailboxItem(
                userId = userId,
                id = "3",
                time = 3000,
                labelIds = listOf(LabelId("0"), LabelId("1")),
                type = Message,
                senders = senders
            ),
            buildMailboxItem(
                userId = userId1,
                id = "3",
                time = 3000,
                labelIds = listOf(LabelId("0"), LabelId("1")),
                type = Message,
                senders = senders
            )
        )
        assertEquals(expected = mailboxItemsOrderedByTimeAscending, actual = mailboxItems)
    }

    @Test
    fun `invoke for Conversation, getLabels and loadConversations`() = runTest {
        // Given
        val pageKey = PageKey(orderDirection = OrderDirection.Ascending, size = 6)
        val mailboxPageKey = MailboxPageKey(listOf(userId, userId1), pageKey)

        // When
        val mailboxItems = usecase.invoke(Conversation, mailboxPageKey)
            .getOrHandle(::error)

        // Then
        coVerify { getLabels(userId, MessageLabel) }
        coVerify { getLabels(userId1, MessageLabel) }
        coVerify { getLabels(userId, LabelType.MessageFolder) }
        coVerify { getLabels(userId1, LabelType.MessageFolder) }
        coVerify { conversationRepository.getLocalConversations(userId, pageKey) }
        coVerify { conversationRepository.getLocalConversations(userId1, pageKey) }
        val mailboxItemsOrderedByTimeAscending = listOf(
            buildMailboxItem(
                userId = userId,
                id = "1",
                time = 1000,
                labelIds = listOf(LabelId("0")),
                type = Conversation,
                hasAttachments = true
            ),
            buildMailboxItem(
                userId = userId1,
                id = "1",
                time = 1000,
                labelIds = listOf(LabelId("0")),
                type = Conversation,
                hasAttachments = true
            ),
            buildMailboxItem(
                userId = userId,
                id = "2",
                time = 2000,
                labelIds = listOf(LabelId("4")),
                type = Conversation,
                hasAttachments = true
            ),
            buildMailboxItem(
                userId = userId1,
                id = "2",
                time = 2000,
                labelIds = listOf(LabelId("4")),
                type = Conversation,
                hasAttachments = true
            ),
            buildMailboxItem(
                userId = userId,
                id = "3",
                time = 3000,
                labelIds = listOf(LabelId("0"), LabelId("1")),
                type = Conversation,
                hasAttachments = true
            ),
            buildMailboxItem(
                userId = userId1,
                id = "3",
                time = 3000,
                labelIds = listOf(LabelId("0"), LabelId("1")),
                type = Conversation,
                hasAttachments = true
            )
        )
        assertEquals(expected = mailboxItemsOrderedByTimeAscending, actual = mailboxItems)
    }
}
