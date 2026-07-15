/*
 * Copyright (c) 2026 Proton Technologies AG
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
import ch.protonmail.android.mailcommon.domain.sample.UserIdSample
import ch.protonmail.android.maillabel.domain.model.CategoryLabelId
import ch.protonmail.android.maillabel.domain.model.CategorySystemLabelId
import ch.protonmail.android.maillabel.domain.repository.LabelRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ResolveLocalCategoryLabelIdTest {

    private val labelRepository = mockk<LabelRepository>()

    private val resolveLocalCategoryLabelId = ResolveLocalCategoryLabelId(labelRepository)

    @Test
    fun `should proxy the call to the labels repository (success)`() = runTest {
        // Given
        val userId = UserIdSample.Primary
        val expected = CategoryLabelId("42")
        coEvery {
            labelRepository.resolveLocalIdByCategory(userId, CategorySystemLabelId.Primary)
        } returns expected.right()

        // When
        val actual = resolveLocalCategoryLabelId(userId, CategorySystemLabelId.Primary)

        // Then
        assertEquals(expected, actual)
        coVerify(exactly = 1) { labelRepository.resolveLocalIdByCategory(userId, CategorySystemLabelId.Primary) }
        confirmVerified(labelRepository)
    }

    @Test
    fun `should proxy the call to the labels repository (failure)`() = runTest {
        // Given
        val userId = UserIdSample.Primary
        coEvery {
            labelRepository.resolveLocalIdByCategory(userId, CategorySystemLabelId.Primary)
        } returns DataError.Local.NoDataCached.left()

        // When
        val actual = resolveLocalCategoryLabelId(userId, CategorySystemLabelId.Primary)

        // Then
        assertNull(actual)
        coVerify(exactly = 1) { labelRepository.resolveLocalIdByCategory(userId, CategorySystemLabelId.Primary) }
        confirmVerified(labelRepository)
    }
}
