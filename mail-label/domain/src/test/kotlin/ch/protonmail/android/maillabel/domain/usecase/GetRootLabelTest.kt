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

import ch.protonmail.android.mailcommon.domain.sample.LabelSample
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import me.proton.core.label.domain.entity.LabelType
import me.proton.core.label.domain.repository.LabelRepository
import org.junit.Test
import kotlin.test.assertEquals

class GetRootLabelTest {

    private val labelRepository = mockk<LabelRepository>()
    private val getRootLabel = GetRootLabel(labelRepository)

    @Test
    fun `verify repository is not called when parent id is null`() = runTest {
        // Given
        val parentLabel = LabelSample.Parent

        // When
        getRootLabel.invoke(parentLabel.userId, parentLabel)

        // Then
        coVerify(exactly = 0) { labelRepository.getLabel(parentLabel.userId, LabelType.MessageFolder, any()) }
    }

    @Test
    fun `verify passed label is returned when parent is not found`() = runTest {
        // Given
        val childLabel = LabelSample.FirstChild
        coEvery {
            labelRepository.getLabel(
                userId = childLabel.userId,
                type = LabelType.MessageFolder,
                labelId = childLabel.parentId!!
            )
        } returns null

        // When
        val actual = getRootLabel.invoke(childLabel.userId, childLabel)

        // Then
        coVerify(exactly = 1) {
            labelRepository.getLabel(childLabel.userId, LabelType.MessageFolder, childLabel.parentId!!)
        }
        confirmVerified(labelRepository)
        assertEquals(childLabel, actual)
    }

    @Test
    fun `verify root label is returned when label with one parent is passed`() = runTest {
        // Given
        val childLabel = LabelSample.FirstChild
        val expected = LabelSample.Parent
        coEvery {
            labelRepository.getLabel(
                userId = childLabel.userId,
                type = LabelType.MessageFolder,
                labelId = childLabel.parentId!!
            )
        } returns LabelSample.Parent

        // When
        val actual = getRootLabel.invoke(childLabel.userId, childLabel)

        // Then
        coVerify(exactly = 1) {
            labelRepository.getLabel(childLabel.userId, LabelType.MessageFolder, childLabel.parentId!!)
        }
        confirmVerified(labelRepository)
        assertEquals(expected, actual)
    }

    @Test
    fun `verify root label is returned when label with two ancestors is passed`() = runTest {
        // Given
        val user = LabelSample.Parent.userId
        val secondLevelChild = LabelSample.SecondChild
        val firstLevelChild = LabelSample.FirstChild
        val parent = LabelSample.Parent
        coEvery {
            labelRepository.getLabel(
                userId = user,
                type = LabelType.MessageFolder,
                labelId = secondLevelChild.parentId!!
            )
        } returns firstLevelChild
        coEvery {
            labelRepository.getLabel(
                userId = user,
                type = LabelType.MessageFolder,
                labelId = firstLevelChild.parentId!!
            )
        } returns parent

        // When
        val actual = getRootLabel.invoke(user, secondLevelChild)

        // Then
        coVerify(exactly = 2) { labelRepository.getLabel(user, LabelType.MessageFolder, any()) }
        assertEquals(parent, actual)
    }

    @Test
    fun `verify latest found label is returned when parent of child is not found`() = runTest {
        // Given
        val user = LabelSample.Parent.userId
        val secondLevelChild = LabelSample.SecondChild
        val firstLevelChild = LabelSample.FirstChild
        coEvery {
            labelRepository.getLabel(
                userId = user,
                type = LabelType.MessageFolder,
                labelId = secondLevelChild.parentId!!
            )
        } returns firstLevelChild
        coEvery {
            labelRepository.getLabel(
                userId = user,
                type = LabelType.MessageFolder,
                labelId = firstLevelChild.parentId!!
            )
        } returns null

        // When
        val actual = getRootLabel.invoke(user, secondLevelChild)

        // Then
        coVerify(exactly = 2) { labelRepository.getLabel(user, LabelType.MessageFolder, any()) }
        assertEquals(firstLevelChild, actual)
    }
}
