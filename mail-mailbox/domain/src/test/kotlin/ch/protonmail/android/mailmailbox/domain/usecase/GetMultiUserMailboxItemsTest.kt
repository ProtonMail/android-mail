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

package ch.protonmail.android.mailmailbox.domain.usecase

import ch.protonmail.android.mailconversation.domain.repository.ConversationRepository
import ch.protonmail.android.mailmailbox.domain.getConversation
import ch.protonmail.android.mailmailbox.domain.getLabel
import ch.protonmail.android.mailmailbox.domain.getMailboxItem
import ch.protonmail.android.mailmailbox.domain.getMessage
import ch.protonmail.android.mailmailbox.domain.model.MailboxItemType
import ch.protonmail.android.mailmailbox.domain.model.MailboxPageKey
import ch.protonmail.android.mailmessage.domain.repository.MessageRepository
import ch.protonmail.android.mailpagination.domain.entity.OrderDirection
import ch.protonmail.android.mailpagination.domain.entity.PageKey
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import me.proton.core.label.domain.entity.LabelType
import me.proton.core.label.domain.repository.LabelRepository
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

class GetMultiUserMailboxItemsTest {

    private val userId1 = UserId("1")
    private val userId2 = UserId("2")

    private val messageRepository = mockk<MessageRepository> {
        coEvery { this@mockk.getMessages(userId1, any()) } returns listOf(
            // userId1
            getMessage(userId1, "1", time = 1000, labelIds = emptyList()),
            getMessage(userId1, "2", time = 2000, labelIds = listOf("4")),
            getMessage(userId1, "3", time = 3000, labelIds = listOf("0", "1")),
        )
        coEvery { this@mockk.getMessages(userId2, any()) } returns listOf(
            // userId1
            getMessage(userId2, "1", time = 1000, labelIds = emptyList()),
            getMessage(userId2, "2", time = 2000, labelIds = listOf("4")),
            getMessage(userId2, "3", time = 3000, labelIds = listOf("0", "1")),
        )
    }
    private val conversationRepository = mockk<ConversationRepository> {
        coEvery { getConversations(userId1, any()) } returns listOf(
            // userId1
            getConversation(userId1, "1", time = 1000, labelIds = listOf("0")),
            getConversation(userId1, "2", time = 2000, labelIds = listOf("4")),
            getConversation(userId1, "3", time = 3000, labelIds = listOf("0", "1")),
        )
        coEvery { getConversations(userId2, any()) } returns listOf(
            // userId1
            getConversation(userId2, "1", time = 1000, labelIds = listOf("0")),
            getConversation(userId2, "2", time = 2000, labelIds = listOf("4")),
            getConversation(userId2, "3", time = 3000, labelIds = listOf("0", "1")),
        )
    }
    private val labelRepository = mockk<LabelRepository> {
        coEvery { this@mockk.getLabels(userId1, any()) } returns listOf(
            getLabel(userId1, LabelType.MessageLabel, "0"),
            getLabel(userId1, LabelType.MessageLabel, "1"),
            getLabel(userId1, LabelType.MessageLabel, "2"),
            getLabel(userId1, LabelType.MessageLabel, "3"),
            getLabel(userId1, LabelType.MessageLabel, "4"),
        )
        coEvery { this@mockk.getLabels(userId2, any()) } returns listOf(
            getLabel(userId2, LabelType.MessageLabel, "0"),
            getLabel(userId2, LabelType.MessageLabel, "1"),
            getLabel(userId2, LabelType.MessageLabel, "2"),
            getLabel(userId2, LabelType.MessageLabel, "3"),
            getLabel(userId2, LabelType.MessageLabel, "4"),
        )
    }

    private lateinit var usecase: GetMultiUserMailboxItems

    @Before
    fun setUp() {
        usecase = GetMultiUserMailboxItems(
            GetMailboxItems(labelRepository, messageRepository, conversationRepository)
        )
    }

    @Test
    fun `invoke for Message, getLabels and getMessages`() = runTest {
        // Given
        val pageKey = PageKey(orderDirection = OrderDirection.Ascending, size = 6)
        val mailboxPageKey = MailboxPageKey(listOf(userId1, userId2), pageKey)

        // When
        val mailboxItems = usecase.invoke(MailboxItemType.Message, mailboxPageKey)

        // Then
        coVerify { labelRepository.getLabels(userId1, LabelType.MessageLabel) }
        coVerify { labelRepository.getLabels(userId2, LabelType.MessageLabel) }
        coVerify { labelRepository.getLabels(userId1, LabelType.MessageFolder) }
        coVerify { labelRepository.getLabels(userId2, LabelType.MessageFolder) }
        coVerify { messageRepository.getMessages(userId1, pageKey) }
        coVerify { messageRepository.getMessages(userId2, pageKey) }
        val mailboxItemsOrderedByTimeAscending = listOf(
            getMailboxItem(userId1, "1", time = 1000, labelIds = emptyList(), type = MailboxItemType.Message),
            getMailboxItem(userId2, "1", time = 1000, labelIds = emptyList(), type = MailboxItemType.Message),
            getMailboxItem(userId1, "2", time = 2000, labelIds = listOf("4"), type = MailboxItemType.Message),
            getMailboxItem(userId2, "2", time = 2000, labelIds = listOf("4"), type = MailboxItemType.Message),
            getMailboxItem(userId1, "3", time = 3000, labelIds = listOf("0", "1"), type = MailboxItemType.Message),
            getMailboxItem(userId2, "3", time = 3000, labelIds = listOf("0", "1"), type = MailboxItemType.Message)
        )
        assertEquals(expected = mailboxItemsOrderedByTimeAscending, actual = mailboxItems)
    }

    @Test
    fun `invoke for Conversation, getLabels and getConversations`() = runTest {
        // Given
        val pageKey = PageKey(orderDirection = OrderDirection.Ascending, size = 6)
        val mailboxPageKey = MailboxPageKey(listOf(userId1, userId2), pageKey)

        // When
        val mailboxItems = usecase.invoke(MailboxItemType.Conversation, mailboxPageKey)

        // Then
        coVerify { labelRepository.getLabels(userId1, LabelType.MessageLabel) }
        coVerify { labelRepository.getLabels(userId2, LabelType.MessageLabel) }
        coVerify { labelRepository.getLabels(userId1, LabelType.MessageFolder) }
        coVerify { labelRepository.getLabels(userId2, LabelType.MessageFolder) }
        coVerify { conversationRepository.getConversations(userId1, pageKey) }
        coVerify { conversationRepository.getConversations(userId2, pageKey) }
        val mailboxItemsOrderedByTimeAscending = listOf(
            getMailboxItem(userId1, "1", time = 1000, labelIds = listOf("0"), type = MailboxItemType.Conversation),
            getMailboxItem(userId2, "1", time = 1000, labelIds = listOf("0"), type = MailboxItemType.Conversation),
            getMailboxItem(userId1, "2", time = 2000, labelIds = listOf("4"), type = MailboxItemType.Conversation),
            getMailboxItem(userId2, "2", time = 2000, labelIds = listOf("4"), type = MailboxItemType.Conversation),
            getMailboxItem(userId1, "3", time = 3000, labelIds = listOf("0", "1"), type = MailboxItemType.Conversation),
            getMailboxItem(userId2, "3", time = 3000, labelIds = listOf("0", "1"), type = MailboxItemType.Conversation)
        )
        assertEquals(expected = mailboxItemsOrderedByTimeAscending, actual = mailboxItems)
    }
}
