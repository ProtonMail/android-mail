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
import arrow.core.getOrElse
import ch.protonmail.android.design.compose.viewmodel.stopTimeoutMillis
import ch.protonmail.android.mailevents.domain.AppEventBroadcaster
import ch.protonmail.android.mailevents.domain.model.AppEvent
import ch.protonmail.android.mailfeatureflags.domain.annotation.IsOnboardingUpsellEnabled
import ch.protonmail.android.mailfeatureflags.domain.model.FeatureFlag
import ch.protonmail.android.mailsession.domain.model.hasSubscription
import ch.protonmail.android.mailsession.domain.usecase.ObservePrimaryUser
import ch.protonmail.android.mailupselling.domain.usecase.GetOnboardingPlanUpgrades
import ch.protonmail.android.mailupselling.presentation.OnboardingUpsellingReducer
import ch.protonmail.android.mailupselling.presentation.model.onboarding.OnboardingUpsellOperation.OnboardingUpsellEvent
import ch.protonmail.android.mailupselling.presentation.model.onboarding.OnboardingUpsellState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
internal class OnboardingUpsellViewModel @Inject constructor(
    observePrimaryUser: ObservePrimaryUser,
    private val onboardingUpsellReducer: OnboardingUpsellingReducer,
    private val getOnboardingPlanUpgrades: GetOnboardingPlanUpgrades,
    @IsOnboardingUpsellEnabled private val isUpsellEnabled: FeatureFlag<Boolean>,
    private val appEventBroadcaster: AppEventBroadcaster
) : ViewModel() {

    val state: StateFlow<OnboardingUpsellState> = observePrimaryUser()
        .mapLatest { userResult ->
            val user = userResult?.getOrNull()
                ?: return@mapLatest onboardingUpsellReducer.newStateFrom(
                    OnboardingUpsellEvent.LoadingError.NoUserId
                )

            if (!isUpsellEnabled.get()) {
                return@mapLatest onboardingUpsellReducer.newStateFrom(
                    OnboardingUpsellEvent.UnsupportedFlow.NotEnabled
                )
            }

            if (user.hasSubscription()) {
                return@mapLatest onboardingUpsellReducer.newStateFrom(
                    OnboardingUpsellEvent.UnsupportedFlow.PaidUser
                )
            }

            val userId = user.userId
            val productDetails = getOnboardingPlanUpgrades(userId).getOrElse {
                return@mapLatest onboardingUpsellReducer.newStateFrom(
                    OnboardingUpsellEvent.UnsupportedFlow.PlansMismatch
                )
            }

            appEventBroadcaster.emit(AppEvent.SubscriptionOnboardingShown)

            onboardingUpsellReducer.newStateFrom(
                OnboardingUpsellEvent.DataLoaded(userId, productDetails)
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis),
            initialValue = OnboardingUpsellState.Loading
        )
}
