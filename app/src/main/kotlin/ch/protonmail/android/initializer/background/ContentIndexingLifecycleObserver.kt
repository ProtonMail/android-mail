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

package ch.protonmail.android.initializer.background

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import ch.protonmail.android.mailcommon.domain.coroutines.AppScope
import ch.protonmail.android.mailcontentsearch.data.background.ContentIndexingWorkScheduler
import ch.protonmail.android.mailcontentsearch.domain.model.ContentIndexingActivity
import ch.protonmail.android.mailcontentsearch.domain.usecase.IsContentSearchFeatureEnabled
import ch.protonmail.android.mailcontentsearch.domain.usecase.ObserveContentIndexingActivity
import ch.protonmail.android.mailcontentsearch.domain.usecase.StartContentIndexing
import ch.protonmail.android.mailsession.domain.usecase.ObservePrimaryUserId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject

/**
 * Starts the Rust indexing orchestrator on every foreground transition, and puts a foreground-service
 * worker behind it whenever it has something to drive.
 *
 * Stopping the orchestrator is deliberately not done on `onStop`: that would undo exactly the case
 * this exists for - indexing while the app is backgrounded.
 */
class ContentIndexingLifecycleObserver @Inject constructor(
    private val observePrimaryUserId: ObservePrimaryUserId,
    private val isContentSearchFeatureEnabled: IsContentSearchFeatureEnabled,
    private val startContentIndexing: StartContentIndexing,
    private val observeContentIndexingActivity: ObserveContentIndexingActivity,
    private val workScheduler: ContentIndexingWorkScheduler,
    @AppScope private val appScope: CoroutineScope
) : DefaultLifecycleObserver {

    // Atomic because the two ends of its life are on different threads: it is set from the app-scoped
    // coroutine `onStart` launches, and cleared from the main thread in `onStop`. Swapped rather than
    // assigned, so that two `onStart` coroutines in flight at once - the round-trip below is long
    // enough for a second foreground transition to overtake it - cannot leave the first one's watch
    // collecting with nobody holding a handle to cancel it.
    private val activityWatch = AtomicReference<Job?>(null)

    /**
     * Held for the same reason as [activityWatch], one step earlier: the account this needs is waited
     * for, so a foreground episode spent signed out leaves the wait parked. Without a handle, every
     * trip to the login screen would leave another one behind, and they would all resume together on
     * the next sign-in - each starting the orchestrator and enqueuing a worker of its own.
     */
    private val indexingStart = AtomicReference<Job?>(null)

    @Volatile
    private var isForegrounded = false

    override fun onStart(owner: LifecycleOwner) {
        isForegrounded = true
        // App-scoped rather than lifecycle-scoped: start() is an actor round-trip and enqueuing the
        // worker must not be dropped if the user leaves the app again straight away.
        val start = appScope.launch {
            // The SDK answers availability from a user session, so there is nobody to ask until an
            // account is signed in. Waited for rather than sampled: the primary account is not
            // resolved yet on a cold start, and a login happens with the app already foregrounded -
            // in both cases the next foreground transition would otherwise be the first chance to
            // start indexing.
            val userId = observePrimaryUserId().filterNotNull().first()
            // The wait can outlast the foreground episode that opened it, and `onStop` cancelling it
            // is not enough on its own: it can already have resumed by then. A worker enqueued from
            // the background cannot be promoted to a foreground service anyway.
            if (!isForegrounded) return@launch
            if (!isContentSearchFeatureEnabled(userId)) return@launch

            // Uncancellable from here on, so that cancelling a wait cannot also drop a start already
            // under way - that is the round-trip this is app-scoped for in the first place.
            withContext(NonCancellable) { startIndexing() }
        }
        indexingStart.getAndSet(start)?.cancel()
    }

    override fun onStop(owner: LifecycleOwner) {
        isForegrounded = false
        indexingStart.getAndSet(null)?.cancel()
        stopWatching()
    }

    private suspend fun startIndexing() {
        // Pausing from the notification holds only until the user is back: starting again here is
        // what ends it. Opting out for good is the settings toggle.
        startContentIndexing().fold(
            ifLeft = { Timber.w("content-search: could not start indexing: $it") },
            ifRight = { summary ->
                if (!summary.hasWorkPending) {
                    Timber.d("content-search: nothing pending, the worker will exit early")
                }
                // Enqueued whatever the summary said, and now, while still foregrounded:
                // WorkManager will not let a background app promote a worker to a foreground
                // service, so a worker started once work turns up would be too late to protect it.
                workScheduler.ensureWorkerRunning()
                watchForIndexingToStart()
            }
        )
    }

    private fun stopWatching() {
        activityWatch.getAndSet(null)?.cancel()
    }

    /**
     * Covers work that appears after the summary was taken - a new account signing in, above all.
     * The summary is a single sample at foreground time, and the orchestrator picks up such an
     * account on its own, so without this the first the app hears of it is the next foreground
     * transition, by which point it has been indexing unprotected for the whole episode.
     *
     * Only worth doing while the app is on screen, because a worker enqueued from the background
     * cannot be promoted to a foreground service anyway - hence the cancel in `onStop`.
     */
    private fun watchForIndexingToStart() {
        val watch = appScope.launch {
            observeContentIndexingActivity()
                .map { it is ContentIndexingActivity.Progress }
                // Progress arrives every batch; the transition into it is the only interesting part.
                .distinctUntilChanged()
                .filter { it }
                .collect {
                    // Usually a no-op: the worker enqueued above is the one reporting this. It is
                    // not when work appeared after the summary, or when a worker has since exited
                    // on its idle timeout, and the scheduler is the one that can tell the
                    // difference - hence the log here rather than around the call.
                    Timber.d("content-search: indexing progressing, checking there is a worker behind it")
                    workScheduler.ensureWorkerRunning()
                }
        }
        activityWatch.getAndSet(watch)?.cancel()
        // `onStart` is app-scoped, so it can land after the app has already left the screen. Paired
        // with the volatile write in `onStop`, so whichever of the two goes second sees the other:
        // either this read finds `isForegrounded` false, or `onStop` finds the watch to cancel.
        if (!isForegrounded) {
            activityWatch.compareAndSet(watch, null)
            watch.cancel()
        }
    }
}
