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
import androidx.work.ExistingWorkPolicy
import androidx.work.ListenableWorker.Result
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.composer.data.usecase.UploadDraft
import ch.protonmail.android.mailcommon.data.worker.Enqueuer
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcommon.domain.model.NetworkError
import ch.protonmail.android.mailcommon.domain.model.ProtonError
import ch.protonmail.android.mailcommon.domain.sample.UserIdSample
import ch.protonmail.android.mailcomposer.domain.usecase.UpdateDraftStateForError
import ch.protonmail.android.mailmessage.domain.model.DraftSyncState
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailmessage.domain.model.SendingError
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
import kotlin.test.assertTrue

internal class UploadDraftWorkerTest {

    private val workManager: WorkManager = mockk {
        coEvery { enqueueUniqueWork(any(), any(), any<OneTimeWorkRequest>()) } returns mockk()
    }
    private val workerParameters: WorkerParameters = mockk {
        every { this@mockk.taskExecutor } returns mockk(relaxed = true)
        every { this@mockk.tags } returns emptySet()
    }
    private val context: Context = mockk()
    private val uploadDraft: UploadDraft = mockk()
    private val updateDraftStateForError: UpdateDraftStateForError = mockk()

    private val uploadDraftWorker = UploadDraftWorker(
        context,
        workerParameters,
        workManager,
        uploadDraft,
        updateDraftStateForError
    )

    @Test
    fun `worker is enqueued with given parameters`() {
        // Given
        val userId = UserIdSample.Primary
        val messageId = MessageIdSample.LocalDraft
        val workerId = "UploadWorker-workerId"
        val workerPolicy = ExistingWorkPolicy.REPLACE
        givenInputData(userId, messageId)

        // When
        Enqueuer(workManager).enqueueUniqueWork<UploadDraftWorker>(
            userId = userId,
            workerId = workerId,
            existingWorkPolicy = workerPolicy,
            params = UploadDraftWorker.params(userId, messageId)
        )

        // Then
        val workerIdSlot = slot<String>()
        val existingPolicySlot = slot<ExistingWorkPolicy>()
        val requestSlot = slot<OneTimeWorkRequest>()
        verify {
            workManager.enqueueUniqueWork(
                capture(workerIdSlot),
                capture(existingPolicySlot),
                capture(requestSlot)
            )
        }

        val workSpec = requestSlot.captured.workSpec
        val constraints = workSpec.constraints
        val inputData = workSpec.input
        val capturedPolicy = existingPolicySlot.captured
        val tags = requestSlot.captured.tags
        val actualUserId = inputData.getString(UploadDraftWorker.RawUserIdKey)
        val actualMessageIds = inputData.getString(UploadDraftWorker.RawMessageIdKey)

        assertEquals(userId.id, actualUserId)
        assertEquals(messageId.id, actualMessageIds)
        assertEquals(NetworkType.CONNECTED, constraints.requiredNetworkType)
        assertEquals(workerPolicy, capturedPolicy)
        assertTrue(tags.contains(workerId))
    }

    @Test
    fun `worker returns success when sync draft succeeds`() = runTest {
        // Given
        val userId = UserIdSample.Primary
        val messageId = MessageIdSample.LocalDraft
        givenInputData(userId, messageId)
        givenUploadDraftSucceeds(userId, messageId)

        // When
        val actual = uploadDraftWorker.doWork()

        // Then
        coVerify { uploadDraft(userId, messageId) }
        assertEquals(Result.success(), actual)
    }

    @Test
    fun `worker fails when userid worker parameter is missing`() = runTest {
        // Given
        val userId = null
        val messageId = MessageIdSample.LocalDraft
        givenInputData(userId, messageId)

        // When - Then
        assertFailsWith<IllegalArgumentException> { uploadDraftWorker.doWork() }
        coVerify { uploadDraft wasNot Called }
    }

    @Test
    fun `worker fails when messageIds worker parameter is empty`() = runTest {
        // Given
        val userId = UserIdSample.Primary
        val messageId = MessageId("")
        givenInputData(userId, messageId)

        // When - Then
        assertFailsWith<IllegalArgumentException> { uploadDraftWorker.doWork() }
        coVerify { uploadDraft wasNot Called }
    }

    @Test
    fun `worker fails and updates draft state for error when upload draft fails with unretryable error`() = runTest {
        // Given
        val userId = UserIdSample.Primary
        val messageId = MessageIdSample.LocalDraft
        val sendingError: SendingError? = null
        givenInputData(userId, messageId)
        givenUploadDraftFailsWithUnretryableError(userId, messageId)
        givenUpdateDraftStateForErrorSucceeds(userId, messageId, sendingError)

        // When
        uploadDraftWorker.doWork()

        // Then
        coVerify { updateDraftStateForError(userId, messageId, DraftSyncState.ErrorUploadDraft, sendingError) }
    }

    @Test
    fun `worker returns a retry result when upload draft fails with retryable error`() = runTest {
        // Given
        val userId = UserIdSample.Primary
        val messageId = MessageIdSample.LocalDraft
        val sendingError: SendingError? = null
        givenInputData(userId, messageId)
        givenUploadDraftFailsWithRetryableError(userId, messageId)
        givenUpdateDraftStateForErrorSucceeds(userId, messageId, sendingError)

        // When
        val actual = uploadDraftWorker.doWork()

        // Then
        assertEquals(Result.retry(), actual)
    }

    @Test
    fun `worker fails and updates draft state passing sending error when failure is Message already sent`() = runTest {
        // Given
        val userId = UserIdSample.Primary
        val messageId = MessageIdSample.LocalDraft
        val sendingError = SendingError.MessageAlreadySent
        givenInputData(userId, messageId)
        givenUploadDraftFailsWithMessageAlreadySentError(userId, messageId)
        givenUpdateDraftStateForErrorSucceeds(userId, messageId, sendingError)

        // When
        uploadDraftWorker.doWork()

        // Then
        coVerify { updateDraftStateForError(userId, messageId, DraftSyncState.ErrorUploadDraft, sendingError) }
    }

    private fun givenUploadDraftSucceeds(userId: UserId, messageId: MessageId) {
        coEvery { uploadDraft(userId, messageId) } returns Unit.right()
    }

    private fun givenUploadDraftFailsWithUnretryableError(userId: UserId, messageId: MessageId) {
        coEvery { uploadDraft(userId, messageId) } returns DataError.Remote.Http(NetworkError.Forbidden).left()
    }

    private fun givenUploadDraftFailsWithRetryableError(userId: UserId, messageId: MessageId) {
        coEvery {
            uploadDraft(userId, messageId)
        } returns DataError.Remote.Http(NetworkError.ServerError, isRetryable = true).left()
    }

    private fun givenUploadDraftFailsWithMessageAlreadySentError(userId: UserId, messageId: MessageId) {
        coEvery {
            uploadDraft(userId, messageId)
        } returns DataError.Remote.Proton(ProtonError.MessageUpdateDraftNotDraft, null).left()
    }

    private fun givenInputData(userId: UserId?, messageId: MessageId?) {
        every { workerParameters.inputData.getString(UploadDraftWorker.RawUserIdKey) } returns userId?.id
        every { workerParameters.inputData.getString(UploadDraftWorker.RawMessageIdKey) } returns messageId?.id
    }

    private fun givenUpdateDraftStateForErrorSucceeds(
        userId: UserId,
        messageId: MessageId,
        sendingError: SendingError?
    ) {
        coJustRun { updateDraftStateForError(userId, messageId, DraftSyncState.ErrorUploadDraft, sendingError) }
    }
}
