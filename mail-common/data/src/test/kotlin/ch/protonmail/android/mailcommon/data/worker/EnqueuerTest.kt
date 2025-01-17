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

package ch.protonmail.android.mailcommon.data.worker

import java.util.UUID
import androidx.work.BackoffPolicy
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.ListenableWorker
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkInfo
import androidx.work.WorkManager
import app.cash.turbine.test
import ch.protonmail.android.mailcommon.domain.sample.UserIdSample
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EnqueuerTest {

    private val workManager = mockk<WorkManager>()

    private val enqueuer = Enqueuer(workManager)

    @Test
    fun `keep the existing enqueued work when trying to enqueue again some unique work`() = runTest {
        // Given
        val workId = "SyncDraftWork-test-message-id"
        val params = mapOf("messageId" to "test-message-id")
        givenEnqueueUniqueWorkSucceeds(workId, ExistingWorkPolicy.KEEP)

        // When
        enqueuer.enqueueUniqueWork<ListenableWorker>(TestData.UserId, workId, params)

        // Then
        val workPolicySlot = slot<ExistingWorkPolicy>()
        coVerify { workManager.enqueueUniqueWork(workId, capture(workPolicySlot), any<OneTimeWorkRequest>()) }
        assertEquals(ExistingWorkPolicy.KEEP, workPolicySlot.captured)
    }

    @Test
    fun `changes default existing work policy when explicitly requested`() = runTest {
        // Given
        val workId = "SyncDraftWork-test-message-id"
        val params = mapOf("messageId" to "test-message-id")
        val existingWorkPolicy = ExistingWorkPolicy.APPEND
        givenEnqueueUniqueWorkSucceeds(workId, existingWorkPolicy)

        // When
        enqueuer.enqueueUniqueWork<ListenableWorker>(
            TestData.UserId,
            workId,
            params,
            existingWorkPolicy = existingWorkPolicy
        )

        // Then
        val workPolicySlot = slot<ExistingWorkPolicy>()
        coVerify { workManager.enqueueUniqueWork(workId, capture(workPolicySlot), any<OneTimeWorkRequest>()) }
        assertEquals(existingWorkPolicy, workPolicySlot.captured)
    }

    @Test
    fun `expects linear backoff policy and duration when enqueuing some work`() = runTest {
        // Given
        val workId = "SyncDraftWork-test-message-id"
        val params = mapOf("messageId" to "test-message-id")
        val existingWorkPolicy = ExistingWorkPolicy.KEEP
        givenEnqueueUniqueWorkSucceeds(workId, existingWorkPolicy)

        // When
        enqueuer.enqueueUniqueWork<ListenableWorker>(
            TestData.UserId,
            workId,
            params,
            existingWorkPolicy = existingWorkPolicy
        )

        // Then
        val workRequest = slot<OneTimeWorkRequest>()
        coVerify { workManager.enqueueUniqueWork(workId, existingWorkPolicy, capture(workRequest)) }
        assertEquals(workRequest.captured.workSpec.backoffPolicy, BackoffPolicy.LINEAR)
        assertEquals(workRequest.captured.workSpec.backoffDelayDuration, 20_000L)
    }

    @Test
    fun `enqueue tags work with userId`() = runTest {
        // Given
        val params = mapOf("messageId" to "test-message-id")
        givenEnqueueWorkSucceeds()

        // When
        enqueuer.enqueue<ListenableWorker>(TestData.UserId, params)

        // Then
        val workRequestSlot = slot<OneTimeWorkRequest>()
        coVerify { workManager.enqueue(capture(workRequestSlot)) }
        assertContains(workRequestSlot.captured.tags, TestData.UserId.id)
    }

    @Test
    fun `enqueue unique work tags work with userId`() = runTest {
        // Given
        val workId = "SyncDraftWork-test-message-id"
        val params = mapOf("messageId" to "test-message-id")
        val existingWorkPolicy = ExistingWorkPolicy.APPEND
        givenEnqueueUniqueWorkSucceeds(workId, existingWorkPolicy)

        // When
        enqueuer.enqueueUniqueWork<ListenableWorker>(
            TestData.UserId,
            workId,
            params,
            existingWorkPolicy = existingWorkPolicy
        )


        // Then
        val workRequestSlot = slot<OneTimeWorkRequest>()
        coVerify { workManager.enqueueUniqueWork(workId, existingWorkPolicy, capture(workRequestSlot)) }
        assertContains(workRequestSlot.captured.tags, TestData.UserId.id)
    }

    @Test
    fun `enqueue in chain tags work with userId`() = runTest {
        // Given
        val workId = "SyncDraftWork-test-message-id"
        val params1 = mapOf("messageId" to "test-message-id")
        val params2 = mapOf("messageId" to "test-message-id")
        val existingWorkPolicy = ExistingWorkPolicy.APPEND
        givenEnqueueWorkInChainSucceeds(workId, existingWorkPolicy)

        // When
        enqueuer.enqueueInChain<ListenableWorker, ListenableWorker>(
            TestData.UserId,
            workId,
            params1,
            params2,
            existingWorkPolicy = existingWorkPolicy
        )


        // Then
        val work1RequestSlot = slot<OneTimeWorkRequest>()
        coVerify { workManager.beginUniqueWork(workId, existingWorkPolicy, capture(work1RequestSlot)) }
        assertContains(work1RequestSlot.captured.tags, TestData.UserId.id)
    }

    @Test
    fun `cancel all work cancels work by tag for the given user Id`() = runTest {
        // Given
        val userId = UserIdSample.Primary
        givenCancelWorkSucceeds(userId)

        // When
        enqueuer.cancelAllWork(userId)

        // Then
        coVerify { workManager.cancelAllWorkByTag(userId.id) }
    }

    @Test
    fun `return true when worker is enqueued`() = runTest {
        // Given
        val workId = "ClearLabelWorker-test-message-id"
        val workInfo = WorkInfo(UUID.randomUUID(), WorkInfo.State.ENQUEUED, emptySet(), Data.EMPTY)
        val expectedFlow = MutableStateFlow(listOf(workInfo))
        givenWorkManagerReturns(workId, expectedFlow)

        // When
        enqueuer.observeWorkStatusIsEnqueuedOrRunning(workId).test {
            // Then
            assertTrue { awaitItem() }
        }
    }

    @Test
    fun `return true when worker is running`() = runTest {
        // Given
        val workId = "ClearLabelWorker-test-message-id"
        val workInfo = WorkInfo(UUID.randomUUID(), WorkInfo.State.ENQUEUED, emptySet(), Data.EMPTY)
        val expectedFlow = MutableStateFlow(listOf(workInfo))
        givenWorkManagerReturns(workId, expectedFlow)

        // When
        enqueuer.observeWorkStatusIsEnqueuedOrRunning(workId).test {
            // Then
            assertTrue { awaitItem() }
        }
    }

    @Test
    fun `return false when worker is not enqueued and not running`() = runTest {
        // Given
        val workId = "ClearLabelWorker-test-message-id"
        val workInfo = WorkInfo(UUID.randomUUID(), WorkInfo.State.SUCCEEDED, emptySet(), Data.EMPTY)
        val expectedFlow = MutableStateFlow(listOf(workInfo))
        givenWorkManagerReturns(workId, expectedFlow)

        // When
        enqueuer.observeWorkStatusIsEnqueuedOrRunning(workId).test {
            // Then
            assertFalse { awaitItem() }
        }
    }

    private fun givenCancelWorkSucceeds(userId: UserId) {
        every { workManager.cancelAllWorkByTag(userId.id) } returns mockk()
    }

    private fun givenEnqueueWorkSucceeds() {
        every { workManager.enqueue(any<OneTimeWorkRequest>()) } returns mockk()
    }

    private fun givenEnqueueWorkInChainSucceeds(workId: String, existingWorkPolicy: ExistingWorkPolicy) {
        every {
            workManager.beginUniqueWork(workId, existingWorkPolicy, any<OneTimeWorkRequest>())
        } returns mockk {
            every { then(any<OneTimeWorkRequest>()) } returns mockk {
                every { enqueue() } returns mockk()
            }
        }
    }

    private fun givenEnqueueUniqueWorkSucceeds(workId: String, existingWorkPolicy: ExistingWorkPolicy) {
        every {
            workManager.enqueueUniqueWork(workId, existingWorkPolicy, any<OneTimeWorkRequest>())
        } returns mockk()
    }

    private fun givenWorkManagerReturns(workId: String, flow: Flow<List<WorkInfo>>) {
        every { workManager.getWorkInfosForUniqueWorkFlow(workId) } returns flow
    }

    companion object {
        object TestData {

            val UserId = UserIdSample.Primary
        }
    }

}
