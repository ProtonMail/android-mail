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

import java.io.IOException
import app.cash.turbine.test
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.ConversationId
import ch.protonmail.android.mailconversation.data.repository.ConversationRepositoryImpl
import ch.protonmail.android.mailconversation.domain.entity.Conversation
import ch.protonmail.android.mailconversation.domain.repository.ConversationLocalDataSource
import ch.protonmail.android.mailconversation.domain.repository.ConversationRemoteDataSource
import ch.protonmail.android.mailpagination.domain.entity.PageFilter
import ch.protonmail.android.mailpagination.domain.entity.PageKey
import ch.protonmail.android.testdata.conversation.ConversationWithContextTestData
import io.mockk.Ordering
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds

class ConversationRepositoryImplTest {

    private val userId = UserId("1")

    private val remoteDataSource = mockk<ConversationRemoteDataSource> {
        coEvery { this@mockk.getConversations(any(), any()) } returns listOf(
            ConversationWithContextTestData.conversation1,
            ConversationWithContextTestData.conversation2,
            ConversationWithContextTestData.conversation3,
            ConversationWithContextTestData.conversation4
        )
    }
    private val localDataSource = mockk<ConversationLocalDataSource>(relaxUnitFun = true) {
        coEvery { this@mockk.getConversations(any(), any()) } returns emptyList()
        coEvery { this@mockk.isLocalPageValid(any(), any(), any()) } returns false
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
            ConversationWithContextTestData.conversation1
        )
        val remote = listOf(
            ConversationWithContextTestData.conversation1,
            ConversationWithContextTestData.conversation2,
            ConversationWithContextTestData.conversation3
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
            ConversationWithContextTestData.conversation1,
            ConversationWithContextTestData.conversation2
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
            ConversationWithContextTestData.conversation1,
            ConversationWithContextTestData.conversation2
        )
        val remote = listOf(
            ConversationWithContextTestData.conversation1,
            ConversationWithContextTestData.conversation2,
            ConversationWithContextTestData.conversation3
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

    @Test
    fun `observe conversation emits conversation when existing in cache and observe updates`() = runTest {
        // Given
        val conversationId = ConversationId("conversationId")
        val conversation = getConversation(userId, conversationId.id)
        val conversationFlow = MutableSharedFlow<Conversation>()
        coEvery { localDataSource.observeConversation(userId, conversationId) } returns conversationFlow
        coEvery { remoteDataSource.getConversation(userId, conversationId) } returns conversation
        // When
        conversationRepository.observeConversation(userId, conversationId).test(timeout = 2.seconds) {
            // Then
            conversationFlow.emit(conversation)
            assertEquals(conversation.right(), awaitItem())

            val updatedConversation = conversation.copy(numUnread = 3)
            conversationFlow.emit(updatedConversation)
            assertEquals(updatedConversation.right(), awaitItem())
        }
    }

    @Test
    fun `observe conversation emits conversation from remote when not present in cache`() = runTest {
        // Given
        val conversationId = ConversationId("conversationId")
        val conversation = getConversation(userId, conversationId.id)
        coEvery { localDataSource.observeConversation(userId, conversationId) } returns flowOf(null)
        coEvery { remoteDataSource.getConversation(userId, conversationId) } returns conversation
        // When
        conversationRepository.observeConversation(userId, conversationId).test {
            // Then
            coVerify(timeout = 500) { remoteDataSource.getConversation(userId, conversationId) }
        }
    }

    @Test
    fun `observe conversation always refreshes local cache with the conversation from remote`() = runTest {
        // Given
        val conversationId = ConversationId("conversationId")
        val conversation = getConversation(userId, conversationId.id)
        val updatedConversation = conversation.copy(numUnread = 5)
        coEvery { localDataSource.observeConversation(userId, conversationId) } returns flowOf(conversation)
        coEvery { remoteDataSource.getConversation(userId, conversationId) } returns updatedConversation
        // When
        conversationRepository.observeConversation(userId, conversationId).test {
            // Then
            coVerify(timeout = 500) { localDataSource.upsertConversation(userId, updatedConversation) }
            cancelAndConsumeRemainingEvents()
        }
    }
}
