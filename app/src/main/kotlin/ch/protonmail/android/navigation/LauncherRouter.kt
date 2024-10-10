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

package ch.protonmail.android.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ch.protonmail.android.MainActivity
import ch.protonmail.android.navigation.model.OnboardingEligibilityState
import ch.protonmail.android.navigation.onboarding.Onboarding
import me.proton.core.compose.component.ProtonCenteredProgress

@Composable
internal fun LauncherRouter(
    activityActions: MainActivity.Actions,
    launcherActions: Launcher.Actions,
    viewModel: LauncherRouterViewModel = hiltViewModel()
) {

    val onboardingState by viewModel.onboardingEligibilityState.collectAsStateWithLifecycle()

    when (onboardingState) {
        OnboardingEligibilityState.Loading -> ProtonCenteredProgress(Modifier.fillMaxSize())
        OnboardingEligibilityState.NotRequired -> Home(activityActions, launcherActions)
        OnboardingEligibilityState.Required -> Onboarding()
    }
}
