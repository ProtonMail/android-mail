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

    /**
     * KEEP, not REPLACE: a worker already holding the foreground service is doing the job, and
     * replacing it would drop and re-acquire the FGS against the Android 15 dataSync budget.
     *
     * This and [restart] can both fire on one background-to-foreground trip - the lifecycle observer
     * calls this while the worker that was holding the service calls that - and they disagree on
     * policy, so which request survives is down to timing. Either way one worker ends up running
     * against a Rust orchestrator that does not care how many times it is asked to start, which is
     * what makes the ordering uninteresting rather than merely unlikely to bite.
     */
    fun enqueueIfWorkPending() {
        enqueue(ExistingWorkPolicy.KEEP)
        Timber.d("content-search: indexing worker enqueued")
    }

    /**
     * Replaces the running worker with a fresh one.
     *
     * The only way to give up a foreground service is to end the worker holding it, so this is how
     * the notification is dropped when the app comes back on screen. The orchestrator itself keeps
     * running - it lives in Rust and persists its progress - so the replacement re-attaches to a run
     * that never stopped.
     *
     * Not entirely free, though: the worker holds its Rust background execution scope through
     * `runInRustBackground`, which releases it as the old worker unwinds, and WorkManager only starts
     * the replacement afterwards. For that window nothing holds a scope. It is a window that only
     * opens while the app is foregrounded and therefore holds one of its own, which is why this is
     * acceptable rather than merely unnoticed.
     */
    fun restart() {
        enqueue(ExistingWorkPolicy.REPLACE)
        Timber.d("content-search: indexing worker restarted")
    }

    private fun enqueue(existingWorkPolicy: ExistingWorkPolicy) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()

        enqueuer.enqueueUniqueWork(
            workerId = WORKER_ID,
            worker = ContentIndexingWorker::class.java,
            existingWorkPolicy = existingWorkPolicy,
            constraints = constraints
        )
    }

    fun cancel() {
        enqueuer.cancelWork(WORKER_ID)
        Timber.d("content-search: indexing worker cancelled")
    }

    companion object {

        const val WORKER_ID = "content_indexing_worker"
    }
}
