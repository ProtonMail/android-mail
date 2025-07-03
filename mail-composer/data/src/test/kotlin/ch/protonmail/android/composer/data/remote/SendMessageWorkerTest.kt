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

package ch.protonmail.android.composer.data.remote

import android.content.Context
import androidx.work.ListenableWorker.Result
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.composer.data.usecase.SendMessage
import ch.protonmail.android.mailcommon.data.worker.Enqueuer
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcommon.domain.model.ProtonError
import ch.protonmail.android.mailcommon.domain.sample.UserIdSample
import ch.protonmail.android.mailmessage.domain.model.DraftSyncState
import ch.protonmail.android.mailmessage.domain.repository.DraftStateRepository
import ch.protonmail.android.mailmessage.domain.model.SendingError
import ch.protonmail.android.mailcomposer.domain.usecase.UpdateDraftStateForError
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailmessage.domain.sample.MessageIdSample
import io.mockk.Called
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SendMessageWorkerTest {

    private val workManager: WorkManager = mockk {
        coEvery { enqueue(any<OneTimeWorkRequest>()) } returns mockk()
    }
    private val parameters: WorkerParameters = mockk {
        every { this@mockk.getTaskExecutor() } returns mockk(relaxed = true)
    }
    private val context: Context = mockk()
    private val sendMessageMock: SendMessage = mockk()
    private val draftStateRepositoryMock: DraftStateRepository = mockk()
    private val updateDraftStateForErrorMock: UpdateDraftStateForError = mockk()

    private val sendMessageWorker = SendMessageWorker(
        context,
        parameters,
        sendMessageMock,
        draftStateRepositoryMock,
        updateDraftStateForErrorMock,
        isApiSendingErrorsEnabled = true
    )

    @Test
    fun `worker is enqueued with given parameters`() {
        // Given
        val userId = UserIdSample.Primary
        val messageId = MessageIdSample.LocalDraft
        givenInputData(userId, messageId)

        // When
        Enqueuer(workManager).enqueue<SendMessageWorker>(userId, SendMessageWorker.params(userId, messageId))

        // Then
        val requestSlot = slot<OneTimeWorkRequest>()
        verify { workManager.enqueue(capture(requestSlot)) }
        val workSpec = requestSlot.captured.workSpec
        val constraints = workSpec.constraints
        val inputData = workSpec.input
        val actualUserId = inputData.getString(SendMessageWorker.RawUserIdKey)
        val actualMessageIds = inputData.getString(SendMessageWorker.RawMessageIdKey)
        assertEquals(userId.id, actualUserId)
        assertEquals(messageId.id, actualMessageIds)
        assertEquals(NetworkType.CONNECTED, constraints.requiredNetworkType)
    }

    @Test
    fun `worker returns failure and updates draft state for error when sendMessage fails`() = runTest {
        // Given
        val userId = UserIdSample.Primary
        val messageId = MessageIdSample.LocalDraft
        val sendingError = SendingError.Other
        givenInputData(userId, messageId)
        givenSendMessageFailsWithDraftNotFound(userId, messageId)
        givenUpdateDraftSyncStateSucceeds(userId, messageId, DraftSyncState.ErrorSending)
        givenUpdateDraftStateForErrorSucceeds(userId, messageId, sendingError)

        // When
        val actual = sendMessageWorker.doWork()

        // Then
        coVerify { updateDraftStateForErrorMock(userId, messageId, DraftSyncState.ErrorSending, sendingError) }
        assertEquals(Result.failure(), actual)
    }

    @Test
    fun `worker updates draft state for error when sendMessage fails with a message already sent error`() = runTest {
        // Given
        val userId = UserIdSample.Primary
        val messageId = MessageIdSample.LocalDraft
        val sendingError = SendingError.MessageAlreadySent
        givenInputData(userId, messageId)
        givenSendMessageFailsWithSendingToApiError(userId, messageId)
        givenUpdateDraftSyncStateSucceeds(userId, messageId, DraftSyncState.ErrorSending)
        givenUpdateDraftStateForErrorSucceeds(userId, messageId, sendingError)

        // When
        val actual = sendMessageWorker.doWork()

        // Then
        coVerify { updateDraftStateForErrorMock(userId, messageId, DraftSyncState.ErrorSending, sendingError) }
        assertEquals(Result.failure(), actual)
    }

    @Test
    fun `worker returns success and updates DraftSyncState when sendMessage succeeds`() = runTest {
        // Given
        val userId = UserIdSample.Primary
        val messageId = MessageIdSample.LocalDraft
        givenInputData(userId, messageId)
        givenSendMessageSucceeds(userId, messageId)
        givenUpdateDraftSyncStateSucceeds(userId, messageId, DraftSyncState.Sent)

        // When
        val actual = sendMessageWorker.doWork()

        // Then
        coVerify { draftStateRepositoryMock.updateDraftSyncState(userId, messageId, DraftSyncState.Sent, null) }
        assertEquals(Result.success(), actual)
    }

    @Test
    fun `worker fails when userid worker parameter is missing`() = runTest {
        // Given
        val userId = null
        val messageId = MessageIdSample.LocalDraft
        givenInputData(userId, messageId)

        // When - Then
        assertFailsWith<IllegalArgumentException> { sendMessageWorker.doWork() }
        coVerify { sendMessageMock wasNot Called }
    }

    @Test
    fun `worker fails when messageIds worker parameter is empty`() = runTest {
        // Given
        val userId = UserIdSample.Primary
        val messageId = MessageId("")
        givenInputData(userId, messageId)

        // When - Then
        assertFailsWith<IllegalArgumentException> { sendMessageWorker.doWork() }
        coVerify { sendMessageMock wasNot Called }
    }

    private fun givenUpdateDraftSyncStateSucceeds(
        userId: UserId,
        messageId: MessageId,
        syncState: DraftSyncState
    ) {
        coEvery { draftStateRepositoryMock.updateDraftSyncState(userId, messageId, syncState, null) } returns
            Unit.right()
    }

    private fun givenUpdateDraftStateForErrorSucceeds(
        userId: UserId,
        messageId: MessageId,
        sendingError: SendingError?
    ) {
        coJustRun { updateDraftStateForErrorMock(userId, messageId, DraftSyncState.ErrorSending, sendingError) }
    }

    private fun givenSendMessageFailsWithDraftNotFound(userId: UserId, messageId: MessageId) {
        coEvery { sendMessageMock(userId, messageId) } returns SendMessage.Error.DraftNotFound.left()
    }

    private fun givenSendMessageFailsWithSendingToApiError(userId: UserId, messageId: MessageId) {
        coEvery {
            sendMessageMock(userId, messageId)
        } returns SendMessage.Error.SendingToApi(
            DataError.Remote.Proton(ProtonError.MessageAlreadySent, apiMessage = "Api message")
        ).left()
    }

    private fun givenSendMessageSucceeds(userId: UserId, messageId: MessageId) {
        coEvery { sendMessageMock(userId, messageId) } returns Unit.right()
    }

    private fun givenInputData(userId: UserId?, messageId: MessageId?) {
        every { parameters.inputData.getString(SendMessageWorker.RawUserIdKey) } returns userId?.id
        every { parameters.inputData.getString(SendMessageWorker.RawMessageIdKey) } returns messageId?.id
    }
}
