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

package ch.protonmail.android.maildetail.domain.usecase

import app.cash.turbine.test
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.Action
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailmessage.domain.entity.MessageId
import ch.protonmail.android.mailmessage.domain.usecase.ObserveMessage
import ch.protonmail.android.testdata.message.MessageTestData
import ch.protonmail.android.testdata.user.UserIdTestData.userId
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals

internal class ObserveMessageDetailActionsTest {

    private val observeMessage = mockk<ObserveMessage> {
        every {
            this@mockk.invoke(userId, MessageId(MessageTestData.RAW_MESSAGE_ID))
        } returns flowOf(MessageTestData.message.right())
    }

    private val observeDetailActions = ObserveMessageDetailActions(
        observeMessage = observeMessage
    )

    @Test
    fun `returns default actions list for message`() = runTest {
        // Given
        val messageId = MessageId(MessageTestData.RAW_MESSAGE_ID)
        // When
        observeDetailActions.invoke(userId, messageId).test {
            // Then
            val expected = listOf(
                Action.Reply,
                Action.MarkUnread,
                Action.Trash,
                Action.Label
            )
            assertEquals(expected.right(), awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `returns reply all action when message has multiple recipients`() = runTest {
        // Given
        val messageId = MessageId(MessageTestData.RAW_MESSAGE_ID)
        val message = MessageTestData.multipleRecipientsMessage
        every { observeMessage.invoke(userId, messageId) } returns flowOf(message.right())
        // When
        observeDetailActions.invoke(userId, messageId).test {
            // Then
            val expected = listOf(
                Action.ReplyAll,
                Action.MarkUnread,
                Action.Trash,
                Action.Label
            )
            assertEquals(expected.right(), awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `returns delete and reply all actions when message is in spam and has multiple recipients`() =
        runTest {
            // Given
            val messageId = MessageId(MessageTestData.RAW_MESSAGE_ID)
            val message = MessageTestData.spamMessageWithMultipleRecipients
            every { observeMessage.invoke(userId, messageId) } returns flowOf(message.right())
            // When
            observeDetailActions.invoke(userId, messageId).test {
                // Then
                val expected = listOf(
                    Action.ReplyAll,
                    Action.MarkUnread,
                    Action.Delete,
                    Action.Label
                )
                assertEquals(expected.right(), awaitItem())
                awaitComplete()
            }
        }

    @Test
    fun `returns delete action when message is in trash`() = runTest {
        // Given
        val messageId = MessageId(MessageTestData.RAW_MESSAGE_ID)
        val message = MessageTestData.trashedMessage
        every { observeMessage.invoke(userId, messageId) } returns flowOf(message.right())
        // When
        observeDetailActions.invoke(userId, messageId).test {
            // Then
            val expected = listOf(
                Action.Reply,
                Action.MarkUnread,
                Action.Delete,
                Action.Label
            )
            assertEquals(expected.right(), awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `returns delete action when message with custom labels is in trash`() = runTest {
        // Given
        val messageId = MessageId(MessageTestData.RAW_MESSAGE_ID)
        val message = MessageTestData.trashedMessageWithCustomLabels
        every { observeMessage.invoke(userId, messageId) } returns flowOf(message.right())
        // When
        observeDetailActions.invoke(userId, messageId).test {
            // Then
            val expected = listOf(
                Action.Reply,
                Action.MarkUnread,
                Action.Delete,
                Action.Label
            )
            assertEquals(expected.right(), awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `returns delete action when message is in spam`() = runTest {
        // Given
        val messageId = MessageId(MessageTestData.RAW_MESSAGE_ID)
        val message = MessageTestData.spamMessage
        every { observeMessage.invoke(userId, messageId) } returns flowOf(message.right())
        // When
        observeDetailActions.invoke(userId, messageId).test {
            // Then
            val expected = listOf(
                Action.Reply,
                Action.MarkUnread,
                Action.Delete,
                Action.Label
            )
            assertEquals(expected.right(), awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `returns data error when failing to get message`() = runTest {
        // Given
        val messageId = MessageId(MessageTestData.RAW_MESSAGE_ID)
        every { observeMessage.invoke(userId, messageId) } returns flowOf(
            DataError.Local.NoDataCached.left()
        )
        // When
        observeDetailActions.invoke(userId, messageId).test {
            // Then
            assertEquals(DataError.Local.NoDataCached.left(), awaitItem())
            awaitComplete()
        }
    }

}
