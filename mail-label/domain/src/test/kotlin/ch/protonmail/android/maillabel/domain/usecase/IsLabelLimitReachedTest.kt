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
import ch.protonmail.android.mailcommon.domain.usecase.ObserveUser
import ch.protonmail.android.testdata.label.LabelTestData
import ch.protonmail.android.testdata.user.UserIdTestData
import ch.protonmail.android.testdata.user.UserTestData
import io.mockk.called
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import me.proton.core.label.domain.entity.LabelType
import me.proton.core.label.domain.repository.LabelRepository
import org.junit.Test
import kotlin.test.assertEquals

class IsLabelLimitReachedTest {

    private val labelRepository: LabelRepository = mockk()
    private val observeUser: ObserveUser = mockk()

    private val isLabelLimitReached = IsLabelLimitReached(labelRepository, observeUser)

    private val defaultTestLabel = LabelTestData.buildLabel(id = "LabelId")

    @Test
    fun `given paid user with mail subscription, return false`() = runTest {
        // Given
        val expectedResult = false
        coEvery { observeUser(UserIdTestData.userId) } returns flowOf(UserTestData.paidMailUser)

        // When
        val result = isLabelLimitReached(UserIdTestData.userId, LabelType.MessageLabel)

        // Then
        coVerify { observeUser(UserIdTestData.userId) }
        verify { labelRepository wasNot called }
        assertEquals(expectedResult.right(), result)
    }

    @Test
    fun `given paid user without mail subscription and 3 existing labels, return true`() = runTest {
        // Given
        val expectedResult = true
        coEvery { observeUser(UserIdTestData.userId) } returns flowOf(UserTestData.paidUser)
        coEvery { labelRepository.getLabels(UserIdTestData.userId, LabelType.MessageLabel) } returns listOf(
            LabelTestData.buildLabel(id = "LabelId1"),
            LabelTestData.buildLabel(id = "LabelId2"),
            LabelTestData.buildLabel(id = "LabelId3")
        )

        // When
        val result = isLabelLimitReached(UserIdTestData.userId, LabelType.MessageLabel)

        // Then
        coVerify { observeUser(UserIdTestData.userId) }
        assertEquals(expectedResult.right(), result)
    }

    @Test
    fun `given user has 1 existing label, when free user, then return false`() = runTest {
        // Given
        val expectedResult = false
        coEvery { observeUser(UserIdTestData.userId) } returns flowOf(UserTestData.freeUser)
        coEvery { labelRepository.getLabels(UserIdTestData.userId, LabelType.MessageLabel) } returns listOf(
            defaultTestLabel
        )

        // When
        val result = isLabelLimitReached(UserIdTestData.userId, LabelType.MessageLabel)

        // Then
        coVerify { observeUser(UserIdTestData.userId) }
        coVerify { labelRepository.getLabels(UserIdTestData.userId, LabelType.MessageLabel) }
        assertEquals(expectedResult.right(), result)
    }

    @Test
    fun `given user has 3 existing label, when free user, then return true`() = runTest {
        // Given
        val expectedResult = true
        coEvery { observeUser(UserIdTestData.userId) } returns flowOf(UserTestData.freeUser)
        coEvery { labelRepository.getLabels(UserIdTestData.userId, LabelType.MessageLabel) } returns listOf(
            LabelTestData.buildLabel(id = "LabelId1"),
            LabelTestData.buildLabel(id = "LabelId2"),
            LabelTestData.buildLabel(id = "LabelId3")
        )

        // When
        val result = isLabelLimitReached(UserIdTestData.userId, LabelType.MessageLabel)

        // Then
        coVerify { observeUser(UserIdTestData.userId) }
        coVerify { labelRepository.getLabels(UserIdTestData.userId, LabelType.MessageLabel) }
        assertEquals(expectedResult.right(), result)
    }

    @Test
    fun `given user is below limit but has system labels, when free user, then return false`() = runTest {
        // Given
        val expectedResult = false
        coEvery { observeUser(UserIdTestData.userId) } returns flowOf(UserTestData.freeUser)
        coEvery {
            labelRepository.getLabels(UserIdTestData.userId, LabelType.MessageLabel)
        } returns LabelTestData.systemLabels.map {
            LabelTestData.buildLabel(id = it.id.labelId.id)
        } + listOf(
            LabelTestData.buildLabel(id = "Custom Label")
        )

        // When
        val result = isLabelLimitReached(UserIdTestData.userId, LabelType.MessageLabel)

        // Then
        coVerify { observeUser(UserIdTestData.userId) }
        coVerify { labelRepository.getLabels(UserIdTestData.userId, LabelType.MessageLabel) }
        assertEquals(expectedResult.right(), result)
    }

    @Test
    fun `given user has 3 custom labels and system labels, when free user, then return true`() = runTest {
        // Given
        val expectedResult = true
        coEvery { observeUser(UserIdTestData.userId) } returns flowOf(UserTestData.freeUser)
        coEvery {
            labelRepository.getLabels(UserIdTestData.userId, LabelType.MessageLabel)
        } returns LabelTestData.systemLabels.map {
            LabelTestData.buildLabel(id = it.id.labelId.id)
        } + listOf(
            LabelTestData.buildLabel(id = "Custom Label 1"),
            LabelTestData.buildLabel(id = "Custom Label 2"),
            LabelTestData.buildLabel(id = "Custom Label 3")
        )

        // When
        val result = isLabelLimitReached(UserIdTestData.userId, LabelType.MessageLabel)

        // Then
        coVerify { observeUser(UserIdTestData.userId) }
        coVerify { labelRepository.getLabels(UserIdTestData.userId, LabelType.MessageLabel) }
        assertEquals(expectedResult.right(), result)
    }
}
