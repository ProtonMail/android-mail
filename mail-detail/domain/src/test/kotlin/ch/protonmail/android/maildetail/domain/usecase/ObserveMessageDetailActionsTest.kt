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
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailmessage.domain.usecase.ObserveMessage
import ch.protonmail.android.mailsettings.domain.usecase.ObserveMailMessageToolbarSettings
import ch.protonmail.android.testdata.message.MessageTestData
import ch.protonmail.android.testdata.user.UserIdTestData.userId
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.BeforeTest
import kotlin.test.assertEquals

internal class ObserveMessageDetailActionsTest {

    private val observeMessage = mockk<ObserveMessage> {
        every {
            this@mockk.invoke(userId, MessageId(MessageTestData.RAW_MESSAGE_ID))
        } returns flowOf(MessageTestData.message.right())
    }

    private val observeToolbarActions = mockk<ObserveMailMessageToolbarSettings> {
        every {
            this@mockk.invoke(userId, false)
        } returns flowOf(null)
    }

    private val observeDetailActions by lazy {
        ObserveMessageDetailActions(
            observeMessage = observeMessage,
            observeToolbarActions = observeToolbarActions
        )
    }

    @BeforeTest
    fun setUp() {
        MockKAnnotations.init(this)
    }

    @Test
    fun `returns default actions list for message`() = runTest {
        // Given
        val messageId = MessageId(MessageTestData.RAW_MESSAGE_ID)
        // When
        observeDetailActions.invoke(userId, messageId).test {
            // Then
            val expected = listOf(
                Action.MarkUnread,
                Action.Trash,
                Action.Label,
                Action.Move,
                Action.More
            )
            assertEquals(expected.right(), awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `returns preferences actions list for message`() = runTest {
        // Given
        val messageId = MessageId(MessageTestData.RAW_MESSAGE_ID)
        every { observeToolbarActions.invoke(userId, false) } returns flowOf(
            listOf(
                Action.Label,
                Action.ReportPhishing,
                Action.Star
            )
        )
        // When
        observeDetailActions.invoke(userId, messageId).test {
            // Then
            val expected = listOf(
                Action.Label,
                Action.ReportPhishing,
                Action.Star,
                Action.More
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
                Action.MarkUnread,
                Action.Delete,
                Action.Label,
                Action.Move,
                Action.More
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
                Action.MarkUnread,
                Action.Delete,
                Action.Label,
                Action.Move,
                Action.More
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
                Action.MarkUnread,
                Action.Delete,
                Action.Label,
                Action.Move,
                Action.More
            )
            assertEquals(expected.right(), awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `returns move to inbox and delete action when the message is in trash or spam`() = runTest {
        // Given
        val messageId = MessageId(MessageTestData.RAW_MESSAGE_ID)
        val message = MessageTestData.trashedMessageWithCustomLabels
        every { observeMessage.invoke(userId, messageId) } returns flowOf(message.right())
        every { observeToolbarActions.invoke(userId, false) } returns flowOf(
            listOf(
                Action.Spam,
                Action.Trash
            )
        )
        // When
        observeDetailActions.invoke(userId, messageId).test {
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
    fun `returns trash action when preference returns delete and the messages is not in trash nor spam`() = runTest {
        // Given
        val messageId = MessageId(MessageTestData.RAW_MESSAGE_ID)
        val message = MessageTestData.message
        every { observeMessage.invoke(userId, messageId) } returns flowOf(message.right())
        every { observeToolbarActions.invoke(userId, false) } returns flowOf(
            listOf(
                Action.Spam,
                Action.Delete
            )
        )
        // When
        observeDetailActions.invoke(userId, messageId).test {
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
    fun `returns reply all action when when message has multiple recipients`() = runTest {
        // Given
        val messageId = MessageId(MessageTestData.RAW_MESSAGE_ID)
        val message = MessageTestData.multipleRecipientsMessage
        every { observeMessage.invoke(userId, messageId) } returns flowOf(message.right())
        every { observeToolbarActions.invoke(userId, false) } returns flowOf(
            listOf(
                Action.Print,
                Action.Reply
            )
        )
        // When
        observeDetailActions.invoke(userId, messageId).test {
            // Then
            val expected = listOf(
                Action.Print,
                Action.ReplyAll,
                Action.More
            )
            assertEquals(expected.right(), awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `returns unstar action when preference returns star and the message is starred`() = runTest {
        // Given
        val messageId = MessageId(MessageTestData.RAW_MESSAGE_ID)
        val message = MessageTestData.starredMessage
        every { observeMessage.invoke(userId, messageId) } returns flowOf(message.right())
        every { observeToolbarActions.invoke(userId, false) } returns flowOf(
            listOf(
                Action.Star,
                Action.Delete
            )
        )
        // When
        observeDetailActions.invoke(userId, messageId).test {
            // Then
            val expected = listOf(
                Action.Unstar,
                Action.Trash,
                Action.More
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
