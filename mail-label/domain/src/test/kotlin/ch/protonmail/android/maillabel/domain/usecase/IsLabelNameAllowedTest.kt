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
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import me.proton.core.label.domain.entity.LabelType
import me.proton.core.label.domain.repository.LabelRepository
import org.junit.Test
import kotlin.test.assertEquals

class IsLabelNameAllowedTest {

    private val labelRepository: LabelRepository = mockk()

    private val isLabelNameAllowed = IsLabelNameAllowed(labelRepository)

    private val defaultTestLabel = LabelTestData.buildLabel(id = "LabelId")
    private val defaultTestFolder = LabelTestData.buildLabel(id = "FolderId", type = LabelType.MessageFolder)

    @Test
    fun `label name doesn't exist`() = runTest {
        // Given
        val expectedResult = true
        coEvery { labelRepository.getLabels(UserIdTestData.userId, LabelType.MessageLabel) } returns listOf(
            defaultTestLabel
        )
        coEvery { labelRepository.getLabels(UserIdTestData.userId, LabelType.MessageFolder) } returns listOf(
            defaultTestFolder
        )

        // When
        val result = isLabelNameAllowed(UserIdTestData.userId, name = "NewName")

        // Then
        coVerify { labelRepository.getLabels(UserIdTestData.userId, LabelType.MessageLabel) }
        coVerify { labelRepository.getLabels(UserIdTestData.userId, LabelType.MessageFolder) }
        assertEquals(expectedResult.right(), result)
    }

    @Test
    fun `label name already exist`() = runTest {
        // Given
        val expectedResult = false
        coEvery { labelRepository.getLabels(UserIdTestData.userId, LabelType.MessageLabel) } returns listOf(
            defaultTestLabel
        )
        coEvery { labelRepository.getLabels(UserIdTestData.userId, LabelType.MessageFolder) } returns listOf(
            defaultTestFolder
        )

        // When
        val result = isLabelNameAllowed(UserIdTestData.userId, name = "LabelId")

        // Then
        coVerify { labelRepository.getLabels(UserIdTestData.userId, LabelType.MessageLabel) }
        coVerify(exactly = 0) { labelRepository.getLabels(UserIdTestData.userId, LabelType.MessageFolder) }
        assertEquals(expectedResult.right(), result)
    }

    @Test
    fun `folder name already exist`() = runTest {
        // Given
        val expectedResult = false
        coEvery { labelRepository.getLabels(UserIdTestData.userId, LabelType.MessageLabel) } returns listOf(
            defaultTestLabel
        )
        coEvery { labelRepository.getLabels(UserIdTestData.userId, LabelType.MessageFolder) } returns listOf(
            defaultTestFolder
        )

        // When
        val result = isLabelNameAllowed(UserIdTestData.userId, name = "FolderId")

        // Then
        coVerify { labelRepository.getLabels(UserIdTestData.userId, LabelType.MessageLabel) }
        coVerify { labelRepository.getLabels(UserIdTestData.userId, LabelType.MessageFolder) }
        assertEquals(expectedResult.right(), result)
    }

    @Test
    fun `name is forbidden`() = runTest {
        // Given
        val expectedResult = false
        coEvery { labelRepository.getLabels(UserIdTestData.userId, LabelType.MessageLabel) } returns listOf(
            defaultTestLabel
        )
        coEvery { labelRepository.getLabels(UserIdTestData.userId, LabelType.MessageFolder) } returns listOf(
            defaultTestFolder
        )

        // When
        val result = isLabelNameAllowed(UserIdTestData.userId, name = "Inbox")

        // Then
        coVerify(exactly = 0) { labelRepository.getLabels(UserIdTestData.userId, LabelType.MessageLabel) }
        coVerify(exactly = 0) { labelRepository.getLabels(UserIdTestData.userId, LabelType.MessageFolder) }
        assertEquals(expectedResult.right(), result)
    }
}
