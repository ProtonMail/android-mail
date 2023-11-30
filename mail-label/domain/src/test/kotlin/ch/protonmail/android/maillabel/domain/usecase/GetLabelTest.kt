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
import me.proton.core.label.domain.entity.LabelType
import me.proton.core.label.domain.repository.LabelRepository
import org.junit.Test
import kotlin.test.assertEquals

class GetLabelTest {


    private val labelRepository: LabelRepository = mockk()

    private val getLabel = GetLabel(labelRepository)

    private val defaultTestLabel = LabelTestData.buildLabel(id = "LabelId")

    @Test
    fun `when get label is successful, then return success `() = runTest {
        // Given
        val expectedResult = defaultTestLabel
        coEvery {
            labelRepository.getLabel(UserIdTestData.userId, LabelType.MessageLabel, defaultTestLabel.labelId)
        } returns expectedResult

        // When
        val result = getLabel(UserIdTestData.userId, defaultTestLabel.labelId)

        // Then
        coVerify { labelRepository.getLabel(UserIdTestData.userId, LabelType.MessageLabel, defaultTestLabel.labelId) }
        assertEquals(expectedResult.right(), result)
    }

    @Test
    fun `when get label fails, then return error `() = runTest {
        // Given
        val expectedResult = DataError.Remote.Http(NetworkError.Unknown)
        coEvery {
            labelRepository.getLabel(UserIdTestData.userId, LabelType.MessageLabel, defaultTestLabel.labelId)
        } throws Exception("Test")

        // When
        val result = getLabel(UserIdTestData.userId, defaultTestLabel.labelId)

        // Then
        coVerify { labelRepository.getLabel(UserIdTestData.userId, LabelType.MessageLabel, defaultTestLabel.labelId) }
        assertEquals(expectedResult.left(), result)
    }
}
