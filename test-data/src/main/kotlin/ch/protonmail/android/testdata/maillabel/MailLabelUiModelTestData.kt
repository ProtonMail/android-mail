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

package ch.protonmail.android.testdata.maillabel

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.maildetail.presentation.R
import ch.protonmail.android.maillabel.domain.model.MailLabelId
import ch.protonmail.android.maillabel.presentation.MailLabelUiModel
import ch.protonmail.android.maillabel.presentation.iconRes
import ch.protonmail.android.maillabel.presentation.textRes
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.label.domain.entity.LabelId

object MailLabelUiModelTestData {

    val spamAndCustomFolder = listOf(
        MailLabelUiModel.System(
            id = MailLabelId.System.Spam,
            text = TextUiModel.TextRes(MailLabelId.System.Spam.systemLabelId.textRes()),
            icon = MailLabelId.System.Spam.systemLabelId.iconRes(),
            iconTint = null,
            isSelected = false,
            count = null
        ),
        MailLabelUiModel.Custom(
            id = MailLabelId.Custom.Folder(LabelId("folder1")),
            text = TextUiModel.Text("Folder1"),
            icon = R.drawable.ic_proton_folders_filled,
            iconTint = Color.Blue,
            isSelected = false,
            count = 1,
            isVisible = true,
            isExpanded = true,
            iconPaddingStart = 0.dp
        )
    )
    val spamAndCustomFolderWithSpamSelected = listOf(
        MailLabelUiModel.System(
            id = MailLabelId.System.Spam,
            text = TextUiModel.TextRes(MailLabelId.System.Spam.systemLabelId.textRes()),
            icon = MailLabelId.System.Spam.systemLabelId.iconRes(),
            iconTint = null,
            isSelected = true,
            count = null
        ),
        MailLabelUiModel.Custom(
            id = MailLabelId.Custom.Folder(LabelId("folder1")),
            text = TextUiModel.Text("Folder1"),
            icon = R.drawable.ic_proton_folders_filled,
            iconTint = Color.Blue,
            isSelected = false,
            count = 1,
            isVisible = true,
            isExpanded = true,
            iconPaddingStart = 0.dp
        )
    )
    val spamAndCustomFolderWithCustomSelected = listOf(
        MailLabelUiModel.System(
            id = MailLabelId.System.Spam,
            text = TextUiModel.TextRes(MailLabelId.System.Spam.systemLabelId.textRes()),
            icon = MailLabelId.System.Spam.systemLabelId.iconRes(),
            iconTint = null,
            isSelected = false,
            count = null
        ),
        MailLabelUiModel.Custom(
            id = MailLabelId.Custom.Folder(LabelId("folder1")),
            text = TextUiModel.Text("Folder1"),
            icon = R.drawable.ic_proton_folders_filled,
            iconTint = Color.Blue,
            isSelected = true,
            count = 1,
            isVisible = true,
            isExpanded = true,
            iconPaddingStart = 0.dp
        )
    )
    val systemAndTwoCustomFolders = listOf(
        MailLabelUiModel.System(
            id = MailLabelId.System.Spam,
            text = TextUiModel.TextRes(MailLabelId.System.Spam.systemLabelId.textRes()),
            icon = MailLabelId.System.Spam.systemLabelId.iconRes(),
            iconTint = null,
            isSelected = true,
            count = null
        ),
        MailLabelUiModel.Custom(
            id = MailLabelId.Custom.Folder(LabelId("folder1")),
            text = TextUiModel.Text("Folder1"),
            icon = R.drawable.ic_proton_folders_filled,
            iconTint = Color.Blue,
            isSelected = false,
            count = 1,
            isVisible = true,
            isExpanded = true,
            iconPaddingStart = 0.dp
        ),
        MailLabelUiModel.Custom(
            id = MailLabelId.Custom.Folder(LabelId("folder2")),
            text = TextUiModel.Text("Folder2"),
            icon = R.drawable.ic_proton_folder_filled,
            iconTint = Color.Red,
            isSelected = true,
            count = 2,
            isVisible = true,
            isExpanded = true,
            iconPaddingStart = ProtonDimens.DefaultSpacing * 1
        )
    )
}
