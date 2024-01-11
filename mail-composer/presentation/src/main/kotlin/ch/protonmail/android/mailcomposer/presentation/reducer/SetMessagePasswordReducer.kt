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

package ch.protonmail.android.mailcomposer.presentation.reducer

import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailcomposer.presentation.model.MessagePasswordOperation
import ch.protonmail.android.mailcomposer.presentation.model.SetMessagePasswordState
import me.proton.core.util.kotlin.EMPTY_STRING
import javax.inject.Inject

class SetMessagePasswordReducer @Inject constructor() {

    fun newStateFrom(
        currentState: SetMessagePasswordState,
        event: MessagePasswordOperation.Event
    ): SetMessagePasswordState = when (event) {
        is MessagePasswordOperation.Event.ExitScreen -> newStateForExitScreen(currentState)
        is MessagePasswordOperation.Event.InitializeScreen -> newStateForPrefillInputFields(event)
        is MessagePasswordOperation.Event.PasswordValidated -> newStateForPasswordValidated(currentState, event)
        is MessagePasswordOperation.Event.RepeatedPasswordValidated ->
            newStateForRepeatedPasswordValidated(currentState, event)
    }

    private fun newStateForExitScreen(currentState: SetMessagePasswordState): SetMessagePasswordState =
        if (currentState is SetMessagePasswordState.Data) {
            currentState.copy(exitScreen = Effect.of(Unit))
        } else {
            currentState
        }

    private fun newStateForPrefillInputFields(
        event: MessagePasswordOperation.Event.InitializeScreen
    ): SetMessagePasswordState = SetMessagePasswordState.Data(
        initialMessagePasswordValue = event.messagePassword?.password ?: EMPTY_STRING,
        initialMessagePasswordHintValue = event.messagePassword?.passwordHint ?: EMPTY_STRING,
        hasMessagePasswordError = false,
        hasRepeatedMessagePasswordError = false,
        isInEditMode = event.messagePassword != null,
        exitScreen = Effect.empty()
    )

    private fun newStateForPasswordValidated(
        currentState: SetMessagePasswordState,
        event: MessagePasswordOperation.Event.PasswordValidated
    ) = if (currentState is SetMessagePasswordState.Data) {
        currentState.copy(hasMessagePasswordError = event.hasMessagePasswordError)
    } else {
        currentState
    }

    private fun newStateForRepeatedPasswordValidated(
        currentState: SetMessagePasswordState,
        event: MessagePasswordOperation.Event.RepeatedPasswordValidated
    ) = if (currentState is SetMessagePasswordState.Data) {
        currentState.copy(hasRepeatedMessagePasswordError = event.hasRepeatedMessagePasswordError)
    } else {
        currentState
    }
}
