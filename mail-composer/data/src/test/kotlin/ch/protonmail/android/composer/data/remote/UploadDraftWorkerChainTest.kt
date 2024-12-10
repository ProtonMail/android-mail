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

import java.util.UUID
import android.content.Context
import androidx.work.ListenableWorker.Result
import androidx.work.OneTimeWorkRequest
import androidx.work.Operation
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import arrow.core.right
import ch.protonmail.android.composer.data.extension.awaitCompletion
import ch.protonmail.android.composer.data.usecase.UploadDraft
import ch.protonmail.android.mailcommon.domain.sample.UserIdSample
import ch.protonmail.android.mailcomposer.domain.usecase.UpdateDraftStateForError
import ch.protonmail.android.mailmessage.domain.model.MessageId
import com.google.common.util.concurrent.ListenableFuture
import io.mockk.called
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.assertEquals

@RunWith(Parameterized::class)
internal class UploadDraftWorkerChainTest(
    @Suppress("UNUSED_PARAMETER") testName: String,
    private val testInput: TestInput
) {

    private val workManager: WorkManager = mockk {
        coEvery { enqueueUniqueWork(any(), any(), any<OneTimeWorkRequest>()) } returns mockk()
    }
    private val workerParameters: WorkerParameters = mockk {
        every { this@mockk.taskExecutor } returns mockk(relaxed = true)
        every { this@mockk.tags } returns emptySet()
    }
    private val context: Context = mockk()
    private val uploadDraft: UploadDraft = mockk {
        coEvery { this@mockk(any(), any()) } returns Unit.right()
    }
    private val updateDraftStateForError: UpdateDraftStateForError = mockk()

    private val listenableFutureMock = mockk<ListenableFuture<MutableList<WorkInfo>?>>()

    private val uploadDraftWorker = UploadDraftWorker(
        context,
        workerParameters,
        workManager,
        uploadDraft,
        updateDraftStateForError
    )

    @BeforeTest
    fun setup() {
        mockkStatic("ch.protonmail.android.composer.data.extension.ListenableFutureExtensionsKt")
    }

    @AfterTest
    fun teardown() {
        mockkStatic("ch.protonmail.android.composer.data.extension.ListenableFutureExtensionsKt")
    }

    @Test
    fun test() = runTest {
        val userId = UserIdSample.Primary
        val messageId = MessageId("messageId")
        val uploadWorkerId = UploadDraftWorker.id(messageId)
        val sendUploadWorkerId = UploadDraftWorker.sendId(messageId)

        every { workerParameters.tags.contains(sendUploadWorkerId) } returns true
        coEvery { workManager.cancelUniqueWork(uploadWorkerId) } returns mockk {
            every { result.get() } returns Operation.SUCCESS
            every { result.isDone } returns true
        }
        coEvery { workManager.getWorkInfosForUniqueWork(uploadWorkerId) } returns listenableFutureMock

        givenInputData(userId, messageId)
        givenExistingUploadWorkWithState(testInput.existingState)

        // When
        val actual = uploadDraftWorker.doWork()

        // Then
        if (testInput.shouldCancelExistingWork) {
            verify(exactly = 1) { workManager.cancelUniqueWork(uploadWorkerId) }
        } else {
            verify(exactly = 0) { workManager.cancelUniqueWork(any()) }
        }

        if (testInput.shouldRunImmediately) {
            coVerify(exactly = 1) { uploadDraft(userId, messageId) }
        } else {
            verify { uploadDraft wasNot called }
        }

        verify(exactly = 1) { workManager.getWorkInfosForUniqueWork(uploadWorkerId) }

        confirmVerified(workManager, uploadDraft)
        assertEquals(testInput.expectedResult, actual)
    }

    private fun givenExistingUploadWorkWithState(state: WorkInfo.State?) {
        val expectedList = state?.let {
            mutableListOf(
                WorkInfo(id = UUID.randomUUID(), state = state, setOf())
            )
        } ?: mutableListOf()

        coEvery {
            listenableFutureMock.awaitCompletion()
        } returns expectedList
    }

    private fun givenInputData(userId: UserId?, messageId: MessageId?) {
        every { workerParameters.inputData.getString(UploadDraftWorker.RawUserIdKey) } returns userId?.id
        every { workerParameters.inputData.getString(UploadDraftWorker.RawMessageIdKey) } returns messageId?.id
    }

    companion object {

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data() = arrayOf(
            TestInput(
                testName = "should cancel enqueued upload draft job and return retry",
                existingState = WorkInfo.State.ENQUEUED,
                shouldCancelExistingWork = true,
                shouldRunImmediately = false,
                expectedResult = Result.retry()
            ),
            TestInput(
                testName = "should cancel blocked upload draft job and return retry",
                existingState = WorkInfo.State.BLOCKED,
                shouldCancelExistingWork = true,
                shouldRunImmediately = false,
                expectedResult = Result.retry()
            ),
            TestInput(
                testName = "should not cancel running upload draft job and return retry",
                existingState = WorkInfo.State.RUNNING,
                shouldCancelExistingWork = false,
                shouldRunImmediately = false,
                expectedResult = Result.retry()
            ),
            TestInput(
                testName = "should not cancel running upload draft job and run normally",
                existingState = WorkInfo.State.SUCCEEDED,
                shouldCancelExistingWork = false,
                shouldRunImmediately = true,
                expectedResult = Result.success()
            ),

            TestInput(
                testName = "should not cancel failed upload draft job and run normally",
                existingState = WorkInfo.State.FAILED,
                shouldCancelExistingWork = false,
                shouldRunImmediately = true,
                expectedResult = Result.success()
            ),
            TestInput(
                testName = "should not cancel cancelled upload draft job and run normally",
                existingState = WorkInfo.State.CANCELLED,
                shouldCancelExistingWork = false,
                shouldRunImmediately = true,
                expectedResult = Result.success()
            ),
            TestInput(
                testName = "should not call cancel if work info is null and run immediately",
                existingState = null,
                shouldCancelExistingWork = false,
                shouldRunImmediately = true,
                expectedResult = Result.success()
            )
        ).map { arrayOf(it.testName, it) }

        data class TestInput(
            val testName: String,
            val existingState: WorkInfo.State?,
            val shouldCancelExistingWork: Boolean,
            val shouldRunImmediately: Boolean,
            val expectedResult: Result
        )
    }
}
