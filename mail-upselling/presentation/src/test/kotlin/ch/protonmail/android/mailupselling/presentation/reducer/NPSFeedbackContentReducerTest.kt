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

import androidx.compose.ui.text.input.TextFieldValue
import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailupselling.presentation.model.NPSFeedbackUIState
import ch.protonmail.android.mailupselling.presentation.model.NPSFeedbackViewEvent
import org.junit.Test
import kotlin.test.assertEquals

internal class NPSFeedbackContentReducerTest {

    private val sut = NPSFeedbackContentReducer()

    private val initialState = NPSFeedbackUIState(
        selection = null,
        feedbackText = TextFieldValue(),
        showSuccess = Effect.empty(),
        submitted = false,
        submitEnabled = false
    )

    private val stateWithSelection = initialState.copy(
        selection = 10,
        submitEnabled = true
    )

    private val stateWithFeedback = stateWithSelection.copy(
        feedbackText = TextFieldValue("feedback")
    )

    private val stateSubmitted = stateWithFeedback.copy(
        submitEnabled = false,
        submitted = true,
        showSuccess = Effect.of(Unit)
    )

    @Test
    fun `should enable submission once option selected`() {
        // When
        val actual = sut.newStateFrom(initialState, NPSFeedbackViewEvent.OptionSelected(10))

        // Then
        assertEquals(
            stateWithSelection,
            actual
        )
    }

    @Test
    fun `should update feedback`() {
        // When
        val actual = sut.newStateFrom(
            stateWithSelection,
            NPSFeedbackViewEvent.FeedbackChanged(TextFieldValue("feedback"))
        )

        // Then
        assertEquals(
            stateWithFeedback,
            actual
        )
    }

    @Test
    fun `should mark submitted`() {
        // When
        val actual = sut.newStateFrom(stateWithFeedback, NPSFeedbackViewEvent.SubmitClicked)

        // Then
        assertEquals(
            stateSubmitted,
            actual
        )
    }
}
