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

package ch.protonmail.android.navigation.onboarding

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import ch.protonmail.android.navigation.listener.withDestinationChangedObservableEffect
import ch.protonmail.android.navigation.model.Destination
import ch.protonmail.android.navigation.route.addOnboarding
import ch.protonmail.android.navigation.route.addOnboardingUpselling
import io.sentry.compose.withSentryObservableEffect

@Composable
fun Onboarding() {
    val navController = rememberNavController()
        .withSentryObservableEffect()
        .withDestinationChangedObservableEffect()
    val onboardingStepViewModel = hiltViewModel<OnboardingStepViewModel>()

    val exitAction = remember {
        {
            onboardingStepViewModel.submit(OnboardingStepAction.MarkOnboardingComplete)
        }
    }

    NavHost(
        modifier = Modifier.fillMaxSize(),
        navController = navController,
        startDestination = Destination.Screen.Onboarding.MainScreen.route
    ) {
        addOnboarding(navController, exitAction)
        addOnboardingUpselling(exitAction)
    }
}
