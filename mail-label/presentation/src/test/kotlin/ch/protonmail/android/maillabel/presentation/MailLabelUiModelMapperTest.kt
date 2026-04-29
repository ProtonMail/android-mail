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

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import ch.protonmail.android.design.compose.theme.ProtonDimens
import ch.protonmail.android.mailcommon.presentation.model.CappedNumberUiModel
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.maillabel.domain.model.LabelId
import ch.protonmail.android.maillabel.domain.model.MailLabelId
import ch.protonmail.android.testdata.maillabel.MailLabelTestData.buildCustomFolder
import org.junit.Test
import kotlin.test.assertEquals

class MailLabelUiModelMapperTest {

    @Test
    fun `the mail labels are correctly mapped to ui models`() {
        // Given
        val selected = MailLabelId.Custom.Folder(LabelId("0"))
        val counters = emptyMap<LabelId, Int?>()

        val folder0 = buildCustomFolder("0", level = 0, order = 0, parent = null, children = listOf("0.1", "0.2"))
        val folder01 = buildCustomFolder("0.1", level = 1, order = 0, parent = folder0)
        val folder02 =
            buildCustomFolder("0.2", level = 1, order = 1, parent = folder0, children = listOf("0.2.1", "0.2.2"))
        val folder021 = buildCustomFolder("0.2.1", level = 2, order = 0, parent = folder02)
        val folder022 = buildCustomFolder("0.2.2", level = 2, order = 1, parent = folder02)

        val items = listOf(folder0, folder01, folder02, folder021, folder022)

        // When
        val actual = items.map { it.toCustomUiModel(counters, selected) }

        // Then
        val expected = listOf(
            MailLabelUiModel.Custom(
                id = MailLabelId.Custom.Folder(LabelId("0")),
                text = TextUiModel.Text("0"),
                icon = R.drawable.ic_proton_folders_filled,
                iconTint = Color(0),
                isSelected = true,
                count = CappedNumberUiModel.Empty,
                isVisible = true,
                isExpanded = true,
                iconPaddingStart = 0.0.dp,
                hasChildren = true
            ),
            MailLabelUiModel.Custom(
                id = MailLabelId.Custom.Folder(LabelId("0.1")),
                text = TextUiModel.Text("0.1"),
                icon = R.drawable.ic_proton_folder_filled,
                iconTint = Color(0),
                isSelected = false,
                count = CappedNumberUiModel.Empty,
                isVisible = true,
                isExpanded = true,
                iconPaddingStart = ProtonDimens.Spacing.Large
            ),
            MailLabelUiModel.Custom(
                id = MailLabelId.Custom.Folder(LabelId("0.2")),
                text = TextUiModel.Text("0.2"),
                icon = R.drawable.ic_proton_folders_filled,
                iconTint = Color(0),
                isSelected = false,
                count = CappedNumberUiModel.Empty,
                isVisible = true,
                isExpanded = true,
                iconPaddingStart = ProtonDimens.Spacing.Large,
                hasChildren = true
            ),
            MailLabelUiModel.Custom(
                id = MailLabelId.Custom.Folder(LabelId("0.2.1")),
                text = TextUiModel.Text("0.2.1"),
                icon = R.drawable.ic_proton_folder_filled,
                iconTint = Color(0),
                isSelected = false,
                count = CappedNumberUiModel.Empty,
                isVisible = true,
                isExpanded = true,
                iconPaddingStart = ProtonDimens.Spacing.Large * 2
            ),
            MailLabelUiModel.Custom(
                id = MailLabelId.Custom.Folder(LabelId("0.2.2")),
                text = TextUiModel.Text("0.2.2"),
                icon = R.drawable.ic_proton_folder_filled,
                iconTint = Color(0),
                isSelected = false,
                count = CappedNumberUiModel.Empty,
                isVisible = true,
                isExpanded = true,
                iconPaddingStart = ProtonDimens.Spacing.Large * 2
            )
        )
        assertEquals(expected = expected, actual = actual)
    }

    @Test
    fun `the mapped folder icons should use each folder's color when available`() {
        // Given
        val selected = MailLabelId.Custom.Folder(LabelId("0"))
        val counters = emptyMap<LabelId, Int?>()

        val folder0 = buildCustomFolder(
            "0",
            level = 0,
            color = Color.Green.toArgb(),
            order = 0,
            parent = null,
            children = emptyList()
        )
        val folder02 = buildCustomFolder(
            "0.2",
            level = 1,
            color = Color.Red.toArgb(),
            order = 1,
            parent = folder0,
            children = emptyList()
        )

        val items = listOf(folder0, folder02)

        // When
        val actual = items.map { it.toCustomUiModel(counters = counters, selected = selected) }

        // Then
        val expected = listOf(
            MailLabelUiModel.Custom(
                id = MailLabelId.Custom.Folder(LabelId("0")),
                text = TextUiModel.Text("0"),
                icon = R.drawable.ic_proton_folder_filled,
                iconTint = Color.Green,
                isSelected = true,
                count = CappedNumberUiModel.Empty,
                isVisible = true,
                isExpanded = true,
                iconPaddingStart = 0.0.dp
            ),
            MailLabelUiModel.Custom(
                id = MailLabelId.Custom.Folder(LabelId("0.2")),
                text = TextUiModel.Text("0.2"),
                icon = R.drawable.ic_proton_folder_filled,
                iconTint = Color.Red,
                isSelected = false,
                count = CappedNumberUiModel.Empty,
                isVisible = true,
                isExpanded = true,
                iconPaddingStart = ProtonDimens.Spacing.Large
            )
        )
        assertEquals(expected = expected, actual = actual)
    }

    @Test
    fun `the mapped folder icons should have no tint when the folder colors is not set`() {
        // Given
        val noFolderColor = null
        val folder0 = buildCustomFolder(
            "0",
            level = 0,
            color = noFolderColor,
            order = 0,
            parent = null,
            children = listOf("0.1", "0.2")
        )

        // When
        val actual = folder0.toCustomUiModel(
            counters = emptyMap(),
            selected = null
        )

        // Then
        val expected = MailLabelUiModel.Custom(
            id = MailLabelId.Custom.Folder(LabelId("0")),
            text = TextUiModel.Text("0"),
            icon = R.drawable.ic_proton_folders,
            iconTint = null,
            isSelected = false,
            count = CappedNumberUiModel.Empty,
            isVisible = true,
            isExpanded = true,
            iconPaddingStart = 0.0.dp,
            hasChildren = true
        )
        assertEquals(expected, actual)
    }
}
