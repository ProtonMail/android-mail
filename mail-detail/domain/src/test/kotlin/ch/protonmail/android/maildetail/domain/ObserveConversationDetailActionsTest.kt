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

package ch.protonmail.android.maildetail.domain

import app.cash.turbine.test
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.Action
import ch.protonmail.android.mailcommon.domain.model.ConversationId
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailconversation.domain.usecase.ObserveConversation
import ch.protonmail.android.maildetail.domain.usecase.ObserveConversationDetailActions
import ch.protonmail.android.testdata.conversation.ConversationTestData
import ch.protonmail.android.testdata.user.UserIdTestData.userId
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals

internal class ObserveConversationDetailActionsTest {

    private val observeConversation = mockk<ObserveConversation> {
        every {
            this@mockk.invoke(userId, ConversationId(ConversationTestData.RAW_CONVERSATION_ID), true)
        } returns flowOf(ConversationTestData.conversation.right())
    }

    private val observeDetailActions = ObserveConversationDetailActions(
        observeConversation = observeConversation
    )

    @Test
    fun `returns default actions list for conversation`() = runTest {
        // Given
        val conversationId = ConversationId(ConversationTestData.RAW_CONVERSATION_ID)
        // When
        observeDetailActions.invoke(userId, conversationId, refreshConversations = true).test {
            // Then
            val expected = listOf(
                Action.MarkUnread,
                Action.Move,
                Action.Trash,
                Action.Label
            )
            assertEquals(expected.right(), awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `returns delete action when all messages in conversation are trash or spam`() = runTest {
        // Given
        val conversationId = ConversationId(ConversationTestData.RAW_CONVERSATION_ID)
        val conversation = ConversationTestData.trashAndSpamConversation
        every { observeConversation.invoke(userId, conversationId, true) } returns flowOf(conversation.right())
        // When
        observeDetailActions.invoke(userId, conversationId, true).test {
            // Then
            val expected = listOf(
                Action.MarkUnread,
                Action.Move,
                Action.Delete,
                Action.Label
            )
            assertEquals(expected.right(), awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `returns delete action when all messages in conversation are trash and have all_drafts or all_sent labels`() =
        runTest {
            // Given
            val conversationId = ConversationId(ConversationTestData.RAW_CONVERSATION_ID)
            val conversation = ConversationTestData.trashConversationWithAllSentAllDrafts
            every {
                observeConversation.invoke(
                    userId,
                    conversationId,
                    refreshData = true
                )
            } returns flowOf(conversation.right())
            // When
            observeDetailActions.invoke(userId, conversationId, refreshConversations = true).test {
                // Then
                val expected = listOf(
                    Action.MarkUnread,
                    Action.Move,
                    Action.Delete,
                    Action.Label
                )
                assertEquals(expected.right(), awaitItem())
                awaitComplete()
            }
        }

    @Test
    fun `returns data error when failing to get conversation`() = runTest {
        // Given
        val conversationId = ConversationId(ConversationTestData.RAW_CONVERSATION_ID)
        every { observeConversation.invoke(userId, conversationId, refreshData = true) } returns flowOf(
            DataError.Local.NoDataCached.left()
        )
        // When
        observeDetailActions.invoke(userId, conversationId, refreshConversations = true).test {
            // Then
            assertEquals(DataError.Local.NoDataCached.left(), awaitItem())
            awaitComplete()
        }
    }
}
