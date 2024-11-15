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
import arrow.core.right
import ch.protonmail.android.mailcommon.presentation.mapper.ColorMapper
import ch.protonmail.android.maildetail.presentation.R
import ch.protonmail.android.maildetail.presentation.model.MessageLocationUiModel
import ch.protonmail.android.maillabel.domain.model.SystemLabelId
import ch.protonmail.android.maillabel.domain.usecase.GetRootLabel
import ch.protonmail.android.maillabel.presentation.iconRes
import ch.protonmail.android.mailsettings.domain.model.AutoDeleteSetting
import ch.protonmail.android.mailsettings.domain.model.FolderColorSettings
import ch.protonmail.android.testdata.label.LabelTestData
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import me.proton.core.label.domain.entity.LabelId
import me.proton.core.label.domain.entity.LabelType
import kotlin.test.Test
import kotlin.test.assertEquals

class MessageLocationUiModelMapperTest {

    private val colorMapper: ColorMapper = mockk()
    private val autoDeleteSetting = AutoDeleteSetting.Disabled
    private val folderColorSettings = FolderColorSettings(useFolderColor = false)
    private val getRootLabel = mockk<GetRootLabel>()

    private val messageLocationUiModelMapper = MessageLocationUiModelMapper(
        colorMapper,
        getRootLabel
    )

    @Test
    fun `when an exclusive system label is found in the list of label ids, its name and icon are returned`() = runTest {
        // Given
        val labelIds = listOf(SystemLabelId.AllMail.labelId, SystemLabelId.Archive.labelId)
        val expectedResult = MessageLocationUiModel(
            SystemLabelId.Archive.name,
            SystemLabelId.enumOf(SystemLabelId.Archive.labelId.id).iconRes()
        )
        // When
        val result = messageLocationUiModelMapper(labelIds, emptyList(), folderColorSettings, autoDeleteSetting)
        // Then
        assertEquals(expectedResult, result)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `when AutoDelete is ON and auto-delete system label is found in the list of label ids, its icon is overridden with clock-trashcan`() =
        runTest {
            // Given
            val labelIds = listOf(
                SystemLabelId.AllMail.labelId,
                SystemLabelId.Archive.labelId,
                SystemLabelId.autoDeleteList.first().labelId
            )
            val expectedResult = MessageLocationUiModel(
                SystemLabelId.autoDeleteList.first().name,
                R.drawable.ic_proton_trash_clock
            )
            // When
            val result =
                messageLocationUiModelMapper(labelIds, emptyList(), folderColorSettings, AutoDeleteSetting.Enabled)
            // Then
            assertEquals(expectedResult, result)
        }

    @Suppress("MaxLineLength")
    @Test
    fun `when AutoDelete is OFF and auto-delete system label is found in the list of label ids, its icon is not overridden with clock-trashcan`() =
        runTest {
            // Given
            val labelIds = listOf(
                SystemLabelId.AllMail.labelId,
                SystemLabelId.Archive.labelId,
                SystemLabelId.autoDeleteList.first().labelId
            )
            val expectedResult = MessageLocationUiModel(
                SystemLabelId.Archive.name,
                SystemLabelId.enumOf(SystemLabelId.Archive.labelId.id).iconRes()
            )
            // When
            val result =
                messageLocationUiModelMapper(labelIds, emptyList(), folderColorSettings, AutoDeleteSetting.Disabled)
            // Then
            assertEquals(expectedResult, result)
        }

    @Test
    fun `when a custom folder is found, it's name, icon and icon color are returned when folder color setting is on`() =
        runTest {
            // Given
            val customLabelId = "customLabelId"
            val customFolderId = "customFolderId"
            val customFolderName = "customFolder"
            val customFolderColor = Color.Red
            val labelIds =
                listOf(SystemLabelId.AllMail.labelId, LabelId(id = customLabelId), LabelId(id = customFolderId))
            val labels = listOf(
                LabelTestData.buildLabel(id = customLabelId, type = LabelType.MessageLabel),
                LabelTestData.buildLabel(
                    id = customFolderId,
                    type = LabelType.MessageFolder,
                    name = customFolderName
                )
            )
            val expectedResult = MessageLocationUiModel(
                customFolderName,
                R.drawable.ic_proton_folder_filled,
                customFolderColor
            )
            every { colorMapper.toColor(any()) } returns customFolderColor.right()

            // When
            val result =
                messageLocationUiModelMapper(labelIds, labels, FolderColorSettings(), AutoDeleteSetting.Disabled)

            // Then
            assertEquals(expectedResult, result)
        }

    @Test
    fun `when a custom folder is found, icon color is ignored when folder color setting is off`() = runTest {
        // Given
        val customLabelId = "customLabelId"
        val customFolderId = "customFolderId"
        val customFolderName = "customFolder"
        val customFolderColor = Color.Red
        val labelIds = listOf(SystemLabelId.AllMail.labelId, LabelId(id = customLabelId), LabelId(id = customFolderId))
        val labels = listOf(
            LabelTestData.buildLabel(id = customLabelId, type = LabelType.MessageLabel),
            LabelTestData.buildLabel(
                id = customFolderId,
                type = LabelType.MessageFolder,
                name = customFolderName
            )
        )
        val expectedResult = MessageLocationUiModel(
            customFolderName,
            R.drawable.ic_proton_folder
        )
        every { colorMapper.toColor(any()) } returns customFolderColor.right()

        // When
        val result = messageLocationUiModelMapper(labelIds, labels, folderColorSettings, AutoDeleteSetting.Disabled)

        // Then
        assertEquals(expectedResult, result)
    }

    @Test
    fun `when no exclusive label has been found, then the name and icon of the all mail location are returned`() =
        runTest {
            // Given
            val labelIds = listOf(SystemLabelId.AllMail.labelId)
            val expectedResult = MessageLocationUiModel(
                SystemLabelId.AllMail.name,
                SystemLabelId.enumOf(SystemLabelId.AllMail.labelId.id).iconRes()
            )
            // When
            val result =
                messageLocationUiModelMapper(labelIds, emptyList(), folderColorSettings, AutoDeleteSetting.Disabled)
            // Then
            assertEquals(expectedResult, result)
        }
}
