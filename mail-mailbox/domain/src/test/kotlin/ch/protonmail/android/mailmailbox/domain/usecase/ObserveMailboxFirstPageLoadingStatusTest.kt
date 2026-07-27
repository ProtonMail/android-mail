/*
 * Copyright (c) 2026 Proton Technologies AG
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
import ch.protonmail.android.mailmailbox.domain.model.MailboxFirstPageLoadingStatus
import ch.protonmail.android.mailmailbox.domain.model.ScrollerType
import ch.protonmail.android.mailmessage.domain.model.MessageScrollerFetchNewStatus
import ch.protonmail.android.mailmessage.domain.repository.MessageRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals

class ObserveMailboxFirstPageLoadingStatusTest {

    private val conversationRepository: ConversationRepository = mockk()
    private val messageRepository: MessageRepository = mockk()

    private val useCase: ObserveMailboxFirstPageLoadingStatus =
        ObserveMailboxFirstPageLoadingStatus(
            conversationRepository = conversationRepository,
            messageRepository = messageRepository
        )

    @Test
    fun `conversation first page loading events are converted to mailbox events`() = runTest {
        // Given
        val conversationFlow = MutableSharedFlow<ConversationScrollerFetchNewStatus>()
        val messageFlow = MutableSharedFlow<MessageScrollerFetchNewStatus>()
        every { conversationRepository.observeScrollerFetchNewStatus() } returns conversationFlow
        every { messageRepository.observeScrollerFetchNewStatus() } returns messageFlow

        // When
        useCase().test {
            conversationFlow.emit(ConversationScrollerFetchNewStatus.FirstPageLoadingStarted(111L))

            // Then
            assertEquals(
                MailboxFirstPageLoadingStatus.Started(111L, ScrollerType.Conversation),
                awaitItem()
            )

            // When
            conversationFlow.emit(ConversationScrollerFetchNewStatus.FirstPageLoadingEnded(222L))

            // Then
            assertEquals(
                MailboxFirstPageLoadingStatus.Ended(222L, ScrollerType.Conversation),
                awaitItem()
            )

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `message first page loading events are converted to mailbox events`() = runTest {
        // Given
        val conversationFlow = MutableSharedFlow<ConversationScrollerFetchNewStatus>()
        val messageFlow = MutableSharedFlow<MessageScrollerFetchNewStatus>()
        every { conversationRepository.observeScrollerFetchNewStatus() } returns conversationFlow
        every { messageRepository.observeScrollerFetchNewStatus() } returns messageFlow

        // When
        useCase().test {
            messageFlow.emit(MessageScrollerFetchNewStatus.FirstPageLoadingStarted(500L))

            // Then
            assertEquals(
                MailboxFirstPageLoadingStatus.Started(500L, ScrollerType.Message),
                awaitItem()
            )

            // When
            messageFlow.emit(MessageScrollerFetchNewStatus.FirstPageLoadingEnded(900L))

            // Then
            assertEquals(
                MailboxFirstPageLoadingStatus.Ended(900L, ScrollerType.Message),
                awaitItem()
            )

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `fetch new events are ignored`() = runTest {
        // Given
        val conversationFlow = MutableSharedFlow<ConversationScrollerFetchNewStatus>()
        val messageFlow = MutableSharedFlow<MessageScrollerFetchNewStatus>()
        every { conversationRepository.observeScrollerFetchNewStatus() } returns conversationFlow
        every { messageRepository.observeScrollerFetchNewStatus() } returns messageFlow

        // When
        useCase().test {
            conversationFlow.emit(ConversationScrollerFetchNewStatus.FetchNewStarted(1L))
            messageFlow.emit(MessageScrollerFetchNewStatus.FetchNewEnded(2L))
            // Only the first-page-loading event should surface.
            messageFlow.emit(MessageScrollerFetchNewStatus.FirstPageLoadingStarted(3L))

            // Then
            assertEquals(
                MailboxFirstPageLoadingStatus.Started(3L, ScrollerType.Message),
                awaitItem()
            )

            cancelAndIgnoreRemainingEvents()
        }
    }
}
