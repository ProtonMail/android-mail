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

package ch.protonmail.android.mailupselling.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.protonmail.android.mailcommon.domain.usecase.ObservePrimaryUser
import ch.protonmail.android.mailupselling.presentation.model.OnboardingUpsellState
import ch.protonmail.android.mailupselling.presentation.model.OnboardingUpsellState.Loading
import ch.protonmail.android.mailupselling.presentation.model.OnboardingUpsellState.OnboardingUpsellOperation.OnboardingUpsellEvent
import ch.protonmail.android.mailupselling.presentation.reducer.OnboardingUpsellReducer
import ch.protonmail.android.mailupselling.presentation.ui.onboarding.PlansType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.update
import me.proton.core.plan.domain.usecase.GetDynamicPlansAdjustedPrices
import javax.inject.Inject

@HiltViewModel
class OnboardingUpsellViewModel @Inject constructor(
    observePrimaryUser: ObservePrimaryUser,
    private val getDynamicPlansAdjustedPrices: GetDynamicPlansAdjustedPrices,
    private val onboardingUpsellReducer: OnboardingUpsellReducer
) : ViewModel() {

    private val mutableState = MutableStateFlow<OnboardingUpsellState>(Loading)
    val state = mutableState.asStateFlow()

    init {
        observePrimaryUser().mapLatest { user ->
            val userId = user?.userId
                ?: return@mapLatest emitNewStateFrom(OnboardingUpsellEvent.LoadingError.NoUserId)

            val dynamicPlans = runCatching { getDynamicPlansAdjustedPrices(userId) }.getOrElse {
                return@mapLatest emitNewStateFrom(OnboardingUpsellEvent.LoadingError.NoSubscriptions)
            }

            emitNewStateFrom(OnboardingUpsellEvent.DataLoaded(userId, dynamicPlans))
        }.launchIn(viewModelScope)
    }

    private fun emitNewStateFrom(operation: OnboardingUpsellState.OnboardingUpsellOperation) {
        val currentState = state.value
        mutableState.update { onboardingUpsellReducer.newStateFrom(currentState, operation) }
    }

    fun handlePlanSelected(plansType: PlansType, planName: String) {
        when (state.value) {
            is OnboardingUpsellState.Data -> {
                val selectedPlanUiModel = (state.value as OnboardingUpsellState.Data).dynamicPlanInstanceUiModels.find {
                    it.name == planName && it.cycle == if (plansType == PlansType.Annual) 12 else 1
                }

                emitNewStateFrom(OnboardingUpsellEvent.PlanSelected(selectedPlanUiModel))
            }
        }
    }
}
