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

package ch.protonmail.android.mailmessage.domain.usecase

import ch.protonmail.android.mailcommon.domain.sample.UserIdSample
import ch.protonmail.android.mailmessage.domain.repository.SearchResultsRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class DeleteSearchResultsTest {

    private val userId = UserIdSample.Primary
    private val searchResultsRepository = mockk<SearchResultsRepository>()
    private val deleteMessages = DeleteSearchResults(searchResultsRepository)

    @Test
    fun `delete search results calls repository with given parameters`() = runTest {
        // Given
        coEvery { searchResultsRepository.deleteAll(userId) } just runs

        // When
        deleteMessages(userId)

        // Then
        coVerify { searchResultsRepository.deleteAll(userId) }
    }
}
