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
        LabelUiModel(name = "first", color = Color.Gray, id = "first"),
        LabelUiModel(name = "second", color = Color.Gray, id = "second"),
        LabelUiModel(name = "third", color = Color.Gray, id = "third"),
        LabelUiModel(name = "fourth", color = Color.Gray, id = "fourth"),
        LabelUiModel(name = "fifth", color = Color.Gray, id = "fifth"),
        LabelUiModel(name = "sixth", color = Color.Gray, id = "sixth"),
        LabelUiModel(name = "seventh", color = Color.Gray, id = "seventh"),
        LabelUiModel(name = "eighth", color = Color.Gray, id = "eighth"),
        LabelUiModel(name = "ninth", color = Color.Gray, id = "ninth")
    )

    val OneVeryLongLabel = listOf(
        LabelUiModel(
            name = "just an extremely super ultra very long label that exceeds the screen width",
            color = Color.Gray,
            id = "idGray"
        )
    )

    val OneLabelAndAVeryLongLabel = listOf(
        LabelUiModel(name = "label", color = Color.Gray, id = "idGray"),
        OneVeryLongLabel.first()
    )

    val OneLabelAVeryLongLabelAndAnotherLabel = listOf(
        LabelUiModel(name = "label", color = Color.Gray, id = "idGray1"),
        OneVeryLongLabel.first(),
        LabelUiModel(name = "label", color = Color.Gray, id = "idGray2")
    )

    val ThreeItems = listOf(
        LabelUiModel(name = "first", color = Color.Gray, id = "idGray1"),
        LabelUiModel(name = "second", color = Color.Gray, id = "idGray2"),
        LabelUiModel(name = "third", color = Color.Gray, id = "idGray3")
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
