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
import ch.protonmail.android.maillabel.presentation.model.MailboxItemLabelUiModel

object MailboxItemLabelsPreviewData {

    val NineItems = listOf(
        MailboxItemLabelUiModel(name = "first", color = Color.Gray),
        MailboxItemLabelUiModel(name = "second", color = Color.Gray),
        MailboxItemLabelUiModel(name = "third", color = Color.Gray),
        MailboxItemLabelUiModel(name = "fourth", color = Color.Gray),
        MailboxItemLabelUiModel(name = "fifth", color = Color.Gray),
        MailboxItemLabelUiModel(name = "sixth", color = Color.Gray),
        MailboxItemLabelUiModel(name = "seventh", color = Color.Gray),
        MailboxItemLabelUiModel(name = "eighth", color = Color.Gray),
        MailboxItemLabelUiModel(name = "ninth", color = Color.Gray)
    )

    val OneVeryLongLabel = listOf(
        MailboxItemLabelUiModel(
            name = "just an extremely super ultra very long label that exceeds the screen width",
            color = Color.Gray
        )
    )

    val OneLabelAndAVeryLongLabel = listOf(
        MailboxItemLabelUiModel(name = "label", color = Color.Gray),
        OneVeryLongLabel.first()
    )

    val OneLabelAVeryLongLabelAndAnotherLabel = listOf(
        MailboxItemLabelUiModel(name = "label", color = Color.Gray),
        OneVeryLongLabel.first(),
        MailboxItemLabelUiModel(name = "label", color = Color.Gray)
    )

    val ThreeItems = listOf(
        MailboxItemLabelUiModel(name = "first", color = Color.Gray),
        MailboxItemLabelUiModel(name = "second", color = Color.Gray),
        MailboxItemLabelUiModel(name = "third", color = Color.Gray)
    )
}

class MailboxItemLabelsPreviewDataProvider : PreviewParameterProvider<List<MailboxItemLabelUiModel>> {

    override val values = sequenceOf(
        MailboxItemLabelsPreviewData.NineItems,
        MailboxItemLabelsPreviewData.OneLabelAndAVeryLongLabel,
        MailboxItemLabelsPreviewData.OneLabelAVeryLongLabelAndAnotherLabel,
        MailboxItemLabelsPreviewData.OneVeryLongLabel,
        MailboxItemLabelsPreviewData.ThreeItems
    )
}
