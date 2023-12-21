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

package ch.protonmail.android.mailmessage.domain.usecase

import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcommon.domain.sample.UserIdSample
import ch.protonmail.android.mailmessage.domain.repository.MessageRepository
import ch.protonmail.android.mailmessage.domain.sample.MessageIdSample
import ch.protonmail.android.mailmessage.domain.sample.MessageSample
import io.mockk.coEvery
import io.mockk.coVerifySequence
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class MarkMessagesAsReadTest {

    private val userId = UserIdSample.Primary
    private val messageIds = listOf(MessageIdSample.Invoice, MessageIdSample.PlainTextMessage)

    private val messageRepository = mockk<MessageRepository>()
    private val decrementUnreadCount = mockk<DecrementUnreadCount>()

    private val markRead = MarkMessagesAsRead(messageRepository, decrementUnreadCount)

    @Test
    fun `when repository fails then error is returned`() = runTest {
        // given
        val error = DataError.Local.NoDataCached.left()
        coEvery { messageRepository.markRead(userId, messageIds) } returns error
        coEvery { messageRepository.observeCachedMessages(userId, messageIds) } returns flowOf()

        // when
        val result = markRead(userId, messageIds)

        // then
        assertEquals(error, result)
    }

    @Test
    fun `when repository succeed then list of messages is returned`() = runTest {
        // given
        val messages = listOf(MessageSample.Invoice, MessageSample.HtmlInvoice).right()
        coEvery { messageRepository.markRead(userId, messageIds) } returns messages
        coEvery { messageRepository.observeCachedMessages(userId, messageIds) } returns flowOf()

        // when
        val result = markRead(userId, messageIds)

        // then
        assertEquals(messages, result)
    }

    @Test
    fun `decrement unread messages count for each unread message that is marked as read`() = runTest {
        // given
        val unreadMessage = MessageSample.UnreadInvoice
        val messages = listOf(MessageSample.Invoice, MessageSample.HtmlInvoice, unreadMessage).right()
        coEvery { messageRepository.markRead(userId, messageIds) } returns messages
        coEvery { messageRepository.observeCachedMessages(userId, messageIds) } returns flowOf(messages)
        coEvery { decrementUnreadCount(userId, unreadMessage.labelIds) } returns Unit.right()

        // when
        markRead(userId, messageIds)

        // then
        coVerifySequence { decrementUnreadCount(userId, unreadMessage.labelIds) }
    }
}
