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

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color
import arrow.core.getOrElse
import ch.protonmail.android.mailcommon.presentation.mapper.ColorMapper
import ch.protonmail.android.maillabel.presentation.R
import ch.protonmail.android.mailsettings.domain.model.FolderColorSettings
import me.proton.core.label.domain.entity.Label
import me.proton.core.label.domain.entity.LabelId

fun List<Label>.toFolderUiModel(
    folderColorSettings: FolderColorSettings,
    colorMapper: ColorMapper
): List<FolderUiModel> {
    val labelById = associateBy { it.labelId }
    val groupByParentId = groupBy { it.parentId }
    val mailLabels = mutableMapOf<LabelId, FolderUiModel>()
    fun getChildren(labelId: LabelId): List<Label> = groupByParentId[labelId].orEmpty().sortedBy { it.order }
    fun getMailLabel(labelId: LabelId): FolderUiModel = mailLabels.getOrPut(labelId) {
        val label = requireNotNull(labelById[labelId])
        label.toMailLabelCustom(::getMailLabel, ::getChildren, folderColorSettings, colorMapper)
    }
    return mapNotNull { label ->
        if (label.parentId == null || this.any { label.parentId == it.labelId }) {
            getMailLabel(label.labelId)
        } else null
    }.sortedBy { it.order }.orderByParent()
}

private fun Label.toMailLabelCustom(
    getMailLabel: (LabelId) -> FolderUiModel,
    getChildren: (LabelId) -> List<Label>,
    folderColorSettings: FolderColorSettings,
    colorMapper: ColorMapper
): FolderUiModel {
    val parent = parentId?.let(getMailLabel)
    val children = getChildren(labelId).map { it.labelId }
    val level = parent?.level?.plus(1) ?: 0
    val folderColor = colorMapper.toColor(color).getOrElse { Color.Black }
    return FolderUiModel(
        id = labelId,
        name = name,
        color = folderColor,
        displayColor = getDisplayColor(folderColorSettings, folderColor, parent),
        parent = parent,
        level = level,
        order = order,
        children = children,
        icon = getFolderIcon(children.isNotEmpty(), folderColorSettings.useFolderColor)
    )
}

private fun getDisplayColor(
    folderColorSettings: FolderColorSettings,
    folderColor: Color,
    parent: FolderUiModel?
): Color? {
    return if (folderColorSettings.useFolderColor &&
        folderColorSettings.inheritParentFolderColor
    ) {
        var parentFolder = parent
        while (parentFolder?.parent != null) {
            parentFolder = parentFolder.parent
        }
        parentFolder?.color ?: folderColor
    } else if (folderColorSettings.useFolderColor) {
        folderColor
    } else null
}

@DrawableRes
private fun getFolderIcon(hasChildren: Boolean, useFolderColor: Boolean): Int {
    return if (hasChildren) {
        if (useFolderColor) R.drawable.ic_proton_folders_filled
        else R.drawable.ic_proton_folders
    } else {
        if (useFolderColor) R.drawable.ic_proton_folder_filled
        else R.drawable.ic_proton_folder
    }
}

private fun List<FolderUiModel>.orderByParent(): List<FolderUiModel> {
    val groupByParent = groupBy { it.parent }
    fun List<FolderUiModel>.itemsAndChildren(): List<FolderUiModel> = fold(emptyList()) { acc, item ->
        acc + item + groupByParent[item].orEmpty().itemsAndChildren()
    }
    return groupByParent[null].orEmpty().itemsAndChildren()
}
