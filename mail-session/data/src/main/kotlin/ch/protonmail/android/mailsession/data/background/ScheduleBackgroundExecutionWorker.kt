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

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.WorkerParameters
import ch.protonmail.android.mailcommon.data.worker.Enqueuer
import ch.protonmail.android.mailfeatureflags.domain.annotation.IsBgProcessingRelaxedBatteryConstraintEnabled
import ch.protonmail.android.mailfeatureflags.domain.model.FeatureFlag
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber

@HiltWorker
internal class ScheduleBackgroundExecutionWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val enqueuer: Enqueuer,
    @IsBgProcessingRelaxedBatteryConstraintEnabled private val bgProcessingNewConstraintEnabled: FeatureFlag<Boolean>
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = runCatching {
        val requiresBatteryNotLow = !bgProcessingNewConstraintEnabled.get()
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(requiresBatteryNotLow)
            .build()

        enqueuer.enqueueUniquePeriodicWork(
            workerId = BackgroundExecutionWorkScheduler.WORKER_ID,
            tag = BackgroundExecutionWorkScheduler.BACKGROUND_WORK_TAG,
            worker = BackgroundExecutionWorker::class.java,
            constraints = constraints,
            existingPeriodicWorkPolicy = ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE
        )

        Timber.d("Periodic work enqueued with requiresBatteryNotLow=$requiresBatteryNotLow")
        Result.success()
    }.getOrElse { throwable ->
        Timber.w(throwable, "Scheduling failed; will retry.")
        Result.retry()
    }

    internal companion object {

        const val WORKER_ID = "schedule_background_execution"
    }
}
