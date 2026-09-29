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

package ch.protonmail.android.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.protonmail.android.mailpinlock.domain.AutoLockCheckPendingState
import ch.protonmail.android.mailpinlock.domain.usecase.ShouldPresentPinInsertionScreen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import javax.inject.Inject

@HiltViewModel
internal class LauncherRouterViewModel @Inject constructor(
    autoLockCheckPendingState: AutoLockCheckPendingState,
    shouldPresentPinInsertionScreen: ShouldPresentPinInsertionScreen
) : ViewModel() {

    private val showLockScreenChannel = Channel<Unit>(Channel.CONFLATED)
    val showLockScreenEvent = showLockScreenChannel.receiveAsFlow()

    init {
        // AutoLockInitializer only requests a check when the app returns from background. A new MainActivity
        // can be created while the app is still considered in foreground (e.g. relaunched right after the lock
        // screen closed the app), so each new instance requests its own check. Requested before collecting,
        // so it is conflated with a check that is already pending.
        autoLockCheckPendingState.triggerAutoLockCheck()

        shouldPresentPinInsertionScreen()
            .onEach { shouldShow ->
                if (shouldShow) {
                    showLockScreenChannel.send(Unit)
                }
            }
            .launchIn(viewModelScope)
    }
}
