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

package ch.protonmail.android.mailconversation.data.repository

import app.cash.turbine.test
import ch.protonmail.android.mailcommon.domain.sample.LabelIdSample
import ch.protonmail.android.mailcommon.domain.sample.UserIdSample
import ch.protonmail.android.mailconversation.data.local.UnreadConversationsCountLocalDataSource
import ch.protonmail.android.mailconversation.data.local.entity.UnreadConversationsCountEntity
import ch.protonmail.android.mailconversation.data.remote.UnreadConversationsCountRemoteDataSource
import ch.protonmail.android.mailconversation.data.remote.resource.UnreadConversationCountResource
import ch.protonmail.android.mailmessage.domain.model.UnreadCounter
import io.mockk.Called
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import me.proton.core.label.domain.entity.LabelId
import org.junit.Test
import kotlin.test.assertEquals

class UnreadConversationCountRepositoryImplTest {

    private val localDataSource = mockk<UnreadConversationsCountLocalDataSource>()
    private val remoteDataSource = mockk<UnreadConversationsCountRemoteDataSource>()

    private val repository = UnreadConversationsCountRepositoryImpl(localDataSource, remoteDataSource)

    @Test
    fun `refresh conversation counters from remote when not existing locally`() = runTest {
        // Given
        val expectedConversations = listOf(inboxUnreadConversationCounter)
        coEvery { remoteDataSource.getConversationCounters(userId) } returns listOf(inboxUnreadCounterResource)
        coEvery { localDataSource.observeConversationCounters(userId) } returns flowOf(emptyList())
        coEvery { localDataSource.saveConversationCounters(expectedConversations) } just Runs


        // When
        repository.observeUnreadCounters(userId).test {
            // Then
            awaitItem()
            coVerify { localDataSource.saveConversationCounters(expectedConversations) }
            awaitComplete()
        }

    }

    @Test
    fun `returns local unread counters when available`() = runTest {
        // Given
        val expectedConversations = listOf(inboxUnreadConversationCounter)
        coEvery { localDataSource.observeConversationCounters(userId) } returns flowOf(expectedConversations)

        // When
        repository.observeUnreadCounters(userId).test {
            // Then
            val expected = expectedConversations.map { UnreadCounter(it.labelId, it.unreadCount) }
            assertEquals(expected, awaitItem())
            verify { remoteDataSource wasNot Called }
            awaitComplete()
        }

    }

    @Test
    fun `increments unread count for the given labelId`() = runTest {
        // Given
        val expectedConversations = listOf(inboxUnreadConversationCounter)
        val labelIds = LabelIdSample.Inbox
        val expected = inboxUnreadConversationCounter.copy(unreadCount = 2)
        coEvery { localDataSource.observeConversationCounters(userId) } returns flowOf(expectedConversations)
        coEvery { localDataSource.saveConversationCounter(expected) } just Runs

        // When
        repository.incrementUnreadCount(userId, labelIds)

        // Then
        coVerify { localDataSource.saveConversationCounter(expected) }
    }

    @Test
    fun `decrements unread count for the given labelId`() = runTest {
        // Given
        val expectedConversations = listOf(inboxUnreadConversationCounter)
        val labelIds = LabelIdSample.Inbox
        val expected = inboxUnreadConversationCounter.copy(unreadCount = 0)
        coEvery { localDataSource.observeConversationCounters(userId) } returns flowOf(expectedConversations)
        coEvery { localDataSource.saveConversationCounter(expected) } just Runs

        // When
        repository.decrementUnreadCount(userId, labelIds)

        // Then
        coVerify { localDataSource.saveConversationCounter(expected) }
    }

    companion object TestData {
        private val userId = UserIdSample.Primary

        val inboxUnreadConversationCounter = UnreadConversationsCountEntity(
            userId,
            LabelId("0"),
            10,
            1
        )

        val inboxUnreadCounterResource = UnreadConversationCountResource("0", 10, 1)
    }
}
