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

package ch.protonmail.android.mailonboarding.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import arrow.core.Either
import ch.protonmail.android.mailcommon.domain.model.PreferencesError
import ch.protonmail.android.mailevents.domain.AppEventBroadcaster
import ch.protonmail.android.mailevents.domain.model.AppEvent
import ch.protonmail.android.mailonboarding.domain.model.OnboardingEligibilityState
import ch.protonmail.android.mailonboarding.domain.model.OnboardingPreference
import ch.protonmail.android.mailonboarding.domain.usecase.ObserveOnboarding
import ch.protonmail.android.mailonboarding.domain.usecase.SaveOnboarding
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingStepViewModel @Inject constructor(
    private val saveOnboarding: SaveOnboarding,
    private val appEventBroadcaster: AppEventBroadcaster,
    observeOnboarding: ObserveOnboarding
) : ViewModel() {

    val onboardingEligibilityState = observeOnboarding()
        .mapLatest { it.toState() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = OnboardingEligibilityState.Loading
        )

    fun submit(action: OnboardingStepAction) {
        viewModelScope.launch {
            when (action) {
                OnboardingStepAction.MarkOnboardingComplete -> {
                    saveOnboarding(display = false)
                    appEventBroadcaster.emit(AppEvent.OnboardingCompleted)
                }
            }
        }
    }

    private fun Either<PreferencesError, OnboardingPreference>.toState(): OnboardingEligibilityState {
        val preference = this.getOrNull() ?: return OnboardingEligibilityState.Required
        return if (preference.display) OnboardingEligibilityState.Required else OnboardingEligibilityState.NotRequired
    }
}

sealed interface OnboardingStepAction {
    data object MarkOnboardingComplete : OnboardingStepAction
}
