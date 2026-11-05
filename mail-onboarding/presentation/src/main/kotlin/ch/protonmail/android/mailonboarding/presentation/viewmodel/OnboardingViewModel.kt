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
import ch.protonmail.android.mailbyoe.domain.model.ByoeDismissMethod
import ch.protonmail.android.mailbyoe.domain.model.ByoeOnboardingStep
import ch.protonmail.android.mailbyoe.domain.model.ByoeSurface
import ch.protonmail.android.mailbyoe.domain.model.ByoeTelemetryEvent
import ch.protonmail.android.mailbyoe.domain.usecase.DismissByoeGuidance
import ch.protonmail.android.mailbyoe.domain.usecase.GetByoeOnboardingTreatment
import ch.protonmail.android.mailbyoe.domain.usecase.RecordOnboardingStepShown
import ch.protonmail.android.mailbyoe.domain.usecase.ReportByoeTelemetryEvent
import ch.protonmail.android.mailonboarding.domain.usecase.SavePendingOnboardingUpsell
import ch.protonmail.android.mailonboarding.presentation.model.OnboardingState
import ch.protonmail.android.mailsession.domain.model.hasSubscription
import ch.protonmail.android.mailsession.domain.usecase.ObservePrimaryUser
import ch.protonmail.android.mailsession.domain.usecase.ObservePrimaryUserId
import ch.protonmail.android.mailupselling.presentation.usecase.GetUpsellingOnboardingVisibility
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    observePrimaryUser: ObservePrimaryUser,
    private val observePrimaryUserId: ObservePrimaryUserId,
    private val getUpsellingOnboardingVisibility: GetUpsellingOnboardingVisibility,
    private val getByoeOnboardingTreatment: GetByoeOnboardingTreatment,
    private val recordOnboardingStepShown: RecordOnboardingStepShown,
    private val dismissByoeGuidance: DismissByoeGuidance,
    private val reportByoeTelemetryEvent: ReportByoeTelemetryEvent,
    private val savePendingOnboardingUpsell: SavePendingOnboardingUpsell
) : ViewModel() {

    private val recordedSteps = mutableSetOf<ByoeOnboardingStep>()

    val state: StateFlow<OnboardingState> = observePrimaryUser().filterNotNull()
        .map { it.getOrNull() }
        .filterNotNull()
        .mapLatest { user ->
            val showGmailPage = getByoeOnboardingTreatment(user.userId) != null
            val showUpselling = !user.hasSubscription() && getUpsellingOnboardingVisibility(user.userId)
            if (showUpselling) OnboardingState.ToUpsell(showGmailPage) else OnboardingState.NoUpsell(showGmailPage)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = OnboardingState.Loading
        )

    fun submit(action: OnboardingAction) {
        viewModelScope.launch {
            when (action) {
                is OnboardingAction.PageShown -> onPageShown(action.step)
                OnboardingAction.GmailPageSkipped -> primaryUserId()?.let {
                    dismissByoeGuidance(it, ByoeSurface.OnboardingScreen, ByoeDismissMethod.Button)
                }
                // The user leaves for the Gmail connection, so the upsell waits until they are back in the mailbox.
                OnboardingAction.GmailExploreClicked -> {
                    reportByoeTelemetryEvent(ByoeTelemetryEvent.ExploreClicked(ByoeSurface.OnboardingScreen))
                    if (state.value is OnboardingState.ToUpsell) savePendingOnboardingUpsell(pending = true)
                }
            }
        }
    }

    /** Each page once per onboarding run: going back to a page does not show it for the first time again. */
    private suspend fun onPageShown(step: ByoeOnboardingStep) {
        if (!recordedSteps.add(step)) return
        primaryUserId()?.let { recordOnboardingStepShown(it, step) }
    }

    private suspend fun primaryUserId() = observePrimaryUserId().first()
}

sealed interface OnboardingAction {
    data class PageShown(val step: ByoeOnboardingStep) : OnboardingAction
    data object GmailPageSkipped : OnboardingAction
    data object GmailExploreClicked : OnboardingAction
}
