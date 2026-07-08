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

import arrow.core.right
import ch.protonmail.android.maillabel.domain.model.CategoryLabelId
import ch.protonmail.android.maillabel.domain.repository.LabelRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import kotlin.test.Test
import kotlin.test.assertEquals

class MarkCategoryLabelSeenTest {

    private val labelRepository = mockk<LabelRepository>()
    private val markCategoryLabelSeen = MarkCategoryLabelSeen(labelRepository)

    @Test
    fun `delegates marking the category label as seen to the repository`() = runTest {
        // Given
        val userId = UserId("user-id")
        val categoryLabelId = CategoryLabelId("15")
        coEvery { labelRepository.markCategoryLabelSeen(userId, categoryLabelId) } returns Unit.right()

        // When
        val result = markCategoryLabelSeen(userId, categoryLabelId)

        // Then
        assertEquals(Unit.right(), result)
        coVerify { labelRepository.markCategoryLabelSeen(userId, categoryLabelId) }
    }
}
