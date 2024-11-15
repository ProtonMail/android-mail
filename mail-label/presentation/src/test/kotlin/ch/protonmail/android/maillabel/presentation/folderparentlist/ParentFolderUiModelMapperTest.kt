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

package ch.protonmail.android.maillabel.presentation.folderparentlist

import androidx.compose.ui.graphics.Color
import ch.protonmail.android.maillabel.presentation.model.FolderUiModel
import ch.protonmail.android.maillabel.presentation.model.ParentFolderUiModel
import ch.protonmail.android.maillabel.presentation.model.toParentFolderUiModel
import me.proton.core.label.domain.entity.LabelId
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
internal class FolderMappingTest(private val testCase: TestCase) {

    @Test
    fun `test folder mapping`() {
        val result = testCase.input.toParentFolderUiModel(
            testCase.labelId,
            testCase.parentLabelId
        )

        assertEquals(testCase.expected.size, result.size)

        result.zip(testCase.expected).forEach { (actual, expected) ->
            assertEquals(expected.folder, actual.folder)
            assertEquals(expected.isEnabled, actual.isEnabled)
            assertEquals(expected.isSelected, actual.isSelected)
            assertEquals(expected.displayDivider, actual.displayDivider)
        }
    }

    data class TestCase(
        val name: String,
        val input: List<FolderUiModel>,
        val labelId: LabelId?,
        val parentLabelId: LabelId?,
        val expected: List<ParentFolderUiModel>
    )

    private companion object {

        val folder1 = FolderUiModel(
            id = LabelId("1"),
            parent = null,
            name = "Root Folder",
            color = Color.Black,
            displayColor = null,
            level = 0,
            order = 0,
            children = listOf(),
            icon = 0
        )

        val folder2 = FolderUiModel(
            id = LabelId("2"),
            parent = folder1,
            name = "Child Folder",
            color = Color.Black,
            displayColor = null,
            level = 1,
            order = 1,
            children = listOf(),
            icon = 0
        )

        val folder3 = FolderUiModel(
            id = LabelId("3"),
            parent = folder2,
            name = "Grandchild Folder",
            color = Color.Black,
            displayColor = null,
            level = 2,
            order = 2,
            children = listOf(),
            icon = 0
        )

        val parentLabel1 = FolderUiModel(
            id = LabelId("parent1"),
            parent = null,
            name = "Parent Label 1",
            color = Color.Black,
            displayColor = null,
            level = 0,
            order = 0,
            children = listOf(LabelId("child1")),
            icon = 0
        )

        val childLabel2 = FolderUiModel(
            id = LabelId("child1"),
            parent = parentLabel1, // Child of parentLabel1
            name = "Child Label 2",
            color = Color.Black,
            displayColor = null,
            level = 1,
            order = 1,
            children = listOf(),
            icon = 0
        )

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun testCases() = listOf(
            TestCase(
                name = "Empty list returns empty result",
                input = emptyList(),
                labelId = null,
                parentLabelId = null,
                expected = emptyList()
            ),

            TestCase(
                name = "Root folder should be enabled when no labelId",
                input = listOf(folder1),
                labelId = null,
                parentLabelId = null,
                expected = listOf(
                    ParentFolderUiModel(
                        folder = folder1,
                        isEnabled = true,
                        isSelected = false,
                        displayDivider = false
                    )
                )
            ),
            TestCase(
                name = "Selected folder should be marked as selected",
                input = listOf(folder1, folder2),
                labelId = null,
                parentLabelId = LabelId("2"),
                expected = listOf(
                    ParentFolderUiModel(
                        folder = folder1,
                        isEnabled = true,
                        isSelected = false,
                        displayDivider = false
                    ),
                    ParentFolderUiModel(
                        folder = folder2,
                        isEnabled = true,
                        isSelected = true,
                        displayDivider = false
                    )
                )
            ),
            TestCase(
                name = "Current labelId folder should be disabled",
                input = listOf(folder1, folder2),
                labelId = LabelId("2"),
                parentLabelId = null,
                expected = listOf(
                    ParentFolderUiModel(
                        folder = folder1,
                        isEnabled = true,
                        isSelected = false,
                        displayDivider = false
                    ),
                    ParentFolderUiModel(
                        folder = folder2,
                        isEnabled = false,
                        isSelected = false,
                        displayDivider = false
                    )
                )
            ),
            TestCase(
                name = "Folders with level >= 2 should be disabled",
                input = listOf(folder1, folder2, folder3),
                labelId = null,
                parentLabelId = null,
                expected = listOf(
                    ParentFolderUiModel(
                        folder = folder1,
                        isEnabled = true,
                        isSelected = false,
                        displayDivider = false
                    ),
                    ParentFolderUiModel(
                        folder = folder2,
                        isEnabled = true,
                        isSelected = false,
                        displayDivider = false
                    ),
                    ParentFolderUiModel(
                        folder = folder3,
                        isEnabled = false,
                        isSelected = false,
                        displayDivider = false
                    )
                )
            ),
            TestCase(
                name = "Parent folder of current selection should be disabled",
                input = listOf(folder1, folder2),
                labelId = LabelId("2"),
                parentLabelId = null,
                expected = listOf(
                    ParentFolderUiModel(
                        folder = folder1,
                        isEnabled = true,
                        isSelected = false,
                        displayDivider = false
                    ),
                    ParentFolderUiModel(
                        folder = folder2,
                        isEnabled = false,
                        isSelected = false,
                        displayDivider = false
                    )
                )
            ),
            TestCase(
                name = "Child folder should be disabled when its parent is the current labelId",
                input = listOf(parentLabel1, childLabel2),
                labelId = LabelId("parent1"),
                parentLabelId = null,
                expected = listOf(
                    ParentFolderUiModel(
                        folder = parentLabel1,
                        isEnabled = false,
                        isSelected = false,
                        displayDivider = false
                    ),
                    ParentFolderUiModel(
                        folder = childLabel2,
                        isEnabled = false, // Child is disabled because its parent is the current labelId
                        isSelected = false,
                        displayDivider = false
                    )
                )
            )
        )
    }
}
