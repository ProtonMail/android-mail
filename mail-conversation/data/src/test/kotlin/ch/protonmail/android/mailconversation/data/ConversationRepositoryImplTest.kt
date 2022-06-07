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

package ch.protonmail.android.mailconversation.data

import ch.protonmail.android.mailconversation.data.repository.ConversationRepositoryImpl
import ch.protonmail.android.mailconversation.domain.repository.ConversationLocalDataSource
import ch.protonmail.android.mailconversation.domain.repository.ConversationRemoteDataSource
import ch.protonmail.android.mailpagination.domain.entity.PageFilter
import ch.protonmail.android.mailpagination.domain.entity.PageKey
import io.mockk.Ordering
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import org.junit.Before
import org.junit.Test
import java.io.IOException
import kotlin.test.assertEquals

class ConversationRepositoryImplTest {

    private val userId = UserId("1")

    private val remoteDataSource = mockk<ConversationRemoteDataSource> {
        coEvery { this@mockk.getConversations(any(), any()) } returns listOf(
            getConversation(id = "1", time = 1000),
            getConversation(id = "2", time = 2000),
            getConversation(id = "3", time = 3000),
            getConversation(id = "4", time = 4000),
        )
    }
    private val localDataSource = mockk<ConversationLocalDataSource> {
        coEvery { this@mockk.getConversations(any(), any()) } returns emptyList()
        coEvery { this@mockk.isLocalPageValid(any(), any(), any()) } returns false
        coEvery { this@mockk.upsertConversations(any(), any(), any()) } just Runs
    }

    private lateinit var conversationRepository: ConversationRepositoryImpl

    @Before
    fun setUp() {
        conversationRepository = ConversationRepositoryImpl(remoteDataSource, localDataSource)
    }

    @Test
    fun `return remote if local page is invalid`() = runTest {
        // Given
        val pageKey = PageKey()
        val local = listOf(
            getConversation(id = "1", time = 1000),
        )
        val remote = listOf(
            getConversation(id = "1", time = 1000),
            getConversation(id = "2", time = 2000),
            getConversation(id = "3", time = 3000),
        )
        coEvery { localDataSource.getConversations(any(), any()) } returns local
        coEvery { localDataSource.isLocalPageValid(any(), any(), any()) } returns false
        coEvery { localDataSource.getClippedPageKey(any(), any()) } returns pageKey
        coEvery { remoteDataSource.getConversations(any(), any()) } returns remote

        // When
        val result = conversationRepository.getConversations(userId, pageKey)

        // Then
        assertEquals(3, result.size)
        coVerify(exactly = 1) { localDataSource.isLocalPageValid(userId, pageKey, local) }
        coVerify(exactly = 1) { remoteDataSource.getConversations(userId, pageKey) }
        coVerify(exactly = 1) { localDataSource.upsertConversations(userId, pageKey, remote) }
    }

    @Test
    fun `return local if remote fail`() = runTest {
        // Given
        val pageKey = PageKey()
        val local = listOf(
            getConversation(id = "1", time = 1000),
            getConversation(id = "2", time = 2000),
        )
        coEvery { localDataSource.getConversations(any(), any()) } returns local
        coEvery { localDataSource.isLocalPageValid(any(), any(), any()) } returns false
        coEvery { localDataSource.getClippedPageKey(any(), any()) } returns pageKey
        coEvery { remoteDataSource.getConversations(any(), any()) } throws IOException()

        // When
        val result = conversationRepository.getConversations(userId, pageKey)

        // Then
        assertEquals(2, result.size)
        coVerify(exactly = 1) { localDataSource.isLocalPageValid(userId, pageKey, local) }
        coVerify(exactly = 1) { remoteDataSource.getConversations(userId, pageKey) }
    }

    @Test
    fun `return local if valid`() = runTest {
        // Given
        val pageKey = PageKey()
        val local = listOf(
            getConversation(id = "1", time = 1000),
            getConversation(id = "2", time = 2000),
        )
        val remote = listOf(
            getConversation(id = "1", time = 1000),
            getConversation(id = "2", time = 2000),
            getConversation(id = "3", time = 3000),
        )
        coEvery { localDataSource.getConversations(any(), any()) } returns local
        coEvery { localDataSource.isLocalPageValid(any(), any(), any()) } returns true
        coEvery { remoteDataSource.getConversations(any(), any()) } returns remote

        // When
        val conversations = conversationRepository.getConversations(userId, pageKey)

        // Then
        assertEquals(2, conversations.size)
        coVerify(exactly = 1) { localDataSource.isLocalPageValid(userId, pageKey, local) }
        coVerify(exactly = 0) { remoteDataSource.getConversations(any(), any()) }
    }

    @Test
    fun `clip pageKey before calling remote`() = runTest {
        // Given
        val pageKey = PageKey()
        val clippedPageKey = PageKey(filter = PageFilter(minTime = 0))
        coEvery { localDataSource.getConversations(any(), any()) } returns emptyList()
        coEvery { localDataSource.isLocalPageValid(any(), any(), any()) } returns false
        coEvery { localDataSource.getClippedPageKey(any(), any()) } returns clippedPageKey
        coEvery { remoteDataSource.getConversations(any(), any()) } returns emptyList()
        // When
        val conversations = conversationRepository.getConversations(userId, pageKey)

        // Then
        assertEquals(0, conversations.size)
        coVerify(exactly = 1) { localDataSource.isLocalPageValid(any(), any(), any()) }
        coVerify(ordering = Ordering.ORDERED) {
            localDataSource.getClippedPageKey(userId, pageKey)
            remoteDataSource.getConversations(userId, clippedPageKey)
        }
    }
}
