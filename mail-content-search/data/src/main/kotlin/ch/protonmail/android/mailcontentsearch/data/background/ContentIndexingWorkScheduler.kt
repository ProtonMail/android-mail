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

package ch.protonmail.android.mailcontentsearch.data.background

import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import ch.protonmail.android.mailcommon.data.worker.Enqueuer
import ch.protonmail.android.mailcontentsearch.data.worker.ContentIndexingWorker
import timber.log.Timber
import javax.inject.Inject

/**
 * Owns the single foreground-service worker that keeps the Rust orchestrator running while the app
 * is backgrounded.
 *
 * Only network and battery constraints are set here. Whether indexing may run over a metered
 * connection is Rust's call - it holds the per-account flag and pauses its own queue - so the
 * worker asks for `CONNECTED` and lets the orchestrator decide.
 */
class ContentIndexingWorkScheduler @Inject constructor(
    private val enqueuer: Enqueuer
) {

    fun enqueueIfWorkPending() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()

        enqueuer.enqueueUniqueWork(
            workerId = WORKER_ID,
            worker = ContentIndexingWorker::class.java,
            // KEEP, not REPLACE: a worker already holding the foreground service is doing the job,
            // and replacing it would drop and re-acquire the FGS against the Android 15 dataSync budget.
            existingWorkPolicy = ExistingWorkPolicy.KEEP,
            constraints = constraints
        )

        Timber.d("content-search: indexing worker enqueued")
    }

    fun cancel() {
        enqueuer.cancelWork(WORKER_ID)
        Timber.d("content-search: indexing worker cancelled")
    }

    companion object {

        const val WORKER_ID = "content_indexing_worker"
    }
}
