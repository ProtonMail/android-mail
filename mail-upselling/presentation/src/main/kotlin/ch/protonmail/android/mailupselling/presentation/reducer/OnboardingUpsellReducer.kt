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
import ch.protonmail.android.mailupselling.presentation.mapper.DynamicPlanInstanceUiMapper
import ch.protonmail.android.mailupselling.presentation.mapper.OnboardingUpsellButtonsUiModelMapper
import ch.protonmail.android.mailupselling.presentation.mapper.OnboardingUpsellPlanSwitcherUiModelMapper
import ch.protonmail.android.mailupselling.presentation.mapper.OnboardingUpsellPlanUiModelsMapper
import ch.protonmail.android.mailupselling.presentation.model.OnboardingUpsellState
import ch.protonmail.android.mailupselling.presentation.model.OnboardingUpsellState.OnboardingUpsellOperation.OnboardingUpsellEvent
import javax.inject.Inject

class OnboardingUpsellReducer @Inject constructor(
    private val planSwitcherUiModelMapper: OnboardingUpsellPlanSwitcherUiModelMapper,
    private val planUiModelsMapper: OnboardingUpsellPlanUiModelsMapper,
    private val buttonsUiModelMapper: OnboardingUpsellButtonsUiModelMapper,
    private val dynamicPlanInstanceUiMapper: DynamicPlanInstanceUiMapper
) {

    fun newStateFrom(
        currentState: OnboardingUpsellState,
        operation: OnboardingUpsellState.OnboardingUpsellOperation
    ): OnboardingUpsellState {
        return when (operation) {
            is OnboardingUpsellEvent.DataLoaded -> reducePlansListToNewState(operation)
            is OnboardingUpsellEvent.LoadingError -> reduceErrorEvent(operation)
            is OnboardingUpsellEvent.PlanSelected -> reducePlanSelected(currentState, operation)
        }
    }

    private fun reducePlansListToNewState(operation: OnboardingUpsellEvent.DataLoaded): OnboardingUpsellState =
        OnboardingUpsellState.Data(
            planSwitcherUiModelMapper.toUiModel(operation.dynamicPlans),
            planUiModelsMapper.toUiModel(operation.dynamicPlans),
            buttonsUiModelMapper.toUiModel(operation.dynamicPlans),
            mapDynamicPlans(operation),
            selectedPlanInstanceUiModel = null
        )

    private fun mapDynamicPlans(operation: OnboardingUpsellEvent.DataLoaded) =
        operation.dynamicPlans.plans.flatMap { dynamicPlan ->
            dynamicPlan.instances.map { (_, instance) ->
                dynamicPlanInstanceUiMapper.toUiModel(
                    operation.userId,
                    instance,
                    highlighted = false,
                    discount = 0,
                    plan = dynamicPlan
                )
            }
        }

    private fun reduceErrorEvent(event: OnboardingUpsellEvent.LoadingError) = when (event) {
        OnboardingUpsellEvent.LoadingError.NoUserId -> OnboardingUpsellState.Error(
            error = Effect.of(TextUiModel.TextRes(R.string.upselling_snackbar_error_no_user_id))
        )

        OnboardingUpsellEvent.LoadingError.NoSubscriptions -> OnboardingUpsellState.Error(
            error = Effect.of(TextUiModel.TextRes(R.string.upselling_snackbar_error_no_subscriptions))
        )
    }

    private fun reducePlanSelected(
        currentState: OnboardingUpsellState,
        operation: OnboardingUpsellEvent.PlanSelected
    ): OnboardingUpsellState = when (currentState) {
        is OnboardingUpsellState.Data -> currentState.copy(
            selectedPlanInstanceUiModel = operation.planUiModel
        )

        else -> currentState
    }
}
