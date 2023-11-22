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

package ch.protonmail.android.maillabel.presentation.folderlist

import ch.protonmail.android.mailcommon.presentation.Effect
import javax.inject.Inject

class FolderListReducer @Inject constructor() {

    internal fun newStateFrom(currentState: FolderListState, operation: FolderListEvent): FolderListState {
        return when (operation) {
            is FolderListEvent.FolderListLoaded -> reduceFolderListLoaded(currentState, operation)
            is FolderListEvent.ErrorLoadingFolderList -> FolderListState.Loading(errorLoading = Effect.of(Unit))
            is FolderListEvent.OpenFolderForm -> reduceOpenFolderForm(currentState)
            is FolderListEvent.DismissSettings -> reduceDismissSettings(currentState)
            is FolderListEvent.OpenSettings -> reduceOpenSettings(currentState)
            is FolderListEvent.UseFolderColorChanged -> reduceUseFolderColorChanged(currentState, operation)
            is FolderListEvent.InheritParentFolderColorChanged -> {
                reduceInheritParentFolderColorChanged(currentState, operation)
            }
        }
    }

    private fun reduceFolderListLoaded(
        currentState: FolderListState,
        operation: FolderListEvent.FolderListLoaded
    ): FolderListState {
        return when (currentState) {
            is FolderListState.Loading -> {
                if (operation.folderList.isNotEmpty()) {
                    FolderListState.ListLoaded.Data(
                        useFolderColor = operation.useFolderColor,
                        inheritParentFolderColor = operation.inheritParentFolderColor,
                        folders = operation.folderList
                    )
                } else FolderListState.ListLoaded.Empty(
                    useFolderColor = operation.useFolderColor,
                    inheritParentFolderColor = operation.inheritParentFolderColor
                )
            }
            is FolderListState.ListLoaded -> {
                if (operation.folderList.isNotEmpty()) {
                    FolderListState.ListLoaded.Data(
                        bottomSheetVisibilityEffect = currentState.bottomSheetVisibilityEffect,
                        useFolderColor = operation.useFolderColor,
                        inheritParentFolderColor = operation.inheritParentFolderColor,
                        openFolderForm = currentState.openFolderForm,
                        folders = operation.folderList
                    )
                } else {
                    FolderListState.ListLoaded.Empty(
                        bottomSheetVisibilityEffect = currentState.bottomSheetVisibilityEffect,
                        useFolderColor = operation.useFolderColor,
                        inheritParentFolderColor = operation.inheritParentFolderColor,
                        openFolderForm = currentState.openFolderForm
                    )
                }
            }
        }
    }

    private fun reduceOpenFolderForm(currentState: FolderListState): FolderListState {
        return when (currentState) {
            is FolderListState.Loading -> currentState
            is FolderListState.ListLoaded.Data -> currentState.copy(openFolderForm = Effect.of(Unit))
            is FolderListState.ListLoaded.Empty -> currentState.copy(openFolderForm = Effect.of(Unit))
        }
    }

    private fun reduceOpenSettings(currentState: FolderListState): FolderListState {
        return when (currentState) {
            is FolderListState.Loading -> currentState
            is FolderListState.ListLoaded.Data -> currentState.copy(
                bottomSheetVisibilityEffect = Effect.of(BottomSheetVisibilityEffect.Show)
            )
            is FolderListState.ListLoaded.Empty -> currentState.copy(
                bottomSheetVisibilityEffect = Effect.of(BottomSheetVisibilityEffect.Show)
            )
        }
    }

    private fun reduceDismissSettings(currentState: FolderListState): FolderListState {
        return when (currentState) {
            is FolderListState.Loading -> currentState
            is FolderListState.ListLoaded.Data -> currentState.copy(
                bottomSheetVisibilityEffect = Effect.of(BottomSheetVisibilityEffect.Hide)
            )
            is FolderListState.ListLoaded.Empty -> currentState.copy(
                bottomSheetVisibilityEffect = Effect.of(BottomSheetVisibilityEffect.Hide)
            )
        }
    }

    private fun reduceUseFolderColorChanged(
        currentState: FolderListState,
        operation: FolderListEvent.UseFolderColorChanged
    ): FolderListState {
        return when (currentState) {
            is FolderListState.ListLoaded.Data -> currentState.copy(useFolderColor = operation.useFolderColor)
            is FolderListState.ListLoaded.Empty -> currentState.copy(useFolderColor = operation.useFolderColor)
            is FolderListState.Loading -> currentState
        }
    }

    private fun reduceInheritParentFolderColorChanged(
        currentState: FolderListState,
        operation: FolderListEvent.InheritParentFolderColorChanged
    ): FolderListState {
        return when (currentState) {
            is FolderListState.ListLoaded.Data -> currentState.copy(useFolderColor = operation.inheritParentFolderColor)
            is FolderListState.ListLoaded.Empty -> {
                currentState.copy(useFolderColor = operation.inheritParentFolderColor)
            }
            is FolderListState.Loading -> currentState
        }
    }
}
