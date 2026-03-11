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

package ch.protonmail.android.navigation

import ch.protonmail.android.mailnotifications.presentation.model.NotificationsPermissionState
import ch.protonmail.android.mailnotifications.presentation.model.NotificationsPermissionStateType
import ch.protonmail.android.mailonboarding.domain.model.OnboardingEligibilityState
import ch.protonmail.android.mailspotlight.presentation.model.FeatureSpotlightState
import ch.protonmail.android.mailupselling.presentation.model.blackfriday.BlackFridayModalState
import ch.protonmail.android.mailupselling.presentation.model.springsale.SpringPromoModalState

sealed interface HomeInterstitialPriority {
    data object Loading : HomeInterstitialPriority
    data object Onboarding : HomeInterstitialPriority
    data class NotificationsPermissions(val type: NotificationsPermissionStateType) : HomeInterstitialPriority
    data object FeatureSpotlight : HomeInterstitialPriority
    data class BlackFriday(val state: BlackFridayModalState.Show) : HomeInterstitialPriority
    data class SpringPromo(val state: SpringPromoModalState.Show) : HomeInterstitialPriority
    data object None : HomeInterstitialPriority
}

fun resolveHomeInterstitialPriority(
    onboardingState: OnboardingEligibilityState,
    notificationsState: NotificationsPermissionState,
    featureSpotlightState: FeatureSpotlightState,
    blackFridayState: BlackFridayModalState,
    springSaleState: SpringPromoModalState
): HomeInterstitialPriority {
    // Wait until all states are loaded
    @Suppress("ComplexCondition")
    if (onboardingState is OnboardingEligibilityState.Loading ||
        notificationsState is NotificationsPermissionState.Loading ||
        featureSpotlightState is FeatureSpotlightState.Loading ||
        blackFridayState is BlackFridayModalState.Loading
    ) {
        return HomeInterstitialPriority.Loading
    }

    return when {
        onboardingState is OnboardingEligibilityState.Required -> HomeInterstitialPriority.Onboarding
        notificationsState is NotificationsPermissionState.RequiresInteraction ->
            HomeInterstitialPriority.NotificationsPermissions(notificationsState.stateType)

        featureSpotlightState is FeatureSpotlightState.Show -> HomeInterstitialPriority.FeatureSpotlight
        blackFridayState is BlackFridayModalState.Show -> HomeInterstitialPriority.BlackFriday(blackFridayState)
        springSaleState is SpringPromoModalState.Show -> HomeInterstitialPriority.SpringPromo(springSaleState)
        else -> HomeInterstitialPriority.None
    }
}
