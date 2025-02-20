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

package ch.protonmail.android.maildetail.data.repository

import java.util.Random
import java.util.UUID
import app.cash.turbine.test
import ch.protonmail.android.mailcommon.domain.sample.UserAddressSample
import ch.protonmail.android.maildetail.domain.repository.InMemoryConversationStateRepository
import ch.protonmail.android.maildetail.domain.repository.InMemoryConversationStateRepository.MessageState.Collapsed
import ch.protonmail.android.maildetail.domain.repository.InMemoryConversationStateRepository.MessageState.Expanded
import ch.protonmail.android.maildetail.domain.repository.InMemoryConversationStateRepository.MessageState.Expanding
import ch.protonmail.android.mailmessage.domain.model.DecryptedMessageBody
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailmessage.domain.model.MimeType
import ch.protonmail.android.mailmessage.domain.sample.MessageIdSample
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class InMemoryConversationStateRepositoryImplTest {

    @Test
    fun `Should emit empty cache on start`() = runTest {
        // Given
        val repo = buildRepository()

        repo.conversationState.test {
            // Then
            assertEquals(emptyMap(), awaitItem().messagesState)
        }
    }

    @Test
    fun `Should emit collapsed when putting a collapsed message id`() = runTest {
        // Given
        val repo = buildRepository()
        val messageId = MessageIdSample.Invoice

        // When
        repo.collapseMessage(messageId)

        repo.conversationState.test {
            val conversationState = awaitItem()

            // Then
            assertEquals(conversationState.messagesState[messageId], Collapsed)
            assertEquals(1, conversationState.messagesState.size)
        }
    }

    @Test
    fun `Should emit expanding when putting a expanding message`() = runTest {
        // Given
        val repo = buildRepository()
        val messageId = MessageIdSample.Invoice

        // When
        repo.expandingMessage(messageId)

        repo.conversationState.test {
            val conversationState = awaitItem()

            // Then
            assertEquals(conversationState.messagesState[messageId], Expanding)
            assertEquals(1, conversationState.messagesState.size)
        }
    }

    @Test
    fun `Should emit expanded when putting a expanded message`() = runTest {
        // Given
        val repo = buildRepository()
        val messageId = MessageIdSample.Invoice
        val decryptedBody = DecryptedMessageBody(
            messageId = messageId,
            value = UUID.randomUUID().toString(),
            mimeType = MimeType.Html,
            userAddress = UserAddressSample.PrimaryAddress
        )

        // When
        repo.expandMessage(messageId, decryptedBody, null)

        repo.conversationState.test {
            val conversationState = awaitItem()

            // Then
            assertEquals(conversationState.messagesState[messageId], Expanded(decryptedBody, null))
            assertEquals(1, conversationState.messagesState.size)
        }
    }

    @Test
    fun `Should emit expanded with effect when putting a expanded message with effect`() = runTest {
        // Given
        val repo = buildRepository()
        val messageId = MessageIdSample.Invoice
        val decryptedBody = DecryptedMessageBody(
            messageId = messageId,
            value = UUID.randomUUID().toString(),
            mimeType = MimeType.Html,
            userAddress = UserAddressSample.PrimaryAddress
        )

        // When
        repo.expandMessage(
            messageId, decryptedBody,
            InMemoryConversationStateRepository.PostExpandEffect.ForwardRequested
        )

        repo.conversationState.test {
            val conversationState = awaitItem()

            // Then
            assertEquals(
                conversationState.messagesState[messageId],
                Expanded(decryptedBody, InMemoryConversationStateRepository.PostExpandEffect.ForwardRequested)
            )
            assertEquals(1, conversationState.messagesState.size)
        }
    }

    @Test
    fun `Should clean any effects when consuming`() = runTest {
        // Given
        val repo = buildRepository()
        val messageId = MessageIdSample.Invoice
        val decryptedBody = DecryptedMessageBody(
            messageId = messageId,
            value = UUID.randomUUID().toString(),
            mimeType = MimeType.Html,
            userAddress = UserAddressSample.PrimaryAddress
        )

        // When
        repo.expandMessage(
            messageId, decryptedBody,
            InMemoryConversationStateRepository.PostExpandEffect.ReplyRequested
        )

        repo.conversationState.test {
            skipItems(1)

            repo.consumeEffect(messageId)

            val conversationState = awaitItem()

            // Then
            assertEquals(conversationState.messagesState[messageId], Expanded(decryptedBody, null))
            assertEquals(1, conversationState.messagesState.size)
        }
    }

    @Test
    fun `Should overwrite content when putting a message id`() = runTest {
        // Given
        val repo = buildRepository()
        val messageId = MessageIdSample.Invoice
        val decryptedBody = DecryptedMessageBody(
            messageId = messageId,
            value = UUID.randomUUID().toString(),
            mimeType = MimeType.Html,
            userAddress = UserAddressSample.PrimaryAddress
        )

        // When
        repo.expandMessage(messageId, decryptedBody, null)
        repo.conversationState.test {
            val conversationState = awaitItem()

            // Then
            assertEquals(conversationState.messagesState[messageId], Expanded(decryptedBody, null))
            assertEquals(1, conversationState.messagesState.size)

            repo.collapseMessage(messageId)
            val conversationState2 = awaitItem()
            assertEquals(conversationState2.messagesState[messageId], Collapsed)
            assertEquals(1, conversationState2.messagesState.size)
        }
    }

    @Test
    @Suppress("ForEachOnRange")
    fun `Should emit all the messages put in the cache`() = runTest {
        // Given
        val repo = buildRepository()
        val itemCount = Random().nextInt(100)

        // When
        (0 until itemCount).forEach {
            repo.collapseMessage(MessageId(it.toString()))
        }

        repo.conversationState.test {
            val conversationState = awaitItem()

            // Then
            assertEquals(itemCount, conversationState.messagesState.size)
        }
    }

    @Test
    fun `should emit the opposite filter value when switching the trashed messages filter`() = runTest {
        // Given
        val repository = buildRepository()

        repository.conversationState.test {
            val illegal = awaitItem().shouldHideMessagesBasedOnTrashFilter

            // When
            repository.switchTrashedMessagesFilter()

            // Then
            assertNotEquals(illegal, awaitItem().shouldHideMessagesBasedOnTrashFilter)
        }
    }

    private fun buildRepository() = InMemoryConversationStateRepositoryImpl()
}
