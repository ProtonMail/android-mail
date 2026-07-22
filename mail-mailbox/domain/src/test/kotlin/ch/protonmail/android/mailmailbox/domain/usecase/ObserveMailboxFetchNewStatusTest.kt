/*
 * Copyright (c) 2025 Proton Technologies AG
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

import app.cash.turbine.test
import ch.protonmail.android.mailconversation.domain.model.ConversationScrollerFetchNewStatus
import ch.protonmail.android.mailconversation.domain.repository.ConversationRepository
import ch.protonmail.android.mailmailbox.domain.model.MailboxFetchNewStatus
import ch.protonmail.android.mailmailbox.domain.model.ScrollerType
import ch.protonmail.android.mailmessage.domain.model.MessageScrollerFetchNewStatus
import ch.protonmail.android.mailmessage.domain.repository.MessageRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals

class ObserveMailboxFetchNewStatusTest {

    private val conversationRepository: ConversationRepository = mockk()
    private val messageRepository: MessageRepository = mockk()

    private val useCase: ObserveMailboxFetchNewStatus =
        ObserveMailboxFetchNewStatus(
            conversationRepository = conversationRepository,
            messageRepository = messageRepository
        )

    @Test
    fun `conversation scroller events are converted to mailbox events`() = runTest {
        // Given
        val conversationFlow = MutableSharedFlow<ConversationScrollerFetchNewStatus>()
        val messageFlow = MutableSharedFlow<MessageScrollerFetchNewStatus>()
        every { conversationRepository.observeScrollerFetchNewStatus() } returns conversationFlow
        every { messageRepository.observeScrollerFetchNewStatus() } returns messageFlow

        // When
        useCase().test {
            conversationFlow.emit(ConversationScrollerFetchNewStatus.FetchNewStarted(111L))

            // Then
            assertEquals(
                MailboxFetchNewStatus.Started(111L, ScrollerType.Conversation),
                awaitItem()
            )

            // When
            conversationFlow.emit(ConversationScrollerFetchNewStatus.FetchNewEnded(222L))

            // Then
            assertEquals(
                MailboxFetchNewStatus.Ended(222L, ScrollerType.Conversation),
                awaitItem()
            )

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `message scroller events are converted to mailbox events`() = runTest {
        // Given
        val conversationFlow = MutableSharedFlow<ConversationScrollerFetchNewStatus>()
        val messageFlow = MutableSharedFlow<MessageScrollerFetchNewStatus>()
        every { conversationRepository.observeScrollerFetchNewStatus() } returns conversationFlow
        every { messageRepository.observeScrollerFetchNewStatus() } returns messageFlow

        // When
        useCase().test {
            messageFlow.emit(MessageScrollerFetchNewStatus.FetchNewStarted(500L))

            // Then
            assertEquals(
                MailboxFetchNewStatus.Started(500L, ScrollerType.Message),
                awaitItem()
            )

            // When
            messageFlow.emit(MessageScrollerFetchNewStatus.FetchNewEnded(900L))

            // Then
            assertEquals(
                MailboxFetchNewStatus.Ended(900L, ScrollerType.Message),
                awaitItem()
            )

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `first page loading events are ignored so the loading bar is unaffected`() = runTest {
        // Given
        val conversationFlow = MutableSharedFlow<ConversationScrollerFetchNewStatus>()
        val messageFlow = MutableSharedFlow<MessageScrollerFetchNewStatus>()
        every { conversationRepository.observeScrollerFetchNewStatus() } returns conversationFlow
        every { messageRepository.observeScrollerFetchNewStatus() } returns messageFlow

        // When
        useCase().test {
            conversationFlow.emit(ConversationScrollerFetchNewStatus.FirstPageLoadingStarted(1L))
            messageFlow.emit(MessageScrollerFetchNewStatus.FirstPageLoadingEnded(2L))
            // Only the fetch-new event should surface.
            conversationFlow.emit(ConversationScrollerFetchNewStatus.FetchNewStarted(3L))

            // Then
            assertEquals(
                MailboxFetchNewStatus.Started(3L, ScrollerType.Conversation),
                awaitItem()
            )

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `message and conversation events are merged and emitted in arrival order`() = runTest {
        // Given
        val conversationFlow = MutableSharedFlow<ConversationScrollerFetchNewStatus>()
        val messageFlow = MutableSharedFlow<MessageScrollerFetchNewStatus>()
        every { conversationRepository.observeScrollerFetchNewStatus() } returns conversationFlow
        every { messageRepository.observeScrollerFetchNewStatus() } returns messageFlow

        // When
        useCase().test {
            conversationFlow.emit(ConversationScrollerFetchNewStatus.FetchNewStarted(1L))
            messageFlow.emit(MessageScrollerFetchNewStatus.FetchNewStarted(2L))
            conversationFlow.emit(ConversationScrollerFetchNewStatus.FetchNewEnded(3L))

            // Then
            assertEquals(
                MailboxFetchNewStatus.Started(1L, ScrollerType.Conversation),
                awaitItem()
            )
            assertEquals(
                MailboxFetchNewStatus.Started(2L, ScrollerType.Message),
                awaitItem()
            )
            assertEquals(
                MailboxFetchNewStatus.Ended(3L, ScrollerType.Conversation),
                awaitItem()
            )

            cancelAndIgnoreRemainingEvents()
        }
    }
}
