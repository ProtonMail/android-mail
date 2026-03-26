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

package ch.protonmail.android.mailevents.presentation

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import ch.protonmail.android.mailcommon.domain.coroutines.AppScope
import ch.protonmail.android.mailevents.domain.repository.EventsRepository
import ch.protonmail.android.mailevents.domain.usecase.TrackAppOpenEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppOpenLifecycleObserver @Inject constructor(
    private val trackAppOpenEvent: TrackAppOpenEvent,
    private val eventsRepository: EventsRepository,
    @AppScope private val scope: CoroutineScope
) : DefaultLifecycleObserver {

    @Volatile
    private var lastTrackedMs: Long? = null

    @Suppress("UnnecessaryParentheses")
    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)

        val now = kotlin.time.Clock.System.now().toEpochMilliseconds()
        val lastMs = lastTrackedMs

        scope.launch {
            // Load persistent timestamp to detect process death
            val persistentTimestamp = eventsRepository.getLastAppOpenTimestamp()

            when {
                // Warm start within 30 minutes (process alive) → throttle
                lastMs != null && (now - lastMs) < THROTTLE_DURATION_MS -> {
                    return@launch
                }

                // First open ever (pairs with install event)
                persistentTimestamp == null -> {
                    trackAppOpenEvent(isNewSession = false).onLeft { error ->
                        Timber.d("Failed to track app open event: $error")
                    }.onRight {
                        lastTrackedMs = now
                        eventsRepository.saveLastAppOpenTimestamp(now)
                    }
                }

                // Process death within 30 minutes OR cold start after 30+ minutes
                else -> {
                    trackAppOpenEvent(isNewSession = true).onLeft { error ->
                        Timber.d("Failed to track app open event: $error")
                    }.onRight {
                        lastTrackedMs = now
                        eventsRepository.saveLastAppOpenTimestamp(now)
                    }
                }
            }
        }
    }

    @VisibleForTesting
    internal fun setLastTrackedMsForTesting(timeMs: Long) {
        lastTrackedMs = timeMs
    }

    companion object {

        internal const val THROTTLE_DURATION_MS = 30L * 60 * 1000 // 30 minutes
    }
}
