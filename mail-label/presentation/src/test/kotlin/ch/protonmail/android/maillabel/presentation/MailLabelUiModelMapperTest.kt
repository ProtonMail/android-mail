/*
 * Copyright (c) 2021 Proton Technologies AG
 * This file is part of Proton Technologies AG and ProtonMail.
 *
 * ProtonMail is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ProtonMail is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with ProtonMail.  If not, see <https://www.gnu.org/licenses/>.
 */

package ch.protonmail.android.maillabel.presentation

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.maillabel.domain.model.MailLabelId
import ch.protonmail.android.mailsettings.domain.model.FolderColorSettings
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.label.domain.entity.LabelId
import me.proton.core.presentation.R
import org.junit.Test
import kotlin.test.assertEquals

class MailLabelUiModelMapperTest {

    @Test
    fun `correctly map MailLabels toUiModels`() {
        // Given
        val settings = FolderColorSettings(useFolderColor = true, inheritParentFolderColor = false)
        val selected = MailLabelId.Custom.Folder(LabelId("0"))
        val counters = emptyMap<LabelId, Int?>()

        val f0 = getMailFolder("0", level = 0, order = 0, parent = null, children = listOf("0.1", "0.2"))
        val f01 = getMailFolder("0.1", level = 1, order = 0, parent = f0)
        val f02 = getMailFolder("0.2", level = 1, order = 1, parent = f0, children = listOf("0.2.1", "0.2.2"))
        val f021 = getMailFolder("0.2.1", level = 2, order = 0, parent = f02)
        val f022 = getMailFolder("0.2.2", level = 2, order = 1, parent = f02)

        val items = listOf(f0, f01, f02, f021, f022)

        // When
        val actual = items.map { it.toCustomUiModel(settings, counters, selected) }

        // Then
        val expected = listOf(
            MailLabelUiModel.Custom(
                id = MailLabelId.Custom.Folder(LabelId("0")),
                text = TextUiModel.Text("0"),
                icon = R.drawable.ic_proton_folders_filled,
                iconTint = Color(0),
                isSelected = true,
                count = null,
                isVisible = true,
                isExpanded = true,
                iconPaddingStart = 0.0.dp,
            ),
            MailLabelUiModel.Custom(
                id = MailLabelId.Custom.Folder(LabelId("0.1")),
                text = TextUiModel.Text("0.1"),
                icon = R.drawable.ic_proton_folder_filled,
                iconTint = Color(0),
                isSelected = false,
                count = null,
                isVisible = true,
                isExpanded = true,
                iconPaddingStart = ProtonDimens.DefaultSpacing * 1,
            ),
            MailLabelUiModel.Custom(
                id = MailLabelId.Custom.Folder(LabelId("0.2")),
                text = TextUiModel.Text("0.2"),
                icon = R.drawable.ic_proton_folders_filled,
                iconTint = Color(0),
                isSelected = false,
                count = null,
                isVisible = true,
                isExpanded = true,
                iconPaddingStart = ProtonDimens.DefaultSpacing * 1,
            ),
            MailLabelUiModel.Custom(
                id = MailLabelId.Custom.Folder(LabelId("0.2.1")),
                text = TextUiModel.Text("0.2.1"),
                icon = R.drawable.ic_proton_folder_filled,
                iconTint = Color(0),
                isSelected = false,
                count = null,
                isVisible = true,
                isExpanded = true,
                iconPaddingStart = ProtonDimens.DefaultSpacing * 2,
            ),
            MailLabelUiModel.Custom(
                id = MailLabelId.Custom.Folder(LabelId("0.2.2")),
                text = TextUiModel.Text("0.2.2"),
                icon = R.drawable.ic_proton_folder_filled,
                iconTint = Color(0),
                isSelected = false,
                count = null,
                isVisible = true,
                isExpanded = true,
                iconPaddingStart = ProtonDimens.DefaultSpacing * 2,
            ),
        )
        assertEquals(expected = expected, actual = actual)
    }

    @Test
    fun `correctly map MailLabels toUiModels according FolderColorSettings`() {
        // Given
        val settings = FolderColorSettings(useFolderColor = true, inheritParentFolderColor = true)
        val selected = MailLabelId.Custom.Folder(LabelId("0"))
        val counters = emptyMap<LabelId, Int?>()

        val f0 = getMailFolder(
            "0",
            level = 0,
            color = Color.Green.toArgb(),
            order = 0,
            parent = null,
            children = listOf("0.1", "0.2")
        )
        val f01 = getMailFolder("0.1", level = 1, order = 0, parent = f0)
        val f02 = getMailFolder(
            "0.2",
            level = 1,
            color = Color.Red.toArgb(),
            order = 1,
            parent = f0,
            children = listOf("0.2.1", "0.2.2")
        )
        val f021 = getMailFolder("0.2.1", level = 2, order = 0, parent = f02)
        val f022 = getMailFolder("0.2.2", level = 2, order = 1, parent = f02)

        val items = listOf(f0, f01, f02, f021, f022)

        // When
        val actual = items.map { it.toCustomUiModel(settings, counters, selected) }

        // Then
        val expected = listOf(
            MailLabelUiModel.Custom(
                id = MailLabelId.Custom.Folder(LabelId("0")),
                text = TextUiModel.Text("0"),
                icon = R.drawable.ic_proton_folders_filled,
                iconTint = Color.Green,
                isSelected = true,
                count = null,
                isVisible = true,
                isExpanded = true,
                iconPaddingStart = 0.0.dp,
            ),
            MailLabelUiModel.Custom(
                id = MailLabelId.Custom.Folder(LabelId("0.1")),
                text = TextUiModel.Text("0.1"),
                icon = R.drawable.ic_proton_folder_filled,
                iconTint = Color.Green,
                isSelected = false,
                count = null,
                isVisible = true,
                isExpanded = true,
                iconPaddingStart = ProtonDimens.DefaultSpacing * 1,
            ),
            MailLabelUiModel.Custom(
                id = MailLabelId.Custom.Folder(LabelId("0.2")),
                text = TextUiModel.Text("0.2"),
                icon = R.drawable.ic_proton_folders_filled,
                iconTint = Color.Green,
                isSelected = false,
                count = null,
                isVisible = true,
                isExpanded = true,
                iconPaddingStart = ProtonDimens.DefaultSpacing * 1,
            ),
            MailLabelUiModel.Custom(
                id = MailLabelId.Custom.Folder(LabelId("0.2.1")),
                text = TextUiModel.Text("0.2.1"),
                icon = R.drawable.ic_proton_folder_filled,
                iconTint = Color.Red,
                isSelected = false,
                count = null,
                isVisible = true,
                isExpanded = true,
                iconPaddingStart = ProtonDimens.DefaultSpacing * 2,
            ),
            MailLabelUiModel.Custom(
                id = MailLabelId.Custom.Folder(LabelId("0.2.2")),
                text = TextUiModel.Text("0.2.2"),
                icon = R.drawable.ic_proton_folder_filled,
                iconTint = Color.Red,
                isSelected = false,
                count = null,
                isVisible = true,
                isExpanded = true,
                iconPaddingStart = ProtonDimens.DefaultSpacing * 2,
            ),
        )
        assertEquals(expected = expected, actual = actual)
    }
}
