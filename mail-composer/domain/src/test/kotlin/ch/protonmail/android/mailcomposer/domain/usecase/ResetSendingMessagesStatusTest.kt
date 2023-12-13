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

import ch.protonmail.android.mailmessage.domain.repository.DraftStateRepository
import ch.protonmail.android.mailcomposer.domain.sample.DraftStateSample
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test

class ResetSendingMessagesStatusTest {

    private val draftStateRepository = mockk<DraftStateRepository>()

    private val resetDraftStateError = mockk<ResetDraftStateError>()
    private val confirmSendingMessageStatus = mockk<ConfirmSendingMessageStatus>()

    private val resetSendingMessagesStatus = ResetSendingMessagesStatus(
        draftStateRepository, resetDraftStateError, confirmSendingMessageStatus
    )

    @Test
    fun `should reset error state and confirm sending status if current state is error sending `() = runTest {
        // Given
        val userId = DraftStateSample.RemoteDraftInErrorSendingState.userId
        val messageId = DraftStateSample.RemoteDraftInErrorSendingState.messageId
        coEvery { draftStateRepository.observeAll(userId) } returns
            flowOf(listOf(DraftStateSample.RemoteDraftInErrorSendingState))
        coJustRun { resetDraftStateError.invoke(userId, messageId) }
        coJustRun { confirmSendingMessageStatus.invoke(userId, messageId) }

        // When
        resetSendingMessagesStatus.invoke(userId)

        // Then
        coVerify { resetDraftStateError.invoke(userId, messageId) }
        coVerify { confirmSendingMessageStatus.invoke(userId, messageId) }

    }

    @Test
    fun `should reset error state and confirm sending status if current state is error uploading attachment`() =
        runTest {
            // Given
            val userId = DraftStateSample.RemoteDraftInErrorAttachmentUploadState.userId
            val messageId = DraftStateSample.RemoteDraftInErrorAttachmentUploadState.messageId
            coEvery { draftStateRepository.observeAll(userId) } returns
                flowOf(listOf(DraftStateSample.RemoteDraftInErrorAttachmentUploadState))
            coJustRun { resetDraftStateError.invoke(userId, messageId) }
            coJustRun { confirmSendingMessageStatus.invoke(userId, messageId) }

            // When
            resetSendingMessagesStatus.invoke(userId)

            // Then
            coVerify { resetDraftStateError.invoke(userId, messageId) }
            coVerify { confirmSendingMessageStatus.invoke(userId, messageId) }
        }

    @Test
    fun `should confirm confirm sending status if current state is sent`() = runTest {
        // Given
        val userId = DraftStateSample.RemoteDraftInSentState.userId
        val messageId = DraftStateSample.RemoteDraftInSentState.messageId
        coEvery { draftStateRepository.observeAll(userId) } returns
            flowOf(listOf(DraftStateSample.RemoteDraftInSentState))
        coJustRun { confirmSendingMessageStatus.invoke(userId, messageId) }

        // When
        resetSendingMessagesStatus.invoke(userId)

        // Then
        coVerify { confirmSendingMessageStatus.invoke(userId, messageId) }
    }
}
