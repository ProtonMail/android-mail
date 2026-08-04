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

package ch.protonmail.android.mailcontentsearch.domain.usecase

import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcontentsearch.domain.model.RecentSearchTerm
import ch.protonmail.android.mailcontentsearch.domain.repository.ContentSearchRecentsRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import kotlin.test.Test
import kotlin.test.assertEquals

internal class GetRecentSearchTermsTest {

    private val userId = UserId("user-1")
    private val repository = mockk<ContentSearchRecentsRepository>()
    private val getRecentSearchTerms = GetRecentSearchTerms(repository)

    @Test
    fun `reads the plain recents list with no prefix and the default limit`() = runTest {
        // Given
        val terms = listOf(term("invoice"))
        coEvery {
            repository.getRecentSearchTerms(userId, null, GetRecentSearchTerms.DefaultLimit)
        } returns terms.right()

        // When
        val result = getRecentSearchTerms(userId)

        // Then
        assertEquals(terms.right(), result)
        coVerify { repository.getRecentSearchTerms(userId, null, GetRecentSearchTerms.DefaultLimit) }
    }

    @Test
    fun `keeps only the queries matching the given prefix`() = runTest {
        // Given
        val terms = listOf(term("invoice"))
        coEvery { repository.getRecentSearchTerms(userId, "inv", 10) } returns terms.right()

        // When
        val result = getRecentSearchTerms(userId, prefix = "inv", limit = 10)

        // Then
        assertEquals(terms.right(), result)
    }

    @Test
    fun `surfaces a failure from the repository`() = runTest {
        // Given
        coEvery {
            repository.getRecentSearchTerms(userId, null, GetRecentSearchTerms.DefaultLimit)
        } returns DataError.Local.Unknown.left()

        // When
        val result = getRecentSearchTerms(userId)

        // Then
        assertEquals(DataError.Local.Unknown.left(), result)
    }

    private fun term(query: String) = RecentSearchTerm(query = query, lastUsedAt = 1_700_000_000L)
}
