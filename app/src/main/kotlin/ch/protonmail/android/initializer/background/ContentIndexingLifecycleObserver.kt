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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * Starts the Rust indexing orchestrator on every foreground transition, and puts a foreground-service
 * worker behind it whenever it has something to drive.
 *
 * Stopping the orchestrator is deliberately not done on `onStop`: that would undo exactly the case
 * this exists for - indexing while the app is backgrounded.
 */
class ContentIndexingLifecycleObserver @Inject constructor(
    private val isContentSearchFeatureEnabled: IsContentSearchFeatureEnabled,
    private val startContentIndexing: StartContentIndexing,
    private val observeContentIndexingActivity: ObserveContentIndexingActivity,
    private val workScheduler: ContentIndexingWorkScheduler,
    @AppScope private val appScope: CoroutineScope
) : DefaultLifecycleObserver {

    private var activityWatch: Job? = null

    @Volatile
    private var isForegrounded = false

    override fun onStart(owner: LifecycleOwner) {
        isForegrounded = true
        // App-scoped rather than lifecycle-scoped: start() is an actor round-trip and enqueuing the
        // worker must not be dropped if the user leaves the app again straight away.
        appScope.launch {
            if (!isContentSearchFeatureEnabled()) return@launch

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
    }

    override fun onStop(owner: LifecycleOwner) {
        isForegrounded = false
        stopWatching()
    }

    private fun stopWatching() {
        activityWatch?.cancel()
        activityWatch = null
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
        stopWatching()
        activityWatch = appScope.launch {
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
        // `onStart` is app-scoped, so it can land after the app has already left the screen.
        if (!isForegrounded) stopWatching()
    }
}
