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
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.maillabel.presentation.R
import ch.protonmail.android.maillabel.presentation.getHexStringFromColor
import javax.inject.Inject

class LabelFormReducer @Inject constructor() {

    internal fun newStateFrom(currentState: LabelFormState, operation: LabelFormOperation): LabelFormState {
        return when (operation) {
            is LabelFormAction.LabelColorChanged -> {
                updateLabelColorTo(currentState, operation.labelColor)
            }
            is LabelFormAction.LabelNameChanged -> {
                updateLabelNameTo(currentState, operation.labelName)
            }
            LabelFormAction.OnCloseLabelForm -> {
                currentState.copy(close = Effect.of(Unit))
            }
            LabelFormAction.OnDeleteClick -> {
                currentState.copy(closeWithDeleteSuccess = Effect.of(Unit))
            }
            LabelFormAction.OnSaveClick -> {
                currentState.copy(closeWithSaveSuccess = Effect.of(Unit))
            }
            LabelFormEvent.LabelCreated -> {
                currentState.copy(closeWithSaveSuccess = Effect.of(Unit))
            }
            LabelFormEvent.LabelDeleted -> {
                currentState.copy(closeWithDeleteSuccess = Effect.of(Unit))
            }
            LabelFormEvent.LabelUpdated -> {
                currentState.copy(closeWithSaveSuccess = Effect.of(Unit))
            }
            LabelFormEvent.DeleteError -> {
                currentState.copy(deleteError = Effect.of(TextUiModel(R.string.delete_label_error)))
            }
            LabelFormEvent.SaveError -> {
                currentState.copy(saveError = Effect.of(TextUiModel(R.string.save_label_error)))
            }
        }
    }

    private fun updateLabelNameTo(currentState: LabelFormState, labelName: String): LabelFormState {
        return if (currentState.label != null) {
            currentState.copy(
                isSaveEnabled = labelName.isNotEmpty(),
                label = currentState.label.copy(name = labelName)
            )
        } else if (currentState.newLabel != null) {
            currentState.copy(
                isSaveEnabled = labelName.isNotEmpty(),
                newLabel = currentState.newLabel.copy(name = labelName)
            )
        } else currentState
    }

    private fun updateLabelColorTo(currentState: LabelFormState, labelColor: Color): LabelFormState {
        return if (currentState.label != null) {
            currentState.copy(
                label = currentState.label.copy(color = labelColor.getHexStringFromColor())
            )
        } else if (currentState.newLabel != null) {
            currentState.copy(
                newLabel = currentState.newLabel.copy(color = labelColor.getHexStringFromColor())
            )
        } else currentState
    }
}
