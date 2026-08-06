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
import ch.protonmail.android.mailcontentsearch.domain.usecase.IsContentSearchFeatureEnabled
import ch.protonmail.android.mailcontentsearch.domain.usecase.StartContentIndexing
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * Starts the Rust indexing orchestrator on every foreground transition, and puts a foreground-service
 * worker behind it only when it reports work to do.
 *
 * All the work is on `onStart`. By the time `onStop` runs the worker is already holding the service,
 * and stopping the orchestrator there would undo exactly the case this exists for - indexing while
 * the app is backgrounded.
 */
class ContentIndexingLifecycleObserver @Inject constructor(
    private val isContentSearchFeatureEnabled: IsContentSearchFeatureEnabled,
    private val startContentIndexing: StartContentIndexing,
    private val workScheduler: ContentIndexingWorkScheduler,
    @AppScope private val appScope: CoroutineScope
) : DefaultLifecycleObserver {

    override fun onStart(owner: LifecycleOwner) {
        // App-scoped rather than lifecycle-scoped: start() is an actor round-trip and enqueuing the
        // worker must not be dropped if the user leaves the app again straight away.
        appScope.launch {
            if (!isContentSearchFeatureEnabled()) return@launch

            // Pausing from the notification holds only until the user is back: starting again here is
            // what ends it. Opting out for good is the settings toggle.
            startContentIndexing().fold(
                ifLeft = { Timber.w("content-search: could not start indexing: $it") },
                ifRight = { summary ->
                    if (summary.hasWorkPending) {
                        // Enqueued now, while still foregrounded: WorkManager will not let a
                        // background app promote a worker to a foreground service.
                        workScheduler.enqueueIfWorkPending()
                    } else {
                        Timber.d("content-search: nothing pending, not enqueuing a worker")
                    }
                }
            )
        }
    }
}
