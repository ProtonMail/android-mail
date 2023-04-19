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
import ch.protonmail.android.maildetail.domain.model.DecryptedMessageBody
import ch.protonmail.android.maildetail.domain.repository.InMemoryConversationStateRepository.MessageState.Collapsed
import ch.protonmail.android.maildetail.domain.repository.InMemoryConversationStateRepository.MessageState.Expanded
import ch.protonmail.android.maildetail.domain.repository.InMemoryConversationStateRepository.MessageState.Expanding
import ch.protonmail.android.mailmessage.domain.entity.MessageId
import ch.protonmail.android.mailmessage.domain.entity.MimeType
import ch.protonmail.android.mailmessage.domain.sample.MessageIdSample
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals

class InMemoryConversationStateRepositoryImplTest {

    @Test
    fun `Should emit empty cache on start`() = runTest {
        // Given
        val repo = buildRepository()

        repo.conversationState.test {
            // Then
            assertEquals(emptyMap(), awaitItem())
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
            assertEquals(conversationState[messageId], Collapsed)
            assertEquals(1, conversationState.size)
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
            assertEquals(conversationState[messageId], Expanding)
            assertEquals(1, conversationState.size)
        }
    }

    @Test
    fun `Should emit expanded when putting a expanded message`() = runTest {
        // Given
        val repo = buildRepository()
        val messageId = MessageIdSample.Invoice
        val decryptedBody = DecryptedMessageBody(
            value = UUID.randomUUID().toString(),
            mimeType = MimeType.Html
        )

        // When
        repo.expandMessage(messageId, decryptedBody)

        repo.conversationState.test {
            val conversationState = awaitItem()

            // Then
            assertEquals(conversationState[messageId], Expanded(decryptedBody))
            assertEquals(1, conversationState.size)
        }
    }

    @Test
    fun `Should overwrite content when putting a message id`() = runTest {
        // Given
        val repo = buildRepository()
        val messageId = MessageIdSample.Invoice
        val decryptedBody = DecryptedMessageBody(
            value = UUID.randomUUID().toString(),
            mimeType = MimeType.Html
        )

        // When
        repo.expandMessage(messageId, decryptedBody)
        repo.conversationState.test {
            val conversationState = awaitItem()

            // Then
            assertEquals(conversationState[messageId], Expanded(decryptedBody))
            assertEquals(1, conversationState.size)

            repo.collapseMessage(messageId)
            val conversationState2 = awaitItem()
            assertEquals(conversationState2[messageId], Collapsed)
            assertEquals(1, conversationState2.size)
        }
    }

    @Test
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
            assertEquals(itemCount, conversationState.size)
        }
    }

    private fun buildRepository() = InMemoryConversationStateRepositoryImpl()
}
