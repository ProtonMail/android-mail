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

package ch.protonmail.android.mailmessage.data.repository

import ch.protonmail.android.mailmessage.data.local.SearchResultsLocalDataSource
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import kotlin.test.Test

class SearchResultsRepositoryImplTest {

    private val userId = UserId("1")
    private val keyword = "keyword"
    private val searchResultsLocalDataSource = mockk<SearchResultsLocalDataSource>()
    private val searchResultsRepository = SearchResultsRepositoryImpl(
        localDataSource = searchResultsLocalDataSource
    )

    @Test
    fun `delete all should call delete search results from local data source`() = runTest {
        // Given
        coEvery { searchResultsLocalDataSource.deleteResults(userId, keyword) } just runs

        // When
        searchResultsRepository.deleteAll(userId, keyword)

        // Then
        coVerify(exactly = 1) { searchResultsLocalDataSource.deleteResults(userId, keyword) }
    }
}
