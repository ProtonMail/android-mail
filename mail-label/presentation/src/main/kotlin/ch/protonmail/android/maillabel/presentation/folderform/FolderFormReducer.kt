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

package ch.protonmail.android.maillabel.presentation.folderform

import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailcommon.presentation.ui.delete.DeleteDialogState
import ch.protonmail.android.maillabel.presentation.R
import ch.protonmail.android.mailupselling.presentation.model.BottomSheetVisibilityEffect
import me.proton.core.label.domain.entity.Label
import javax.inject.Inject

class FolderFormReducer @Inject constructor() {

    @SuppressWarnings("ComplexMethod")
    internal fun newStateFrom(currentState: FolderFormState, event: FolderFormEvent): FolderFormState {
        return when (event) {
            is FolderFormEvent.FolderLoaded -> reduceFolderLoaded(event)
            is FolderFormEvent.UpdateFolderColor -> reduceUpdateFolderColor(currentState, event.color)
            is FolderFormEvent.UpdateFolderName -> reduceUpdateFolderName(currentState, event.name)
            is FolderFormEvent.UpdateFolderNotifications -> reduceUpdateFolderNotifications(
                currentState,
                event.enabled
            )
            is FolderFormEvent.UpdateFolderParent -> reduceUpdateFolderParent(currentState, event.parent)
            FolderFormEvent.FolderCreated -> reduceFolderCreated(currentState)
            FolderFormEvent.FolderDeleted -> reduceFolderDeleted(currentState)
            FolderFormEvent.FolderUpdated -> reduceFolderUpdated(currentState)
            FolderFormEvent.FolderAlreadyExists -> reduceFolderAlreadyExists(currentState)
            FolderFormEvent.FolderLimitReached -> reduceFolderLimitReached(currentState)
            FolderFormEvent.SaveFolderError -> reduceSaveFolderError(currentState)
            FolderFormEvent.CloseFolderForm -> reduceCloseFolderForm(currentState)
            FolderFormEvent.LoadFolderError -> reduceLoadFolderError()
            FolderFormEvent.CreatingFolder -> reduceCreatingFolder(currentState)
            FolderFormEvent.HideUpselling -> reduceHideUpselling(currentState)
            FolderFormEvent.ShowUpselling -> reduceShowUpselling(currentState)
            FolderFormEvent.UpsellingInProgress -> reduceUpsellingInProgress(currentState)
            FolderFormEvent.ShowDeleteDialog -> reduceShowDeleteDialog(currentState)
            FolderFormEvent.HideDeleteDialog -> reduceHideDeleteDialog(currentState)
        }
    }

    private fun reduceFolderLoaded(event: FolderFormEvent.FolderLoaded): FolderFormState {
        val displayColorPicker = displayColorPicker(
            hasParent = event.parent != null,
            useFolderColor = event.useFolderColor,
            inheritParentFolderColor = event.inheritParentFolderColor
        )
        return if (event.labelId != null) {
            FolderFormState.Data.Update(
                isSaveEnabled = event.name.isNotEmpty(),
                labelId = event.labelId,
                name = event.name,
                color = event.color,
                parent = event.parent,
                notifications = event.notifications,
                colorList = event.colorList,
                displayColorPicker = displayColorPicker,
                useFolderColor = event.useFolderColor,
                inheritParentFolderColor = event.inheritParentFolderColor
            )
        } else {
            FolderFormState.Data.Create(
                isSaveEnabled = event.name.isNotEmpty(),
                name = event.name,
                color = event.color,
                parent = event.parent,
                notifications = event.notifications,
                colorList = event.colorList,
                displayColorPicker = displayColorPicker,
                useFolderColor = event.useFolderColor,
                inheritParentFolderColor = event.inheritParentFolderColor
            )
        }
    }

    private fun reduceUpdateFolderName(currentState: FolderFormState, name: String): FolderFormState {
        return when (currentState) {
            is FolderFormState.Data.Create -> currentState.copy(isSaveEnabled = name.isNotEmpty(), name = name)
            is FolderFormState.Data.Update -> currentState.copy(isSaveEnabled = name.isNotEmpty(), name = name)
            is FolderFormState.Loading -> currentState
        }
    }

    private fun reduceUpdateFolderColor(currentState: FolderFormState, color: String): FolderFormState {
        return when (currentState) {
            is FolderFormState.Data.Create -> currentState.copy(color = color)
            is FolderFormState.Data.Update -> currentState.copy(color = color)
            is FolderFormState.Loading -> currentState
        }
    }

    private fun reduceUpdateFolderParent(currentState: FolderFormState, parent: Label?): FolderFormState {
        return when (currentState) {
            is FolderFormState.Data -> {
                val displayColorPicker = displayColorPicker(
                    hasParent = parent != null,
                    useFolderColor = currentState.useFolderColor,
                    inheritParentFolderColor = currentState.inheritParentFolderColor
                )
                when (currentState) {
                    is FolderFormState.Data.Create -> currentState.copy(
                        parent = parent,
                        displayColorPicker = displayColorPicker
                    )
                    is FolderFormState.Data.Update -> currentState.copy(
                        parent = parent,
                        displayColorPicker = displayColorPicker
                    )
                }
            }
            is FolderFormState.Loading -> currentState
        }
    }

    private fun reduceUpdateFolderNotifications(
        currentState: FolderFormState,
        notifications: Boolean
    ): FolderFormState {
        return when (currentState) {
            is FolderFormState.Data.Create -> currentState.copy(notifications = notifications)
            is FolderFormState.Data.Update -> currentState.copy(notifications = notifications)
            is FolderFormState.Loading -> currentState
        }
    }

    private fun reduceCloseFolderForm(currentState: FolderFormState): FolderFormState {
        return when (currentState) {
            is FolderFormState.Data.Create -> currentState.copy(close = Effect.of(Unit))
            is FolderFormState.Data.Update -> currentState.copy(close = Effect.of(Unit))
            is FolderFormState.Loading -> currentState.copy(close = Effect.of(Unit))
        }
    }

    private fun reduceFolderCreated(currentState: FolderFormState): FolderFormState {
        return when (currentState) {
            is FolderFormState.Data.Create -> currentState.copy(
                closeWithSuccess = Effect.of(TextUiModel(R.string.folder_saved))
            )
            is FolderFormState.Data.Update -> currentState.copy(
                closeWithSuccess = Effect.of(TextUiModel(R.string.folder_saved))
            )
            is FolderFormState.Loading -> currentState
        }
    }

    private fun reduceFolderDeleted(currentState: FolderFormState): FolderFormState {
        return when (currentState) {
            is FolderFormState.Data.Create -> currentState
            is FolderFormState.Data.Update -> currentState.copy(
                closeWithSuccess = Effect.of(TextUiModel(R.string.folder_deleted)),
                confirmDeleteDialogState = DeleteDialogState.Hidden
            )
            is FolderFormState.Loading -> currentState
        }
    }

    private fun reduceFolderUpdated(currentState: FolderFormState): FolderFormState {
        return when (currentState) {
            is FolderFormState.Data.Create -> currentState.copy(
                closeWithSuccess = Effect.of(TextUiModel(R.string.folder_saved))
            )
            is FolderFormState.Data.Update -> currentState.copy(
                closeWithSuccess = Effect.of(TextUiModel(R.string.folder_saved))
            )
            is FolderFormState.Loading -> currentState
        }
    }

    private fun reduceFolderAlreadyExists(currentState: FolderFormState): FolderFormState {
        return when (currentState) {
            is FolderFormState.Data.Create -> currentState.copy(
                showErrorSnackbar = Effect.of(TextUiModel(R.string.label_already_exists)),
                displayCreateLoader = false
            )
            is FolderFormState.Data.Update -> currentState.copy(
                showErrorSnackbar = Effect.of(TextUiModel(R.string.label_already_exists))
            )
            is FolderFormState.Loading -> currentState
        }
    }

    private fun reduceFolderLimitReached(currentState: FolderFormState): FolderFormState {
        return when (currentState) {
            is FolderFormState.Data.Create -> currentState.copy(
                showNormSnackbar = Effect.of(TextUiModel(R.string.folder_limit_reached_error)),
                displayCreateLoader = false
            )
            is FolderFormState.Data.Update -> currentState.copy(
                showNormSnackbar = Effect.of(TextUiModel(R.string.folder_limit_reached_error))
            )
            is FolderFormState.Loading -> currentState
        }
    }

    private fun reduceSaveFolderError(currentState: FolderFormState): FolderFormState {
        return when (currentState) {
            is FolderFormState.Data.Create -> currentState.copy(
                showErrorSnackbar = Effect.of(TextUiModel(R.string.save_folder_error)),
                displayCreateLoader = false
            )
            is FolderFormState.Data.Update -> currentState.copy(
                showErrorSnackbar = Effect.of(TextUiModel(R.string.save_folder_error))
            )
            is FolderFormState.Loading -> currentState
        }
    }

    private fun reduceLoadFolderError() =
        FolderFormState.Loading(errorLoading = Effect.of(TextUiModel(R.string.folder_loading_error)))

    private fun reduceCreatingFolder(currentState: FolderFormState): FolderFormState {
        return when (currentState) {
            is FolderFormState.Data.Create -> currentState.copy(displayCreateLoader = true)
            is FolderFormState.Data.Update -> currentState
            is FolderFormState.Loading -> currentState
        }
    }

    private fun reduceShowUpselling(currentState: FolderFormState): FolderFormState {
        return when (currentState) {
            is FolderFormState.Data.Create -> currentState.copy(
                upsellingVisibility = Effect.of(BottomSheetVisibilityEffect.Show),
                displayCreateLoader = false
            )

            is FolderFormState.Data.Update -> currentState
            is FolderFormState.Loading -> currentState
        }
    }

    private fun reduceHideUpselling(currentState: FolderFormState): FolderFormState {
        return when (currentState) {
            is FolderFormState.Data.Create -> currentState.copy(
                upsellingVisibility = Effect.of(BottomSheetVisibilityEffect.Hide)
            )

            is FolderFormState.Data.Update -> currentState
            is FolderFormState.Loading -> currentState
        }
    }

    private fun reduceUpsellingInProgress(currentState: FolderFormState): FolderFormState {
        return when (currentState) {
            is FolderFormState.Data.Create -> currentState.copy(
                upsellingInProgress = Effect.of(TextUiModel(R.string.upselling_snackbar_upgrade_in_progress)),
                displayCreateLoader = false
            )

            is FolderFormState.Data.Update -> currentState
            is FolderFormState.Loading -> currentState
        }
    }

    private fun displayColorPicker(
        hasParent: Boolean,
        useFolderColor: Boolean,
        inheritParentFolderColor: Boolean
    ): Boolean {
        return hasParent && useFolderColor && !inheritParentFolderColor ||
            !hasParent && useFolderColor
    }

    private fun reduceShowDeleteDialog(currentState: FolderFormState): FolderFormState {
        return when (currentState) {
            is FolderFormState.Loading -> currentState
            is FolderFormState.Data.Create -> currentState
            is FolderFormState.Data.Update -> currentState.copy(
                confirmDeleteDialogState = DeleteDialogState.Shown(
                    title = TextUiModel.TextRes(R.string.delete_folder),
                    message = TextUiModel.TextRes(R.string.delete_folder_message)
                )
            )
        }
    }

    private fun reduceHideDeleteDialog(currentState: FolderFormState): FolderFormState {
        return when (currentState) {
            is FolderFormState.Loading -> currentState
            is FolderFormState.Data.Create -> currentState
            is FolderFormState.Data.Update -> currentState.copy(
                confirmDeleteDialogState = DeleteDialogState.Hidden
            )
        }
    }
}
