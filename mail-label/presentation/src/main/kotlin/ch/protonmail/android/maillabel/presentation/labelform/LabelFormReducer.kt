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
import ch.protonmail.android.maillabel.presentation.getHexStringFromColor
import javax.inject.Inject

class LabelFormReducer @Inject constructor() {

    internal fun newStateFrom(currentState: LabelFormState, operation: LabelFormOperation): LabelFormState {
        return when (operation) {
            is LabelFormEvent.EnableSaveButton -> TODO()
            is LabelFormEvent.DisableSaveButton -> TODO()
            is LabelFormAction.OnCloseLabelForm -> TODO()
            is LabelFormAction.LabelColorChanged -> updateLabelColorTo(currentState, operation.labelColor)
            is LabelFormAction.LabelNameChanged -> updateLabelNameTo(currentState, operation.labelName)
            LabelFormAction.OnDeleteClick -> TODO()
            LabelFormAction.OnSaveClick -> TODO()
            LabelFormEvent.LabelCreated -> TODO()
            LabelFormEvent.LabelDeleted -> TODO()
            LabelFormEvent.LabelUpdated -> TODO()
        }
    }

    private fun updateLabelNameTo(currentState: LabelFormState, labelName: String): LabelFormState {
        return when (currentState) {
            is LabelFormState.CreateLabel -> LabelFormState.CreateLabel(
                currentState.newLabel.copy(name = labelName)
            )
            is LabelFormState.EditLabel -> LabelFormState.EditLabel(
                currentState.label.copy(name = labelName)
            )
            LabelFormState.Loading -> LabelFormState.Loading
        }
    }

    private fun updateLabelColorTo(currentState: LabelFormState, labelColor: Color): LabelFormState {
        return when (currentState) {
            is LabelFormState.CreateLabel -> LabelFormState.CreateLabel(
                currentState.newLabel.copy(color = labelColor.getHexStringFromColor())
            )
            is LabelFormState.EditLabel -> LabelFormState.EditLabel(
                currentState.label.copy(color = labelColor.getHexStringFromColor())
            )
            LabelFormState.Loading -> LabelFormState.Loading
        }
    }
}
