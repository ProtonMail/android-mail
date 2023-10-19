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

package ch.protonmail.android.mailcomposer.domain.usecase

import arrow.core.right
import ch.protonmail.android.mailcommon.domain.sample.UserIdSample
import ch.protonmail.android.mailcomposer.domain.model.DraftAction
import ch.protonmail.android.mailcomposer.domain.model.DraftState
import ch.protonmail.android.mailcomposer.domain.model.DraftSyncState
import ch.protonmail.android.mailcomposer.domain.repository.DraftStateRepository
import ch.protonmail.android.mailcomposer.domain.repository.MessageRepository
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailmessage.domain.sample.MessageIdSample
import io.mockk.Called
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import org.junit.Test

class UpdateDraftStateForErrorTest {

    private val draftStateRepository: DraftStateRepository = mockk()
    private val messageRepository: MessageRepository = mockk()

    private val updateDraftStateForError = UpdateDraftStateForError(
        draftStateRepository,
        messageRepository
    )

    @Test
    fun `update draft state to the given one when current state is not Sending`() = runTest {
        // Given
        val userId = UserIdSample.Primary
        val messageId = MessageIdSample.Invoice
        val newState = DraftSyncState.ErrorUploadDraft
        givenExistingDraftState(userId, messageId) { buildDraftState(DraftSyncState.Local) }
        givenUpdateDraftSyncStateSucceeds(userId, messageId, newState)

        // When
        updateDraftStateForError(userId, messageId, newState)

        // Then
        coVerify { draftStateRepository.updateDraftSyncState(userId, messageId, newState) }
    }

    @Test
    fun `does not move message back to draft folder when current state is not Sending`() = runTest {
        // Given
        val userId = UserIdSample.Primary
        val messageId = MessageIdSample.Invoice
        val newState = DraftSyncState.ErrorUploadDraft
        givenExistingDraftState(userId, messageId) { buildDraftState(DraftSyncState.Local) }
        givenUpdateDraftSyncStateSucceeds(userId, messageId, newState)

        // When
        updateDraftStateForError(userId, messageId, newState)

        // Then
        verify { messageRepository wasNot Called }
    }

    @Test
    fun `update draft state to Error Sending when current state is Sending`() = runTest {
        // Given
        val userId = UserIdSample.Primary
        val messageId = MessageIdSample.Invoice
        val newState = DraftSyncState.ErrorUploadAttachments
        val expectedState = DraftSyncState.ErrorSending
        givenExistingDraftState(userId, messageId) { buildDraftState(DraftSyncState.Sending) }
        givenUpdateDraftSyncStateSucceeds(userId, messageId, expectedState)
        givenMoveMessageBackFromSentToDraftsSucceeds(userId, messageId)

        // When
        updateDraftStateForError(userId, messageId, newState)

        // Then
        coVerify { draftStateRepository.updateDraftSyncState(userId, messageId, expectedState) }
    }

    @Test
    fun `moves message back to draft folder when current state is Sending`() = runTest {
        // Given
        val userId = UserIdSample.Primary
        val messageId = MessageIdSample.Invoice
        val newState = DraftSyncState.ErrorUploadDraft
        val expectedState = DraftSyncState.ErrorSending
        givenExistingDraftState(userId, messageId) { buildDraftState(DraftSyncState.Sending) }
        givenUpdateDraftSyncStateSucceeds(userId, messageId, expectedState)
        givenMoveMessageBackFromSentToDraftsSucceeds(userId, messageId)

        // When
        updateDraftStateForError(userId, messageId, newState)

        // Then
        coVerify { messageRepository.moveMessageBackFromSentToDrafts(userId, messageId) }
    }

    private fun buildDraftState(syncState: DraftSyncState) = DraftState(
        userId = UserIdSample.Primary,
        messageId = MessageIdSample.LocalDraft,
        apiMessageId = null,
        state = syncState,
        action = DraftAction.Compose
    )

    private fun givenExistingDraftState(
        userId: UserId,
        messageId: MessageId,
        expected: () -> DraftState
    ) = expected().also {
        coEvery { draftStateRepository.observe(userId, messageId) } returns flowOf(it.right())
    }

    private fun givenUpdateDraftSyncStateSucceeds(
        userId: UserId,
        messageId: MessageId,
        state: DraftSyncState
    ) {
        coEvery { draftStateRepository.updateDraftSyncState(userId, messageId, state) } returns Unit.right()
    }

    private fun givenMoveMessageBackFromSentToDraftsSucceeds(userId: UserId, messageId: MessageId) {
        coJustRun { messageRepository.moveMessageBackFromSentToDrafts(userId, messageId) }
    }

}
