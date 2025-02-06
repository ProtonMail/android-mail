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

package ch.protonmail.android.maillabel.domain.usecase

import arrow.core.right
import ch.protonmail.android.testdata.label.LabelTestData
import ch.protonmail.android.testdata.user.UserIdTestData
import io.mockk.called
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import me.proton.core.label.domain.entity.LabelType
import org.junit.Test
import kotlin.test.assertEquals

class IsLabelNameAllowedTest {

    private val getLabels: GetLabels = mockk()

    private val isLabelNameAllowed = IsLabelNameAllowed(getLabels)

    private val defaultTestLabel = LabelTestData.buildLabel(id = "LabelId")
    private val defaultTestFolder = LabelTestData.buildLabel(id = "FolderId", type = LabelType.MessageFolder)

    @Test
    fun `when label name doesn't exist, then return true`() = runTest {
        // Given
        val expectedResult = true
        coEvery { getLabels(UserIdTestData.userId, LabelType.MessageLabel) } returns listOf(
            defaultTestLabel
        ).right()
        coEvery { getLabels(UserIdTestData.userId, LabelType.MessageFolder) } returns listOf(
            defaultTestFolder
        ).right()

        // When
        val result = isLabelNameAllowed(UserIdTestData.userId, name = "NewName", null)

        // Then
        coVerify {
            getLabels(UserIdTestData.userId, LabelType.MessageLabel)
            getLabels(UserIdTestData.userId, LabelType.MessageFolder)
        }
        assertEquals(expectedResult.right(), result)
    }

    @Test
    fun `when sub folder name doesn't exist, then return true`() = runTest {
        // Given
        val expectedResult = true
        coEvery { getLabels(UserIdTestData.userId, LabelType.MessageFolder) } returns listOf(
            defaultTestFolder
        ).right()

        // When
        val result = isLabelNameAllowed(
            UserIdTestData.userId,
            name = "SubFolderName",
            parentId = defaultTestFolder.labelId
        )

        // Then
        coVerify(exactly = 0) {
            getLabels(UserIdTestData.userId, LabelType.MessageLabel)
        }
        coVerify {
            getLabels(UserIdTestData.userId, LabelType.MessageFolder)
        }
        assertEquals(expectedResult.right(), result)
    }

    @Test
    fun `when label name already exist, then return false`() = runTest {
        // Given
        val expectedResult = false
        coEvery { getLabels(UserIdTestData.userId, LabelType.MessageLabel) } returns listOf(
            defaultTestLabel
        ).right()

        // When
        val result = isLabelNameAllowed(UserIdTestData.userId, name = "LabelId", null)

        // Then
        coVerify { getLabels(UserIdTestData.userId, LabelType.MessageLabel) }
        assertEquals(expectedResult.right(), result)
    }

    @Test
    fun `when folder name already exist, then return false`() = runTest {
        // Given
        val expectedResult = false
        coEvery { getLabels(UserIdTestData.userId, LabelType.MessageLabel) } returns listOf(
            defaultTestLabel
        ).right()
        coEvery { getLabels(UserIdTestData.userId, LabelType.MessageFolder) } returns listOf(
            defaultTestFolder
        ).right()

        // When
        val result = isLabelNameAllowed(UserIdTestData.userId, name = "FolderId", null)

        // Then
        coVerify {
            getLabels(UserIdTestData.userId, LabelType.MessageLabel)
            getLabels(UserIdTestData.userId, LabelType.MessageFolder)
        }
        assertEquals(expectedResult.right(), result)
    }

    @Test
    fun `when sub folder name already exist, then return false`() = runTest {
        // Given
        val expectedResult = false
        val subFolder = defaultTestFolder.copy(name = "SubFolderName", parentId = defaultTestFolder.labelId)
        coEvery { getLabels(UserIdTestData.userId, LabelType.MessageLabel) } returns listOf(
            defaultTestLabel
        ).right()
        coEvery { getLabels(UserIdTestData.userId, LabelType.MessageFolder) } returns listOf(
            defaultTestFolder,
            subFolder
        ).right()

        // When
        val result = isLabelNameAllowed(
            UserIdTestData.userId,
            name = "SubFolderName",
            parentId = defaultTestFolder.labelId
        )

        // Then
        coVerify(exactly = 0) {
            getLabels(UserIdTestData.userId, LabelType.MessageLabel)
        }
        coVerify {
            getLabels(UserIdTestData.userId, LabelType.MessageFolder)
        }
        assertEquals(expectedResult.right(), result)
    }

    @Test
    fun `when name is forbidden, then return false`() = runTest {
        // Given
        val expectedResult = false

        // When
        val result = isLabelNameAllowed(UserIdTestData.userId, name = "Inbox", null)

        // Then
        coVerify { getLabels wasNot called }
        assertEquals(expectedResult.right(), result)
    }

    @Test
    fun `when name is forbidden but it is a sub folder, then return true`() = runTest {
        // Given
        val expectedResult = true
        coEvery { getLabels(UserIdTestData.userId, LabelType.MessageFolder) } returns listOf(
            defaultTestFolder
        ).right()

        // When
        val result = isLabelNameAllowed(UserIdTestData.userId, name = "Inbox", parentId = defaultTestFolder.labelId)

        // Then
        coVerify {
            getLabels(UserIdTestData.userId, LabelType.MessageFolder)
        }
        assertEquals(expectedResult.right(), result)
    }
}
