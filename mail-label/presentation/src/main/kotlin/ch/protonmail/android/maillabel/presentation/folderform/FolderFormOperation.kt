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

import androidx.compose.ui.graphics.Color
import me.proton.core.label.domain.entity.Label
import me.proton.core.label.domain.entity.LabelId

sealed interface FolderFormOperation

internal sealed interface FolderFormViewAction : FolderFormOperation {
    data class FolderNameChanged(val name: String) : FolderFormViewAction
    data class FolderColorChanged(val color: Color) : FolderFormViewAction
    data class FolderParentChanged(val parentId: LabelId) : FolderFormViewAction
    data class FolderNotificationsChanged(val enabled: Boolean) : FolderFormViewAction
    object OnSaveClick : FolderFormViewAction
    object OnDeleteClick : FolderFormViewAction
    object OnCloseFolderFormClick : FolderFormViewAction
}

sealed interface FolderFormEvent : FolderFormOperation {
    data class FolderLoaded(
        val labelId: LabelId?,
        val name: String,
        val color: String,
        val parent: Label?,
        val notifications: Boolean,
        val colorList: List<Color>,
        val useFolderColor: Boolean,
        val inheritParentFolderColor: Boolean
    ) : FolderFormEvent
    data class UpdateFolderName(
        val name: String
    ) : FolderFormEvent
    data class UpdateFolderColor(
        val color: String
    ) : FolderFormEvent
    data class UpdateFolderParent(
        val parent: Label?
    ) : FolderFormEvent
    data class UpdateFolderNotifications(
        val enabled: Boolean
    ) : FolderFormEvent
    object FolderCreated : FolderFormEvent
    object FolderUpdated : FolderFormEvent
    object FolderDeleted : FolderFormEvent
    object FolderAlreadyExists : FolderFormEvent
    object FolderLimitReached : FolderFormEvent
    object LoadFolderError : FolderFormEvent
    object SaveFolderError : FolderFormEvent
    object CloseFolderForm : FolderFormEvent
}
