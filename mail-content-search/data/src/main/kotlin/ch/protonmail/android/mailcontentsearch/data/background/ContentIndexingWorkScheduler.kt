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
import ch.protonmail.android.mailcontentsearch.domain.ContentIndexingScheduler
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
) : ContentIndexingScheduler {

    /**
     * This and [restart] can both fire on one background-to-foreground trip - the lifecycle observer
     * calls this while the worker that was holding the service calls that. Both go through the same
     * [enqueue], so the two cannot disagree about policy, and the guard below usually makes the
     * observer's call a no-op against a worker that is already running.
     *
     * Usually, because the guard is a sample and not a lock: a worker that starts between the check
     * and the enqueue is replaced anyway, and so is a replacement that [restart] left sitting in
     * `ENQUEUED`. Both cost a round of churn and neither loses work - one worker is still scheduled
     * either way - which is the reason this is a cheap guard rather than a synchronised one.
     */
    override suspend fun ensureWorkerRunning() {
        // A worker that is already executing is observing the same orchestrator a new one would, so
        // replacing it would only drop and re-acquire its foreground service. Work parked in
        // `ENQUEUED` deliberately does not count - see [Enqueuer.isWorkRunning].
        if (enqueuer.isWorkRunning(WORKER_ID)) {
            Timber.d("content-search: indexing worker already running")
            return
        }
        enqueue()
        Timber.d("content-search: indexing worker enqueued")
    }

    /**
     * Replaces the running worker with a fresh one.
     *
     * The only way to give up a foreground service is to end the worker holding it, so this is how
     * the notification is dropped when the app comes back on screen.
     *
     * Not free: the worker holds its Rust background execution scope through `runInRustBackground`,
     * which releases it as the old worker unwinds, and WorkManager only starts the replacement
     * afterwards. For that window nothing holds a scope. It is a window that only opens while the app
     * is foregrounded and therefore holds one of its own, which is why this is acceptable rather than
     * merely unnoticed.
     */
    fun restart() {
        enqueue()
        Timber.d("content-search: indexing worker restarted")
    }

    /**
     * Always REPLACE.
     *
     * KEEP would be the cheaper answer - it leaves a worker that is already holding the foreground
     * service alone instead of dropping and re-acquiring it against the Android 15 dataSync budget
     * - but it also has no way to dislodge work the platform has parked. A request left sitting in
     * `ENQUEUED` behind an exhausted job quota would swallow every later enqueue silently, and
     * nothing else in the app ever retries.
     *
     * Replacing costs little: the worker drives no indexing of its own, so a replacement re-attaches
     * to a Rust run that never stopped, and starting the orchestrator again is idempotent. Not
     * nothing, though - see the scope window in [restart].
     */
    private fun enqueue() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()

        enqueuer.enqueueUniqueWork(
            workerId = WORKER_ID,
            worker = ContentIndexingWorker::class.java,
            existingWorkPolicy = ExistingWorkPolicy.REPLACE,
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
