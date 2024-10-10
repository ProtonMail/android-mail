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
import ch.protonmail.android.mailcommon.domain.usecase.ObservePrimaryUser
import ch.protonmail.android.mailonboarding.presentation.model.OnboardingState
import ch.protonmail.android.mailupselling.domain.usecase.GetAccountAgeInDays
import ch.protonmail.android.mailupselling.presentation.usecase.ObserveUpsellingOnboardingVisibility
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import me.proton.core.user.domain.extension.hasSubscription
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    observePrimaryUser: ObservePrimaryUser,
    private val getAccountAgeInDays: GetAccountAgeInDays,
    private val observeUpsellingOnboardingVisibility: ObserveUpsellingOnboardingVisibility
) : ViewModel() {

    val state: StateFlow<OnboardingState> = observePrimaryUser().filterNotNull()
        .flatMapLatest { user ->
            when {
                user.hasSubscription() ||
                    getAccountAgeInDays(user).days == 0 -> flowOf(OnboardingState.NoUpsell)

                else -> observeUpsellingOnboardingVisibility().map { showUpselling ->
                    if (showUpselling) OnboardingState.ToUpsell else OnboardingState.NoUpsell
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = OnboardingState.Loading
        )
}
