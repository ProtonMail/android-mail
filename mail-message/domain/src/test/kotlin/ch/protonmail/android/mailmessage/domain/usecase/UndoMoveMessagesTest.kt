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

import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.UndoableOperation
import ch.protonmail.android.mailcommon.domain.repository.UndoableOperationRepository
import ch.protonmail.android.mailcommon.domain.sample.LabelIdSample
import ch.protonmail.android.mailcommon.domain.sample.UserIdSample
import ch.protonmail.android.mailcommon.domain.usecase.GetUndoableOperation
import ch.protonmail.android.mailcommon.domain.usecase.RegisterUndoableOperation
import ch.protonmail.android.mailcommon.domain.usecase.UndoLastOperation
import ch.protonmail.android.maillabel.domain.model.MailLabels
import ch.protonmail.android.maillabel.domain.model.SystemLabelId
import ch.protonmail.android.maillabel.domain.model.toMailLabelSystem
import ch.protonmail.android.maillabel.domain.usecase.ObserveExclusiveMailLabels
import ch.protonmail.android.mailmessage.domain.model.Message
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailmessage.domain.repository.MessageRepository
import ch.protonmail.android.mailmessage.domain.sample.MessageIdSample
import ch.protonmail.android.mailmessage.domain.sample.MessageSample
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.spyk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import me.proton.core.label.domain.entity.LabelId
import kotlin.test.Test

class UndoMoveMessagesTest {

    private val userId = UserIdSample.Primary
    private val exclusiveMailLabels = SystemLabelId.exclusiveList.map { it.toMailLabelSystem() }

    private val messageRepository = mockk<MessageRepository>()
    private val decrementUnreadCount = mockk<DecrementUnreadCount>()
    private val incrementUnreadCount = mockk<IncrementUnreadCount>()
    private val observeExclusiveMailLabels = mockk<ObserveExclusiveMailLabels>()
    private val registerUndoableOperation = mockk<RegisterUndoableOperation>()
    private val undoableOperationRepository = mockk<UndoableOperationRepository>()

    private val moveMessages = spyk(
        MoveMessages(
            messageRepository,
            decrementUnreadCount,
            incrementUnreadCount,
            observeExclusiveMailLabels,
            registerUndoableOperation
        )
    )
    private val getUndoableOperation = GetUndoableOperation(undoableOperationRepository)
    private val undoLastOperation = UndoLastOperation(getUndoableOperation)

    @Test
    fun `when undo operation then move messages is called to reverse the operation`() = runTest {
        // Given
        val expectedMessages = listOf(MessageSample.AugWeatherForecast)
        val destinationLabel = LabelIdSample.Spam
        val movedMessages = listOf(MessageSample.AugWeatherForecast.copy(labelIds = listOf(destinationLabel)))
        val messageIds = expectedMessages.map { it.messageId }
        expectObserveExclusiveMailLabelSucceeds()
        expectGetLocalMessagesSucceeds(messageIds, expectedMessages)
        expectMoveSucceeds(
            SystemLabelId.Spam.labelId,
            expectedMessages,
            mapOf(MessageIdSample.AugWeatherForecast to LabelIdSample.Archive)
        )
        expectMoveSucceeds(
            SystemLabelId.Archive.labelId,
            expectedMessages,
            mapOf(MessageIdSample.AugWeatherForecast to destinationLabel)
        )
        expectRegisterUndoOperationSucceeds()


        val undoOperationSlot = slot<UndoableOperation>()
        moveMessages(userId, messageIds, destinationLabel)
        coVerify { registerUndoableOperation(capture(undoOperationSlot)) }
        coEvery { undoableOperationRepository.getLastOperation() } returns undoOperationSlot.captured
        // the message is now moved in the local DB
        expectGetLocalMessagesSucceeds(messageIds, movedMessages)

        // When
        undoLastOperation()

        // Then
        coVerify { moveMessages(userId, messageIds, LabelIdSample.Archive) }
    }

    @Test
    fun `when undo is called then move messages is called with messages grouped by label`() = runTest {
        // Given
        val expectedMessages = listOf(MessageSample.AugWeatherForecast, MessageSample.OctWeatherForecast)
        val destinationLabel = LabelIdSample.Trash
        val movedMessages = expectedMessages.map { it.copy(labelIds = listOf(destinationLabel)) }
        val messageIds = expectedMessages.map { it.messageId }
        expectObserveExclusiveMailLabelSucceeds()
        expectGetLocalMessagesSucceeds(messageIds, expectedMessages)
        expectMoveSucceeds(
            SystemLabelId.Trash.labelId,
            expectedMessages,
            expectedMessages.associate { it.messageId to LabelIdSample.Archive }
        )
        expectMoveSucceeds(
            SystemLabelId.Archive.labelId,
            expectedMessages,
            expectedMessages.associate { it.messageId to destinationLabel }
        )
        expectRegisterUndoOperationSucceeds()

        val undoOperationSlot = slot<UndoableOperation>()
        moveMessages(userId, messageIds, destinationLabel)
        coVerify { registerUndoableOperation(capture(undoOperationSlot)) }
        coEvery { undoableOperationRepository.getLastOperation() } returns undoOperationSlot.captured
        // the message is now moved in the local DB
        expectGetLocalMessagesSucceeds(messageIds, movedMessages)

        // When
        undoLastOperation()

        // Then
        coVerify { moveMessages(userId, messageIds, LabelIdSample.Archive) }
    }

    private fun expectRegisterUndoOperationSucceeds() {
        coEvery { registerUndoableOperation(any<UndoableOperation.UndoMoveMessages>()) } just Runs
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

    private fun expectGetLocalMessagesSucceeds(messageIds: List<MessageId>, withMessages: List<Message>) {
        coEvery { messageRepository.getLocalMessages(userId, messageIds) } returns withMessages
    }

    private fun expectMoveSucceeds(
        destinationLabel: LabelId,
        expectedMessages: List<Message>,
        messageIdToLabel: Map<MessageId, LabelId?>
    ) {
        coEvery {
            messageRepository.moveTo(userId, messageIdToLabel, destinationLabel)
        } returns expectedMessages.right()
    }
}
