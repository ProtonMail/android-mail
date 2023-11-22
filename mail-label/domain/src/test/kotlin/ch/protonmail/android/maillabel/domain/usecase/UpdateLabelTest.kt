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

import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcommon.domain.model.NetworkError
import ch.protonmail.android.testdata.label.LabelTestData
import ch.protonmail.android.testdata.user.UserIdTestData
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import me.proton.core.label.domain.repository.LabelRepository
import org.junit.Test
import kotlin.test.assertEquals

class UpdateLabelTest {

    private val labelRepository: LabelRepository = mockk()

    private val updateLabel = UpdateLabel(labelRepository)

    private val defaultTestLabel = LabelTestData.buildLabel(id = "LabelId")

    @Test
    fun `when label is updated successfully, then return success`() = runTest {
        // Given
        val expectedResult = Unit
        coEvery { labelRepository.updateLabel(UserIdTestData.userId, defaultTestLabel) } returns expectedResult

        // When
        val result = updateLabel(UserIdTestData.userId, defaultTestLabel)

        // Then
        coVerify { labelRepository.updateLabel(UserIdTestData.userId, defaultTestLabel) }
        assertEquals(expectedResult.right(), result)
    }

    @Test
    fun `when label update fails, then return error`() = runTest {
        // Given
        val expectedResult = DataError.Remote.Http(NetworkError.Unknown)
        coEvery { labelRepository.updateLabel(UserIdTestData.userId, defaultTestLabel) } throws Exception("Test")

        // When
        val result = updateLabel(UserIdTestData.userId, defaultTestLabel)

        // Then
        coVerify { labelRepository.updateLabel(UserIdTestData.userId, defaultTestLabel) }
        assertEquals(expectedResult.left(), result)
    }
}
