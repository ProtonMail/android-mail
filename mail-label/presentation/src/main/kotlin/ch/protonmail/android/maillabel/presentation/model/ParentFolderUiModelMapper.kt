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

package ch.protonmail.android.maillabel.presentation.model

import me.proton.core.label.domain.entity.LabelId

internal fun List<FolderUiModel>.toParentFolderUiModel(
    labelId: LabelId?,
    parentLabelId: LabelId?
): List<ParentFolderUiModel> {
    return this.mapIndexed { index, folder ->
        val isDifferentLabelId = folder.id != labelId
        val isRootOrFirstLevel = folder.level < 2
        val isFolderChildOfSelectedLabelId = folder.parent != null && folder.parent.id.id == labelId?.id

        ParentFolderUiModel(
            folder = folder,
            isEnabled = isDifferentLabelId && isRootOrFirstLevel && !isFolderChildOfSelectedLabelId,
            isSelected = parentLabelId == folder.id,
            displayDivider = index != 0 && folder.parent == null
        )
    }
}
