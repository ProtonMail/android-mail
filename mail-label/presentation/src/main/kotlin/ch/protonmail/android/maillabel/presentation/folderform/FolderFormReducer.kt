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
import ch.protonmail.android.maillabel.presentation.R
import me.proton.core.label.domain.entity.Label
import javax.inject.Inject

class FolderFormReducer @Inject constructor() {

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
        }
    }

    private fun reduceFolderLoaded(event: FolderFormEvent.FolderLoaded): FolderFormState {
        return if (event.labelId != null) {
            FolderFormState.Data.Update(
                isSaveEnabled = event.name.isNotEmpty(),
                labelId = event.labelId,
                name = event.name,
                color = event.color,
                parent = event.parent,
                notifications = event.notifications,
                colorList = event.colorList
            )
        } else {
            FolderFormState.Data.Create(
                isSaveEnabled = event.name.isNotEmpty(),
                name = event.name,
                color = event.color,
                parent = event.parent,
                notifications = event.notifications,
                colorList = event.colorList
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
            is FolderFormState.Data.Create -> currentState.copy(parent = parent)
            is FolderFormState.Data.Update -> currentState.copy(parent = parent)
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
                closeWithSuccess = Effect.of(TextUiModel(R.string.folder_deleted))
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
                showErrorSnackbar = Effect.of(TextUiModel(R.string.label_already_exists))
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
                showErrorSnackbar = Effect.of(TextUiModel(R.string.folder_limit_reached_error))
            )
            is FolderFormState.Data.Update -> currentState.copy(
                showErrorSnackbar = Effect.of(TextUiModel(R.string.folder_limit_reached_error))
            )
            is FolderFormState.Loading -> currentState
        }
    }

    private fun reduceSaveFolderError(currentState: FolderFormState): FolderFormState {
        return when (currentState) {
            is FolderFormState.Data.Create -> currentState.copy(
                showErrorSnackbar = Effect.of(TextUiModel(R.string.save_folder_error))
            )
            is FolderFormState.Data.Update -> currentState.copy(
                showErrorSnackbar = Effect.of(TextUiModel(R.string.save_folder_error))
            )
            is FolderFormState.Loading -> currentState
        }
    }
}
