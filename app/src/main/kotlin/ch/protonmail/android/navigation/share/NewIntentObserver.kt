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

package ch.protonmail.android.navigation.share

import android.content.Intent
import dagger.hilt.android.scopes.ActivityRetainedScoped
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import javax.inject.Inject

@ActivityRetainedScoped
class NewIntentObserver @Inject constructor() {

    private val _intentFlow = MutableStateFlow<Intent?>(null)
    private val intentFlow: StateFlow<Intent?> = _intentFlow

    operator fun invoke(): Flow<Intent> {
        return intentFlow
            .filterIntentValues()
            .distinctUntilChanged()
    }

    fun onNewIntent(intent: Intent?) {
        _intentFlow.value = intent
    }

    /** Drops [intent] once handled, so a screen that starts observing later does not handle it again. */
    fun consume(intent: Intent) {
        _intentFlow.compareAndSet(intent, null)
    }

    private fun StateFlow<Intent?>.filterIntentValues() = this.filterNotNull().filter { intent ->
        val availableIntentActions = buildSet {
            // Standard intents (launch, notifications)
            addAll(listOf(Intent.ACTION_MAIN, Intent.ACTION_VIEW))

            // Share via (Send) intents
            addAll(listOf(Intent.ACTION_SEND, Intent.ACTION_SEND_MULTIPLE, Intent.ACTION_SENDTO))
        }

        intent.action in availableIntentActions
    }
}
