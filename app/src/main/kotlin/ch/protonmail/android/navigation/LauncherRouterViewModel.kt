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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import arrow.core.Either
import ch.protonmail.android.mailcommon.domain.model.PreferencesError
import ch.protonmail.android.mailonboarding.domain.model.OnboardingPreference
import ch.protonmail.android.mailonboarding.domain.usecase.ObserveOnboarding
import ch.protonmail.android.navigation.model.OnboardingEligibilityState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
internal class LauncherRouterViewModel @Inject constructor(
    observeOnboarding: ObserveOnboarding
) : ViewModel() {

    val onboardingEligibilityState = observeOnboarding()
        .mapLatest { it.toState() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = OnboardingEligibilityState.Loading
        )

    private fun Either<PreferencesError, OnboardingPreference>.toState(): OnboardingEligibilityState {
        val preference = this.getOrNull() ?: return OnboardingEligibilityState.Required
        return if (preference.display) OnboardingEligibilityState.Required else OnboardingEligibilityState.NotRequired
    }
}
