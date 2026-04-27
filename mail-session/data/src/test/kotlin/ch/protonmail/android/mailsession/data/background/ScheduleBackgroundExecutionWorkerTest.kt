/*
 * Copyright (c) 2025 Proton Technologies AG
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

package ch.protonmail.android.mailsession.data.background

import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ListenableWorker
import androidx.work.NetworkType
import androidx.work.WorkerParameters
import ch.protonmail.android.mailcommon.data.worker.Enqueuer
import ch.protonmail.android.mailfeatureflags.domain.model.FeatureFlag
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

internal class ScheduleBackgroundExecutionWorkerTest {

    private val enqueuer = mockk<Enqueuer>()
    private val bgProcessingNewConstraintEnabled = mockk<FeatureFlag<Boolean>>()
    private val params = mockk<WorkerParameters>()

    private val worker = ScheduleBackgroundExecutionWorker(
        mockk(),
        params,
        enqueuer,
        bgProcessingNewConstraintEnabled
    )

    @Test
    fun `should enqueue periodic work requiring battery not low when FF is off`() = runTest {
        // Given
        coEvery { bgProcessingNewConstraintEnabled.get() } returns false
        coEvery {
            enqueuer.enqueueUniquePeriodicWork(
                workerId = WORKER_ID,
                tag = BACKGROUND_WORK_TAG,
                worker = BackgroundExecutionWorker::class.java,
                constraints = any(),
                existingPeriodicWorkPolicy = ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE
            )
        } just runs

        // When
        val result = worker.doWork()

        // Then
        assertEquals(ListenableWorker.Result.success(), result)
        coVerify(exactly = 1) {
            enqueuer.enqueueUniquePeriodicWork(
                workerId = WORKER_ID,
                tag = BACKGROUND_WORK_TAG,
                worker = BackgroundExecutionWorker::class.java,
                constraints = expectedConstraints(requiresBatteryNotLow = true),
                existingPeriodicWorkPolicy = ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE
            )
        }
    }

    @Test
    fun `should enqueue periodic work ignoring battery not low when FF is on`() = runTest {
        // Given
        coEvery { bgProcessingNewConstraintEnabled.get() } returns true
        coEvery {
            enqueuer.enqueueUniquePeriodicWork(
                workerId = WORKER_ID,
                tag = BACKGROUND_WORK_TAG,
                worker = BackgroundExecutionWorker::class.java,
                constraints = any(),
                existingPeriodicWorkPolicy = ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE
            )
        } just runs

        // When
        val result = worker.doWork()

        // Then
        assertEquals(ListenableWorker.Result.success(), result)
        coVerify(exactly = 1) {
            enqueuer.enqueueUniquePeriodicWork(
                workerId = WORKER_ID,
                tag = BACKGROUND_WORK_TAG,
                worker = BackgroundExecutionWorker::class.java,
                constraints = expectedConstraints(requiresBatteryNotLow = false),
                existingPeriodicWorkPolicy = ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE
            )
        }
    }

    @Test
    fun `should retry when feature flag lookup fails`() = runTest {
        // Given
        coEvery { bgProcessingNewConstraintEnabled.get() } throws IllegalStateException("boom")

        // When
        val result = worker.doWork()

        // Then
        assertEquals(ListenableWorker.Result.retry(), result)
    }

    private fun expectedConstraints(requiresBatteryNotLow: Boolean): Constraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .setRequiresBatteryNotLow(requiresBatteryNotLow)
        .build()

    private companion object {

        const val BACKGROUND_WORK_TAG = "background_work_execution"
        const val WORKER_ID = "background_work_execution_task"
    }
}
