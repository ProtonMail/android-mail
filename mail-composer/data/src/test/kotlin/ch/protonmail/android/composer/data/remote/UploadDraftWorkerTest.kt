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
import ch.protonmail.android.composer.data.usecase.UploadDraft
import ch.protonmail.android.mailcommon.data.worker.Enqueuer
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcommon.domain.model.NetworkError
import ch.protonmail.android.mailcommon.domain.sample.UserIdSample
import ch.protonmail.android.mailcomposer.domain.model.DraftSyncState
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

class UploadDraftWorkerTest {

    private val workManager: WorkManager = mockk {
        coEvery { enqueue(any<OneTimeWorkRequest>()) } returns mockk()
    }
    private val parameters: WorkerParameters = mockk {
        every { this@mockk.getTaskExecutor() } returns mockk(relaxed = true)
    }
    private val context: Context = mockk()
    private val uploadDraft: UploadDraft = mockk()
    private val updateDraftStateForError: UpdateDraftStateForError = mockk()

    private val uploadDraftWorker = UploadDraftWorker(
        context,
        parameters,
        uploadDraft,
        updateDraftStateForError
    )

    @Test
    fun `worker is enqueued with given parameters`() {
        // Given
        val userId = UserIdSample.Primary
        val messageId = MessageIdSample.LocalDraft
        givenInputData(userId, messageId)

        // When
        Enqueuer(workManager).enqueue<UploadDraftWorker>(userId, UploadDraftWorker.params(userId, messageId))

        // Then
        val requestSlot = slot<OneTimeWorkRequest>()
        verify { workManager.enqueue(capture(requestSlot)) }
        val workSpec = requestSlot.captured.workSpec
        val constraints = workSpec.constraints
        val inputData = workSpec.input
        val actualUserId = inputData.getString(UploadDraftWorker.RawUserIdKey)
        val actualMessageIds = inputData.getString(UploadDraftWorker.RawMessageIdKey)
        assertEquals(userId.id, actualUserId)
        assertEquals(messageId.id, actualMessageIds)
        assertEquals(NetworkType.CONNECTED, constraints.requiredNetworkType)
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
        givenInputData(userId, messageId)
        givenUploadDraftFailsWithUnretryableError(userId, messageId)
        givenUpdateDraftStateForErrorSucceeds(userId, messageId)

        // When
        uploadDraftWorker.doWork()

        // Then
        coVerify { updateDraftStateForError(userId, messageId, DraftSyncState.ErrorUploadDraft) }
    }

    @Test
    fun `worker returns a retry result when upload draft fails with retryable error`() = runTest {
        // Given
        val userId = UserIdSample.Primary
        val messageId = MessageIdSample.LocalDraft
        givenInputData(userId, messageId)
        givenUploadDraftFailsWithRetryableError(userId, messageId)
        givenUpdateDraftStateForErrorSucceeds(userId, messageId)

        // When
        val actual = uploadDraftWorker.doWork()

        // Then
        assertEquals(Result.retry(), actual)
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

    private fun givenInputData(userId: UserId?, messageId: MessageId?) {
        every { parameters.inputData.getString(UploadDraftWorker.RawUserIdKey) } returns userId?.id
        every { parameters.inputData.getString(UploadDraftWorker.RawMessageIdKey) } returns messageId?.id
    }

    private fun givenUpdateDraftStateForErrorSucceeds(userId: UserId, messageId: MessageId) {
        coJustRun { updateDraftStateForError(userId, messageId, DraftSyncState.ErrorUploadDraft) }
    }
}
