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

package ch.protonmail.android.mailsession.data.background

import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import ch.protonmail.android.mailcommon.data.worker.CancelWorkManagerWork
import ch.protonmail.android.mailcommon.data.worker.Enqueuer
import ch.protonmail.android.mailfeatureflags.domain.model.FeatureFlag
import io.mockk.called
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

internal class BackgroundExecutionWorkSchedulerTest {

    private val enqueuer = mockk<Enqueuer>()
    private val cancelWorkManagerWork = mockk<CancelWorkManagerWork>()
    private val bgProcessingNewConstraintEnabled = mockk<FeatureFlag<Boolean>>()
    private val backgroundScheduler = BackgroundExecutionWorkScheduler(
        enqueuer,
        cancelWorkManagerWork,
        bgProcessingNewConstraintEnabled
    )

    @Test
    fun `should enqueue periodic work requiring battery not low when FF is off`() = runTest {
        // Given
        coEvery { bgProcessingNewConstraintEnabled.get() } returns false
        coEvery {
            enqueuer.enqueueUniquePeriodicWork(
                workerId = any(),
                tag = BACKGROUND_WORK_TAG,
                worker = BackgroundExecutionWorker::class.java,
                constraints = any(),
                existingPeriodicWorkPolicy = ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE
            )
        } just runs

        // When
        backgroundScheduler.scheduleWork()

        // Then
        coVerify(exactly = 1) {
            enqueuer.enqueueUniquePeriodicWork(
                workerId = DEFAULT_WORKER_ID,
                tag = BACKGROUND_WORK_TAG,
                worker = BackgroundExecutionWorker::class.java,
                constraints = expectedConstraints(requiresBatteryNotLow = true),
                existingPeriodicWorkPolicy = ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE
            )

            cancelWorkManagerWork wasNot called
        }
    }

    @Test
    fun `should enqueue periodic work ignoring battery not low when FF is on`() = runTest {
        // Given
        coEvery { bgProcessingNewConstraintEnabled.get() } returns true
        coEvery {
            enqueuer.enqueueUniquePeriodicWork(
                workerId = any(),
                tag = BACKGROUND_WORK_TAG,
                worker = BackgroundExecutionWorker::class.java,
                constraints = any(),
                existingPeriodicWorkPolicy = ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE
            )
        } just runs

        // When
        backgroundScheduler.scheduleWork()

        // Then
        coVerify(exactly = 1) {
            enqueuer.enqueueUniquePeriodicWork(
                workerId = DEFAULT_WORKER_ID,
                tag = BACKGROUND_WORK_TAG,
                worker = BackgroundExecutionWorker::class.java,
                constraints = expectedConstraints(requiresBatteryNotLow = false),
                existingPeriodicWorkPolicy = ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE
            )

            cancelWorkManagerWork wasNot called
        }
    }

    @Test
    fun `should cancel periodic work when requested`() = runTest {
        // Given
        coEvery { cancelWorkManagerWork.cancelAllWorkByTag(any()) } just runs

        // When
        backgroundScheduler.cancelPendingWork()

        // Then
        coVerify(exactly = 1) {
            enqueuer wasNot called
            cancelWorkManagerWork.cancelAllWorkByTag(BACKGROUND_WORK_TAG)
        }
    }

    private fun expectedConstraints(requiresBatteryNotLow: Boolean): Constraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .setRequiresBatteryNotLow(requiresBatteryNotLow)
        .build()

    private companion object {

        const val BACKGROUND_WORK_TAG = "background_work_execution"
        const val DEFAULT_WORKER_ID = "background_work_execution_task"
    }
}
