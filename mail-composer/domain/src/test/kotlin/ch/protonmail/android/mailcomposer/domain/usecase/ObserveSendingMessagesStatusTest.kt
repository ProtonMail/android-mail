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

import ch.protonmail.android.mailcommon.domain.sample.UserIdSample
import ch.protonmail.android.mailcomposer.domain.model.MessageSendingStatus
import ch.protonmail.android.mailcomposer.domain.sample.DraftStateSample
import ch.protonmail.android.mailmessage.domain.model.DraftState
import ch.protonmail.android.mailmessage.domain.model.SendingError
import ch.protonmail.android.mailmessage.domain.repository.DraftStateRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import org.junit.Assert.assertEquals
import org.junit.Test

class ObserveSendingMessagesStatusTest {

    private val draftStateRepository = mockk<DraftStateRepository>()

    private val observeSendingMessageState = ObserveSendingMessagesStatus(draftStateRepository)

    @Test
    fun `when there are draft that failed sending then emit error sending messages`() = runTest {
        // Given
        val userId = UserIdSample.Primary
        val errorSendingDraftState = DraftStateSample.RemoteDraftInErrorSendingState
        expectedDraftStates(userId) { listOf(DraftStateSample.RemoteDraftInSendingState, errorSendingDraftState) }

        // When
        val actual = observeSendingMessageState.invoke(userId).first()

        // Then
        assertEquals(
            MessageSendingStatus.SendMessageError(
                SendingError.ExternalAddressSendDisabled("Api message for disabled")
            ),
            actual
        )
    }

    @Test
    fun `when there are drafts that failed to upload attachments then emit error upload attachment status`() = runTest {
        // Given
        val userId = UserIdSample.Primary
        val errorUploadingAttachmentsDraftState = DraftStateSample.RemoteDraftInErrorAttachmentUploadState
        expectedDraftStates(userId) {
            listOf(DraftStateSample.RemoteDraftInSendingState, errorUploadingAttachmentsDraftState)
        }

        // When
        val actual = observeSendingMessageState.invoke(userId).first()

        // Then
        assertEquals(MessageSendingStatus.UploadAttachmentsError, actual)
    }

    @Test
    fun `when there are draft that succeeded sending then emit messages sent status`() = runTest {
        // Given
        val userId = UserIdSample.Primary
        val sentDraftState = DraftStateSample.RemoteDraftInSentState
        expectedDraftStates(userId) { listOf(DraftStateSample.RemoteDraftInSendingState, sentDraftState) }

        // When
        val actual = observeSendingMessageState.invoke(userId).first()

        // Then
        assertEquals(MessageSendingStatus.MessageSent, actual)
    }

    @Test
    fun `when there are both failed and succeeded draft states then emit error sending status`() = runTest {
        // Given
        val userId = UserIdSample.Primary
        val sentDraftState = DraftStateSample.RemoteDraftInSentState
        val errorSendingDraftState = DraftStateSample.RemoteDraftInErrorSendingState
        expectedDraftStates(userId) {
            listOf(DraftStateSample.RemoteDraftInSendingState, sentDraftState, errorSendingDraftState)
        }

        // When
        val actual = observeSendingMessageState.invoke(userId).first()

        // Then
        assertEquals(
            MessageSendingStatus.SendMessageError(
                SendingError.ExternalAddressSendDisabled("Api message for disabled")
            ),
            actual
        )
    }

    @Test
    fun `when there are no draft states sent or failed then emit None`() = runTest {
        // Given
        val userId = UserIdSample.Primary
        expectedDraftStates(userId) { listOf(DraftStateSample.RemoteDraftInSendingState) }

        // When
        val actual = observeSendingMessageState.invoke(userId).first()

        // Then
        assertEquals(MessageSendingStatus.None, actual)
    }

    private fun expectedDraftStates(userId: UserId, states: () -> List<DraftState>): List<DraftState> = states().also {
        coEvery { draftStateRepository.observeAll(userId) } returns flowOf(it)
    }
}
