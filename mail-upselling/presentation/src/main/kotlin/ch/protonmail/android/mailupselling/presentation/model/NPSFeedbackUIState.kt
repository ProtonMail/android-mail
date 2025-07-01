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

package ch.protonmail.android.mailupselling.presentation.model

import androidx.compose.ui.text.input.TextFieldValue
import ch.protonmail.android.mailcommon.presentation.Effect

internal data class NPSFeedbackUIState(
    val selection: Int?,
    val feedbackText: TextFieldValue,
    val showSuccess: Effect<Unit>,
    val submitted: Boolean,
    val submitEnabled: Boolean
) {

    companion object {

        const val MAX_VALUE = 10

        val Initial = NPSFeedbackUIState(
            selection = null,
            feedbackText = TextFieldValue(),
            showSuccess = Effect.empty(),
            submitted = false,
            submitEnabled = false
        )
    }
}

internal sealed interface NPSFeedbackOperation

internal sealed interface NPSFeedbackViewEvent : NPSFeedbackOperation {
    data object ContentShown : NPSFeedbackViewEvent
    data object Dismissed : NPSFeedbackViewEvent
    data object SubmitClicked : NPSFeedbackViewEvent
    data class OptionSelected(val value: Int) : NPSFeedbackViewEvent
    data class FeedbackChanged(val value: TextFieldValue) : NPSFeedbackViewEvent
}
