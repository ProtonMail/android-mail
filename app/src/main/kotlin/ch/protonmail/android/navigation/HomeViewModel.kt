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
import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.navigation.model.HomeState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import me.proton.core.network.domain.NetworkManager
import me.proton.core.network.domain.NetworkStatus
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val networkManager: NetworkManager
) : ViewModel() {

    val state: StateFlow<HomeState> = observeNetworkStatus()
        .map { networkStatus ->
            HomeState(
                networkStatusEffect = if (networkStatus == NetworkStatus.Disconnected) {
                    delay(5000)
                    Effect.of(networkManager.networkStatus)
                } else {
                    Effect.of(networkStatus)
                }
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = HomeState.Initial
        )

    private fun observeNetworkStatus() = networkManager.observe().distinctUntilChanged()
}
