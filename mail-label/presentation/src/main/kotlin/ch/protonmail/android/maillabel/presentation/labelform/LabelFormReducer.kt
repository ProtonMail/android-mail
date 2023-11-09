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

package ch.protonmail.android.maillabel.presentation.labelform

import androidx.compose.ui.graphics.Color
import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.maillabel.presentation.getHexStringFromColor
import javax.inject.Inject

class LabelFormReducer @Inject constructor() {

    internal fun newStateFrom(currentState: LabelFormState, operation: LabelFormOperation): LabelFormState {
        return when (operation) {
            is LabelFormEvent.LabelLoaded -> reduceLabelLoaded(operation)
            is LabelFormViewAction.LabelColorChanged -> reduceLabelColorChanged(currentState, operation.labelColor)
            is LabelFormViewAction.LabelNameChanged -> reduceLabelNameChanged(currentState, operation.labelName)
            LabelFormViewAction.OnCloseLabelForm -> reduceOnCloseLabelForm(currentState)
            LabelFormViewAction.OnDeleteClick -> reduceOnDeleteClick(currentState)
            LabelFormViewAction.OnSaveClick -> reduceOnSaveClick(currentState)
            LabelFormEvent.LabelCreated -> reduceLabelCreated(currentState)
            LabelFormEvent.LabelDeleted -> reduceLabelDeleted(currentState)
            LabelFormEvent.LabelUpdated -> reduceLabelUpdated(currentState)
        }
    }

    private fun reduceLabelLoaded(operation: LabelFormEvent.LabelLoaded): LabelFormState {
        return if (operation.labelId != null) {
            LabelFormState.Update(
                isSaveEnabled = operation.name.isNotEmpty(),
                labelId = operation.labelId,
                name = operation.name,
                color = operation.color,
                close = Effect.empty(),
                closeWithSave = Effect.empty(),
                closeWithDelete = Effect.empty()
            )
        } else {
            LabelFormState.Create(
                isSaveEnabled = operation.name.isNotEmpty(),
                name = operation.name,
                color = operation.color,
                close = Effect.empty(),
                closeWithSave = Effect.empty()
            )
        }
    }

    private fun reduceLabelNameChanged(currentState: LabelFormState, labelName: String): LabelFormState {
        return when (currentState) {
            is LabelFormState.Create -> currentState.copy(isSaveEnabled = labelName.isNotEmpty(), name = labelName)
            is LabelFormState.Update -> currentState.copy(isSaveEnabled = labelName.isNotEmpty(), name = labelName)
            LabelFormState.Loading -> currentState
        }
    }

    private fun reduceLabelColorChanged(currentState: LabelFormState, labelColor: Color): LabelFormState {
        return when (currentState) {
            is LabelFormState.Create -> currentState.copy(color = labelColor.getHexStringFromColor())
            is LabelFormState.Update -> currentState.copy(color = labelColor.getHexStringFromColor())
            LabelFormState.Loading -> currentState
        }
    }

    private fun reduceOnCloseLabelForm(currentState: LabelFormState): LabelFormState {
        return when (currentState) {
            is LabelFormState.Create -> currentState.copy(close = Effect.of(Unit))
            is LabelFormState.Update -> currentState.copy(close = Effect.of(Unit))
            LabelFormState.Loading -> currentState
        }
    }

    private fun reduceOnDeleteClick(currentState: LabelFormState): LabelFormState {
        return when (currentState) {
            is LabelFormState.Create -> currentState
            is LabelFormState.Update -> currentState.copy(closeWithDelete = Effect.of(Unit))
            LabelFormState.Loading -> currentState
        }
    }

    private fun reduceOnSaveClick(currentState: LabelFormState): LabelFormState {
        return when (currentState) {
            is LabelFormState.Create -> currentState.copy(closeWithSave = Effect.of(Unit))
            is LabelFormState.Update -> currentState.copy(closeWithSave = Effect.of(Unit))
            LabelFormState.Loading -> currentState
        }
    }

    private fun reduceLabelCreated(currentState: LabelFormState): LabelFormState {
        return when (currentState) {
            is LabelFormState.Create -> currentState.copy(closeWithSave = Effect.of(Unit))
            is LabelFormState.Update -> currentState.copy(closeWithSave = Effect.of(Unit))
            LabelFormState.Loading -> currentState
        }
    }

    private fun reduceLabelDeleted(currentState: LabelFormState): LabelFormState {
        return when (currentState) {
            is LabelFormState.Create -> currentState
            is LabelFormState.Update -> currentState.copy(closeWithDelete = Effect.of(Unit))
            LabelFormState.Loading -> currentState
        }
    }

    private fun reduceLabelUpdated(currentState: LabelFormState): LabelFormState {
        return when (currentState) {
            is LabelFormState.Create -> currentState.copy(closeWithSave = Effect.of(Unit))
            is LabelFormState.Update -> currentState.copy(closeWithSave = Effect.of(Unit))
            LabelFormState.Loading -> currentState
        }
    }
}
