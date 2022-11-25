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
import ch.protonmail.android.mailconversation.domain.entity.ConversationWithMessages
import ch.protonmail.android.mailconversation.domain.repository.ConversationLocalDataSource
import ch.protonmail.android.mailconversation.domain.repository.ConversationRemoteDataSource
import ch.protonmail.android.mailmessage.data.local.MessageLocalDataSource
import ch.protonmail.android.mailpagination.domain.model.PageFilter
import ch.protonmail.android.mailpagination.domain.model.PageKey
import ch.protonmail.android.testdata.conversation.ConversationTestData
import ch.protonmail.android.testdata.conversation.ConversationWithContextTestData
import ch.protonmail.android.testdata.message.MessageTestData
import io.mockk.Ordering
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import me.proton.core.label.domain.entity.LabelId
import me.proton.core.test.kotlin.TestCoroutineScopeProvider
import org.junit.Test
import kotlin.test.assertEquals

class ConversationRepositoryImplTest {

    private val userId = UserId("1")

    private val conversationLocalDataSource = mockk<ConversationLocalDataSource>(relaxUnitFun = true) {
        coEvery { this@mockk.getConversations(any(), any()) } returns emptyList()
        coEvery { this@mockk.isLocalPageValid(any(), any(), any()) } returns false
    }
    private val conversationRemoteDataSource = mockk<ConversationRemoteDataSource> {
        coEvery { this@mockk.getConversations(any(), any()) } returns listOf(
            ConversationWithContextTestData.conversation1,
            ConversationWithContextTestData.conversation2,
            ConversationWithContextTestData.conversation3,
            ConversationWithContextTestData.conversation4
        )
    }

    private val coroutineScopeProvider = TestCoroutineScopeProvider

    private val messageLocalDataSource: MessageLocalDataSource = mockk(relaxUnitFun = true) {
        coEvery { this@mockk.observeMessages(any(), any<ConversationId>()) } returns flowOf(
            MessageTestData.unStarredMessagesByConversation
        )
    }

    private val conversationRepository = ConversationRepositoryImpl(
        conversationLocalDataSource = conversationLocalDataSource,
        conversationRemoteDataSource = conversationRemoteDataSource,
        coroutineScopeProvider = coroutineScopeProvider,
        messageLocalDataSource = messageLocalDataSource
    )

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
        coEvery { conversationLocalDataSource.getConversations(any(), any()) } returns local
        coEvery { conversationLocalDataSource.isLocalPageValid(any(), any(), any()) } returns false
        coEvery { conversationLocalDataSource.getClippedPageKey(any(), any()) } returns pageKey
        coEvery { conversationRemoteDataSource.getConversations(any(), any()) } returns remote

        // When
        val result = conversationRepository.getConversations(userId, pageKey)

        // Then
        assertEquals(3, result.size)
        coVerify(exactly = 1) { conversationLocalDataSource.isLocalPageValid(userId, pageKey, local) }
        coVerify(exactly = 1) { conversationRemoteDataSource.getConversations(userId, pageKey) }
        coVerify(exactly = 1) { conversationLocalDataSource.upsertConversations(userId, pageKey, remote) }
    }

    @Test
    fun `return local if remote fail`() = runTest {
        // Given
        val pageKey = PageKey()
        val local = listOf(
            ConversationWithContextTestData.conversation1,
            ConversationWithContextTestData.conversation2
        )
        coEvery { conversationLocalDataSource.getConversations(any(), any()) } returns local
        coEvery { conversationLocalDataSource.isLocalPageValid(any(), any(), any()) } returns false
        coEvery { conversationLocalDataSource.getClippedPageKey(any(), any()) } returns pageKey
        coEvery { conversationRemoteDataSource.getConversations(any(), any()) } throws IOException()

        // When
        val result = conversationRepository.getConversations(userId, pageKey)

        // Then
        assertEquals(2, result.size)
        coVerify(exactly = 1) { conversationLocalDataSource.isLocalPageValid(userId, pageKey, local) }
        coVerify(exactly = 1) { conversationRemoteDataSource.getConversations(userId, pageKey) }
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
        coEvery { conversationLocalDataSource.getConversations(any(), any()) } returns local
        coEvery { conversationLocalDataSource.isLocalPageValid(any(), any(), any()) } returns true
        coEvery { conversationRemoteDataSource.getConversations(any(), any()) } returns remote

        // When
        val conversations = conversationRepository.getConversations(userId, pageKey)

        // Then
        assertEquals(2, conversations.size)
        coVerify(exactly = 1) { conversationLocalDataSource.isLocalPageValid(userId, pageKey, local) }
        coVerify(exactly = 0) { conversationRemoteDataSource.getConversations(any(), any()) }
    }

    @Test
    fun `clip pageKey before calling remote`() = runTest {
        // Given
        val pageKey = PageKey()
        val clippedPageKey = PageKey(filter = PageFilter(minTime = 0))
        coEvery { conversationLocalDataSource.getConversations(any(), any()) } returns emptyList()
        coEvery { conversationLocalDataSource.isLocalPageValid(any(), any(), any()) } returns false
        coEvery { conversationLocalDataSource.getClippedPageKey(any(), any()) } returns clippedPageKey
        coEvery { conversationRemoteDataSource.getConversations(any(), any()) } returns emptyList()

        // When
        val conversations = conversationRepository.getConversations(userId, pageKey)

        // Then
        assertEquals(0, conversations.size)
        coVerify(exactly = 1) { conversationLocalDataSource.isLocalPageValid(any(), any(), any()) }
        coVerify(ordering = Ordering.ORDERED) {
            conversationLocalDataSource.getClippedPageKey(userId, pageKey)
            conversationRemoteDataSource.getConversations(userId, clippedPageKey)
        }
    }

    @Test
    fun `observe conversation emits conversation when existing in cache and observe updates`() = runTest {
        // Given
        val conversationId = ConversationId("conversationId")
        val conversation = getConversation(userId, conversationId.id)
        val conversationFlow = MutableSharedFlow<Conversation>()
        coEvery { conversationLocalDataSource.observeConversation(userId, conversationId) } returns conversationFlow
        coEvery { conversationRemoteDataSource.getConversationWithMessages(userId, conversationId) } returns
            ConversationWithMessages(conversation = conversation, messages = emptyList())

        // When
        conversationRepository.observeConversation(userId, conversationId).test {
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
        coEvery { conversationLocalDataSource.observeConversation(userId, conversationId) } returns flowOf(null)
        coEvery { conversationRemoteDataSource.getConversationWithMessages(userId, conversationId) } returns
            ConversationWithMessages(conversation = conversation, messages = emptyList())

        // When
        conversationRepository.observeConversation(userId, conversationId).test {
            // Then
            coVerify { conversationRemoteDataSource.getConversationWithMessages(userId, conversationId) }
        }
    }

    @Test
    fun `observe conversation always refreshes local cache with the conversation from remote`() = runTest {
        // Given
        val conversationId = ConversationId("conversationId")
        val conversation = getConversation(userId, conversationId.id)
        val updatedConversation = conversation.copy(numUnread = 5)
        coEvery { conversationLocalDataSource.observeConversation(userId, conversationId) } returns flowOf(conversation)
        coEvery {
            conversationRemoteDataSource.getConversationWithMessages(
                userId,
                conversationId
            )
        } returns ConversationWithMessages(conversation = updatedConversation, messages = emptyList())

        // When
        conversationRepository.observeConversation(userId, conversationId).test {
            // Then
            coVerify { conversationLocalDataSource.upsertConversation(userId, updatedConversation) }
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `add label returns conversation with label when upsert was successful`() = runTest {
        // Given
        every { conversationLocalDataSource.observeConversation(any(), any()) } returns flowOf(
            ConversationTestData.conversation
        )

        every { messageLocalDataSource.observeMessages(any(), any<ConversationId>()) } returns flowOf(
            MessageTestData.unStarredMessagesByConversation
        )

        // When
        val actual = conversationRepository.addLabel(
            userId, ConversationId(ConversationTestData.RAW_CONVERSATION_ID), LabelId("10")
        )

        // Then
        assertEquals(ConversationTestData.starredConversation.right(), actual)
        coVerify { conversationLocalDataSource.upsertConversation(userId, ConversationTestData.starredConversation) }
    }

    @Test
    fun `add label to stored messages of conversation`() = runTest {
        // Given
        every { conversationLocalDataSource.observeConversation(any(), any()) } returns flowOf(
            ConversationTestData.conversation
        )

        // When
        conversationRepository.addLabel(
            userId,
            ConversationId(ConversationTestData.RAW_CONVERSATION_ID),
            LabelId("10")
        )

        // Then
        coVerify { messageLocalDataSource.upsertMessages(MessageTestData.starredMessagesByConversation) }
    }

    @Test
    fun `add label returns updated conversation containing new label with latest message time`() = runTest {
        // Given
        every { conversationLocalDataSource.observeConversation(any(), any()) } returns flowOf(
            ConversationTestData.conversationWithConversationLabels
        )

        // When
        val actual = conversationRepository.addLabel(
            userId, ConversationId(ConversationTestData.RAW_CONVERSATION_ID), LabelId("10")
        )

        // Then
        val actualTime = actual.orNull()!!.labels.first { it.labelId == LabelId("10") }.contextTime
        assertEquals(10, actualTime)
    }

    @Test
    fun `add label returns updated conversation containing new label with sum of messages size`() = runTest {
        // Given
        every { conversationLocalDataSource.observeConversation(any(), any()) } returns flowOf(
            ConversationTestData.conversationWithConversationLabels
        )
        coEvery { messageLocalDataSource.observeMessages(any(), any<ConversationId>()) } returns flowOf(
            MessageTestData.messagesWithSizeByConversation
        )

        // When
        val actual = conversationRepository.addLabel(
            userId, ConversationId(ConversationTestData.RAW_CONVERSATION_ID), LabelId("10")
        )

        // Then
        val actualSize = actual.orNull()!!.labels.first { it.labelId == LabelId("10") }.contextSize
        assertEquals(1200L, actualSize)
    }

    @Test
    fun `add label returns updated conversation containing new label with conversation values`() = runTest {
        // Given
        every { conversationLocalDataSource.observeConversation(any(), any()) } returns flowOf(
            ConversationTestData.conversationWithInformation
        )

        // When
        val actual = conversationRepository.addLabel(
            userId, ConversationId(ConversationTestData.RAW_CONVERSATION_ID), LabelId("10")
        )

        // Then
        val actualAddedLabel = actual.orNull()!!.labels.first { it.labelId == LabelId("10") }
        assertEquals(1, actualAddedLabel.contextNumMessages)
        assertEquals(5, actualAddedLabel.contextNumAttachments)
        assertEquals(6, actualAddedLabel.contextNumUnread)
    }

    @Test
    fun `add label conversation even if no messages are stored`() = runTest {
        // Given
        every { conversationLocalDataSource.observeConversation(any(), any()) } returns flowOf(
            ConversationTestData.conversation
        )

        every { messageLocalDataSource.observeMessages(any(), any<ConversationId>()) } returns flowOf(
            listOf()
        )

        // When
        val actual = conversationRepository.addLabel(
            userId, ConversationId(ConversationTestData.RAW_CONVERSATION_ID), LabelId("10")
        )

        // Then
        assertEquals(ConversationTestData.starredConversation.right(), actual)
    }

    @Test
    fun `add label to messages of a conversation`() = runTest {
        // Given
        every { conversationLocalDataSource.observeConversation(any(), any()) } returns flowOf(
            ConversationTestData.conversation
        )
        every { messageLocalDataSource.observeMessages(userId, any<ConversationId>()) } returns flowOf(
            MessageTestData.unStarredMessagesByConversation
        )

        // When
        conversationRepository.addLabel(userId, ConversationId(ConversationTestData.RAW_CONVERSATION_ID), LabelId("10"))

        // Then
        val expectedMessage = MessageTestData.starredMessagesByConversation
        coVerify { messageLocalDataSource.upsertMessages(expectedMessage) }
    }
}
