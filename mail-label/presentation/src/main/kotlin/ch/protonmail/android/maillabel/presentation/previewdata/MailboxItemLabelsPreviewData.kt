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

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import ch.protonmail.android.maillabel.presentation.model.LabelUiModel

object MailboxItemLabelsPreviewData {

    val NineItems = listOf(
        LabelUiModel(name = "first", color = Color.Gray),
        LabelUiModel(name = "second", color = Color.Gray),
        LabelUiModel(name = "third", color = Color.Gray),
        LabelUiModel(name = "fourth", color = Color.Gray),
        LabelUiModel(name = "fifth", color = Color.Gray),
        LabelUiModel(name = "sixth", color = Color.Gray),
        LabelUiModel(name = "seventh", color = Color.Gray),
        LabelUiModel(name = "eighth", color = Color.Gray),
        LabelUiModel(name = "ninth", color = Color.Gray)
    )

    val OneVeryLongLabel = listOf(
        LabelUiModel(
            name = "just an extremely super ultra very long label that exceeds the screen width",
            color = Color.Gray
        )
    )

    val OneLabelAndAVeryLongLabel = listOf(
        LabelUiModel(name = "label", color = Color.Gray),
        OneVeryLongLabel.first()
    )

    val OneLabelAVeryLongLabelAndAnotherLabel = listOf(
        LabelUiModel(name = "label", color = Color.Gray),
        OneVeryLongLabel.first(),
        LabelUiModel(name = "label", color = Color.Gray)
    )

    val ThreeItems = listOf(
        LabelUiModel(name = "first", color = Color.Gray),
        LabelUiModel(name = "second", color = Color.Gray),
        LabelUiModel(name = "third", color = Color.Gray)
    )
}

class MailboxItemLabelsPreviewDataProvider : PreviewParameterProvider<List<LabelUiModel>> {

    override val values = sequenceOf(
        MailboxItemLabelsPreviewData.NineItems,
        MailboxItemLabelsPreviewData.OneLabelAndAVeryLongLabel,
        MailboxItemLabelsPreviewData.OneLabelAVeryLongLabelAndAnotherLabel,
        MailboxItemLabelsPreviewData.OneVeryLongLabel,
        MailboxItemLabelsPreviewData.ThreeItems
    )
}
