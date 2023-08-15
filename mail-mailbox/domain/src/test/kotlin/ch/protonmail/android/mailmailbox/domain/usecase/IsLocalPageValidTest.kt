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

import ch.protonmail.android.mailcommon.domain.sample.UserIdSample
import ch.protonmail.android.mailconversation.domain.entity.ConversationWithContext
import ch.protonmail.android.mailconversation.domain.repository.ConversationRepository
import ch.protonmail.android.mailmailbox.domain.model.MailboxItemType
import ch.protonmail.android.mailmessage.domain.model.Message
import ch.protonmail.android.mailmessage.domain.repository.MessageRepository
import ch.protonmail.android.mailmessage.domain.sample.MessageSample
import ch.protonmail.android.mailpagination.domain.model.PageKey
import ch.protonmail.android.testdata.conversation.ConversationWithContextTestData
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class IsLocalPageValidTest {

    private val userId = UserIdSample.Primary
    private val messageRepository = mockk<MessageRepository>()
    private val conversationRepository = mockk<ConversationRepository>()

    private val isLocalPageValid = IsLocalPageValid(messageRepository, conversationRepository)

    @Test
    fun `given load messages contains items and is valid, then return true`() = runTest {
        // Given
        val pageKey = PageKey()
        val items = listOf(
            MessageSample.Invoice,
            MessageSample.AugWeatherForecast
        )
        coEvery { messageRepository.getLocalMessages(userId, pageKey) } returns items
        coEvery { messageRepository.isLocalPageValid(userId, pageKey, items) } returns true

        // When
        val result = isLocalPageValid(userId, MailboxItemType.Message, pageKey)

        // Then
        assertTrue(result)
    }

    @Test
    fun `given load messages does not contain items but is valid, then return true`() = runTest {
        // Given
        val pageKey = PageKey()
        val items = emptyList<Message>()
        coEvery { messageRepository.getLocalMessages(userId, pageKey) } returns items
        coEvery { messageRepository.isLocalPageValid(userId, pageKey, items) } returns true

        // When
        val result = isLocalPageValid(userId, MailboxItemType.Message, pageKey)

        // Then
        assertTrue(result)
    }

    @Test
    fun `given the loaded messages are not valid, then return false`() = runTest {
        // Given
        val pageKey = PageKey()
        val items = listOf(
            MessageSample.Invoice,
            MessageSample.AugWeatherForecast
        )
        coEvery { messageRepository.getLocalMessages(userId, pageKey) } returns items
        coEvery { messageRepository.isLocalPageValid(userId, pageKey, items) } returns false

        // When
        val result = isLocalPageValid(userId, MailboxItemType.Message, pageKey)

        // Then
        assertFalse(result)
    }

    @Test
    fun `given load conversations contains items and is valid, then return true`() = runTest {
        // Given
        val pageKey = PageKey()

        val items = listOf(
            ConversationWithContextTestData.conversation1,
            ConversationWithContextTestData.conversation2
        )
        coEvery { conversationRepository.getLocalConversations(userId, pageKey) } returns items
        coEvery { conversationRepository.isLocalPageValid(userId, pageKey, items) } returns true

        // When
        val result = isLocalPageValid(userId, MailboxItemType.Conversation, pageKey)

        // Then
        assertTrue(result)
    }

    @Test
    fun `given load conversation does not contain items but is valid, then return true`() = runTest {
        // Given
        val pageKey = PageKey()
        val items = emptyList<ConversationWithContext>()
        coEvery { conversationRepository.getLocalConversations(userId, pageKey) } returns items
        coEvery { conversationRepository.isLocalPageValid(userId, pageKey, items) } returns true

        // When
        val result = isLocalPageValid(userId, MailboxItemType.Conversation, pageKey)

        // Then
        assertTrue(result)
    }

    @Test
    fun `given the loaded conversations are not valid, then return false`() = runTest {
        // Given
        val pageKey = PageKey()

        val items = listOf(
            ConversationWithContextTestData.conversation1,
            ConversationWithContextTestData.conversation2
        )
        coEvery { conversationRepository.getLocalConversations(userId, pageKey) } returns items
        coEvery { conversationRepository.isLocalPageValid(userId, pageKey, items) } returns false

        // When
        val result = isLocalPageValid(userId, MailboxItemType.Conversation, pageKey)

        // Then
        assertFalse(result)
    }
}
