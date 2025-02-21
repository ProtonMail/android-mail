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

package ch.protonmail.android.mailupselling.presentation.model.onboarding

import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailupselling.domain.model.UpsellingEntryPoint
import ch.protonmail.android.mailupselling.presentation.model.dynamicplans.DynamicPlanInstanceUiModel
import me.proton.core.domain.entity.UserId
import me.proton.core.plan.domain.entity.DynamicPlans

interface OnboardingUpsellState {

    object Loading : OnboardingUpsellState

    data class Data(
        val planSwitcherUiModel: OnboardingUpsellPlanSwitcherUiModel,
        val planUiModels: OnboardingUpsellPlanUiModels,
        val buttonsUiModel: OnboardingUpsellButtonsUiModel,
        val selectedPayButtonPlanUiModel: DynamicPlanInstanceUiModel?
    ) : OnboardingUpsellState

    data class Error(val error: Effect<TextUiModel>) : OnboardingUpsellState
    data object UnsupportedFlow : OnboardingUpsellState

    sealed interface OnboardingUpsellOperation {

        sealed interface OnboardingUpsellEvent : OnboardingUpsellOperation {

            data class DataLoaded(
                val userId: UserId,
                val dynamicPlans: DynamicPlans,
                val upsellingEntryPoint: UpsellingEntryPoint = UpsellingEntryPoint.PostOnboarding
            ) : OnboardingUpsellEvent

            sealed interface LoadingError : OnboardingUpsellEvent {
                data object NoUserId : LoadingError
            }

            data class PlanSelected(val planUiModel: DynamicPlanInstanceUiModel?) : OnboardingUpsellEvent

            sealed interface UnsupportedFlow : OnboardingUpsellEvent {
                data object NoSubscriptions : UnsupportedFlow
                data object NotEnabled : UnsupportedFlow
                data object PaidUser : UnsupportedFlow
                data object PlansMismatch : UnsupportedFlow
            }
        }
    }
}
