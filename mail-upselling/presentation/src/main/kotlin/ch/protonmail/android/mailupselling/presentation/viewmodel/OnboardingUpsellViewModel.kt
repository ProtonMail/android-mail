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
import arrow.core.getOrElse
import ch.protonmail.android.mailcommon.domain.usecase.ObservePrimaryUser
import ch.protonmail.android.mailupselling.domain.annotations.UpsellingOnboardingEnabled
import ch.protonmail.android.mailupselling.domain.model.UpsellingEntryPoint
import ch.protonmail.android.mailupselling.domain.model.telemetry.UpsellingTelemetryEventType
import ch.protonmail.android.mailupselling.domain.repository.UpsellingTelemetryRepository
import ch.protonmail.android.mailupselling.presentation.model.onboarding.OnboardingUpsellOperation.Action
import ch.protonmail.android.mailupselling.presentation.model.onboarding.OnboardingUpsellState
import ch.protonmail.android.mailupselling.presentation.model.onboarding.OnboardingUpsellState.Loading
import ch.protonmail.android.mailupselling.presentation.model.onboarding.OnboardingUpsellState.OnboardingUpsellOperation.OnboardingUpsellEvent
import ch.protonmail.android.mailupselling.presentation.reducer.OnboardingUpsellReducer
import ch.protonmail.android.mailupselling.presentation.ui.onboarding.PlansType
import ch.protonmail.android.mailupselling.presentation.usecase.GetOnboardingUpsellingPlans
import ch.protonmail.android.mailupselling.presentation.usecase.GetOnboardingUpsellingPlans.GetOnboardingPlansError.GenericError
import ch.protonmail.android.mailupselling.presentation.usecase.GetOnboardingUpsellingPlans.GetOnboardingPlansError.MismatchingPlanCycles
import ch.protonmail.android.mailupselling.presentation.usecase.GetOnboardingUpsellingPlans.GetOnboardingPlansError.MismatchingPlans
import ch.protonmail.android.mailupselling.presentation.usecase.GetOnboardingUpsellingPlans.GetOnboardingPlansError.NoPlans
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.update
import me.proton.core.user.domain.extension.hasSubscription
import javax.inject.Inject

@HiltViewModel
class OnboardingUpsellViewModel @Inject constructor(
    observePrimaryUser: ObservePrimaryUser,
    private val getOnboardingUpsellingPlans: GetOnboardingUpsellingPlans,
    private val onboardingUpsellReducer: OnboardingUpsellReducer,
    @UpsellingOnboardingEnabled private val isUpsellingEnabled: Boolean,
    private val upsellingTelemetryRepository: UpsellingTelemetryRepository
) : ViewModel() {

    private val mutableState = MutableStateFlow<OnboardingUpsellState>(Loading)
    val state = mutableState.asStateFlow()

    init {
        observePrimaryUser().mapLatest { user ->
            val userId = user?.userId
                ?: return@mapLatest emitNewStateFrom(OnboardingUpsellEvent.LoadingError.NoUserId)

            if (!isUpsellingEnabled) return@mapLatest emitNewStateFrom(OnboardingUpsellEvent.UnsupportedFlow.NotEnabled)

            if (user.hasSubscription()) {
                return@mapLatest emitNewStateFrom(OnboardingUpsellEvent.UnsupportedFlow.PaidUser)
            }

            val dynamicPlans = getOnboardingUpsellingPlans(userId).getOrElse {
                return@mapLatest emitNewStateFrom(it.toUpsellingEvent())
            }

            emitNewStateFrom(OnboardingUpsellEvent.DataLoaded(userId, dynamicPlans))
        }.launchIn(viewModelScope)
    }

    fun submit(action: Action) = when (action) {
        is Action.PlanSelected -> handlePlanSelected(action.plansType, action.planName)
        is Action.TrackEvent -> handleTrackEvent(action)
    }

    private fun emitNewStateFrom(operation: OnboardingUpsellState.OnboardingUpsellOperation) {
        val currentState = state.value
        mutableState.update { onboardingUpsellReducer.newStateFrom(currentState, operation) }
    }

    private fun handlePlanSelected(plansType: PlansType, planName: String) {
        when (state.value) {
            is OnboardingUpsellState.Data -> {
                val selectedPlanUiModel = if (plansType == PlansType.Annual) {
                    (state.value as OnboardingUpsellState.Data).planUiModels.annualPlans.find {
                        it.title == planName
                    }
                } else {
                    (state.value as OnboardingUpsellState.Data).planUiModels.monthlyPlans.find {
                        it.title == planName
                    }
                }

                emitNewStateFrom(OnboardingUpsellEvent.PlanSelected(selectedPlanUiModel?.payButtonPlanUiModel))
            }
        }
    }

    private fun handleTrackEvent(event: Action.TrackEvent) {
        val eventType = when (event) {
            is Action.TrackEvent.UpgradeAttempt -> UpsellingTelemetryEventType.Upgrade.UpgradeAttempt(event.payload)
            is Action.TrackEvent.UpgradeCancelled -> UpsellingTelemetryEventType.Upgrade.UpgradeCancelled(event.payload)
            is Action.TrackEvent.UpgradeErrored -> UpsellingTelemetryEventType.Upgrade.UpgradeErrored(event.payload)
            is Action.TrackEvent.UpgradeSuccess -> UpsellingTelemetryEventType.Upgrade.PurchaseCompleted(event.payload)
        }

        upsellingTelemetryRepository.trackEvent(eventType, UpsellingEntryPoint.PostOnboarding)
    }

    private fun GetOnboardingUpsellingPlans.GetOnboardingPlansError.toUpsellingEvent() = when (this) {
        // We map to GenericError -> NoSubscriptions for non GMS users
        GenericError, NoPlans -> OnboardingUpsellEvent.UnsupportedFlow.NoSubscriptions
        MismatchingPlanCycles, MismatchingPlans -> OnboardingUpsellEvent.UnsupportedFlow.PlansMismatch
    }
}
