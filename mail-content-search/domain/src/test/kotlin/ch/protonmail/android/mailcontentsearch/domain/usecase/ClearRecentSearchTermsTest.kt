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

internal class ClearRecentSearchTermsTest {

    private val userId = UserId("user-1")
    private val repository = mockk<ContentSearchRecentsRepository>()
    private val clearRecentSearchTerms = ClearRecentSearchTerms(repository)

    @Test
    fun `dismisses every remembered query, not just the ones the UI shows`() = runTest {
        // Given
        coEvery { repository.getRecentSearchTerms(userId, null, ClearRecentSearchTerms.WholeHistoryLimit) } returns
            listOf(term("invoice"), term("parcel"), term("flight")).right()
        coEvery { repository.dismissSearchTerm(userId, any()) } returns Unit.right()

        // When
        clearRecentSearchTerms(userId)

        // Then
        coVerify { repository.dismissSearchTerm(userId, "invoice") }
        coVerify { repository.dismissSearchTerm(userId, "parcel") }
        coVerify { repository.dismissSearchTerm(userId, "flight") }
    }

    @Test
    fun `keeps dismissing the rest when one dismiss fails`() = runTest {
        // Given
        coEvery { repository.getRecentSearchTerms(userId, null, ClearRecentSearchTerms.WholeHistoryLimit) } returns
            listOf(term("invoice"), term("parcel"), term("flight")).right()
        coEvery { repository.dismissSearchTerm(userId, "invoice") } returns DataError.Local.Unknown.left()
        coEvery { repository.dismissSearchTerm(userId, "parcel") } returns Unit.right()
        coEvery { repository.dismissSearchTerm(userId, "flight") } returns Unit.right()

        // When
        clearRecentSearchTerms(userId)

        // Then
        coVerify { repository.dismissSearchTerm(userId, "parcel") }
        coVerify { repository.dismissSearchTerm(userId, "flight") }
    }

    @Test
    fun `dismisses nothing when the term history cannot be read`() = runTest {
        // Given
        coEvery { repository.getRecentSearchTerms(userId, null, any()) } returns DataError.Local.Unknown.left()

        // When
        clearRecentSearchTerms(userId)

        // Then
        coVerify(exactly = 0) { repository.dismissSearchTerm(any(), any()) }
    }

    private fun term(query: String) = RecentSearchTerm(query = query, lastUsedAt = 1_700_000_000L)
}
