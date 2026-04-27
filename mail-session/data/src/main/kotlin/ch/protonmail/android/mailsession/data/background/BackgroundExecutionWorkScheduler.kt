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
import timber.log.Timber
import javax.inject.Inject

class BackgroundExecutionWorkScheduler @Inject constructor(
    private val enqueuer: Enqueuer,
    private val cancelWorkManagerWork: CancelWorkManagerWork
) {

    fun scheduleWork() {
        enqueuer.enqueueUniqueWork(
            workerId = ScheduleBackgroundExecutionWorker.WORKER_ID,
            worker = ScheduleBackgroundExecutionWorker::class.java,
            existingWorkPolicy = ExistingWorkPolicy.REPLACE
        )

        Timber.d("Schedule background execution worker enqueued.")
    }

    suspend fun cancelPendingWork() {
        enqueuer.cancelWork(ScheduleBackgroundExecutionWorker.WORKER_ID)
        cancelWorkManagerWork.cancelAllWorkByTag(BACKGROUND_WORK_TAG)
    }

    internal companion object {

        const val BACKGROUND_WORK_TAG = "background_work_execution"
        const val WORKER_ID = "background_work_execution_task"
    }
}
