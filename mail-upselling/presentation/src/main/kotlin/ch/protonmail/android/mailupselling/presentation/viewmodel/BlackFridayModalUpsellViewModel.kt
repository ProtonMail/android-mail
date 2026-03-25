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

package ch.protonmail.android.mailupselling.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.protonmail.android.design.compose.viewmodel.stopTimeoutMillis
import ch.protonmail.android.mailupselling.domain.model.BlackFridayPhase
import ch.protonmail.android.mailupselling.domain.model.UpsellingEntryPoint
import ch.protonmail.android.mailupselling.presentation.model.UpsellingVisibility
import ch.protonmail.android.mailupselling.presentation.model.blackfriday.BlackFridayModalState
import ch.protonmail.android.mailupselling.presentation.usecase.BlackFridayModalTrigger
import ch.protonmail.android.mailupselling.presentation.usecase.ObserveUpsellingVisibility
import ch.protonmail.android.mailupselling.presentation.usecase.SaveBlackFridayModalSeen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BlackFridayModalUpsellViewModel @Inject constructor(
    observeUpsellingVisibility: ObserveUpsellingVisibility,
    private val saveBlackFridayModalSeen: SaveBlackFridayModalSeen,
    private val blackFridayModalTrigger: BlackFridayModalTrigger
) : ViewModel() {

    val state = observeUpsellingVisibility(UpsellingEntryPoint.Feature.Navbar)
        .flatMapLatest { visibility ->
            if (visibility !is UpsellingVisibility.Promotional.BlackFriday) {
                flowOf(BlackFridayModalState.NotRequired)
            } else {
                blackFridayModalTrigger.observe().map { phaseToShow ->
                    when (phaseToShow) {
                        BlackFridayPhase.None -> BlackFridayModalState.NotRequired
                        BlackFridayPhase.Active.Wave1 ->
                            BlackFridayModalState.Show(UpsellingVisibility.Promotional.BlackFriday.Wave1)

                        BlackFridayPhase.Active.Wave2 ->
                            BlackFridayModalState.Show(UpsellingVisibility.Promotional.BlackFriday.Wave2)
                    }
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis),
            initialValue = BlackFridayModalState.Loading
        )

    fun recordModalSeen(visibility: UpsellingVisibility.Promotional.BlackFriday) {
        viewModelScope.launch {
            saveBlackFridayModalSeen(phase = visibility.toPhase())
        }
    }

    private fun UpsellingVisibility.Promotional.BlackFriday.toPhase() = when (this) {
        UpsellingVisibility.Promotional.BlackFriday.Wave1 -> BlackFridayPhase.Active.Wave1
        UpsellingVisibility.Promotional.BlackFriday.Wave2 -> BlackFridayPhase.Active.Wave2
    }
}
