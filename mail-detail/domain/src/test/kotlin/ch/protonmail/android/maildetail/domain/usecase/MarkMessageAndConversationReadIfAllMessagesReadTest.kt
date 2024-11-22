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

package ch.protonmail.android.maildetail.domain.usecase

import arrow.core.left
import arrow.core.nonEmptyListOf
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcommon.domain.sample.UserIdSample
import ch.protonmail.android.mailconversation.domain.repository.ConversationRepository
import ch.protonmail.android.mailconversation.domain.usecase.MarkConversationsAsRead
import ch.protonmail.android.maildetail.domain.model.MarkConversationReadError
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailmessage.domain.repository.MessageRepository
import ch.protonmail.android.mailmessage.domain.sample.MessageSample
import ch.protonmail.android.testdata.conversation.ConversationTestData
import io.mockk.Called
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals

class MarkMessageAndConversationReadIfAllMessagesReadTest {

    private val messageRepository: MessageRepository = mockk()
    private val markMessageAsRead: MarkMessageAsRead = mockk()
    private val conversationRepository: ConversationRepository = mockk()
    private val markConversationsAsRead: MarkConversationsAsRead = mockk()
    private val userId = UserIdSample.Primary

    @Test
    fun `Should return error when querying the message returns error`() = runTest {
        // given
        val sampleMessage = MessageSample.Invoice
        val sampleConversation = ConversationTestData.conversation
        val error = DataError.Local.NoDataCached
        coEvery { messageRepository.isMessageRead(userId, sampleMessage.messageId) } returns error.left()

        // when
        val result = buildUseCase()(userId, sampleMessage.messageId, sampleConversation.conversationId)

        // then
        assertEquals(MarkConversationReadError.DataSourceError(error).left(), result)
    }

    @Test
    fun `Should return error when marking the message as read returns error`() = runTest {
        // given
        val sampleMessage = MessageSample.Invoice
        val sampleConversation = ConversationTestData.conversation
        val error = DataError.Local.NoDataCached
        coEvery { messageRepository.isMessageRead(userId, sampleMessage.messageId) } returns false.right()
        coEvery { markMessageAsRead.invoke(userId, sampleMessage.messageId) } returns error.left()

        // when
        val result = buildUseCase()(userId, sampleMessage.messageId, sampleConversation.conversationId)

        // then
        assertEquals(MarkConversationReadError.DataSourceError(error).left(), result)
    }

    @Test
    fun `Should return error when querying the conversation read state returns error`() = runTest {
        // given
        val sampleMessage = MessageSample.Invoice
        val sampleConversation = ConversationTestData.conversation
        val error = DataError.Local.NoDataCached
        coEvery { messageRepository.isMessageRead(userId, sampleMessage.messageId) } returns false.right()
        coEvery { markMessageAsRead.invoke(userId, sampleMessage.messageId) } returns sampleMessage.right()
        coEvery { conversationRepository.isCachedConversationRead(userId, sampleConversation.conversationId) } returns
            error.left()

        // when
        val result = buildUseCase()(userId, sampleMessage.messageId, sampleConversation.conversationId)

        // then
        assertEquals(MarkConversationReadError.DataSourceError(error).left(), result)
    }

    @Test
    fun `Should return error when retrieving the messages for the conversation returns error`() = runTest {
        // given
        val sampleMessage = MessageSample.Invoice
        val sampleConversation = ConversationTestData.conversation
        val error = DataError.Local.NoDataCached
        coEvery { messageRepository.isMessageRead(userId, sampleMessage.messageId) } returns false.right()
        coEvery { markMessageAsRead.invoke(userId, sampleMessage.messageId) } returns sampleMessage.right()
        coEvery { conversationRepository.isCachedConversationRead(userId, sampleConversation.conversationId) } returns
            false.right()
        coEvery { messageRepository.observeCachedMessages(userId, sampleConversation.conversationId) } returns
            flowOf(error.left())

        // when
        val result = buildUseCase()(userId, sampleMessage.messageId, sampleConversation.conversationId)

        // then
        assertEquals(MarkConversationReadError.DataSourceError(error).left(), result)
    }

    @Test
    fun `Should return error when marking the conversation as read returns error`() = runTest {
        // given
        val sampleMessage = MessageSample.Invoice
        val sampleConversation = ConversationTestData.conversation
        val error = DataError.Local.NoDataCached
        coEvery { messageRepository.isMessageRead(userId, sampleMessage.messageId) } returns false.right()
        coEvery { messageRepository.observeCachedMessages(userId, sampleConversation.conversationId) } returns
            flowOf(nonEmptyListOf(sampleMessage).right())
        coEvery { markMessageAsRead.invoke(userId, sampleMessage.messageId) } returns sampleMessage.right()
        coEvery { conversationRepository.isCachedConversationRead(userId, sampleConversation.conversationId) } returns
            false.right()
        coEvery {
            markConversationsAsRead(userId, listOf(sampleConversation.conversationId))
        } returns error.left()

        // when
        val result = buildUseCase()(userId, sampleMessage.messageId, sampleConversation.conversationId)

        // then
        assertEquals(MarkConversationReadError.DataSourceError(error).left(), result)
    }

    @Test
    fun `Should mark the conversation and message as read if they are unread`() = runTest {
        // given
        val sampleMessage = MessageSample.Invoice.copy(unread = false)
        val sampleConversation = ConversationTestData.conversation
        coEvery { messageRepository.isMessageRead(userId, sampleMessage.messageId) } returns false.right()
        coEvery { messageRepository.observeCachedMessages(userId, sampleConversation.conversationId) } returns
            flowOf(nonEmptyListOf(sampleMessage).right())
        coEvery { markMessageAsRead.invoke(userId, sampleMessage.messageId) } returns sampleMessage.right()
        coEvery { conversationRepository.isCachedConversationRead(userId, sampleConversation.conversationId) } returns
            false.right()
        coEvery {
            markConversationsAsRead(userId, listOf(sampleConversation.conversationId))
        } returns listOf(sampleConversation).right()

        // when
        val result = buildUseCase()(userId, sampleMessage.messageId, sampleConversation.conversationId)

        // then
        assertEquals(Unit.right(), result)
        coVerify(exactly = 1) { markMessageAsRead.invoke(userId, sampleMessage.messageId) }
        coVerify(exactly = 1) { markConversationsAsRead(userId, listOf(sampleConversation.conversationId)) }
    }

    @Test
    fun `Should mark the unread message as read but not the conversation if it has other unread messages`() = runTest {
        // given
        val sampleMessage = MessageSample.Invoice.copy(unread = true)
        val sampleConversation = ConversationTestData.conversation
        val otherMessage = MessageSample.Invoice.copy(messageId = MessageId("other"), unread = true)
        coEvery { messageRepository.isMessageRead(userId, any()) } returns false.right()
        coEvery { messageRepository.observeCachedMessages(userId, sampleConversation.conversationId) } returns
            flowOf(nonEmptyListOf(sampleMessage, otherMessage).right())
        coEvery { markMessageAsRead.invoke(userId, sampleMessage.messageId) } returns sampleMessage.right()
        coEvery {
            conversationRepository.isCachedConversationRead(userId, sampleConversation.conversationId)
        } returns false.right()

        // when
        val result = buildUseCase()(userId, sampleMessage.messageId, sampleConversation.conversationId)

        // then
        assertEquals(Unit.right(), result)
        coVerify(exactly = 1) { markMessageAsRead.invoke(userId, sampleMessage.messageId) }
        coVerify { markConversationsAsRead wasNot Called }
    }

    @Test
    fun `Should not mark the conversation as read if it is already read`() = runTest {
        // given
        val sampleMessage = MessageSample.Invoice.copy(unread = true)
        val sampleConversation = ConversationTestData.conversation
        coEvery { messageRepository.isMessageRead(userId, sampleMessage.messageId) } returns false.right()
        coEvery { messageRepository.observeCachedMessages(userId, sampleConversation.conversationId) } returns
            flowOf(nonEmptyListOf(sampleMessage).right())
        coEvery { markMessageAsRead.invoke(userId, sampleMessage.messageId) } returns sampleMessage.right()
        coEvery { conversationRepository.isCachedConversationRead(userId, sampleConversation.conversationId) } returns
            true.right()

        // when
        val result = buildUseCase()(userId, sampleMessage.messageId, sampleConversation.conversationId)

        // then
        assertEquals(Unit.right(), result)
        coVerify(exactly = 1) { markMessageAsRead.invoke(userId, sampleMessage.messageId) }
        coVerify { markConversationsAsRead wasNot Called }
    }

    private fun buildUseCase() = MarkMessageAndConversationReadIfAllMessagesRead(
        messageRepository = messageRepository,
        markMessageAsRead = markMessageAsRead,
        conversationRepository = conversationRepository,
        markConversationsAsRead = markConversationsAsRead
    )
}
