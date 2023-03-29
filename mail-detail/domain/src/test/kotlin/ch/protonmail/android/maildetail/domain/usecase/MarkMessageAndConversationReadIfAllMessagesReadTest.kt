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
import ch.protonmail.android.mailcommon.domain.sample.ConversationIdSample
import ch.protonmail.android.mailcommon.domain.sample.UserIdSample
import ch.protonmail.android.mailconversation.domain.repository.ConversationRepository
import ch.protonmail.android.maildetail.domain.model.MarkConversationReadError
import ch.protonmail.android.maillabel.domain.SelectedMailLabelId
import ch.protonmail.android.mailmessage.domain.repository.MessageRepository
import ch.protonmail.android.mailmessage.domain.sample.MessageIdSample
import ch.protonmail.android.mailmessage.domain.sample.MessageSample
import ch.protonmail.android.testdata.conversation.ConversationTestData
import ch.protonmail.android.testdata.maillabel.MailLabelTestData
import io.mockk.Called
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals

class MarkMessageAndConversationReadIfAllMessagesReadTest {

    private val messageRepository: MessageRepository = mockk()
    private val markMessageAsRead: MarkMessageAsRead = mockk()
    private val conversationRepository: ConversationRepository = mockk()
    private val selectedMailLabelId: SelectedMailLabelId = mockk {
        coEvery { this@mockk.flow } returns MutableStateFlow(MailLabelTestData.customLabelOne.id)
    }

    @Test
    fun `Should return error when marking the message as read returns error`() = runTest {
        // given
        theConversationCacheIsUpToDate()
        val error = DataError.Local.NoDataCached
        coEvery { markMessageAsRead.invoke(any(), any()) } returns error.left()

        // when
        val result = buildUseCase()(UserIdSample.Primary, MessageIdSample.Invoice, ConversationIdSample.Invoices)

        // then
        assertEquals(MarkConversationReadError.DataSourceError(error).left(), result)
    }

    @Test
    fun `Should return error when retrieving the cached conversation messages returns error`() = runTest {
        // given
        theConversationCacheIsUpToDate()
        val error = DataError.Local.NoDataCached
        coEvery { markMessageAsRead.invoke(any(), any()) } returns MessageSample.Invoice.right()
        coEvery { messageRepository.observeCachedMessages(any(), any()) } returns flowOf(error.left())

        // when
        val result = buildUseCase()(UserIdSample.Primary, MessageIdSample.Invoice, ConversationIdSample.Invoices)

        // then
        assertEquals(MarkConversationReadError.DataSourceError(error).left(), result)
    }

    @Test
    fun `Should return error when all messages are read but mark as read returns error`() = runTest {
        // given
        theConversationCacheIsUpToDate()
        val error = DataError.Local.NoDataCached
        coEvery { markMessageAsRead.invoke(any(), any()) } returns MessageSample.Invoice.right()
        coEvery { messageRepository.observeCachedMessages(any(), any()) } returns
            flowOf(nonEmptyListOf(MessageSample.Invoice.copy(unread = false)).right())
        coEvery { conversationRepository.markRead(any(), any(), any()) } returns error.left()

        // when
        val result = buildUseCase()(UserIdSample.Primary, MessageIdSample.Invoice, ConversationIdSample.Invoices)

        // then
        assertEquals(MarkConversationReadError.DataSourceError(error).left(), result)
    }

    @Test
    fun `Should not mark the conversation as read if there are unread messages and return error`() = runTest {
        // given
        theConversationCacheIsUpToDate()
        coEvery { markMessageAsRead.invoke(any(), any()) } returns MessageSample.Invoice.right()
        coEvery { messageRepository.observeCachedMessages(any(), any()) } returns
            flowOf(nonEmptyListOf(MessageSample.Invoice.copy(unread = true)).right())

        // when
        val result = buildUseCase()(UserIdSample.Primary, MessageIdSample.Invoice, ConversationIdSample.Invoices)

        // then
        coVerify { conversationRepository.markRead(any(), any(), any()) wasNot Called }
        assertEquals(MarkConversationReadError.ConversationHasUnreadMessages.left(), result)
    }

    @Test
    fun `Should mark the conversation as read if all messages are read`() = runTest {
        // given
        theConversationCacheIsUpToDate()
        val conversation = ConversationTestData.conversation
        coEvery { markMessageAsRead.invoke(any(), any()) } returns MessageSample.Invoice.right()
        coEvery { messageRepository.observeCachedMessages(any(), any()) } returns
            flowOf(nonEmptyListOf(MessageSample.Invoice.copy(unread = false)).right())
        coEvery {
            conversationRepository.markRead(any(), any(), any())
        } returns conversation.right()

        // when
        val result = buildUseCase()(UserIdSample.Primary, MessageIdSample.Invoice, ConversationIdSample.Invoices)

        // then
        coVerify { conversationRepository.markRead(UserIdSample.Primary, ConversationIdSample.Invoices, any()) }
        assertEquals(Unit.right(), result)
    }

    @Test
    fun `Should not mark the message as read and return error when the conversation cache does not emit`() = runTest {
        // given
        coEvery { conversationRepository.observeConversationCacheUpToDate(any(), any()) } returns flow { }

        // when
        val result = buildUseCase()(UserIdSample.Primary, MessageIdSample.Invoice, ConversationIdSample.Invoices)

        // then
        coVerify { markMessageAsRead wasNot Called }
        coVerify { conversationRepository.markRead(any(), any(), any()) wasNot Called }
        assertEquals(MarkConversationReadError.DataSourceError(DataError.Local.Unknown).left(), result)
    }

    private fun theConversationCacheIsUpToDate() {
        coEvery { conversationRepository.observeConversationCacheUpToDate(any(), any()) } returns flowOf(Unit.right())
    }

    private fun buildUseCase() = MarkMessageAndConversationReadIfAllMessagesRead(
        messageRepository = messageRepository,
        markMessageAsRead = markMessageAsRead,
        conversationRepository = conversationRepository,
        selectedMailLabelId = selectedMailLabelId
    )
}
