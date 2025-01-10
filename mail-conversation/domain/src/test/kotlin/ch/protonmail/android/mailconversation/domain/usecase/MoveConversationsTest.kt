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

package ch.protonmail.android.mailconversation.domain.usecase

import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcommon.domain.model.UndoableOperation
import ch.protonmail.android.mailcommon.domain.sample.ConversationIdSample
import ch.protonmail.android.mailcommon.domain.sample.LabelIdSample
import ch.protonmail.android.mailcommon.domain.sample.LabelSample
import ch.protonmail.android.mailcommon.domain.sample.UserIdSample
import ch.protonmail.android.mailcommon.domain.usecase.RegisterUndoableOperation
import ch.protonmail.android.mailconversation.domain.entity.Conversation
import ch.protonmail.android.mailconversation.domain.repository.ConversationRepository
import ch.protonmail.android.mailconversation.domain.sample.ConversationLabelSample
import ch.protonmail.android.mailconversation.domain.sample.ConversationSample
import ch.protonmail.android.maillabel.domain.model.MailLabels
import ch.protonmail.android.maillabel.domain.model.SystemLabelId
import ch.protonmail.android.maillabel.domain.model.toMailLabelSystem
import ch.protonmail.android.maillabel.domain.usecase.ObserveExclusiveMailLabels
import ch.protonmail.android.maillabel.domain.usecase.ObserveMailLabels
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import me.proton.core.label.domain.entity.LabelId
import kotlin.test.Test
import kotlin.test.assertEquals

class MoveConversationsTest {

    private val userId = UserIdSample.Primary
    private val conversationIds = listOf(ConversationIdSample.Invoices, ConversationIdSample.WeatherForecast)
    private val exclusiveMailLabels = SystemLabelId.exclusiveList.map { it.toMailLabelSystem() }

    private val conversationRepository = mockk<ConversationRepository>()
    private val observeMailLabels = mockk<ObserveMailLabels>()
    private val observeExclusiveMailLabels = mockk<ObserveExclusiveMailLabels>()
    private val decrementUnreadCount: DecrementUnreadCount = mockk()
    private val incrementUnreadCount: IncrementUnreadCount = mockk()
    private val registerUndoableOperation = mockk<RegisterUndoableOperation>()

    private val moveConversations by lazy {
        MoveConversations(
            conversationRepository = conversationRepository,
            observeExclusiveMailLabels = observeExclusiveMailLabels,
            observeMailLabels = observeMailLabels,
            incrementUnreadCount = incrementUnreadCount,
            decrementUnreadCount = decrementUnreadCount,
            registerUndoableOperation = registerUndoableOperation
        )
    }

    @Test
    fun `when move succeeds then Unit is returned`() = runTest {
        // Given
        val destinationLabel = LabelId("labelId")
        val expectedConversations = listOf(ConversationSample.WeatherForecast, ConversationSample.AlphaAppFeedback)

        expectObserveMailLabelsSucceeds()
        expectObserveExclusiveMailLabelSucceeds()
        expectMoveSucceeds(destinationLabel, expectedConversations)
        expectRegisterUndoOperationSucceeds()
        coEvery { conversationRepository.observeCachedConversations(userId, conversationIds) } returns flowOf()

        // When
        val result = moveConversations(userId, conversationIds, destinationLabel)

        // Then
        assertEquals(Unit.right(), result)
    }

    @Test
    fun `when move fails then DataError is returned`() = runTest {
        // Given
        val destinationLabel = LabelId("labelId")

        expectObserveMailLabelsSucceeds()
        expectObserveExclusiveMailLabelSucceeds()
        expectMoveFails(destinationLabel)
        coEvery { conversationRepository.observeCachedConversations(userId, conversationIds) } returns flowOf()

        // When
        val result = moveConversations(userId, conversationIds, destinationLabel)

        // Then
        assertEquals(DataError.Local.NoDataCached.left(), result)
    }

    @Test
    fun `decrement unread count for each conversation's label that has unread messages`() = runTest {
        // given
        val destinationLabel = LabelId("labelId")
        val forecastConversation = ConversationSample.WeatherForecast.copy(
            labels = listOf(
                ConversationLabelSample.build(
                    conversationId = ConversationSample.WeatherForecast.conversationId,
                    labelId = LabelIdSample.Inbox,
                    numMessages = 2,
                    numUnread = 1
                ),
                ConversationLabelSample.build(
                    conversationId = ConversationSample.WeatherForecast.conversationId,
                    labelId = LabelIdSample.Archive,
                    numMessages = 2,
                    numUnread = 0
                )
            )
        )
        val conversations = listOf(forecastConversation)
        val whetherForecastExpectedLabelIds = listOf(LabelSample.Inbox.labelId)
        expectObserveMailLabelsSucceeds()
        expectObserveExclusiveMailLabelSucceeds()
        expectMoveSucceeds(destinationLabel, conversations)
        expectRegisterUndoOperationSucceeds()
        coEvery {
            conversationRepository.observeCachedConversations(userId, conversationIds)
        } returns flowOf(conversations)
        coEvery { decrementUnreadCount(userId, whetherForecastExpectedLabelIds) } just Runs

        // when
        moveConversations(userId, conversationIds, destinationLabel)

        // then
        coVerify {
            decrementUnreadCount(userId, whetherForecastExpectedLabelIds)
        }
    }

    @Test
    fun `increments unread count for the destination label when it has unread messages`() = runTest {
        // given
        val destinationLabel = LabelId("labelId")
        val forecastConversation = ConversationSample.WeatherForecast.copy(
            labels = listOf(
                ConversationLabelSample.build(
                    conversationId = ConversationSample.WeatherForecast.conversationId,
                    labelId = LabelIdSample.Inbox,
                    numMessages = 2,
                    numUnread = 0
                ),
                ConversationLabelSample.build(
                    conversationId = ConversationSample.WeatherForecast.conversationId,
                    labelId = destinationLabel,
                    numMessages = 2,
                    numUnread = 1
                )
            )
        )
        val conversations = listOf(forecastConversation)
        val whetherForecastExpectedLabelIds = listOf(destinationLabel)
        expectObserveMailLabelsSucceeds()
        expectObserveExclusiveMailLabelSucceeds()
        expectMoveSucceeds(destinationLabel, conversations)
        expectRegisterUndoOperationSucceeds()
        coEvery {
            conversationRepository.observeCachedConversations(userId, conversationIds)
        } returns flowOf(conversations)
        coEvery { incrementUnreadCount(userId, whetherForecastExpectedLabelIds) } just Runs

        // when
        moveConversations(userId, conversationIds, destinationLabel)

        // then
        coVerify {
            incrementUnreadCount(userId, whetherForecastExpectedLabelIds)
        }
    }

    @Test
    fun `store undoable operation when moving conversation locally succeeds`() = runTest {
        // given
        val destinationLabel = LabelId("labelId")
        val alphaAppConversation = ConversationSample.AlphaAppFeedback
        val conversations = listOf(alphaAppConversation)
        expectObserveMailLabelsSucceeds()
        expectObserveExclusiveMailLabelSucceeds()
        expectMoveSucceeds(destinationLabel, conversations)
        expectRegisterUndoOperationSucceeds()
        coEvery {
            conversationRepository.observeCachedConversations(userId, conversationIds)
        } returns flowOf(conversations)

        // when
        moveConversations(userId, conversationIds, destinationLabel)

        // then
        coVerify { registerUndoableOperation(any<UndoableOperation.UndoMoveConversations>()) }
    }

    private fun expectRegisterUndoOperationSucceeds() {
        coEvery { registerUndoableOperation(any<UndoableOperation.UndoMoveConversations>()) } just Runs
    }

    private fun expectMoveSucceeds(destinationLabel: LabelId, expectedList: List<Conversation>) {
        val exclusiveList = exclusiveMailLabels.map { it.id.labelId }
        coEvery {
            conversationRepository.move(userId, conversationIds, exclusiveList, exclusiveList, destinationLabel)
        } returns expectedList.right()
    }

    private fun expectMoveFails(destinationLabel: LabelId) {
        val exclusiveList = exclusiveMailLabels.map { it.id.labelId }
        coEvery {
            conversationRepository.move(userId, conversationIds, exclusiveList, exclusiveList, destinationLabel)
        } returns DataError.Local.NoDataCached.left()
    }

    private fun expectObserveExclusiveMailLabelSucceeds() {
        every { observeExclusiveMailLabels(userId) } returns flowOf(
            MailLabels(
                systemLabels = exclusiveMailLabels,
                folders = emptyList(),
                labels = emptyList()
            )
        )
    }

    private fun expectObserveMailLabelsSucceeds() {
        every { observeMailLabels(userId, any()) } returns flowOf(
            MailLabels(
                systemLabels = exclusiveMailLabels,
                folders = emptyList(),
                labels = emptyList()
            )
        )
    }
}
