/*
 * Copyright (c) 2026 Proton Technologies AG
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
import ch.protonmail.android.mailupselling.domain.model.FallPromoPhase
import ch.protonmail.android.mailupselling.domain.model.UpsellingEntryPoint
import ch.protonmail.android.mailupselling.domain.repository.FallPromoRepository
import ch.protonmail.android.mailupselling.presentation.model.UpsellingVisibility
import ch.protonmail.android.mailupselling.presentation.model.fallpromo.FallPromoModalState
import ch.protonmail.android.mailupselling.presentation.usecase.FallPromoModalTrigger
import ch.protonmail.android.mailupselling.presentation.usecase.ObserveUpsellingVisibility
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FallPromoModalUpsellViewModel @Inject constructor(
    observeUpsellingVisibility: ObserveUpsellingVisibility,
    private val fallPromoRepository: FallPromoRepository,
    private val fallPromoModalTrigger: FallPromoModalTrigger
) : ViewModel() {

    val state = observeUpsellingVisibility(UpsellingEntryPoint.Feature.Navbar)
        .flatMapLatest { visibility ->
            if (visibility !is UpsellingVisibility.Promotional.FallPromo) {
                flowOf(FallPromoModalState.NotRequired)
            } else {
                fallPromoModalTrigger.observe().map { phaseToShow ->
                    when (phaseToShow) {
                        FallPromoPhase.None -> FallPromoModalState.NotRequired
                        FallPromoPhase.Active.Wave1 ->
                            FallPromoModalState.Show(UpsellingVisibility.Promotional.FallPromo.Wave1)

                        FallPromoPhase.Active.Wave2 ->
                            FallPromoModalState.Show(UpsellingVisibility.Promotional.FallPromo.Wave2)
                    }
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis),
            initialValue = FallPromoModalState.Loading
        )

    fun saveModalSeenTimestamp(visibility: UpsellingVisibility.Promotional.FallPromo) {
        viewModelScope.launch {
            fallPromoRepository.saveSeen(visibility.toPhase())
        }
    }

    private fun UpsellingVisibility.Promotional.FallPromo.toPhase() = when (this) {
        UpsellingVisibility.Promotional.FallPromo.Wave1 -> FallPromoPhase.Active.Wave1
        UpsellingVisibility.Promotional.FallPromo.Wave2 -> FallPromoPhase.Active.Wave2
    }
}
