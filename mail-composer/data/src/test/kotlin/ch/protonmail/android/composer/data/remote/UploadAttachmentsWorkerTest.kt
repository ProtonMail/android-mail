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
import androidx.work.ListenableWorker
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.composer.data.usecase.AttachmentUploadError
import ch.protonmail.android.composer.data.usecase.UploadAttachments
import ch.protonmail.android.mailcommon.data.worker.Enqueuer
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcommon.domain.model.ProtonError
import ch.protonmail.android.mailcommon.domain.sample.UserIdSample
import ch.protonmail.android.mailmessage.domain.model.DraftSyncState
import ch.protonmail.android.mailcomposer.domain.usecase.UpdateDraftStateForError
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

class UploadAttachmentsWorkerTest {

    private val workManager: WorkManager = mockk {
        coEvery { enqueue(any<OneTimeWorkRequest>()) } returns mockk()
    }
    private val parameters: WorkerParameters = mockk {
        every { this@mockk.getTaskExecutor() } returns mockk(relaxed = true)
    }
    private val context: Context = mockk()
    private val uploadAttachments: UploadAttachments = mockk()
    private val updateDraftStateForError: UpdateDraftStateForError = mockk()

    private val uploadAttachmentWorker =
        UploadAttachmentsWorker(context, parameters, uploadAttachments, updateDraftStateForError)

    @Test
    fun `worker is enqueued with given parameters`() {
        // Given
        val userId = UserIdSample.Primary
        val messageId = MessageIdSample.LocalDraft
        givenInputData(userId, messageId)

        // When
        Enqueuer(workManager).enqueue<UploadAttachmentsWorker>(
            userId, UploadAttachmentsWorker.params(userId, messageId)
        )

        // Then
        val requestSlot = slot<OneTimeWorkRequest>()
        verify { workManager.enqueue(capture(requestSlot)) }
        val workSpec = requestSlot.captured.workSpec
        val constraints = workSpec.constraints
        val inputData = workSpec.input
        val actualUserId = inputData.getString(UploadAttachmentsWorker.RawUserIdKey)
        val actualMessageIds = inputData.getString(UploadAttachmentsWorker.RawMessageIdKey)
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
        givenUploadAttachmentsSucceeds(userId, messageId)

        // When
        val actual = uploadAttachmentWorker.doWork()

        // Then
        coVerify { uploadAttachments(userId, messageId) }
        assertEquals(ListenableWorker.Result.success(), actual)
    }

    @Test
    fun `worker fails when userid worker parameter is missing`() = runTest {
        // Given
        val userId = null
        val messageId = MessageIdSample.LocalDraft
        givenInputData(userId, messageId)

        // When - Then
        assertFailsWith<IllegalArgumentException> { uploadAttachmentWorker.doWork() }
        coVerify { uploadAttachments wasNot Called }
    }

    @Test
    fun `worker fails when messageIds worker parameter is empty`() = runTest {
        // Given
        val userId = UserIdSample.Primary
        val messageId = MessageId("")
        givenInputData(userId, messageId)

        // When - Then
        assertFailsWith<IllegalArgumentException> { uploadAttachmentWorker.doWork() }
        coVerify { uploadAttachments wasNot Called }
    }

    @Test
    fun `worker updates draft state for error when upload attachment fails`() = runTest {
        // Given
        val userId = UserIdSample.Primary
        val messageId = MessageIdSample.LocalDraft
        givenInputData(userId, messageId)
        givenUploadAttachmentsFails(
            userId, messageId,
            AttachmentUploadError.UploadFailed(
                DataError.Remote.Proton(ProtonError.UploadFailure, null)
            )
        )
        givenUpdateDraftStateForErrorSucceeds(userId, messageId)

        // When
        val actual = uploadAttachmentWorker.doWork()

        // Then
        coVerify { uploadAttachments(userId, messageId) }
        coVerify { updateDraftStateForError(userId, messageId, DraftSyncState.ErrorUploadAttachments, null) }
        assertEquals(ListenableWorker.Result.failure(), actual)
    }

    @Test
    fun `worker updates draft state for error when upload attachment fails with a message already sent error`() =
        runTest {
            // Given
            val userId = UserIdSample.Primary
            val messageId = MessageIdSample.LocalDraft
            givenInputData(userId, messageId)
            givenUploadAttachmentsFails(
                userId,
                messageId,
                AttachmentUploadError.UploadFailed(
                    DataError.Remote.Proton(ProtonError.AttachmentUploadMessageAlreadySent, null)
                )
            )
            givenUpdateDraftStateForErrorSucceeds(userId, messageId, SendingError.MessageAlreadySent)

            // When
            val actual = uploadAttachmentWorker.doWork()

            // Then
            coVerify { uploadAttachments(userId, messageId) }
            coVerify {
                updateDraftStateForError(
                    userId,
                    messageId,
                    DraftSyncState.ErrorUploadAttachments,
                    SendingError.MessageAlreadySent
                )
            }
            assertEquals(ListenableWorker.Result.failure(), actual)
        }

    private fun givenUploadAttachmentsSucceeds(userId: UserId, messageId: MessageId) {
        coEvery { uploadAttachments(userId, messageId) } returns Unit.right()
    }

    private fun givenUpdateDraftStateForErrorSucceeds(
        userId: UserId,
        messageId: MessageId,
        sendingError: SendingError? = null
    ) {
        coJustRun { updateDraftStateForError(userId, messageId, DraftSyncState.ErrorUploadAttachments, sendingError) }
    }

    private fun givenUploadAttachmentsFails(
        userId: UserId,
        messageId: MessageId,
        error: AttachmentUploadError
    ) {
        coEvery { uploadAttachments(userId, messageId) } returns error.left()
    }

    private fun givenInputData(userId: UserId?, messageId: MessageId?) {
        every { parameters.inputData.getString(UploadAttachmentsWorker.RawUserIdKey) } returns userId?.id
        every { parameters.inputData.getString(UploadAttachmentsWorker.RawMessageIdKey) } returns messageId?.id
    }
}
