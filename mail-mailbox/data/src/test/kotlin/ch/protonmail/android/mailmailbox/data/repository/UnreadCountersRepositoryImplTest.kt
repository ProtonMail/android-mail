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

package ch.protonmail.android.mailmailbox.data.repository

import app.cash.turbine.test
import ch.protonmail.android.mailcommon.domain.sample.UserIdSample
import ch.protonmail.android.mailmailbox.data.entity.UnreadConversationsCountEntity
import ch.protonmail.android.mailmailbox.data.entity.UnreadMessagesCountEntity
import ch.protonmail.android.mailmailbox.data.local.UnreadConversationsCountLocalDataSource
import ch.protonmail.android.mailmailbox.data.remote.UnreadConversationsCountRemoteDataSource
import ch.protonmail.android.mailmailbox.data.remote.response.UnreadCountResource
import ch.protonmail.android.mailmailbox.domain.model.UnreadCounter
import ch.protonmail.android.mailmailbox.domain.model.UnreadCounters
import io.mockk.Called
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import me.proton.core.label.domain.entity.LabelId
import org.junit.Ignore
import org.junit.Test
import kotlin.test.assertEquals

class UnreadCountersRepositoryImplTest {

    private val localDataSource = mockk<UnreadConversationsCountLocalDataSource>()
    private val remoteDataSource = mockk<UnreadConversationsCountRemoteDataSource>()

    private lateinit var repository: UnreadCountersRepositoryImpl

    @Test
    @Ignore("Not working due to intermediate step - Fixed through the next step of the refactoring")
    fun `refresh message and conversation counters from remote when not existing locally`() = runTest {
        // Given
        val expectedMessages = listOf(inboxUnreadMessageCounter)
        val expectedConversations = listOf(inboxUnreadConversationCounter)

        // When
        repository.observeUnreadCounters(userId).test {
            // Then
            awaitItem()
            coVerify {
                localDataSource.saveConversationCounters(expectedConversations)
            }
            awaitComplete()
        }

    }

    @Test
    @Ignore("Not working due to intermediate step - Fixed through the next step of the refactoring")
    fun `returns local unread counters when available`() = runTest {
        // Given
        val expectedMessages = listOf(inboxUnreadMessageCounter)
        val expectedConversations = listOf(inboxUnreadConversationCounter)
        coEvery { localDataSource.observeConversationCounters(userId) } returns flowOf(expectedConversations)

        // When
        repository.observeUnreadCounters(userId).test {
            // Then
            val expected = UnreadCounters(
                expectedConversations.map { UnreadCounter(it.labelId, it.unreadCount) },
                expectedMessages.map { UnreadCounter(it.labelId, it.unreadCount) }
            )
            assertEquals(expected, awaitItem())
            verify { remoteDataSource wasNot Called }
            awaitComplete()
        }

    }

    companion object TestData {
        private val userId = UserIdSample.Primary

        val inboxUnreadConversationCounter = UnreadConversationsCountEntity(
            userId,
            LabelId("0"),
            10,
            1
        )

        val inboxUnreadMessageCounter = UnreadMessagesCountEntity(userId, LabelId("0"), 10, 1)

        val inboxUnreadCounterResource = UnreadCountResource("0", 10, 1)
    }
}
