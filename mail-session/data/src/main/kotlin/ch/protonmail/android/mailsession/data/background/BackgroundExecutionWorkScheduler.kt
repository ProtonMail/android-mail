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
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import ch.protonmail.android.mailcommon.data.worker.CancelWorkManagerWork
import ch.protonmail.android.mailcommon.data.worker.Enqueuer
import ch.protonmail.android.mailsession.domain.background.SendCompletionScheduler
import timber.log.Timber
import javax.inject.Inject

class BackgroundExecutionWorkScheduler @Inject constructor(
    private val enqueuer: Enqueuer,
    private val cancelWorkManagerWork: CancelWorkManagerWork,
    private val workManager: WorkManager
) : SendCompletionScheduler {

    /**
     * Runs the drain immediately and expedited, so a queued send completes even after the process is
     * reclaimed. RUN_AS_NON_EXPEDITED falls back to a regular job if the expedited quota is spent
     */
    override fun scheduleSendCompletion() {
        val request = OneTimeWorkRequestBuilder<SendMessageCompletionWorker>()
            .setConstraints(
                Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
            )
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .build()

        workManager.enqueueUniqueWork(SEND_DRAIN_WORKER_ID, ExistingWorkPolicy.KEEP, request)
    }

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
        const val SEND_DRAIN_WORKER_ID = "send_message_foreground_drain"
    }
}
