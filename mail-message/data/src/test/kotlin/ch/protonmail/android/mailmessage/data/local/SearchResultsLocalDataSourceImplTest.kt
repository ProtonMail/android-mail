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

package ch.protonmail.android.mailmessage.data.local

import ch.protonmail.android.mailmessage.data.getMessage
import ch.protonmail.android.mailmessage.data.local.dao.SearchResultDao
import ch.protonmail.android.mailmessage.data.local.entity.SearchResultEntity
import io.mockk.coEvery
import io.mockk.coInvoke
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import org.junit.Before
import kotlin.test.Test

class SearchResultsLocalDataSourceImplTest {

    private val userId = UserId("1")

    private val searchResultDao = mockk<SearchResultDao>()

    private val db = mockk<SearchResultsDatabase>(relaxed = true) {
        every { searchResultsDao() } returns searchResultDao
        coEvery { inTransaction(captureCoroutine<suspend () -> Any>()) } coAnswers {
            coroutine<suspend () -> Any>().coInvoke()
        }
    }

    private lateinit var searchResultsLocalDataSource: SearchResultsLocalDataSourceImpl

    @Before
    fun setUp() {
        searchResultsLocalDataSource = SearchResultsLocalDataSourceImpl(db)
    }

    @Test
    fun `upsert search results with given parameters`() = runTest {
        // Given
        val message = getMessage(userId, "1", time = 1000)
        val searchResult = SearchResultEntity(userId, "keyword", message.messageId)
        coEvery { searchResultDao.insertOrUpdate(searchResult) } just runs

        // When
        searchResultsLocalDataSource.upsertResults(userId, "keyword", listOf(message))

        // Then
        coVerify { db.inTransaction(any()) }
        coVerify(exactly = 1) { searchResultDao.insertOrUpdate(searchResult) }
    }

    @Test
    fun `delete search results calls corresponding dao function with given parameters`() = runTest {
        // Given
        val keyword = "keyword"
        coEvery { searchResultDao.deleteAllForKeyword(userId, keyword) } just runs

        // When
        searchResultsLocalDataSource.deleteResults(userId, keyword)

        // Then
        coVerify(exactly = 1) { searchResultDao.deleteAllForKeyword(userId, keyword) }
    }

    @Test
    fun `delete all search results calls corresponding dao function with given parameters`() = runTest {
        // Given
        coEvery { searchResultDao.deleteAll(userId) } just runs

        // When
        searchResultsLocalDataSource.deleteAllResults(userId)

        // Then
        coVerify(exactly = 1) { searchResultDao.deleteAll(userId) }
    }
}
