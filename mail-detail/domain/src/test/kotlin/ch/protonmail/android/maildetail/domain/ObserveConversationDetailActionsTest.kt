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
import arrow.core.nonEmptyListOf
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.Action
import ch.protonmail.android.mailcommon.domain.model.ConversationId
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailconversation.domain.usecase.ObserveConversation
import ch.protonmail.android.maildetail.domain.usecase.ObserveConversationDetailActions
import ch.protonmail.android.maildetail.domain.usecase.ObserveConversationMessagesWithLabels
import ch.protonmail.android.mailmessage.domain.sample.MessageWithLabelsSample
import ch.protonmail.android.mailsettings.domain.usecase.ObserveMailMessageToolbarSettings
import ch.protonmail.android.testdata.conversation.ConversationTestData
import ch.protonmail.android.testdata.user.UserIdTestData.userId
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.BeforeTest
import kotlin.test.assertEquals

internal class ObserveConversationDetailActionsTest {

    private val archivedConversationId = "archived-id"
    private val customFolderConversationId = "custom-folder-conv-id"
    private val starredConversationId = "starred-folder-conv-id"

    private val observeConversation = mockk<ObserveConversation> {
        every {
            this@mockk.invoke(userId, ConversationId(ConversationTestData.RAW_CONVERSATION_ID), true)
        } returns flowOf(ConversationTestData.conversation.right())
        every {
            this@mockk.invoke(userId, ConversationId(archivedConversationId), true)
        } returns flowOf(
            ConversationTestData.conversationWithArchiveLabel
                .copy(conversationId = ConversationId(archivedConversationId))
                .right()
        )
        every {
            this@mockk.invoke(userId, ConversationId(customFolderConversationId), true)
        } returns flowOf(
            ConversationTestData.customFolderConversation
                .copy(conversationId = ConversationId(customFolderConversationId))
                .right()
        )
        every {
            this@mockk.invoke(userId, ConversationId(starredConversationId), true)
        } returns flowOf(
            ConversationTestData.starredConversation
                .copy(conversationId = ConversationId(starredConversationId))
                .right()
        )
    }

    private val observeToolbarActions = mockk<ObserveMailMessageToolbarSettings> {
        every {
            this@mockk.invoke(userId, false)
        } returns flowOf(null)
    }

    private val observeMessages = mockk<ObserveConversationMessagesWithLabels> {
        every {
            this@mockk.invoke(userId, any())
        } returns flowOf(nonEmptyListOf(MessageWithLabelsSample.InvoiceWithLabel).right())
    }

    private val observeDetailActions by lazy {
        ObserveConversationDetailActions(
            observeConversation = observeConversation,
            observeToolbarActions = observeToolbarActions,
            observeMessages = observeMessages
        )
    }

    @BeforeTest
    fun setUp() {
        MockKAnnotations.init(this)
    }

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
                Action.Label,
                Action.More
            )
            assertEquals(expected.right(), awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `returns settings actions list for conversation`() = runTest {
        // Given
        val conversationId = ConversationId(ConversationTestData.RAW_CONVERSATION_ID)
        every { observeToolbarActions.invoke(userId, false) } returns flowOf(
            listOf(
                Action.Label,
                Action.ReportPhishing,
                Action.Star,
                Action.Trash
            )
        )
        // When
        observeDetailActions.invoke(userId, conversationId, refreshConversations = true).test {
            // Then
            val expected = listOf(
                Action.Label,
                Action.ReportPhishing,
                Action.Star,
                Action.Trash,
                Action.More
            )
            assertEquals(expected.right(), awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `returns settings actions list for conversation and de-duplicates`() = runTest {
        // Given
        val conversationId = ConversationId(archivedConversationId)
        every { observeToolbarActions.invoke(userId, false) } returns flowOf(
            listOf(
                Action.Move,
                Action.Archive,
                Action.Star
            )
        )
        // When
        observeDetailActions.invoke(userId, conversationId, refreshConversations = true).test {
            // Then
            val expected = listOf(
                Action.Move,
                Action.Star,
                Action.More
            )
            assertEquals(expected.right(), awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `returns unstar action for starred conversations`() = runTest {
        // Given
        val conversationId = ConversationId(starredConversationId)
        every { observeToolbarActions.invoke(userId, false) } returns flowOf(
            listOf(
                Action.Move,
                Action.Archive,
                Action.Star
            )
        )
        // When
        observeDetailActions.invoke(userId, conversationId, refreshConversations = true).test {
            // Then
            val expected = listOf(
                Action.Move,
                Action.Archive,
                Action.Unstar,
                Action.More
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
                Action.Label,
                Action.More
            )
            assertEquals(expected.right(), awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `returns move to mailbox and delete action when all messages in conversation are trash or spam`() = runTest {
        // Given
        val conversationId = ConversationId(ConversationTestData.RAW_CONVERSATION_ID)
        val conversation = ConversationTestData.trashAndSpamConversation
        every { observeConversation.invoke(userId, conversationId, true) } returns flowOf(conversation.right())
        every { observeToolbarActions.invoke(userId, false) } returns flowOf(
            listOf(
                Action.Spam,
                Action.Trash
            )
        )
        // When
        observeDetailActions.invoke(userId, conversationId, true).test {
            // Then
            val expected = listOf(
                Action.Move,
                Action.Delete,
                Action.More
            )
            assertEquals(expected.right(), awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `returns standard actions for a conversation in a custom folder`() = runTest {
        // Given
        val conversationId = ConversationId(customFolderConversationId)
        val conversation = ConversationTestData.customFolderConversation
        every { observeConversation.invoke(userId, conversationId, true) } returns flowOf(conversation.right())
        every { observeToolbarActions.invoke(userId, false) } returns flowOf(
            listOf(
                Action.Spam,
                Action.Trash
            )
        )
        // When
        observeDetailActions.invoke(userId, conversationId, true).test {
            // Then
            val expected = listOf(
                Action.Spam,
                Action.Trash,
                Action.More
            )
            assertEquals(expected.right(), awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `returns trash action when preference returns delete and all messages are not trash nor spam`() = runTest {
        // Given
        val conversationId = ConversationId(ConversationTestData.RAW_CONVERSATION_ID)
        val conversation = ConversationTestData.conversation
        every { observeConversation.invoke(userId, conversationId, true) } returns flowOf(conversation.right())
        every { observeToolbarActions.invoke(userId, false) } returns flowOf(
            listOf(
                Action.Spam,
                Action.Delete
            )
        )
        // When
        observeDetailActions.invoke(userId, conversationId, true).test {
            // Then
            val expected = listOf(
                Action.Spam,
                Action.Trash,
                Action.More
            )
            assertEquals(expected.right(), awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `returns move to action when preference returns archive and all messages are in archive`() = runTest {
        // Given
        val conversationId = ConversationId(ConversationTestData.RAW_CONVERSATION_ID)
        val conversation = ConversationTestData.conversationWithArchiveLabel
        every { observeConversation.invoke(userId, conversationId, true) } returns flowOf(conversation.right())
        every { observeToolbarActions.invoke(userId, false) } returns flowOf(
            listOf(
                Action.Spam,
                Action.Delete,
                Action.Archive
            )
        )
        // When
        observeDetailActions.invoke(userId, conversationId, true).test {
            // Then
            val expected = listOf(
                Action.Spam,
                Action.Trash,
                Action.Move,
                Action.More
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
                    Action.Label,
                    Action.More
                )
                assertEquals(expected.right(), awaitItem())
                awaitComplete()
            }
        }

    @Test
    fun `returns reply all action when the last message has multiple recipients`() = runTest {
        // Given
        val conversationId = ConversationId(ConversationTestData.RAW_CONVERSATION_ID)
        val conversation = ConversationTestData.conversation
        every { observeConversation.invoke(userId, conversationId, true) } returns flowOf(conversation.right())
        every { observeToolbarActions.invoke(userId, false) } returns flowOf(
            listOf(
                Action.Spam,
                Action.Move,
                Action.Reply
            )
        )
        every {
            observeMessages.invoke(userId, conversationId)
        } returns flowOf(
            nonEmptyListOf(
                MessageWithLabelsSample.InvoiceWithoutLabelsMultipleRecipients,
                MessageWithLabelsSample.EmptyDraft
            ).right()
        )
        // When
        observeDetailActions.invoke(userId, conversationId, true).test {
            // Then
            val expected = listOf(
                Action.Spam,
                Action.Move,
                Action.ReplyAll,
                Action.More
            )
            assertEquals(expected.right(), awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `returns reply action when the last message has one recipient, regardless of previous`() = runTest {
        // Given
        val conversationId = ConversationId(ConversationTestData.RAW_CONVERSATION_ID)
        val conversation = ConversationTestData.conversation
        every { observeConversation.invoke(userId, conversationId, true) } returns flowOf(conversation.right())
        every { observeToolbarActions.invoke(userId, false) } returns flowOf(
            listOf(
                Action.Spam,
                Action.Move,
                Action.Reply
            )
        )
        every {
            observeMessages.invoke(userId, conversationId)
        } returns flowOf(
            nonEmptyListOf(
                MessageWithLabelsSample.InvoiceWithoutLabelsMultipleRecipients,
                MessageWithLabelsSample.InvoiceWithLabel,
                MessageWithLabelsSample.EmptyDraft
            ).right()
        )
        // When
        observeDetailActions.invoke(userId, conversationId, true).test {
            // Then
            val expected = listOf(
                Action.Spam,
                Action.Move,
                Action.Reply,
                Action.More
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
