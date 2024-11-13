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

package ch.protonmail.android.mailupselling.presentation.reducer

import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailupselling.presentation.R
import ch.protonmail.android.mailupselling.presentation.mapper.OnboardingUpsellButtonsUiModelMapper
import ch.protonmail.android.mailupselling.presentation.mapper.OnboardingUpsellPlanSwitcherUiModelMapper
import ch.protonmail.android.mailupselling.presentation.mapper.OnboardingUpsellPlanUiModelsMapper
import ch.protonmail.android.mailupselling.presentation.model.onboarding.OnboardingUpsellState
import ch.protonmail.android.mailupselling.presentation.model.onboarding.OnboardingUpsellState.OnboardingUpsellOperation.OnboardingUpsellEvent
import javax.inject.Inject

class OnboardingUpsellReducer @Inject constructor(
    private val planSwitcherUiModelMapper: OnboardingUpsellPlanSwitcherUiModelMapper,
    private val planUiModelsMapper: OnboardingUpsellPlanUiModelsMapper,
    private val buttonsUiModelMapper: OnboardingUpsellButtonsUiModelMapper
) {

    fun newStateFrom(
        currentState: OnboardingUpsellState,
        operation: OnboardingUpsellState.OnboardingUpsellOperation
    ): OnboardingUpsellState {
        return when (operation) {
            is OnboardingUpsellEvent.DataLoaded -> reducePlansListToNewState(operation)
            is OnboardingUpsellEvent.LoadingError -> reduceErrorEvent(operation)
            is OnboardingUpsellEvent.PlanSelected -> reducePlanSelected(currentState, operation)
            is OnboardingUpsellEvent.UnsupportedFlow -> reduceUnsupportedFlow()
        }
    }

    private fun reducePlansListToNewState(operation: OnboardingUpsellEvent.DataLoaded): OnboardingUpsellState =
        OnboardingUpsellState.Data(
            planSwitcherUiModelMapper.toUiModel(operation.dynamicPlans),
            planUiModelsMapper.toUiModel(operation.dynamicPlans, operation.userId),
            buttonsUiModelMapper.toUiModel(operation.dynamicPlans),
            selectedPayButtonPlanUiModel = null
        )

    private fun reduceErrorEvent(event: OnboardingUpsellEvent.LoadingError) = when (event) {
        OnboardingUpsellEvent.LoadingError.NoUserId -> OnboardingUpsellState.Error(
            error = Effect.of(TextUiModel.TextRes(R.string.upselling_snackbar_error_no_user_id))
        )
    }

    private fun reducePlanSelected(
        currentState: OnboardingUpsellState,
        operation: OnboardingUpsellEvent.PlanSelected
    ): OnboardingUpsellState = when (currentState) {
        is OnboardingUpsellState.Data -> currentState.copy(
            selectedPayButtonPlanUiModel = operation.planUiModel
        )

        else -> currentState
    }

    private fun reduceUnsupportedFlow() = OnboardingUpsellState.UnsupportedFlow
}
