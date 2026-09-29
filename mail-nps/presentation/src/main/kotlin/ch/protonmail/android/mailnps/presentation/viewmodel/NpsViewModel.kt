/*
 * Copyright (c) 2026 Proton Technologies AG
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

package ch.protonmail.android.mailnps.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.protonmail.android.mailfeatureflags.domain.annotation.IsNpsFeedbackEnabled
import ch.protonmail.android.mailfeatureflags.domain.model.FeatureFlag
import ch.protonmail.android.mailnps.domain.usecase.DismissNps
import ch.protonmail.android.mailnps.domain.usecase.SubmitNpsFeedback
import ch.protonmail.android.mailnps.presentation.model.NpsEligibilityState
import ch.protonmail.android.mailnps.presentation.model.NpsOperation
import ch.protonmail.android.mailnps.presentation.model.NpsOperation.NpsAction
import ch.protonmail.android.mailnps.presentation.model.NpsOperation.NpsEvent
import ch.protonmail.android.mailnps.presentation.model.NpsState
import ch.protonmail.android.mailnps.presentation.reducer.NpsReducer
import ch.protonmail.android.mailsession.domain.usecase.ObservePrimaryUserId
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class NpsViewModel @Inject constructor(
    private val submitNpsFeedback: SubmitNpsFeedback,
    private val dismissNps: DismissNps,
    private val reducer: NpsReducer,
    @IsNpsFeedbackEnabled private val isNpsFeedbackEnabled: FeatureFlag<Boolean>,
    observePrimaryUserId: ObservePrimaryUserId
) : ViewModel() {

    private val primaryUserId = observePrimaryUserId().filterNotNull()

    private val mutableState = MutableStateFlow(NpsState.Initial)
    val state: StateFlow<NpsState> = mutableState.asStateFlow()

    // Re-evaluated on primary user change, so the survey is only considered once a user is signed in.
    val eligibility: StateFlow<NpsEligibilityState> = primaryUserId
        .distinctUntilChanged()
        .flatMapLatest {
            flow {
                val isEnabled = isNpsFeedbackEnabled.get()
                emit(if (isEnabled) NpsEligibilityState.Show else NpsEligibilityState.Hide)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = NpsEligibilityState.Loading
        )

    fun submit(action: NpsAction) {
        when (action) {
            is NpsAction.ScoreSelected,
            is NpsAction.CommentChanged -> emitNewStateFrom(action)

            is NpsAction.ShareClicked -> onShare()
            is NpsAction.SheetDismissed -> onSheetDismissed()
        }
    }

    private fun onShare() = viewModelScope.launch {
        val score = mutableState.value.selectedScore ?: return@launch
        // The survey is closed either way: a failure is not actionable for the user.
        submitNpsFeedback(primaryUserId.first(), score, mutableState.value.comment)
            .onLeft { Timber.e("nps: recording the score failed: $it") }
        emitNewStateFrom(NpsEvent.Submitted)
    }

    private fun onSheetDismissed() = viewModelScope.launch {
        // Closing the sheet after sharing is not a dismissal.
        if (mutableState.value.isAnswered) return@launch
        dismissNps(primaryUserId.first())
            .onLeft { Timber.e("nps: recording the dismissal failed: $it") }
        emitNewStateFrom(NpsEvent.Dismissed)
    }

    private fun emitNewStateFrom(operation: NpsOperation) {
        mutableState.update { reducer.newStateFrom(it, operation) }
    }
}
