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

package ch.protonmail.android.navigation.route

import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import ch.protonmail.android.mailonboarding.presentation.OnboardingScreen
import ch.protonmail.android.mailupselling.presentation.ui.onboarding.OnboardingUpsellScreen
import ch.protonmail.android.navigation.model.Destination

fun NavGraphBuilder.addOnboarding(navController: NavHostController, exitAction: () -> Unit) {
    composable(route = Destination.Screen.Onboarding.MainScreen.route) {
        OnboardingScreen(
            exitAction = exitAction,
            onUpsellingNavigation = {
                navController.navigate(Destination.Screen.Onboarding.Upselling.route) {
                    popUpTo(Destination.Screen.Onboarding.MainScreen.route) { inclusive = true }
                }
            }
        )
    }
}

fun NavGraphBuilder.addOnboardingUpselling(exitAction: () -> Unit) {
    composable(route = Destination.Screen.Onboarding.Upselling.route) {
        OnboardingUpsellScreen(
            modifier = Modifier,
            exitScreen = exitAction
        )
    }
}
