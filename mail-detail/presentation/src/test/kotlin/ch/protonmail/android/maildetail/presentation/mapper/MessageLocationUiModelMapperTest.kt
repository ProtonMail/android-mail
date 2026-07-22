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
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcategory.presentation.design.activeCategoryColor
import ch.protonmail.android.mailcategory.presentation.mapper.categoryIconRes
import ch.protonmail.android.mailcategory.presentation.mapper.categoryTextRes
import ch.protonmail.android.mailcommon.presentation.mapper.ColorMapper
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.maildetail.presentation.R
import ch.protonmail.android.maildetail.presentation.model.MessageLocationUiModel
import ch.protonmail.android.maillabel.domain.model.CategoryLocation
import ch.protonmail.android.maillabel.domain.model.CategorySystemLabelId
import ch.protonmail.android.maillabel.domain.model.ExclusiveLocation
import ch.protonmail.android.maillabel.domain.model.LabelId
import ch.protonmail.android.maillabel.domain.model.SystemLabelId
import ch.protonmail.android.maillabel.presentation.iconRes
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals

class MessageLocationUiModelMapperTest {

    private val colorMapper: ColorMapper = mockk()

    private val messageLocationUiModelMapper = MessageLocationUiModelMapper(colorMapper)

    @Test
    fun `when an exclusive system label is found in the list of label ids, its name and icon are returned`() = runTest {
        // Given
        val messageLocation = ExclusiveLocation.System(SystemLabelId.Archive, SystemLabelId.Archive.labelId)
        val expectedResult = MessageLocationUiModel(
            name = TextUiModel.TextRes(R.string.label_title_archive),
            icon = SystemLabelId.enumOf(SystemLabelId.Archive.labelId.id).iconRes()
        )

        // When
        val result = messageLocationUiModelMapper(messageLocation)
        // Then
        assertEquals(expectedResult, result)
    }

    @Test
    fun `when a custom folder is found, it's name, icon and icon color are returned when folder color setting is on`() =
        runTest {
            // Given
            val customFolderId = "customFolderId"
            val customFolderName = "customFolder"
            val customFolderColor = Color.Red
            val messageLocation = ExclusiveLocation.Folder(customFolderName, LabelId(customFolderId), "#rrr")

            val expectedResult = MessageLocationUiModel(
                name = TextUiModel.Text(customFolderName),
                icon = R.drawable.ic_proton_folder_filled,
                color = customFolderColor
            )
            every { colorMapper.toColor(any()) } returns customFolderColor.right()

            // When
            val result = messageLocationUiModelMapper(messageLocation)

            // Then
            assertEquals(expectedResult, result)
        }

    @Test
    @Ignore("Re-implement once ET-2268 is addressed")
    fun `when a custom folder is found, icon color is ignored when folder color setting is off`() = runTest {
        // Given
        val customFolderId = "customFolderId"
        val customFolderName = "customFolder"
        val messageLocation = ExclusiveLocation.Folder(customFolderName, LabelId(customFolderId), "#rrr")

        val expectedResult = MessageLocationUiModel(
            name = TextUiModel.TextRes(R.string.label_title_archive),
            icon = R.drawable.ic_proton_folder
        )
        every { colorMapper.toColor(null) } returns "invalid".left()

        // When
        val result = messageLocationUiModelMapper(messageLocation)

        // Then
        assertEquals(expectedResult, result)
    }

    @Test
    fun `when no exclusive label has been found, then the name and icon of the all mail location are returned`() =
        runTest {
            // Given
            val messageLocation = ExclusiveLocation.System(SystemLabelId.AllMail, SystemLabelId.AllMail.labelId)
            val expectedResult = MessageLocationUiModel(
                name = TextUiModel.TextRes(R.string.label_title_all_mail),
                icon = SystemLabelId.enumOf(SystemLabelId.AllMail.labelId.id).iconRes()
            )

            // When
            val result = messageLocationUiModelMapper(messageLocation)

            // Then
            assertEquals(expectedResult, result)
        }

    @Test
    fun `when a category is present, it is mapped to its name, icon and color`() = runTest {
        // Given
        val category = CategoryLocation.Value(CategorySystemLabelId.Social)
        val expectedResult = MessageLocationUiModel(
            name = TextUiModel(CategorySystemLabelId.Social.categoryTextRes()),
            icon = CategorySystemLabelId.Social.categoryIconRes(),
            color = CategorySystemLabelId.Social.activeCategoryColor()
        )

        // When
        val result = messageLocationUiModelMapper(category)

        // Then
        assertEquals(expectedResult, result)
    }

    @Test
    fun `when the category has no value, null is returned`() = runTest {
        // When
        val result = messageLocationUiModelMapper(CategoryLocation.NoValue)

        // Then
        assertEquals(null, result)
    }
}
