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

package ch.protonmail.android.maildetail.presentation.mapper

import androidx.compose.ui.graphics.Color
import arrow.core.getOrElse
import ch.protonmail.android.mailcommon.presentation.mapper.ColorMapper
import ch.protonmail.android.maildetail.presentation.R
import ch.protonmail.android.maildetail.presentation.model.MessageLocationUiModel
import ch.protonmail.android.maillabel.domain.model.SystemLabelId
import ch.protonmail.android.maillabel.domain.usecase.GetRootLabel
import ch.protonmail.android.maillabel.presentation.iconRes
import ch.protonmail.android.mailsettings.domain.model.AutoDeleteSetting
import ch.protonmail.android.mailsettings.domain.model.FolderColorSettings
import me.proton.core.domain.entity.UserId
import me.proton.core.label.domain.entity.Label
import me.proton.core.label.domain.entity.LabelId
import me.proton.core.label.domain.entity.LabelType
import javax.inject.Inject

class MessageLocationUiModelMapper @Inject constructor(
    private val colorMapper: ColorMapper,
    private val getRootLabel: GetRootLabel
) {

    @Suppress("ReturnCount")
    suspend operator fun invoke(
        labelIds: List<LabelId>,
        labels: List<Label>,
        colorSettings: FolderColorSettings,
        autoDeleteSetting: AutoDeleteSetting
    ): MessageLocationUiModel {

        val autoDeleteIsEnabled = autoDeleteSetting == AutoDeleteSetting.Enabled

        SystemLabelId.autoDeleteList.forEach { systemLabelId ->
            if (autoDeleteIsEnabled && systemLabelId.labelId in labelIds) {
                return MessageLocationUiModel(
                    systemLabelId.name,
                    ch.protonmail.android.maillabel.presentation.R.drawable.ic_proton_trash_clock
                )
            }
        }

        SystemLabelId.exclusiveList.forEach { systemLabelId ->
            if (systemLabelId.labelId in labelIds) {
                return MessageLocationUiModel(
                    systemLabelId.name,
                    SystemLabelId.enumOf(systemLabelId.labelId.id).iconRes()
                )
            }
        }

        // Check if the location is a custom folder
        labels.forEach { label ->
            if (label.labelId in labelIds && label.type == LabelType.MessageFolder) {
                return MessageLocationUiModel(
                    name = label.name,
                    icon = when {
                        colorSettings.useFolderColor -> R.drawable.ic_proton_folder_filled
                        else -> R.drawable.ic_proton_folder
                    },
                    color = when {
                        colorSettings.useFolderColor -> getLocationIconColor(label.userId, label, colorSettings)
                        else -> null
                    }
                )
            }
        }

        // If no location has been found, then the message is orphaned and is only in All Mail
        return MessageLocationUiModel(
            SystemLabelId.AllMail.name,
            SystemLabelId.enumOf(SystemLabelId.AllMail.labelId.id).iconRes()
        )
    }

    private suspend fun getLocationIconColor(
        userId: UserId,
        label: Label,
        folderColorSettings: FolderColorSettings
    ): Color {
        val colorToMap = when {
            folderColorSettings.inheritParentFolderColor -> getRootLabel(userId, label).color
            else -> label.color
        }
        return colorMapper.toColor(colorToMap).getOrElse { Color.Unspecified }
    }
}
