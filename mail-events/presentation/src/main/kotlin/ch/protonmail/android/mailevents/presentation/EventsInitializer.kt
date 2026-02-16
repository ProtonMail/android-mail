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

import ch.protonmail.android.mailevents.domain.usecase.TrackInstallEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Initializer for tracking app events called during app startup.
 *
 * App open events are tracked separately via [AppOpenLifecycleObserver].
 */
@Singleton
class EventsInitializer @Inject constructor(
    private val trackInstallEvent: TrackInstallEvent,
    private val accountEventObserver: AccountEventObserver,
    private val appEventObserver: AppEventObserver
) {

    fun initialize(scope: CoroutineScope, isReinstall: Boolean = false) {
        scope.launch {
            trackInstallEvent(isReinstall = isReinstall).onLeft { error ->
                Timber.e("Failed to track install event: $error")
            }
        }
        accountEventObserver.start(scope)
        appEventObserver.start(scope)
    }
}
