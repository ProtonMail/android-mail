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

import androidx.compose.ui.tooling.preview.PreviewParameterProvider

object MailboxItemLabelsPreviewData {

    val NineItems = listOf(
        "first",
        "second",
        "third",
        "fourth",
        "fifth",
        "sixth",
        "seventh",
        "eighth",
        "ninth"
    )

    val OneVeryLongLabel = listOf(
        "just an extremely super ultra very long label that exceeds the screen width"
    )

    val OneLabelAndAVeryLongLabel = listOf(
        "label",
        OneVeryLongLabel.first()
    )

    val OneLabelAVeryLongLabelAndAnotherLabel = listOf(
        "label",
        OneVeryLongLabel.first(),
        "label"
    )

    val ThreeItems = listOf(
        "first",
        "second",
        "third"
    )
}

class MailboxItemLabelsPreviewDataProvider : PreviewParameterProvider<List<String>> {

    override val values = sequenceOf(
        MailboxItemLabelsPreviewData.NineItems,
        MailboxItemLabelsPreviewData.OneLabelAndAVeryLongLabel,
        MailboxItemLabelsPreviewData.OneLabelAVeryLongLabelAndAnotherLabel,
        MailboxItemLabelsPreviewData.OneVeryLongLabel,
        MailboxItemLabelsPreviewData.ThreeItems
    )
}
