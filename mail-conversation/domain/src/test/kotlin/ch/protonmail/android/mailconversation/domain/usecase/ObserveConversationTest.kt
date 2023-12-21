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

package ch.protonmail.android.mailconversation.domain.usecase

import app.cash.turbine.test
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.ConversationId
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailconversation.domain.repository.ConversationRepository
import ch.protonmail.android.testdata.conversation.ConversationTestData
import ch.protonmail.android.testdata.user.UserIdTestData.userId
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals

class ObserveConversationTest {

    private val repository = mockk<ConversationRepository> {
        every {
            this@mockk.observeConversation(
                userId,
                any(),
                true
            )
        } returns flowOf(DataError.Local.NoDataCached.left())
    }

    private val observeConversation = ObserveConversation(repository)

    @Test
    fun `returns local data error when conversation does not exist in repository`() = runTest {
        // Given
        val conversationId = ConversationId(ConversationTestData.RAW_CONVERSATION_ID)
        val error = DataError.Local.NoDataCached
        every { repository.observeConversation(userId, conversationId, true) } returns flowOf(error.left())

        // When
        observeConversation(userId, conversationId, refreshData = true).test {
            // Then
            assertEquals(error.left(), awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `returns conversation when it exists in repository`() = runTest {
        // Given
        val conversationId = ConversationId(ConversationTestData.RAW_CONVERSATION_ID)
        val conversation = ConversationTestData.conversation
        every { repository.observeConversation(userId, conversationId, true) } returns flowOf(conversation.right())

        // When
        observeConversation(userId, conversationId, refreshData = true).test {
            // Then
            assertEquals(conversation.right(), awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `returns conversation when it exists in repository and refresh is not requested`() = runTest {
        // Given
        val conversationId = ConversationId(ConversationTestData.RAW_CONVERSATION_ID)
        val conversation = ConversationTestData.conversation
        every { repository.observeConversation(userId, conversationId, false) } returns flowOf(conversation.right())

        // When
        observeConversation(userId, conversationId, refreshData = false).test {
            // Then
            assertEquals(conversation.right(), awaitItem())
            awaitComplete()
        }
    }
}
