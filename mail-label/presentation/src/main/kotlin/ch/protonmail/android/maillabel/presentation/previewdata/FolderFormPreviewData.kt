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

package ch.protonmail.android.maillabel.presentation.previewdata

import ch.protonmail.android.maillabel.presentation.folderform.FolderFormState
import ch.protonmail.android.maillabel.presentation.getHexStringFromColor
import ch.protonmail.android.maillabel.presentation.previewdata.LabelListPreviewData.labelSampleData
import ch.protonmail.android.maillabel.presentation.sample.LabelColorListSample
import me.proton.core.label.domain.entity.LabelId

object FolderFormPreviewData {

    val createFolderFormState = FolderFormState.Data.Create(
        isSaveEnabled = true,
        name = "Folder name",
        color = LabelColorListSample.colorListSample().random().getHexStringFromColor(),
        parent = null,
        notifications = true,
        colorList = LabelColorListSample.colorListSample(),
        displayColorPicker = true,
        useFolderColor = true,
        inheritParentFolderColor = true
    )

    val editFolderFormState = FolderFormState.Data.Update(
        isSaveEnabled = true,
        name = "Folder name",
        color = LabelColorListSample.colorListSample().random().getHexStringFromColor(),
        parent = labelSampleData,
        notifications = true,
        colorList = LabelColorListSample.colorListSample(),
        displayColorPicker = true,
        useFolderColor = true,
        inheritParentFolderColor = true,
        labelId = LabelId("Label Id")
    )
}
