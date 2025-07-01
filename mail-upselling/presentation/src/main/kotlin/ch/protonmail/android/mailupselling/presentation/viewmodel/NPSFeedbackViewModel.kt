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

package ch.protonmail.android.mailupselling.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.protonmail.android.mailupselling.domain.usecase.RecordNPSFeedbackTriggered
import ch.protonmail.android.mailupselling.domain.usecase.SkipNPSFeedback
import ch.protonmail.android.mailupselling.domain.usecase.SubmitNPSFeedback
import ch.protonmail.android.mailupselling.presentation.model.NPSFeedbackUIState
import ch.protonmail.android.mailupselling.presentation.model.NPSFeedbackViewEvent
import ch.protonmail.android.mailupselling.presentation.reducer.NPSFeedbackContentReducer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class NPSFeedbackViewModel @Inject constructor(
    private val reducer: NPSFeedbackContentReducer,
    private val recordNPSFeedbackTriggered: RecordNPSFeedbackTriggered,
    private val submitNPSFeedback: SubmitNPSFeedback,
    private val skipNPSFeedback: SkipNPSFeedback
) : ViewModel() {

    private val mutableState = MutableStateFlow<NPSFeedbackUIState>(NPSFeedbackUIState.Initial)
    val state = mutableState.asStateFlow()

    fun submit(event: NPSFeedbackViewEvent) = viewModelScope.launch {
        when (event) {
            NPSFeedbackViewEvent.ContentShown -> recordTriggered()
            NPSFeedbackViewEvent.SubmitClicked -> onSubmitted()
            is NPSFeedbackViewEvent.OptionSelected -> Unit
            is NPSFeedbackViewEvent.FeedbackChanged -> Unit
            NPSFeedbackViewEvent.Dismissed -> onDismissed()
        }
        emitNewStateFrom(event)
    }

    private suspend fun recordTriggered() {
        recordNPSFeedbackTriggered.invoke()
    }

    private fun onSubmitted() {
        val state = mutableState.value
        submitNPSFeedback.invoke(
            comment = state.feedbackText.text,
            ratingValue = state.selection ?: 0
        )
    }

    private fun onDismissed() {
        val submitted = mutableState.value.submitted
        if (submitted) return
        skipNPSFeedback()
    }

    private fun emitNewStateFrom(operation: NPSFeedbackViewEvent) {
        mutableState.update { reducer.newStateFrom(mutableState.value, operation) }
    }
}
