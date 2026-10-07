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

import arrow.core.getOrElse
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailconversation.domain.repository.ConversationRepository
import ch.protonmail.android.maillabel.domain.model.LabelType
import ch.protonmail.android.mailmailbox.domain.mapper.ConversationMailboxItemMapper
import ch.protonmail.android.mailmailbox.domain.mapper.MessageMailboxItemMapper
import ch.protonmail.android.mailmailbox.domain.model.MailboxItemType
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailmessage.domain.model.SearchMatchField
import ch.protonmail.android.mailmessage.domain.model.SearchMatchPosition
import ch.protonmail.android.mailmessage.domain.repository.MessageRepository
import ch.protonmail.android.mailpagination.domain.model.PageKey
import ch.protonmail.android.mailpagination.domain.model.PaginationError
import ch.protonmail.android.testdata.conversation.ConversationWithContextTestData
import ch.protonmail.android.testdata.label.LabelTestData
import ch.protonmail.android.testdata.message.MessageTestData.buildMessage
import ch.protonmail.android.testdata.user.UserIdTestData.userId
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GetMailboxItemsTest {

    private val messageRepository = mockk<MessageRepository> {
        coEvery { getMessages(any<UserId>(), any<PageKey>()) } returns listOf(
            // userId1
            buildMessage(userId, "1", time = 1000, customLabels = emptyList()),
            buildMessage(userId, "2", time = 2000, customLabels = listOf("4").map(::buildLabel)),
            buildMessage(userId, "3", time = 3000, customLabels = listOf("0", "1").map(::buildLabel))
        ).right()
    }
    private val conversationRepository = mockk<ConversationRepository> {
        coEvery { getConversations(any(), any()) } returns listOf(
            // userId1
            ConversationWithContextTestData.conversation1Labeled,
            ConversationWithContextTestData.conversation2Labeled,
            ConversationWithContextTestData.conversation3Labeled
        ).right()
    }

    private val messageMailboxItemMapper = MessageMailboxItemMapper()
    private val conversationMailboxItemMapper = ConversationMailboxItemMapper()

    private lateinit var usecase: GetMailboxItems

    @Before
    fun setUp() {
        usecase = GetMailboxItems(
            messageRepository,
            conversationRepository,
            messageMailboxItemMapper,
            conversationMailboxItemMapper
        )
    }

    @Test
    fun `invoke for Message, getLabels and loadMessages`() = runTest {
        // Given
        val pageKey = PageKey.DefaultPageKey()

        // When
        val mailboxItems = usecase.invoke(userId, MailboxItemType.Message, pageKey)
            .getOrElse(::error)

        // Then
        coVerify { messageRepository.getMessages(userId, pageKey) }
        assertEquals(3, mailboxItems.size)
        assertEquals(0, mailboxItems[0].labels.size)
        assertEquals(1, mailboxItems[1].labels.size)
        assertEquals(2, mailboxItems[2].labels.size)
        var previous = mailboxItems.first()
        mailboxItems.forEach { current ->
            assert(current.time >= previous.time)
            previous = current
        }
    }

    @Test
    fun `should not ask for search match positions outside search`() = runTest {
        // When
        val mailboxItems = usecase.invoke(userId, MailboxItemType.Message, PageKey.DefaultPageKey())
            .getOrElse(::error)

        // Then
        coVerify(exactly = 0) { messageRepository.getSearchMatchPositions(any(), any(), any()) }
        assertTrue(mailboxItems.all { it.searchMatchedTerms.isEmpty() })
    }

    @Test
    fun `should attach the search matched terms to each search result`() = runTest {
        // Given
        val pageKey = PageKey.PageKeyForSearch(keyword = "governmnt")
        coEvery { messageRepository.getMessages(userId, pageKey) } returns listOf(
            buildMessage(userId, "1", subject = "New government policy"),
            buildMessage(userId, "2", subject = "Unrelated")
        ).right()
        coEvery {
            messageRepository.getSearchMatchPositions(userId, pageKey, listOf(MessageId("1"), MessageId("2")))
        } returns mapOf(
            MessageId("1") to listOf(SearchMatchPosition(SearchMatchField.Subject, valueIndex = 0, start = 4, end = 14))
        ).right()

        // When
        val mailboxItems = usecase.invoke(userId, MailboxItemType.Message, pageKey)
            .getOrElse(::error)

        // Then
        assertEquals(listOf(listOf("government"), emptyList()), mailboxItems.map { it.searchMatchedTerms })
    }

    @Test
    fun `should still return search results when match positions fail`() = runTest {
        // Given
        val pageKey = PageKey.PageKeyForSearch(keyword = "governmnt")
        coEvery {
            messageRepository.getSearchMatchPositions(userId, pageKey, any())
        } returns PaginationError.Other(DataError.Local.IllegalStateError).left()

        // When
        val mailboxItems = usecase.invoke(userId, MailboxItemType.Message, pageKey)
            .getOrElse(::error)

        // Then
        assertEquals(3, mailboxItems.size)
        assertTrue(mailboxItems.all { it.searchMatchedTerms.isEmpty() })
    }

    @Test
    fun `invoke for Conversation, getLabels and getConversations`() = runTest {
        // Given
        val pageKey = PageKey.DefaultPageKey()

        // When
        val mailboxItems = usecase.invoke(userId, MailboxItemType.Conversation, pageKey)
            .getOrElse(::error)

        // Then
        coVerify { conversationRepository.getConversations(userId, pageKey) }
        assertEquals(3, mailboxItems.size)
        assertEquals(1, mailboxItems[0].labels.size)
        assertEquals(1, mailboxItems[1].labels.size)
        assertEquals(2, mailboxItems[2].labels.size)
        var previous = mailboxItems.first()
        mailboxItems.forEach { current ->
            assert(current.time >= previous.time)
            previous = current
        }
    }

    private fun buildLabel(value: String) = LabelTestData.buildLabel(
        id = value,
        type = LabelType.MessageLabel,
        order = value.hashCode()
    )
}
