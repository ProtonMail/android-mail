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
import ch.protonmail.android.design.compose.theme.ProtonDimens
import ch.protonmail.android.mailcategory.presentation.design.activeCategoryColor
import ch.protonmail.android.mailcategory.presentation.mapper.categoryIconRes
import ch.protonmail.android.mailcategory.presentation.mapper.categoryTextRes
import ch.protonmail.android.mailcommon.presentation.model.NullCountPolicy
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailcommon.presentation.model.ZeroCountPolicy
import ch.protonmail.android.mailcommon.presentation.model.toCappedNumberUiModel
import ch.protonmail.android.maillabel.domain.model.LabelId
import ch.protonmail.android.maillabel.domain.model.MailLabel
import ch.protonmail.android.maillabel.domain.model.MailLabelId
import ch.protonmail.android.maillabel.domain.model.MailLabels
import ch.protonmail.android.maillabel.presentation.bottomsheet.moveto.MoveToBottomSheetDestinationUiModel
import ch.protonmail.android.design.compose.R as presentationComposeR

fun MailLabels.toUiModels(counters: Map<LabelId, Int?>, selected: MailLabelId): MailLabelsUiModel = MailLabelsUiModel(
    systemLabels = system.map { it.toDynamicSystemUiModel(counters, selected) },
    folders = folders.map { it.toCustomUiModel(counters, selected) },
    labels = labels.map { it.toCustomUiModel(counters, selected) }
)

fun MailLabel.toUiModel(counters: Map<LabelId, Int?>, selected: MailLabelId): MailLabelUiModel = when (this) {
    is MailLabel.Category -> throw UnsupportedOperationException()
    is MailLabel.Custom -> toCustomUiModel(counters, selected)
    is MailLabel.System -> toDynamicSystemUiModel(counters, selected)
}

fun MailLabels.toUiModels(): MailLabelsUiModel = MailLabelsUiModel(
    systemLabels = system.map { it.toDynamicSystemUiModel(emptyMap(), null) },
    folders = folders.map { it.toCustomUiModel(emptyMap(), null) },
    labels = labels.map { it.toCustomUiModel(emptyMap(), null) }
)

fun MailLabel.System.toDynamicSystemUiModel(
    counters: Map<LabelId, Int?>,
    selected: MailLabelId?
): MailLabelUiModel.System = MailLabelUiModel.System(
    id = id,
    text = text() as TextUiModel.TextRes,
    icon = iconRes(),
    iconTint = iconTintColor(),
    isSelected = id.labelId == selected?.labelId,
    count = counters[id.labelId].toCappedNumberUiModel(
        nullPolicy = NullCountPolicy.Empty,
        zeroPolicy = ZeroCountPolicy.Empty
    )
)

fun MailLabel.Custom.toCustomUiModel(counters: Map<LabelId, Int?>, selected: MailLabelId?): MailLabelUiModel.Custom =
    MailLabelUiModel.Custom(
        id = id,
        text = text() as TextUiModel.Text,
        icon = iconRes(),
        iconTint = iconTintColor(),
        isVisible = generateSequence(parent) { it.parent }.all { it.isExpanded },
        isExpanded = isExpanded,
        isSelected = id.labelId == selected?.labelId,
        iconPaddingStart = ProtonDimens.Spacing.Large * level,
        hasChildren = children.isNotEmpty(),
        count = counters[id.labelId].toCappedNumberUiModel(
            nullPolicy = NullCountPolicy.Empty,
            zeroPolicy = ZeroCountPolicy.Empty
        )
    )

fun MailLabel.text(): TextUiModel = when (this) {
    is MailLabel.Category -> TextUiModel.TextRes(categorySystemLabelId.categoryTextRes())
    is MailLabel.Custom -> TextUiModel.Text(text)
    is MailLabel.System -> TextUiModel.TextRes(systemLabelId.textRes())
}

@DrawableRes
fun MailLabel.iconRes(): Int = when (this) {
    is MailLabel.Category -> this.categorySystemLabelId.categoryIconRes()
    is MailLabel.Custom -> when (id) {
        is MailLabelId.Custom.Label -> presentationComposeR.drawable.ic_proton_circle_filled_small
        is MailLabelId.Custom.Folder -> {
            val useFolderColor = color != null
            when {
                useFolderColor -> when {
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
    is MailLabel.System -> systemLabelId.iconRes()
}

fun MailLabel.iconTintColor(): Color? = when (this) {
    is MailLabel.Category -> categorySystemLabelId.activeCategoryColor()
    is MailLabel.Custom -> when (id) {
        is MailLabelId.Custom.Label -> color?.let(::Color)
        is MailLabelId.Custom.Folder -> color?.let(::Color)
    }
    is MailLabel.System -> null
}

fun List<MailLabel.Category>?.toMoveToInboxCategories(): List<MoveToBottomSheetDestinationUiModel.Inbox.Category> =
    this.orEmpty().map { category -> category.toMoveToInboxCategory() }

private fun MailLabel.Category.toMoveToInboxCategory() = MoveToBottomSheetDestinationUiModel.Inbox.Category(
    id = id,
    text = TextUiModel.TextRes(categorySystemLabelId.categoryTextRes()),
    icon = categorySystemLabelId.categoryIconRes(),
    iconTint = categorySystemLabelId.activeCategoryColor()
)
