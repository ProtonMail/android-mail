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
import ch.protonmail.android.mailcomposer.domain.repository.MessageRepository
import ch.protonmail.android.mailmessage.domain.model.DraftAction
import ch.protonmail.android.mailmessage.domain.model.DraftState
import ch.protonmail.android.mailmessage.domain.model.DraftSyncState
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailmessage.domain.model.SendingError
import ch.protonmail.android.mailmessage.domain.repository.DraftStateRepository
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
        val sendingError = SendingError.Other
        givenExistingDraftState(userId, messageId) { buildDraftState(DraftSyncState.Local) }
        givenUpdateDraftSyncStateSucceeds(userId, messageId, newState, null)

        // When
        updateDraftStateForError(userId, messageId, newState, sendingError)

        // Then
        coVerify { draftStateRepository.updateDraftSyncState(userId, messageId, newState, null) }
    }

    @Test
    fun `does not move message back to draft folder when current state is not Sending`() = runTest {
        // Given
        val userId = UserIdSample.Primary
        val messageId = MessageIdSample.Invoice
        val newState = DraftSyncState.ErrorUploadDraft
        val sendingError = SendingError.Other
        givenExistingDraftState(userId, messageId) { buildDraftState(DraftSyncState.Local) }
        givenUpdateDraftSyncStateSucceeds(userId, messageId, newState, null)

        // When
        updateDraftStateForError(userId, messageId, newState, sendingError)

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
        val sendingError = SendingError.Other
        givenExistingDraftState(userId, messageId) { buildDraftState(DraftSyncState.Sending) }
        givenUpdateDraftSyncStateSucceeds(userId, messageId, expectedState, sendingError)
        givenMoveMessageBackFromSentToDraftsSucceeds(userId, messageId)

        // When
        updateDraftStateForError(userId, messageId, newState, sendingError)

        // Then
        coVerify { draftStateRepository.updateDraftSyncState(userId, messageId, expectedState, sendingError) }
    }

    @Test
    fun `moves message back to draft folder when current state is Sending`() = runTest {
        // Given
        val userId = UserIdSample.Primary
        val messageId = MessageIdSample.Invoice
        val newState = DraftSyncState.ErrorUploadDraft
        val expectedState = DraftSyncState.ErrorSending
        val sendingError = SendingError.Other
        givenExistingDraftState(userId, messageId) { buildDraftState(DraftSyncState.Sending) }
        givenUpdateDraftSyncStateSucceeds(userId, messageId, expectedState, sendingError)
        givenMoveMessageBackFromSentToDraftsSucceeds(userId, messageId)

        // When
        updateDraftStateForError(userId, messageId, newState, sendingError)

        // Then
        coVerify { messageRepository.moveMessageBackFromSentToDrafts(userId, messageId) }
    }

    @Test
    fun `moves message back to draft folder using api message if available when current state is Sending`() = runTest {
        // Given
        val userId = UserIdSample.Primary
        val messageId = MessageIdSample.Invoice
        val newState = DraftSyncState.ErrorUploadDraft
        val expectedState = DraftSyncState.ErrorSending
        val sendingError = SendingError.Other
        val apiMessageId = MessageIdSample.RemoteDraft
        givenExistingDraftState(userId, messageId) {
            buildDraftStateWithApiMessageId(DraftSyncState.Sending, apiMessageId)
        }
        givenUpdateDraftSyncStateSucceeds(userId, messageId, expectedState, sendingError)
        givenMoveMessageBackFromSentToDraftsSucceeds(userId, apiMessageId)

        // When
        updateDraftStateForError(userId, messageId, newState, sendingError)

        // Then
        coVerify { messageRepository.moveMessageBackFromSentToDrafts(userId, apiMessageId) }
    }

    @Test
    fun `update draft state to Sent when SendingError is MessageAlreadySent`() = runTest {
        // Given
        val userId = UserIdSample.Primary
        val messageId = MessageIdSample.Invoice
        val newState = DraftSyncState.ErrorUploadDraft
        val expectedState = DraftSyncState.Sent
        val sendingError = SendingError.MessageAlreadySent
        givenExistingDraftState(userId, messageId) { buildDraftState(DraftSyncState.Sending) }
        givenUpdateDraftSyncStateSucceeds(userId, messageId, expectedState, null)
        givenMoveMessageBackFromSentToDraftsSucceeds(userId, messageId)

        // When
        updateDraftStateForError(userId, messageId, newState, sendingError)

        // Then
        coVerify { draftStateRepository.updateDraftSyncState(userId, messageId, expectedState, null) }
    }

    private fun buildDraftState(syncState: DraftSyncState) = DraftState(
        userId = UserIdSample.Primary,
        messageId = MessageIdSample.LocalDraft,
        apiMessageId = null,
        state = syncState,
        action = DraftAction.Compose,
        sendingError = null,
        sendingStatusConfirmed = false
    )

    private fun buildDraftStateWithApiMessageId(syncState: DraftSyncState, apiMessageId: MessageId) = DraftState(
        userId = UserIdSample.Primary,
        messageId = MessageIdSample.LocalDraft,
        apiMessageId = apiMessageId,
        state = syncState,
        action = DraftAction.Compose,
        sendingError = null,
        sendingStatusConfirmed = false
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
        state: DraftSyncState,
        error: SendingError?
    ) {
        coEvery { draftStateRepository.updateDraftSyncState(userId, messageId, state, error) } returns Unit.right()
    }

    private fun givenMoveMessageBackFromSentToDraftsSucceeds(userId: UserId, messageId: MessageId) {
        coJustRun { messageRepository.moveMessageBackFromSentToDrafts(userId, messageId) }
    }
}
