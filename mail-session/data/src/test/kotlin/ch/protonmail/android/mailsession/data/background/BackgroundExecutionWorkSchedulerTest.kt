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

import androidx.work.ExistingWorkPolicy
import ch.protonmail.android.mailcommon.data.worker.CancelWorkManagerWork
import ch.protonmail.android.mailcommon.data.worker.Enqueuer
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

internal class BackgroundExecutionWorkSchedulerTest {

    private val enqueuer = mockk<Enqueuer>()
    private val cancelWorkManagerWork = mockk<CancelWorkManagerWork>()
    private val backgroundScheduler = BackgroundExecutionWorkScheduler(
        enqueuer,
        cancelWorkManagerWork
    )

    @Test
    fun `should enqueue scheduler worker with replace policy`() {
        // Given
        every {
            enqueuer.enqueueUniqueWork(
                workerId = SCHEDULER_WORKER_ID,
                worker = ScheduleBackgroundExecutionWorker::class.java,
                existingWorkPolicy = ExistingWorkPolicy.REPLACE
            )
        } just runs

        // When
        backgroundScheduler.scheduleWork()

        // Then
        verify(exactly = 1) {
            enqueuer.enqueueUniqueWork(
                workerId = SCHEDULER_WORKER_ID,
                worker = ScheduleBackgroundExecutionWorker::class.java,
                existingWorkPolicy = ExistingWorkPolicy.REPLACE
            )
        }
    }

    @Test
    fun `should cancel both scheduler and periodic work when requested`() = runTest {
        // Given
        every { enqueuer.cancelWork(SCHEDULER_WORKER_ID) } just runs
        coEvery { cancelWorkManagerWork.cancelAllWorkByTag(BACKGROUND_WORK_TAG) } just runs

        // When
        backgroundScheduler.cancelPendingWork()

        // Then
        verify(exactly = 1) { enqueuer.cancelWork(SCHEDULER_WORKER_ID) }
        coVerify(exactly = 1) { cancelWorkManagerWork.cancelAllWorkByTag(BACKGROUND_WORK_TAG) }
    }

    private companion object {

        const val BACKGROUND_WORK_TAG = "background_work_execution"
        const val SCHEDULER_WORKER_ID = "schedule_background_execution"
    }
}
