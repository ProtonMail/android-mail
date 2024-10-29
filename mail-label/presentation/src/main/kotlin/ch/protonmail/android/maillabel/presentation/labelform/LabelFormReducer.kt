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

import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailcommon.presentation.ui.delete.DeleteDialogState
import ch.protonmail.android.maillabel.presentation.R
import ch.protonmail.android.mailupselling.presentation.model.BottomSheetVisibilityEffect
import javax.inject.Inject

class LabelFormReducer @Inject constructor() {

    internal fun newStateFrom(currentState: LabelFormState, event: LabelFormEvent): LabelFormState {
        return when (event) {
            is LabelFormEvent.LabelLoaded -> reduceLabelLoaded(event)
            is LabelFormEvent.UpdateLabelColor -> reduceUpdateLabelColor(currentState, event.color)
            is LabelFormEvent.UpdateLabelName -> reduceUpdateLabelName(currentState, event.name)
            LabelFormEvent.LabelCreated -> reduceLabelCreated(currentState)
            LabelFormEvent.LabelDeleted -> reduceLabelDeleted(currentState)
            LabelFormEvent.LabelUpdated -> reduceLabelUpdated(currentState)
            LabelFormEvent.LabelAlreadyExists -> reduceLabelAlreadyExists(currentState)
            LabelFormEvent.LabelLimitReached -> reduceLabelLimitReached(currentState)
            LabelFormEvent.SaveLabelError -> reduceSaveLabelError(currentState)
            LabelFormEvent.CloseLabelForm -> reduceCloseLabelForm(currentState)
            LabelFormEvent.CreatingLabel -> reduceCreatingLabel(currentState)
            LabelFormEvent.ShowUpselling -> reduceShowUpselling(currentState)
            LabelFormEvent.HideUpselling -> reduceHideUpselling(currentState)
            LabelFormEvent.UpsellingInProgress -> reduceUpsellingInProgress(currentState)
            LabelFormEvent.ShowDeleteDialog -> reduceShowDeleteDialog(currentState)
            LabelFormEvent.HideDeleteDialog -> reduceHideDeleteDialog(currentState)
        }
    }

    private fun reduceLabelLoaded(event: LabelFormEvent.LabelLoaded): LabelFormState {
        return if (event.labelId != null) {
            LabelFormState.Data.Update(
                isSaveEnabled = event.name.isNotEmpty(),
                labelId = event.labelId,
                name = event.name,
                color = event.color,
                colorList = event.colorList
            )
        } else {
            LabelFormState.Data.Create(
                isSaveEnabled = event.name.isNotEmpty(),
                name = event.name,
                color = event.color,
                colorList = event.colorList
            )
        }
    }

    private fun reduceUpdateLabelName(currentState: LabelFormState, labelName: String): LabelFormState {
        return when (currentState) {
            is LabelFormState.Data.Create -> currentState.copy(isSaveEnabled = labelName.isNotEmpty(), name = labelName)
            is LabelFormState.Data.Update -> currentState.copy(isSaveEnabled = labelName.isNotEmpty(), name = labelName)
            is LabelFormState.Loading -> currentState
        }
    }

    private fun reduceUpdateLabelColor(currentState: LabelFormState, labelColor: String): LabelFormState {
        return when (currentState) {
            is LabelFormState.Data.Create -> currentState.copy(color = labelColor)
            is LabelFormState.Data.Update -> currentState.copy(color = labelColor)
            is LabelFormState.Loading -> currentState
        }
    }

    private fun reduceCloseLabelForm(currentState: LabelFormState): LabelFormState {
        return when (currentState) {
            is LabelFormState.Data.Create -> currentState.copy(close = Effect.of(Unit))
            is LabelFormState.Data.Update -> currentState.copy(close = Effect.of(Unit))
            is LabelFormState.Loading -> currentState.copy(close = Effect.of(Unit))
        }
    }

    private fun reduceLabelCreated(currentState: LabelFormState): LabelFormState {
        return when (currentState) {
            is LabelFormState.Data.Create -> currentState.copy(closeWithSave = Effect.of(Unit))
            is LabelFormState.Data.Update -> currentState.copy(closeWithSave = Effect.of(Unit))
            is LabelFormState.Loading -> currentState
        }
    }

    private fun reduceLabelDeleted(currentState: LabelFormState): LabelFormState {
        return when (currentState) {
            is LabelFormState.Data.Create -> currentState
            is LabelFormState.Data.Update -> currentState.copy(
                closeWithDelete = Effect.of(Unit),
                confirmDeleteDialogState = DeleteDialogState.Hidden
            )
            is LabelFormState.Loading -> currentState
        }
    }

    private fun reduceLabelUpdated(currentState: LabelFormState): LabelFormState {
        return when (currentState) {
            is LabelFormState.Data.Create -> currentState.copy(closeWithSave = Effect.of(Unit))
            is LabelFormState.Data.Update -> currentState.copy(closeWithSave = Effect.of(Unit))
            is LabelFormState.Loading -> currentState
        }
    }

    private fun reduceLabelAlreadyExists(currentState: LabelFormState): LabelFormState {
        return when (currentState) {
            is LabelFormState.Data.Create -> currentState.copy(
                showLabelAlreadyExistsSnackbar = Effect.of(Unit),
                displayCreateLoader = false
            )

            is LabelFormState.Data.Update -> currentState.copy(showLabelAlreadyExistsSnackbar = Effect.of(Unit))
            is LabelFormState.Loading -> currentState
        }
    }

    private fun reduceLabelLimitReached(currentState: LabelFormState): LabelFormState {
        return when (currentState) {
            is LabelFormState.Data.Create -> currentState.copy(
                showLabelLimitReachedSnackbar = Effect.of(Unit),
                displayCreateLoader = false
            )

            is LabelFormState.Data.Update -> currentState.copy(showLabelLimitReachedSnackbar = Effect.of(Unit))
            is LabelFormState.Loading -> currentState
        }
    }

    private fun reduceSaveLabelError(currentState: LabelFormState): LabelFormState {
        return when (currentState) {
            is LabelFormState.Data.Create -> currentState.copy(
                showSaveLabelErrorSnackbar = Effect.of(Unit),
                displayCreateLoader = false
            )

            is LabelFormState.Data.Update -> currentState.copy(showSaveLabelErrorSnackbar = Effect.of(Unit))
            is LabelFormState.Loading -> currentState
        }
    }

    private fun reduceCreatingLabel(currentState: LabelFormState): LabelFormState {
        return when (currentState) {
            is LabelFormState.Data.Create -> currentState.copy(displayCreateLoader = true)
            is LabelFormState.Data.Update -> currentState
            is LabelFormState.Loading -> currentState
        }
    }

    private fun reduceShowUpselling(currentState: LabelFormState): LabelFormState {
        return when (currentState) {
            is LabelFormState.Data.Create -> currentState.copy(
                upsellingVisibility = Effect.of(BottomSheetVisibilityEffect.Show),
                displayCreateLoader = false
            )

            is LabelFormState.Data.Update -> currentState
            is LabelFormState.Loading -> currentState
        }
    }

    private fun reduceHideUpselling(currentState: LabelFormState): LabelFormState {
        return when (currentState) {
            is LabelFormState.Data.Create -> currentState.copy(
                upsellingVisibility = Effect.of(BottomSheetVisibilityEffect.Hide)
            )

            is LabelFormState.Data.Update -> currentState
            is LabelFormState.Loading -> currentState
        }
    }

    private fun reduceUpsellingInProgress(currentState: LabelFormState): LabelFormState {
        return when (currentState) {
            is LabelFormState.Data.Create -> currentState.copy(
                upsellingInProgress = Effect.of(TextUiModel(R.string.upselling_snackbar_upgrade_in_progress)),
                displayCreateLoader = false
            )

            is LabelFormState.Data.Update -> currentState
            is LabelFormState.Loading -> currentState
        }
    }

    private fun reduceShowDeleteDialog(currentState: LabelFormState): LabelFormState {
        return when (currentState) {
            is LabelFormState.Loading -> currentState
            is LabelFormState.Data.Create -> currentState
            is LabelFormState.Data.Update -> currentState.copy(
                confirmDeleteDialogState = DeleteDialogState.Shown(
                    title = TextUiModel.TextRes(R.string.delete_label),
                    message = TextUiModel.TextRes(R.string.delete_label_message)
                )
            )
        }
    }

    private fun reduceHideDeleteDialog(currentState: LabelFormState): LabelFormState {
        return when (currentState) {
            is LabelFormState.Loading -> currentState
            is LabelFormState.Data.Create -> currentState
            is LabelFormState.Data.Update -> currentState.copy(
                confirmDeleteDialogState = DeleteDialogState.Hidden
            )
        }
    }
}
