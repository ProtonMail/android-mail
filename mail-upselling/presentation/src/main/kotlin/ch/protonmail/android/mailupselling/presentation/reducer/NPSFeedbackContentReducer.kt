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

package ch.protonmail.android.mailupselling.presentation.reducer

import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailupselling.presentation.model.NPSFeedbackUIState
import ch.protonmail.android.mailupselling.presentation.model.NPSFeedbackViewEvent
import javax.inject.Inject

internal class NPSFeedbackContentReducer @Inject constructor() {

    fun newStateFrom(state: NPSFeedbackUIState, event: NPSFeedbackViewEvent): NPSFeedbackUIState = when (event) {
        NPSFeedbackViewEvent.ContentShown -> state
        is NPSFeedbackViewEvent.OptionSelected -> state.copy(selection = event.value, submitEnabled = true)
        is NPSFeedbackViewEvent.FeedbackChanged -> state.copy(feedbackText = event.value)
        NPSFeedbackViewEvent.SubmitClicked -> state.copy(
            showSuccess = Effect.of(Unit),
            submitted = true,
            submitEnabled = false
        )
        NPSFeedbackViewEvent.Dismissed -> state
    }
}
