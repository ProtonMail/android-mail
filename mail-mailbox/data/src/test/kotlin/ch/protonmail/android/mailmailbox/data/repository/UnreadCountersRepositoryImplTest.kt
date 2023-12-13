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
import ch.protonmail.android.mailconversation.domain.repository.UnreadConversationsCountRepository
import ch.protonmail.android.mailmessage.domain.model.UnreadCounter
import ch.protonmail.android.mailmessage.domain.repository.UnreadMessagesCountRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import me.proton.core.label.domain.entity.LabelId
import org.junit.Test
import kotlin.test.assertEquals

class UnreadCountersRepositoryImplTest {

    private val messageUnreadCountRepository = mockk<UnreadMessagesCountRepository>()
    private val conversationUnreadCountRepository = mockk<UnreadConversationsCountRepository>()

    private val repository = UnreadCountersRepositoryImpl(
        messageUnreadCountRepository,
        conversationUnreadCountRepository
    )

    @Test
    fun `combines messages and conversations unread counters`() = runTest {
        // Given
        val expectedMessages = listOf(inboxUnreadMessageCounter)
        val expectedConversations = listOf(inboxUnreadConversationCounter)
        coEvery { messageUnreadCountRepository.observeUnreadCounters(userId) } returns flowOf(expectedMessages)
        coEvery {
            conversationUnreadCountRepository.observeUnreadCounters(userId)
        } returns flowOf(expectedConversations)

        // When
        repository.observeUnreadCounters(userId).test {
            // Then
            val actual = awaitItem()
            assertEquals(expectedMessages, actual.messagesUnreadCount)
            assertEquals(expectedConversations, actual.conversationsUnreadCount)
            awaitComplete()
        }

    }

    companion object TestData {
        private val userId = UserIdSample.Primary

        val inboxUnreadConversationCounter = UnreadCounter(LabelId("0"), 10)

        val inboxUnreadMessageCounter = UnreadCounter(LabelId("0"), 2)
    }
}
