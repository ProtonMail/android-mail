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

package ch.protonmail.android.maillabel.presentation.folderparentlist

import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.maillabel.presentation.R
import javax.inject.Inject

class ParentFolderListReducer @Inject constructor() {

    internal fun newStateFrom(
        currentState: ParentFolderListState,
        event: ParentFolderListEvent
    ): ParentFolderListState {
        return when (event) {
            is ParentFolderListEvent.FolderListLoaded -> reduceFolderListLoaded(currentState, event)
            is ParentFolderListEvent.ErrorLoadingFolderList -> reduceErrorLoadingFolderList()
        }
    }

    private fun reduceFolderListLoaded(
        currentState: ParentFolderListState,
        event: ParentFolderListEvent.FolderListLoaded
    ): ParentFolderListState {
        return when (currentState) {
            is ParentFolderListState.Loading -> {
                if (event.folderList.isNotEmpty()) {
                    ParentFolderListState.ListLoaded.Data(
                        useFolderColor = event.useFolderColor,
                        inheritParentFolderColor = event.inheritParentFolderColor,
                        labelId = event.labelId,
                        parentLabelId = event.parentLabelId,
                        folders = event.folderList
                    )
                } else ParentFolderListState.ListLoaded.Empty(
                    labelId = event.labelId,
                    parentLabelId = event.parentLabelId
                )
            }
            is ParentFolderListState.ListLoaded -> {
                if (event.folderList.isNotEmpty()) {
                    ParentFolderListState.ListLoaded.Data(
                        useFolderColor = event.useFolderColor,
                        inheritParentFolderColor = event.inheritParentFolderColor,
                        labelId = event.labelId,
                        parentLabelId = event.parentLabelId,
                        folders = event.folderList
                    )
                } else {
                    ParentFolderListState.ListLoaded.Empty(
                        labelId = event.labelId,
                        parentLabelId = event.parentLabelId
                    )
                }
            }
        }
    }

    private fun reduceErrorLoadingFolderList() =
        ParentFolderListState.Loading(errorLoading = Effect.of(TextUiModel(R.string.folder_list_loading_error)))
}
