/*
 * Copyright (c) 2025 Proton Technologies AG
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

package ch.protonmail.android.mailcontentsearch.domain.usecase

import ch.protonmail.android.mailcontentsearch.domain.repository.ContentSearchRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

internal class GetContentSearchIndexSizeTest {

    private val userId = UserId("user-1")
    private val repository = mockk<ContentSearchRepository>()

    private val getContentSearchIndexSize = GetContentSearchIndexSize(repository)

    @Test
    fun `reports the size the repository has for the account`() = runTest {
        // Given
        coEvery { repository.getIndexSizeBytes(userId) } returns 1_288_490_188L

        // When
        val result = getContentSearchIndexSize(userId)

        // Then
        assertEquals(1_288_490_188L, result)
    }

    @Test
    fun `reports nothing for an account whose index size cannot be read`() = runTest {
        // Given
        coEvery { repository.getIndexSizeBytes(userId) } returns null

        // When
        val result = getContentSearchIndexSize(userId)

        // Then
        assertNull(result)
    }
}
