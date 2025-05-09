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

package ch.protonmail.android.maillabel.presentation

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.maillabel.domain.model.MailLabel
import ch.protonmail.android.maillabel.domain.model.MailLabelId
import ch.protonmail.android.maillabel.domain.model.MailLabels
import ch.protonmail.android.mailsettings.domain.model.FolderColorSettings
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.label.domain.entity.LabelId

fun MailLabels.toUiModels(
    settings: FolderColorSettings,
    counters: Map<LabelId, Int?>,
    selected: MailLabelId
): MailLabelsUiModel = MailLabelsUiModel(
    systems = systemLabels.map { it.toSystemUiModel(settings, counters, selected) }.distinctBy { it.key },
    folders = folders.map { it.toCustomUiModel(settings, counters, selected) }.distinctBy { it.key },
    labels = labels.map { it.toCustomUiModel(settings, counters, selected) }.distinctBy { it.key }
)

fun MailLabel.toUiModel(
    settings: FolderColorSettings,
    counters: Map<LabelId, Int?>,
    selected: MailLabelId
): MailLabelUiModel = when (this) {
    is MailLabel.System -> toSystemUiModel(settings, counters, selected)
    is MailLabel.Custom -> toCustomUiModel(settings, counters, selected)
}

fun MailLabels.toUiModels(settings: FolderColorSettings): MailLabelsUiModel = MailLabelsUiModel(
    systems = systemLabels.map { it.toSystemUiModel(settings, emptyMap(), null) }.distinctBy { it.key },
    folders = folders.map { it.toCustomUiModel(settings, emptyMap(), null) }.distinctBy { it.key },
    labels = labels.map { it.toCustomUiModel(settings, emptyMap(), null) }.distinctBy { it.key }
)

fun MailLabel.System.toSystemUiModel(
    settings: FolderColorSettings,
    counters: Map<LabelId, Int?>,
    selected: MailLabelId?
): MailLabelUiModel.System = MailLabelUiModel.System(
    id = id,
    key = id.labelId.id,
    text = text() as TextUiModel.TextRes,
    icon = iconRes(settings),
    iconTint = iconTintColor(settings),
    isSelected = id.labelId == selected?.labelId,
    count = counters[id.labelId]
)

fun MailLabel.Custom.toCustomUiModel(
    settings: FolderColorSettings,
    counters: Map<LabelId, Int?>,
    selected: MailLabelId?
): MailLabelUiModel.Custom = MailLabelUiModel.Custom(
    id = id,
    key = id.labelId.id,
    text = text() as TextUiModel.Text,
    icon = iconRes(settings),
    iconTint = iconTintColor(settings),
    isVisible = parent == null || parent?.isExpanded == true,
    isExpanded = isExpanded,
    isSelected = id.labelId == selected?.labelId,
    iconPaddingStart = ProtonDimens.DefaultSpacing * level,
    count = counters[id.labelId]
)

fun MailLabel.text(): TextUiModel = when (this) {
    is MailLabel.System -> TextUiModel.TextRes(id.systemLabelId.textRes())
    is MailLabel.Custom -> TextUiModel.Text(text)
}

@DrawableRes
fun MailLabel.iconRes(settings: FolderColorSettings): Int = when (this) {
    is MailLabel.System -> id.systemLabelId.iconRes()
    is MailLabel.Custom -> when (id) {
        is MailLabelId.Custom.Label -> R.drawable.ic_proton_circle_filled
        is MailLabelId.Custom.Folder -> when {
            settings.useFolderColor -> when {
                children.isEmpty() -> R.drawable.ic_proton_folder_filled
                else -> R.drawable.ic_proton_folders_filled
            }
            else -> when {
                children.isEmpty() -> R.drawable.ic_proton_folder
                else -> R.drawable.ic_proton_folders
            }
        }
    }
}

fun MailLabel.iconTintColor(settings: FolderColorSettings): Color? = when (this) {
    is MailLabel.System -> null
    is MailLabel.Custom -> when (id) {
        is MailLabelId.Custom.Label -> color
        is MailLabelId.Custom.Folder -> when {
            settings.useFolderColor.not() -> null
            settings.inheritParentFolderColor -> {
                var parentFolder = parent
                while (parentFolder?.parent != null) {
                    parentFolder = parentFolder.parent
                }
                parentFolder?.color ?: color
            }
            else -> color
        }
    }
}?.let { Color(it) }
