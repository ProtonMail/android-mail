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

package ch.protonmail.android.mailmessage.data

import app.cash.turbine.test
import arrow.core.getOrHandle
import arrow.core.left
import arrow.core.nonEmptyListOf
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcommon.domain.sample.ConversationIdSample
import ch.protonmail.android.mailcommon.domain.sample.DataErrorSample
import ch.protonmail.android.mailcommon.domain.sample.LabelIdSample
import ch.protonmail.android.mailcommon.domain.sample.UserIdSample
import ch.protonmail.android.maillabel.domain.model.SystemLabelId
import ch.protonmail.android.mailmessage.data.local.MessageLocalDataSource
import ch.protonmail.android.mailmessage.data.remote.MessageRemoteDataSource
import ch.protonmail.android.mailmessage.data.repository.MessageRepositoryImpl
import ch.protonmail.android.mailmessage.domain.entity.Message
import ch.protonmail.android.mailmessage.domain.entity.MessageId
import ch.protonmail.android.mailmessage.domain.entity.MessageWithBody
import ch.protonmail.android.mailmessage.domain.sample.MessageIdSample
import ch.protonmail.android.mailmessage.domain.sample.MessageSample
import ch.protonmail.android.mailpagination.domain.model.PageFilter
import ch.protonmail.android.mailpagination.domain.model.PageKey
import ch.protonmail.android.testdata.message.MessageBodyTestData
import ch.protonmail.android.testdata.message.MessageTestData
import io.mockk.Called
import io.mockk.Ordering
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import me.proton.core.label.domain.entity.LabelId
import me.proton.core.test.kotlin.TestCoroutineScopeProvider
import me.proton.core.test.kotlin.TestDispatcherProvider
import kotlin.test.Test
import kotlin.test.assertEquals

class MessageRepositoryImplTest {

    private val userId = UserId("1")

    private val remoteDataSource = mockk<MessageRemoteDataSource>(relaxUnitFun = true) {
        coEvery { getMessages(userId = any(), pageKey = any()) } returns listOf(
            getMessage(id = "1", time = 1000),
            getMessage(id = "2", time = 2000),
            getMessage(id = "3", time = 3000),
            getMessage(id = "4", time = 4000)
        ).right()
        coEvery {
            getMessage(userId = any(), messageId = any())
        } returns MessageWithBody(MessageTestData.message, MessageBodyTestData.messageBody)
    }
    private val localDataSource = mockk<MessageLocalDataSource>(relaxUnitFun = true) {
        coEvery { addLabel(userId = any(), messageId = any(), labelId = any()) } returns MessageTestData.message.right()
        coEvery {
            removeLabel(userId = any(), messageId = any(), labelId = any())
        } returns MessageTestData.message.right()
        coEvery { observeMessage(userId = any(), messageId = any()) } returns flowOf(MessageTestData.message)
        coEvery { getMessages(userId = any(), pageKey = any()) } returns emptyList()
        coEvery { isLocalPageValid(userId = any(), pageKey = any(), items = any()) } returns false
        coEvery {
            observeMessageWithBody(userId = any(), messageId = any())
        } returns flowOf(MessageWithBody(MessageTestData.message, MessageBodyTestData.messageBody))
    }

    private val messageRepository = MessageRepositoryImpl(
        remoteDataSource = remoteDataSource,
        localDataSource = localDataSource,
        coroutineScopeProvider = TestCoroutineScopeProvider(TestDispatcherProvider(UnconfinedTestDispatcher()))
    )

    @Test
    fun `return remote if local page is invalid`() = runTest {
        // Given
        val pageKey = PageKey()
        val localMessages = listOf(
            getMessage(id = "1", time = 1000)
        )
        val remoteMessages = listOf(
            getMessage(id = "1", time = 1000),
            getMessage(id = "2", time = 2000),
            getMessage(id = "3", time = 3000)
        )
        coEvery { localDataSource.getMessages(any(), any()) } returns localMessages
        coEvery { localDataSource.isLocalPageValid(any(), any(), any()) } returns false
        coEvery { localDataSource.getClippedPageKey(any(), any()) } returns pageKey
        coEvery { remoteDataSource.getMessages(any(), any()) } returns remoteMessages.right()

        // When
        val result = messageRepository.getMessages(userId, pageKey)
            .getOrHandle(::error)

        // Then
        assertEquals(3, result.size)
        coVerify(exactly = 1) { localDataSource.isLocalPageValid(userId, pageKey, localMessages) }
        coVerify(exactly = 1) { remoteDataSource.getMessages(userId, pageKey) }
        coVerify(exactly = 1) { localDataSource.upsertMessages(userId, pageKey, remoteMessages) }
    }

    @Test
    fun `return cached data if remote fails`() = runTest {
        // Given
        val pageKey = PageKey()
        val localMessages = listOf(
            getMessage(id = "1", time = 1000),
            getMessage(id = "2", time = 2000)
        )
        val error = DataErrorSample.Unreachable.left()
        coEvery { localDataSource.getMessages(any(), any()) } returns localMessages
        coEvery { localDataSource.isLocalPageValid(any(), any(), any()) } returns false
        coEvery { localDataSource.getClippedPageKey(any(), any()) } returns pageKey
        coEvery { remoteDataSource.getMessages(any(), any()) } returns error

        // When
        val result = messageRepository.getMessages(userId, pageKey)

        // Then
        assertEquals(localMessages.right(), result)
        coVerify(exactly = 1) { localDataSource.isLocalPageValid(userId, pageKey, localMessages) }
        coVerify(exactly = 1) { remoteDataSource.getMessages(userId, pageKey) }
    }

    @Test
    fun `return local if valid`() = runTest {
        // Given
        val pageKey = PageKey()
        val localMessages = listOf(
            getMessage(id = "1", time = 1000),
            getMessage(id = "2", time = 2000)
        )
        val remoteMessages = listOf(
            getMessage(id = "1", time = 1000),
            getMessage(id = "2", time = 2000),
            getMessage(id = "3", time = 3000)
        )
        coEvery { localDataSource.getMessages(any(), any()) } returns localMessages
        coEvery { localDataSource.isLocalPageValid(any(), any(), any()) } returns true
        coEvery { remoteDataSource.getMessages(any(), any()) } returns remoteMessages.right()

        // When
        val messages = messageRepository.getMessages(userId, pageKey)
            .getOrHandle(::error)

        // Then
        assertEquals(2, messages.size)
        coVerify(exactly = 1) { localDataSource.isLocalPageValid(userId, pageKey, localMessages) }
        coVerify(exactly = 0) { remoteDataSource.getMessages(any(), any()) }
    }

    @Test
    fun `clip pageKey before calling remote`() = runTest {
        // Given
        val pageKey = PageKey()
        val clippedPageKey = PageKey(filter = PageFilter(minTime = 0))
        coEvery { localDataSource.getMessages(any(), any()) } returns emptyList()
        coEvery { localDataSource.isLocalPageValid(any(), any(), any()) } returns false
        coEvery { localDataSource.getClippedPageKey(any(), any()) } returns clippedPageKey
        coEvery { remoteDataSource.getMessages(any(), any()) } returns emptyList<Message>().right()
        // When
        val messages = messageRepository.getMessages(userId, pageKey)
            .getOrHandle(::error)

        // Then
        assertEquals(0, messages.size)
        coVerify(exactly = 1) { localDataSource.isLocalPageValid(any(), any(), any()) }
        coVerify(ordering = Ordering.ORDERED) {
            localDataSource.getClippedPageKey(userId, pageKey)
            remoteDataSource.getMessages(userId, clippedPageKey)
        }
    }

    @Test
    fun `observe cached message emits message when existing in cache`() = runTest {
        // Given
        val messageId = MessageId("messageId")
        val message = getMessage(userId, "1")
        every { localDataSource.observeMessage(userId, messageId) } returns flowOf(message)
        // When
        messageRepository.observeCachedMessage(userId, messageId).test {
            // Then
            assertEquals(message.right(), awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `observe cached message emits no data cached error when message does not exist in cache`() = runTest {
        // Given
        val messageId = MessageId("messageId")
        every { localDataSource.observeMessage(userId, messageId) } returns flowOf(null)
        // When
        messageRepository.observeCachedMessage(userId, messageId).test {
            // Then
            assertEquals(DataError.Local.NoDataCached.left(), awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `observe cached messages for a conversation id calls the local source with correct parameters`() = runTest {
        // given
        val userId = UserIdSample.Primary
        val conversationId = ConversationIdSample.WeatherForecast
        val messages = nonEmptyListOf(
            MessageSample.AugWeatherForecast,
            MessageSample.SepWeatherForecast
        )
        every { localDataSource.observeMessages(userId, conversationId) } returns flowOf(messages)

        // when
        messageRepository.observeCachedMessages(userId, conversationId).test {

            // then
            assertEquals(messages.right(), awaitItem())
            verify { localDataSource.observeMessages(userId, conversationId) }
            awaitComplete()
        }
    }

    @Test
    fun `observe cached messages for a conversation emits error when the list is empty`() = runTest {
        // Given
        val userId = UserIdSample.Primary
        val conversationId = ConversationIdSample.WeatherForecast
        every { localDataSource.observeMessages(userId, conversationId) } returns flowOf(emptyList())
        // When
        messageRepository.observeCachedMessages(userId, conversationId).test {
            // Then
            assertEquals(DataError.Local.NoDataCached.left(), awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `observe message with body emits cached message with body when it exists`() = runTest {
        // Given
        val messageId = MessageIdSample.AugWeatherForecast
        val expected = MessageWithBody(MessageTestData.message, MessageBodyTestData.messageBody).right()

        // When
        messageRepository.observeMessageWithBody(userId, messageId).test {

            // Then
            assertEquals(expected, awaitItem())
        }
    }

    @Test
    fun `observe message with body fetches remote message with body and saves it locally when cached does not exist`() =
        runTest {
            // Given
            val messageId = MessageIdSample.AugWeatherForecast
            val expected = MessageWithBody(MessageTestData.message, MessageBodyTestData.messageBody)
            coEvery { localDataSource.observeMessageWithBody(userId, messageId) } returns flowOf(null)

            // When
            messageRepository.observeMessageWithBody(userId, messageId).test {

                // Then
                coVerify {
                    remoteDataSource.getMessage(userId, messageId)
                    localDataSource.upsertMessageWithBody(userId, expected)
                }
            }
        }

    @Test
    fun `observe message with body returns an error when remote call fails`() = runTest {
        // Given
        val messageId = MessageIdSample.AugWeatherForecast
        coEvery { localDataSource.observeMessageWithBody(userId, messageId) } returns flowOf(null)
        coEvery { remoteDataSource.getMessage(userId, messageId) } throws Exception()

        // When
        messageRepository.observeMessageWithBody(userId, messageId).test {

            // Then
            awaitError()
            coVerify(exactly = 1) {
                localDataSource.observeMessageWithBody(userId, messageId)
                remoteDataSource.getMessage(userId, messageId)
            }
            coVerify(exactly = 0) {
                localDataSource.upsertMessageWithBody(userId, any())
            }
        }
    }

    @Test
    fun `add label emits message with label when adding label was successful`() = runTest {
        // Given
        val messageId = MessageId(MessageTestData.RAW_MESSAGE_ID)
        val labelId = LabelId("10")
        coEvery {
            localDataSource.addLabel(
                userId = userId,
                messageId = messageId,
                labelId = labelId
            )
        } returns MessageTestData.starredMessage.right()
        // When
        val actual = messageRepository.addLabel(userId, messageId, labelId)
        // Then
        val starredMessage = MessageTestData.starredMessage
        coVerify { localDataSource.addLabel(userId, messageId, labelId) }
        assertEquals(starredMessage.right(), actual)
    }

    @Test
    fun `add label updates remote data source`() = runTest {
        // Given
        val messageId = MessageId(MessageTestData.RAW_MESSAGE_ID)
        val labelId = LabelId("10")
        // When
        messageRepository.addLabel(userId, messageId, labelId)
        // Then
        coVerify { remoteDataSource.addLabel(userId, messageId, labelId) }
    }

    @Test
    fun `add label emits error when local data source fails`() = runTest {
        // Given
        val messageId = MessageId("messageId")
        val labelId = LabelId("42")
        coEvery {
            localDataSource.addLabel(
                userId,
                messageId,
                labelId
            )
        } returns DataError.Local.NoDataCached.left()
        // When
        val actual = messageRepository.addLabel(userId, messageId, labelId)
        // Then
        assertEquals(DataError.Local.NoDataCached.left(), actual)
    }

    @Test
    fun `remove label returns message without label when successful`() = runTest {
        // Given
        val messageId = MessageId(MessageTestData.RAW_MESSAGE_ID)
        val labelId = LabelId("10")
        coEvery {
            localDataSource.removeLabel(userId, messageId, labelId)
        } returns MessageTestData.message.right()
        // When
        val actual = messageRepository.removeLabel(userId, messageId, LabelId("10"))
        // Then
        val unstarredMessage = MessageTestData.message
        assertEquals(unstarredMessage.right(), actual)
    }

    @Test
    fun `remove label updates remote data source`() = runTest {
        // Given
        val messageId = MessageId(MessageTestData.RAW_MESSAGE_ID)
        val labelId = LabelId("10")
        // When
        messageRepository.removeLabel(userId, messageId, labelId)
        // Then
        coVerify { remoteDataSource.removeLabel(userId, messageId, labelId) }
    }

    @Test
    fun `remove label emits error when local data source fails`() = runTest {
        // Given
        val messageId = MessageId("messageId")
        coEvery {
            localDataSource.removeLabel(userId, messageId, LabelId("42"))
        } returns DataError.Local.NoDataCached.left()
        // When
        val actual = messageRepository.removeLabel(userId, messageId, LabelId("42"))
        // Then
        assertEquals(DataError.Local.NoDataCached.left(), actual)
    }

    @Test
    fun `move to trash add the trash label to the message`() = runTest {
        // given
        val messageId = MessageIdSample.EmptyDraft
        val message = MessageSample.EmptyDraft.copy(
            labelIds = listOf(LabelIdSample.AllDraft)
        )
        val trashedMessage = MessageSample.EmptyDraft.copy(
            labelIds = listOf(LabelIdSample.AllDraft, SystemLabelId.Trash.labelId)
        )
        every { localDataSource.observeMessage(userId, messageId) } returns flowOf(message)
        coEvery { localDataSource.addLabel(userId, messageId, SystemLabelId.Trash.labelId) } returns
            trashedMessage.right()

        // when
        val result =
            messageRepository.moveTo(userId, messageId, LabelIdSample.AllDraft, SystemLabelId.Trash.labelId)

        // then
        assertEquals(trashedMessage.right(), result)
    }

    @Test
    fun `move to trash removes all the labels, except AllMail, AllDraft and AllSent`() = runTest {
        // given
        val messageId = MessageIdSample.EmptyDraft
        val message = MessageSample.EmptyDraft.copy(
            labelIds = listOf(
                SystemLabelId.AllDrafts.labelId,
                SystemLabelId.AllMail.labelId,
                SystemLabelId.AllSent.labelId,
                SystemLabelId.Inbox.labelId,
                LabelIdSample.Document
            )
        )
        val messageWithoutLabels = MessageSample.EmptyDraft.copy(
            labelIds = listOf(
                SystemLabelId.AllDrafts.labelId,
                SystemLabelId.AllMail.labelId,
                SystemLabelId.AllSent.labelId
            )
        )
        val trashedMessage = messageWithoutLabels.copy(
            labelIds = messageWithoutLabels.labelIds + SystemLabelId.Trash.labelId
        )
        val messageFlow = MutableStateFlow(message)
        every { localDataSource.observeMessage(userId, messageId) } returns messageFlow
        coEvery { localDataSource.upsertMessage(messageWithoutLabels) } coAnswers {
            messageFlow.emit(messageWithoutLabels)
        }
        coEvery { localDataSource.addLabel(userId, messageId, SystemLabelId.Trash.labelId) } returns
            trashedMessage.right()

        // when
        val result = messageRepository.moveTo(
            userId,
            messageId,
            SystemLabelId.Inbox.labelId,
            SystemLabelId.Trash.labelId
        )

        // then
        assertEquals(trashedMessage.right(), result)
    }

    @Test
    fun `move removes previous exclusive label and adds new label`() = runTest {
        // Given
        val message = MessageTestData.message.copy(
            labelIds = listOf(
                SystemLabelId.Inbox.labelId,
                SystemLabelId.AllMail.labelId,
                SystemLabelId.Starred.labelId
            )
        )
        coEvery { localDataSource.observeMessage(userId, MessageId(message.id)) } returns flowOf(message)
        val destinationLabel = SystemLabelId.Spam.labelId

        val updatedMessage = message.copy(
            labelIds = listOf(
                SystemLabelId.AllMail.labelId,
                SystemLabelId.Starred.labelId,
                destinationLabel
            )
        )

        // When
        val result =
            messageRepository.moveTo(
                userId,
                MessageId(message.id),
                SystemLabelId.Inbox.labelId,
                destinationLabel
            )

        // Then
        coVerify { localDataSource.upsertMessage(updatedMessage) }
        verify { remoteDataSource.addLabel(userId, MessageId(message.id), destinationLabel) }
        assertEquals(updatedMessage.right(), result)
    }

    @Test
    fun `move message without exclusive labels adds new label`() = runTest {
        // Given
        val message = MessageTestData.message.copy(labelIds = listOf(SystemLabelId.AllMail.labelId))
        coEvery { localDataSource.observeMessage(userId, MessageId(message.id)) } returns flowOf(message)
        val destinationLabel = SystemLabelId.Spam.labelId

        val updatedMessage = message.copy(
            labelIds = listOf(
                SystemLabelId.AllMail.labelId,
                destinationLabel
            )
        )

        // When
        val result = messageRepository.moveTo(userId, MessageId(message.id), null, destinationLabel)

        // Then
        coVerify { localDataSource.upsertMessage(updatedMessage) }
        verify { remoteDataSource.addLabel(userId, MessageId(message.id), destinationLabel) }
        assertEquals(updatedMessage.right(), result)
    }

    @Test
    fun `move message to trash without exclusive labels`() = runTest {
        // Given
        val message = MessageTestData.message.copy(
            labelIds = listOf(
                SystemLabelId.AllMail.labelId
            )
        )
        coEvery { localDataSource.observeMessage(userId, MessageId(message.id)) } returns flowOf(message)
        val destinationLabel = SystemLabelId.Trash.labelId

        val updatedMessage = message.copy(labelIds = listOf(destinationLabel))
        coEvery {
            localDataSource.addLabel(userId, MessageId(message.id), destinationLabel)
        } returns updatedMessage.right()

        // When
        val result = messageRepository.moveTo(userId, MessageId(message.id), null, destinationLabel)

        // Then
        coVerify { localDataSource.upsertMessage(message) }
        coVerify { localDataSource.addLabel(userId, MessageId(message.id), destinationLabel) }
        verify { remoteDataSource.addLabel(userId, MessageId(message.id), destinationLabel) }
        assertEquals(updatedMessage.right(), result)
    }

    @Test
    fun `move emits error when local data source fails`() = runTest {
        // Given
        coEvery { localDataSource.observeMessage(userId, any()) } returns flowOf(null)

        // When
        val result = messageRepository.moveTo(
            userId,
            MessageId(MessageTestData.RAW_MESSAGE_ID),
            SystemLabelId.Inbox.labelId,
            LabelId("42")
        )

        // Then
        assertEquals(DataError.Local.NoDataCached.left(), result)
    }

    @Test
    fun `move to calls move to trash when destinationLabel is trash`() = runTest {
        // Given
        val message = MessageTestData.message.copy(
            labelIds = listOf(
                SystemLabelId.Inbox.labelId,
                SystemLabelId.AllMail.labelId,
                SystemLabelId.Starred.labelId
            )
        )
        val trashedMessage = message.copy(
            labelIds = listOf(
                LabelIdSample.AllDraft,
                SystemLabelId.Trash.labelId
            )
        )

        val messageId = MessageId(message.id)
        every { localDataSource.observeMessage(userId, messageId) } returns flowOf(message)
        coEvery { localDataSource.addLabel(userId, messageId, SystemLabelId.Trash.labelId) } returns
            trashedMessage.right()

        // When
        val actual = messageRepository.moveTo(
            userId,
            messageId,
            SystemLabelId.Inbox.labelId,
            SystemLabelId.Trash.labelId
        )

        // Then
        assertEquals(trashedMessage.right(), actual)
    }

    @Test
    fun `mark unread returns error when local data source fails`() = runTest {
        // given
        val messageId = MessageIdSample.Invoice
        val error = DataErrorSample.NoCache.left()
        coEvery { localDataSource.markUnread(userId, messageId) } returns error

        // When
        val result = messageRepository.markUnread(userId, messageId)

        // Then
        assertEquals(error, result)
        verify { remoteDataSource wasNot Called }
    }

    @Test
    fun `mark unread returns updated message when local data source succeed`() = runTest {
        // given
        val messageId = MessageIdSample.Invoice
        val message = MessageSample.Invoice.right()
        coEvery { localDataSource.markUnread(userId, messageId) } returns message

        // When
        val result = messageRepository.markUnread(userId, messageId)

        // Then
        assertEquals(message, result)
        verify { remoteDataSource.markUnread(userId, messageId) }
    }

    @Test
    fun `relabel adds missing and removes existing labels from a message`() = runTest {
        // Given
        val labelToStay = LabelId("CustomLabel1")
        val labelToBeAdded = LabelId("CustomLabel3")
        val labelToBeRemoved = LabelId("CustomLabel2")
        val message = MessageTestData.message.copy(labelIds = listOf(labelToStay, labelToBeRemoved))
        every { localDataSource.observeMessage(userId, MessageId(message.id)) } returns flowOf(message)

        // When
        val actual = messageRepository.relabel(userId, MessageId(message.id), listOf(labelToBeRemoved, labelToBeAdded))

        // Then
        val expectedMessage = message.copy(labelIds = listOf(labelToStay, labelToBeAdded))
        assertEquals(expectedMessage.right(), actual)
    }

    @Test
    fun `relabel emits error when local data source fails`() = runTest {
        // Given
        val labelToStay = LabelId("CustomLabel1")
        val labelToBeAdded = LabelId("CustomLabel3")
        val labelToBeRemoved = LabelId("CustomLabel2")
        val message = MessageTestData.message.copy(labelIds = listOf(labelToStay, labelToBeRemoved))

        // When
        every { localDataSource.observeMessage(userId, MessageId(message.id)) } returns flowOf(null)
        val actual = messageRepository.relabel(userId, MessageId(message.id), listOf(labelToBeRemoved, labelToBeAdded))

        // Then
        assertEquals(DataError.Local.NoDataCached.left(), actual)
    }

    @Test
    fun `verify relabel calls local data source`() = runTest {
        // Given
        val labelToStay = LabelId("CustomLabel1")
        val labelToBeAdded = LabelId("CustomLabel3")
        val labelToBeRemoved = LabelId("CustomLabel2")
        val message = MessageTestData.message.copy(labelIds = listOf(labelToStay, labelToBeRemoved))
        every { localDataSource.observeMessage(userId, MessageId(message.id)) } returns flowOf(message)

        // When
        messageRepository.relabel(userId, MessageId(message.id), listOf(labelToBeRemoved, labelToBeAdded))

        // Then
        val expectedMessage = message.copy(labelIds = listOf(labelToStay, labelToBeAdded))
        coVerify { localDataSource.upsertMessage(expectedMessage) }
    }

    @Test(expected = IllegalArgumentException::class)
    fun `relabel throws exception when list is larger than 100 items`() = runTest {
        // Given
        val labelList = mutableListOf<LabelId>()
        for (i in 1..101) {
            labelList.add(LabelId("CustomLabel$i"))
        }
        val message = MessageTestData.message.copy(
            labelIds = listOf(LabelId("CustomLabel1"), LabelId("CustomLabel2"))
        )

        // When
        every { localDataSource.observeMessage(userId, MessageId(message.id)) } returns flowOf(message)
        messageRepository.relabel(userId, MessageId(message.id), labelList)
    }
}
