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

import java.io.IOException
import java.util.UUID
import app.cash.turbine.test
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.nonEmptyListOf
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcommon.domain.model.NetworkError
import ch.protonmail.android.mailcommon.domain.sample.ConversationIdSample
import ch.protonmail.android.mailcommon.domain.sample.DataErrorSample
import ch.protonmail.android.mailcommon.domain.sample.LabelIdSample
import ch.protonmail.android.mailcommon.domain.sample.UserIdSample
import ch.protonmail.android.maillabel.domain.model.SystemLabelId
import ch.protonmail.android.mailmessage.data.local.MessageBodyFileWriteException
import ch.protonmail.android.mailmessage.data.local.MessageLocalDataSource
import ch.protonmail.android.mailmessage.data.remote.MessageRemoteDataSource
import ch.protonmail.android.mailmessage.data.repository.MessageRepositoryImpl
import ch.protonmail.android.mailmessage.data.usecase.ExcludeDraftMessagesAlreadyInOutbox
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailmessage.domain.model.MessageWithBody
import ch.protonmail.android.mailmessage.domain.model.RefreshedMessageWithBody
import ch.protonmail.android.mailmessage.domain.sample.MessageAttachmentSample
import ch.protonmail.android.mailmessage.domain.sample.MessageIdSample
import ch.protonmail.android.mailmessage.domain.sample.MessageSample
import ch.protonmail.android.mailmessage.domain.sample.MessageWithBodySample
import ch.protonmail.android.mailpagination.domain.model.PageFilter
import ch.protonmail.android.mailpagination.domain.model.PageKey
import ch.protonmail.android.testdata.message.DecryptedMessageBodyTestData
import ch.protonmail.android.testdata.message.MessageBodyTestData
import ch.protonmail.android.testdata.message.MessageTestData
import ch.protonmail.android.testdata.message.MessageTestData.unmodifiableLabels
import io.mockk.Called
import io.mockk.Ordering
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import me.proton.core.label.domain.entity.LabelId
import me.proton.core.test.kotlin.TestCoroutineScopeProvider
import me.proton.core.test.kotlin.TestDispatcherProvider
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MessageRepositoryImplTest {

    private val userId = UserId("1")
    private val remoteMessageList = listOf(
        getMessage(id = "1", time = 1000),
        getMessage(id = "2", time = 2000),
        getMessage(id = "3", time = 3000),
        getMessage(id = "4", time = 4000)
    )
    private val excludeDraftMessagesAlreadyInOutbox = mockk<ExcludeDraftMessagesAlreadyInOutbox> {
        // Mock the behavior to return the same list
        coEvery { invoke(userId = userId, entities = remoteMessageList) } returns remoteMessageList
    }
    private val remoteDataSource = mockk<MessageRemoteDataSource>(relaxUnitFun = true) {
        coEvery { getMessages(userId = any(), pageKey = any()) } returns remoteMessageList.right()

        coEvery {
            getMessageOrThrow(userId = any(), messageId = any())
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
        every { observeMessageAttachments(any(), any()) } returns flowOf(listOf(MessageAttachmentSample.invoice))
        coEvery { upsertMessages(any()) } returns Unit.right()
        coEvery { upsertMessages(any(), any(), any()) } returns Unit.right()
    }

    private val messageRepository = MessageRepositoryImpl(
        remoteDataSource = remoteDataSource,
        localDataSource = localDataSource,
        excludeDraftMessagesAlreadyInOutbox = excludeDraftMessagesAlreadyInOutbox,
        coroutineScopeProvider = TestCoroutineScopeProvider(TestDispatcherProvider(UnconfinedTestDispatcher()))
    )

    @Test
    fun `load messages returns local data`() = runTest {
        // Given
        val pageKey = PageKey()
        val expected = listOf(
            getMessage(id = "1", time = 1000),
            getMessage(id = "2", time = 2000)
        )
        coEvery { localDataSource.getMessages(userId, pageKey) } returns expected

        // When
        val messages = messageRepository.getLocalMessages(userId, pageKey)

        // Then
        assertEquals(expected, messages)
        coVerify(exactly = 1) { localDataSource.getMessages(userId, pageKey) }
    }

    @Test
    fun `get local messages returns local data`() = runTest {
        // Given
        val messageIds = listOf(MessageIdSample.AugWeatherForecast, MessageIdSample.Invoice)
        val expected = listOf(MessageSample.AugWeatherForecast, MessageSample.Invoice)
        coEvery { localDataSource.observeMessages(userId, messageIds) } returns flowOf(expected)

        // When
        val actual = messageRepository.getLocalMessages(userId, messageIds)

        // Then
        assertEquals(expected, actual)
        coVerify(exactly = 1) { localDataSource.observeMessages(userId, messageIds) }
    }

    @Test
    fun `when clip pageKey returns a valid clipped key then remote is called with clipped key`() = runTest {
        // Given
        val pageKey = PageKey()
        val clippedPageKey = PageKey(filter = PageFilter(minTime = 0))
        val expected = listOf(getMessage(id = "1", time = 1000))
        coEvery { excludeDraftMessagesAlreadyInOutbox.invoke(any(), any()) } returns expected
        coEvery { localDataSource.getClippedPageKey(userId, pageKey) } returns clippedPageKey
        coEvery { remoteDataSource.getMessages(userId, clippedPageKey) } returns expected.right()

        // When
        val actual = messageRepository.getRemoteMessages(userId, pageKey).getOrElse(::error)

        // Then
        assertEquals(expected, actual)
        coVerify(ordering = Ordering.ORDERED) {
            localDataSource.getClippedPageKey(userId, pageKey)
            remoteDataSource.getMessages(userId, clippedPageKey)
        }
    }

    @Test
    fun `when clip pageKey returns null then empty list is returned`() = runTest {
        // Given
        val pageKey = PageKey()
        val messages = listOf(getMessage(id = "1", time = 1000))
        coEvery { localDataSource.getClippedPageKey(userId, pageKey) } returns null
        coEvery { remoteDataSource.getMessages(userId, any()) } returns messages.right()

        // When
        val actual = messageRepository.getRemoteMessages(userId, pageKey).getOrElse(::error)

        // Then
        assertEquals(emptyList(), actual)
        coVerify { localDataSource.getClippedPageKey(userId, pageKey) }
        coVerify { remoteDataSource wasNot Called }
    }

    @Test
    fun `verify messages are inserted when remote call was successful`() = runTest {
        // Given
        val pageKey = PageKey()
        val expected = listOf(
            getMessage(id = "1", time = 1000),
            getMessage(id = "2", time = 2000),
            getMessage(id = "3", time = 3000)
        )
        coEvery { excludeDraftMessagesAlreadyInOutbox.invoke(any(), any()) } returns expected
        coEvery { localDataSource.getClippedPageKey(userId, pageKey) } returns pageKey
        coEvery { remoteDataSource.getMessages(any(), any()) } returns expected.right()

        // When
        val actual = messageRepository.getRemoteMessages(userId, pageKey).getOrElse(::error)

        // Then
        assertEquals(expected, actual)
        coVerify(exactly = 1) { remoteDataSource.getMessages(userId, pageKey) }
        coVerify(exactly = 1) { localDataSource.upsertMessages(userId, pageKey, expected) }
    }

    @Test
    fun `verify error is returned when remote call fails with Unreachable Error`() = runTest {
        // Given
        val pageKey = PageKey()
        val expected = DataError.Remote.Http(NetworkError.Unreachable).left()
        coEvery { localDataSource.getClippedPageKey(userId, pageKey) } returns pageKey
        coEvery { remoteDataSource.getMessages(any(), any()) } returns expected

        // When
        val actual = messageRepository.getRemoteMessages(userId, pageKey)

        // Then
        assertEquals(expected, actual)
        coVerify(exactly = 1) { remoteDataSource.getMessages(userId, pageKey) }
        coVerify(exactly = 0) { localDataSource.upsertMessages(any(), any(), any()) }
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
    fun `observe cached messages emits messages when existing in cache`() = runTest {
        // Given
        val messageIds = listOf(MessageId("messageId"), MessageId("messageId2"))
        val message = getMessage(userId, "1")
        val message2 = getMessage(userId, "2")
        val expected = listOf(message, message2)
        every { localDataSource.observeMessages(userId, messageIds) } returns flowOf(expected)
        // When
        messageRepository.observeCachedMessages(userId, messageIds).test {
            // Then
            assertEquals(expected.right(), awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `observe cached messages emits no data cached error when messages do not exist in cache`() = runTest {
        // Given
        val messageIds = listOf(MessageId("messageId"), MessageId("messageId2"))
        every { localDataSource.observeMessages(userId, messageIds) } returns flowOf(emptyList())
        // When
        messageRepository.observeCachedMessages(userId, messageIds).test {
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
    fun `get cached message for conversation ids calls the local source with correct parameters`() = runTest {
        // Given
        val userId = UserIdSample.Primary
        val conversationIds = listOf(ConversationIdSample.WeatherForecast)
        val messages = nonEmptyListOf(
            MessageSample.AugWeatherForecast,
            MessageSample.SepWeatherForecast
        )
        coEvery { localDataSource.observeMessagesForConversation(userId, conversationIds) } returns flowOf(messages)

        // When
        messageRepository.observeCachedMessagesForConversations(userId, conversationIds).test {
            // Then
            assertEquals(messages, awaitItem())
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
                    remoteDataSource.getMessageOrThrow(userId, messageId)
                    localDataSource.upsertMessageWithBody(userId, expected)
                }
            }
        }

    @Test
    fun `observe message with body returns an error when remote call fails with a generic exception`() = runTest {
        // Given
        val messageId = MessageIdSample.AugWeatherForecast
        coEvery { localDataSource.observeMessageWithBody(userId, messageId) } returns flowOf(null)
        coEvery { remoteDataSource.getMessageOrThrow(userId, messageId) } throws Exception()

        // When
        messageRepository.observeMessageWithBody(userId, messageId).test {
            // Then
            assertEquals(DataError.Remote.Unknown.left(), awaitItem())
            coVerify(exactly = 1) {
                localDataSource.observeMessageWithBody(userId, messageId)
                remoteDataSource.getMessageOrThrow(userId, messageId)
            }
            coVerify(exactly = 0) {
                localDataSource.upsertMessageWithBody(userId, any())
            }
        }
    }

    @Test
    fun `observe message attachments calls the local data source and returns the result`() = runTest {
        // Given
        val messageId = MessageIdSample.Invoice
        val expected = listOf(MessageAttachmentSample.invoice)

        // When
        messageRepository.observeMessageAttachments(userId, messageId).test {

            // Then
            assertEquals(expected, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `add label emits message with label when adding label was successful`() = runTest {
        // Given
        val messageId = MessageId(MessageTestData.RAW_MESSAGE_ID)
        val labelId = LabelId("10")
        coEvery {
            localDataSource.relabelMessages(
                userId = userId,
                messageIds = listOf(messageId),
                labelIdsToAdd = setOf(labelId)
            )
        } returns listOf(MessageTestData.starredMessage).right()

        // When
        val actual = messageRepository.relabel(userId, listOf(messageId), labelsToBeAdded = listOf(labelId))

        // Then
        val starredMessage = MessageTestData.starredMessage
        coVerify { localDataSource.relabelMessages(userId, listOf(messageId), labelIdsToAdd = setOf(labelId)) }
        assertEquals(listOf(starredMessage).right(), actual)
    }

    @Test
    fun `add label updates remote data source`() = runTest {
        // Given
        val messageId = MessageId(MessageTestData.RAW_MESSAGE_ID)
        val labelId = LabelId("10")
        coEvery {
            localDataSource.relabelMessages(userId, listOf(messageId), labelIdsToAdd = setOf(labelId))
        } returns listOf(MessageTestData.starredMessage).right()

        // When
        messageRepository.relabel(userId, listOf(messageId), labelsToBeAdded = listOf(labelId))

        // Then
        coVerify { remoteDataSource.addLabelsToMessages(userId, listOf(messageId), listOf(labelId)) }
    }

    @Test
    fun `add label emits error when local data source fails`() = runTest {
        // Given
        val messageId = MessageId("messageId")
        val labelId = LabelId("42")
        coEvery {
            localDataSource.relabelMessages(userId, listOf(messageId), labelIdsToAdd = setOf(labelId))
        } returns DataError.Local.NoDataCached.left()

        // When
        val actual = messageRepository.relabel(userId, listOf(messageId), labelsToBeAdded = listOf(labelId))

        // Then
        assertEquals(DataError.Local.NoDataCached.left(), actual)
    }

    @Test
    fun `remove label returns message without label when successful`() = runTest {
        // Given
        val messageId = MessageId(MessageTestData.RAW_MESSAGE_ID)
        val labelId = LabelId("10")
        coEvery {
            localDataSource.relabelMessages(userId, listOf(messageId), labelIdsToRemove = setOf(labelId))
        } returns listOf(MessageTestData.message).right()
        // When
        val actual = messageRepository.relabel(userId, listOf(messageId), labelsToBeRemoved = listOf(LabelId("10")))
        // Then
        val unstarredMessage = MessageTestData.message
        assertEquals(listOf(unstarredMessage).right(), actual)
    }

    @Test
    fun `remove label updates remote data source`() = runTest {
        // Given
        val messageId = MessageId(MessageTestData.RAW_MESSAGE_ID)
        val labelId = LabelId("10")
        coEvery {
            localDataSource.relabelMessages(userId, listOf(messageId), labelIdsToRemove = setOf(labelId))
        } returns listOf(MessageTestData.message).right()
        // When
        messageRepository.relabel(userId, listOf(messageId), listOf(labelId))
        // Then
        coVerify { remoteDataSource.removeLabelsFromMessages(userId, listOf(messageId), listOf(labelId)) }
    }

    @Test
    fun `remove label emits error when local data source fails`() = runTest {
        // Given
        val messageId = MessageId("messageId")
        val labelId = LabelId("42")
        coEvery {
            localDataSource.relabelMessages(userId, listOf(messageId), labelIdsToRemove = setOf(labelId))
        } returns DataError.Local.NoDataCached.left()
        // When
        val actual = messageRepository.relabel(userId, listOf(messageId), listOf(labelId))
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
        coEvery { localDataSource.observeMessages(userId, listOf(messageId)) } returns flowOf(listOf(message))
        coEvery {
            localDataSource.relabelMessages(
                userId,
                listOf(messageId),
                labelIdsToAdd = setOf(SystemLabelId.Trash.labelId)
            )
        } returns listOf(trashedMessage).right()

        // when
        val result =
            messageRepository.moveTo(userId, mapOf(messageId to LabelIdSample.AllDraft), SystemLabelId.Trash.labelId)

        // then
        assertEquals(listOf(trashedMessage).right(), result)
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
        val messageFlow = MutableStateFlow(listOf(message))
        coEvery { localDataSource.observeMessages(userId, listOf(messageId)) } returns messageFlow
        coEvery { localDataSource.upsertMessage(messageWithoutLabels) } coAnswers {
            messageFlow.emit(listOf(messageWithoutLabels)).right()
        }
        coEvery {
            localDataSource.relabelMessages(
                userId,
                listOf(messageId),
                labelIdsToRemove = setOf(LabelIdSample.Inbox, LabelIdSample.Document),
                labelIdsToAdd = setOf(SystemLabelId.Trash.labelId)
            )
        } returns listOf(trashedMessage).right()

        // when
        val result = messageRepository.moveTo(
            userId,
            mapOf(messageId to SystemLabelId.Inbox.labelId),
            SystemLabelId.Trash.labelId
        )

        // then
        assertEquals(listOf(trashedMessage).right(), result)
    }

    @Test
    fun `move to spam removes all the labels, except AllMail, AllDraft and AllSent`() = runTest {
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
        val spammedMessage = messageWithoutLabels.copy(
            labelIds = messageWithoutLabels.labelIds + SystemLabelId.Spam.labelId
        )
        val messageFlow = MutableStateFlow(listOf(message))
        coEvery { localDataSource.observeMessages(userId, listOf(messageId)) } returns messageFlow
        coEvery { localDataSource.upsertMessage(messageWithoutLabels) } coAnswers {
            messageFlow.emit(listOf(messageWithoutLabels)).right()
        }
        coEvery {
            localDataSource.relabelMessages(
                userId,
                listOf(messageId),
                labelIdsToRemove = setOf(SystemLabelId.Inbox.labelId, LabelIdSample.Document),
                labelIdsToAdd = setOf(SystemLabelId.Spam.labelId)
            )
        } returns listOf(spammedMessage).right()

        // when
        val result = messageRepository.moveTo(
            userId,
            mapOf(messageId to SystemLabelId.Inbox.labelId),
            SystemLabelId.Spam.labelId
        )

        // then
        assertEquals(listOf(spammedMessage).right(), result)
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
        val message2 = MessageTestData.spamMessage.copy(
            messageId = MessageIdSample.Invoice,
            labelIds = listOf(
                SystemLabelId.Spam.labelId,
                SystemLabelId.AllMail.labelId
            )
        )
        val messageList = listOf(message, message2)
        val messageIdList = messageList.map { it.messageId }
        coEvery { localDataSource.observeMessages(userId, messageIdList) } returns flowOf(messageList)

        val destinationLabel = SystemLabelId.Archive.labelId
        val updatedMessage = message.copy(
            labelIds = listOf(
                SystemLabelId.AllMail.labelId,
                SystemLabelId.Starred.labelId,
                destinationLabel
            )
        )
        val updatedMessage2 = message2.copy(
            labelIds = listOf(
                SystemLabelId.AllMail.labelId,
                destinationLabel
            )
        )
        val updatedMessageList = listOf(updatedMessage, updatedMessage2)

        val map = mapOf(
            message.messageId to SystemLabelId.Inbox.labelId,
            message2.messageId to SystemLabelId.Spam.labelId
        )

        // When
        val result = messageRepository.moveTo(userId, map, destinationLabel)

        // Then
        coVerify { localDataSource.upsertMessages(updatedMessageList) }
        verify { remoteDataSource.addLabelsToMessages(userId, messageIdList, listOf(destinationLabel)) }
        assertEquals(updatedMessageList.right(), result)
    }

    @Test
    fun `move message without exclusive labels adds new label`() = runTest {
        // Given
        val message = MessageTestData.message.copy(labelIds = listOf(SystemLabelId.AllMail.labelId))
        coEvery { localDataSource.observeMessages(userId, listOf(message.messageId)) } returns flowOf(listOf(message))
        val destinationLabel = SystemLabelId.Archive.labelId

        val updatedMessage = message.copy(
            labelIds = listOf(
                SystemLabelId.AllMail.labelId,
                destinationLabel
            )
        )

        // When
        val result = messageRepository.moveTo(userId, mapOf(message.messageId to null), destinationLabel)

        // Then
        coVerify { localDataSource.upsertMessages(listOf(updatedMessage)) }
        verify { remoteDataSource.addLabelsToMessages(userId, listOf(message.messageId), listOf(destinationLabel)) }
        assertEquals(listOf(updatedMessage).right(), result)
    }

    @Test
    fun `move message to trash without exclusive labels`() = runTest {
        // Given
        val message = MessageTestData.message.copy(
            labelIds = listOf(
                SystemLabelId.AllMail.labelId
            )
        )
        coEvery { localDataSource.observeMessages(userId, listOf(message.messageId)) } returns flowOf(listOf(message))
        val destinationLabel = SystemLabelId.Trash.labelId

        val updatedMessage = message.copy(labelIds = listOf(destinationLabel))
        coEvery {
            localDataSource.relabelMessages(userId, listOf(message.messageId), labelIdsToAdd = setOf(destinationLabel))
        } returns listOf(updatedMessage).right()

        // When
        val result = messageRepository.moveTo(userId, mapOf(message.messageId to null), destinationLabel)

        // Then
        coVerify {
            localDataSource.relabelMessages(userId, listOf(message.messageId), labelIdsToAdd = setOf(destinationLabel))
        }
        verify { remoteDataSource.addLabelsToMessages(userId, listOf(message.messageId), listOf(destinationLabel)) }
        assertEquals(listOf(updatedMessage).right(), result)
    }

    @Test
    fun `move emits error when local data source fails`() = runTest {
        // Given
        val messageId = MessageId(MessageTestData.RAW_MESSAGE_ID)
        val messageList = listOf(messageId)
        coEvery { localDataSource.observeMessages(userId, messageList) } returns flowOf(emptyList())

        // When
        val result = messageRepository.moveTo(
            userId,
            mapOf(messageId to SystemLabelId.Inbox.labelId),
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
        coEvery { localDataSource.observeMessages(userId, listOf(messageId)) } returns flowOf(listOf(message))
        coEvery {
            localDataSource.relabelMessages(
                userId = userId,
                messageIds = listOf(messageId),
                labelIdsToRemove = setOf(SystemLabelId.Inbox.labelId, SystemLabelId.Starred.labelId),
                labelIdsToAdd = setOf(SystemLabelId.Trash.labelId)
            )
        } returns listOf(trashedMessage).right()

        // When
        val actual = messageRepository.moveTo(
            userId,
            mapOf(messageId to SystemLabelId.Inbox.labelId),
            SystemLabelId.Trash.labelId
        )

        // Then
        assertEquals(listOf(trashedMessage).right(), actual)
    }

    @Test
    fun `mark unread returns error when local data source fails`() = runTest {
        // given
        val messageId = listOf(MessageIdSample.Invoice)
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
        val messageId = listOf(MessageIdSample.Invoice)
        val message = listOf(MessageSample.Invoice).right()
        coEvery { localDataSource.markUnread(userId, messageId) } returns message

        // When
        val result = messageRepository.markUnread(userId, messageId)

        // Then
        assertEquals(message, result)
        verify { remoteDataSource.markUnread(userId, messageId) }
    }

    @Test
    fun `mark read returns updated message when local data source succeed`() = runTest {
        // given
        val messageId = listOf(MessageIdSample.Invoice)
        val message = listOf(MessageSample.Invoice).right()
        coEvery { localDataSource.markRead(userId, messageId) } returns message

        // When
        val result = messageRepository.markRead(userId, messageId)

        // Then
        assertEquals(message, result)
        verify { remoteDataSource.markRead(userId, messageId) }
    }

    @Test
    fun `mark read returns error when local data source fails`() = runTest {
        // given
        val messageId = listOf(MessageIdSample.Invoice)
        val error = DataErrorSample.NoCache.left()
        coEvery { localDataSource.markRead(userId, messageId) } returns error

        // When
        val result = messageRepository.markRead(userId, messageId)

        // Then
        assertEquals(error, result)
        verify { remoteDataSource wasNot Called }
    }

    @Test
    fun `verify relabel calls local and remote data source`() = runTest {
        // Given
        val labelToStay = LabelId("CustomLabel1")
        val labelToBeAdded = LabelId("CustomLabel3")
        val labelToBeRemoved = LabelId("CustomLabel2")
        val message = MessageTestData.message.copy(labelIds = listOf(labelToStay, labelToBeRemoved))

        coEvery {
            localDataSource.relabelMessages(
                userId,
                listOf(message.messageId),
                labelIdsToRemove = setOf(labelToBeRemoved),
                labelIdsToAdd = setOf(labelToBeAdded)
            )
        } returns listOf(message.copy(labelIds = listOf(labelToStay, labelToBeAdded))).right()

        // When
        messageRepository.relabel(
            userId,
            listOf(message.messageId),
            labelsToBeRemoved = listOf(labelToBeRemoved),
            labelsToBeAdded = listOf(labelToBeAdded)
        )

        // Then
        coVerify {
            localDataSource.relabelMessages(
                userId,
                listOf(message.messageId),
                labelIdsToRemove = setOf(labelToBeRemoved),
                labelIdsToAdd = setOf(labelToBeAdded)
            )
        }
        coVerify {
            remoteDataSource.removeLabelsFromMessages(userId, listOf(message.messageId), listOf(labelToBeRemoved))
            remoteDataSource.addLabelsToMessages(userId, listOf(message.messageId), listOf(labelToBeAdded))
        }
    }

    @Test
    fun `relabel call does not add unmodifiable labels`() = runTest {
        // Given
        val labelIds = listOf(SystemLabelId.Starred.labelId)
        val message = MessageTestData.message.copy(labelIds = labelIds)
        val labelsToAdd = unmodifiableLabels.map { it.labelId }
        val labelsToRemove = listOf(SystemLabelId.Starred.labelId)
        coEvery {
            localDataSource.relabelMessages(
                userId,
                listOf(message.messageId),
                labelIdsToRemove = labelsToRemove.toSet(),
                labelIdsToAdd = emptySet()
            )
        } returns listOf(message).right()

        // When
        messageRepository.relabel(
            userId,
            listOf(message.messageId),
            labelsToBeRemoved = labelsToRemove,
            labelsToBeAdded = labelsToAdd
        )

        // Then
        coVerify {
            localDataSource.relabelMessages(
                userId,
                listOf(message.messageId),
                labelIdsToRemove = labelsToRemove.toSet(),
                labelIdsToAdd = emptySet()
            )
        }
        coVerify {
            remoteDataSource.removeLabelsFromMessages(userId, listOf(message.messageId), labelsToRemove)
            remoteDataSource.addLabelsToMessages(userId, listOf(message.messageId), emptyList())
        }
    }

    @Test
    fun `relabel call does not remove unmodifiable labels`() = runTest {
        // Given
        val labelIds = listOf(SystemLabelId.Inbox.labelId)
        val message = MessageTestData.message.copy(labelIds = labelIds)
        val labelsToAdd = listOf(SystemLabelId.Starred.labelId)
        val labelsToRemove = unmodifiableLabels.map { it.labelId }
        coEvery {
            localDataSource.relabelMessages(
                userId,
                listOf(message.messageId),
                labelIdsToRemove = emptySet(),
                labelIdsToAdd = labelsToAdd.toSet()
            )
        } returns listOf(message).right()

        // When
        messageRepository.relabel(
            userId,
            listOf(message.messageId),
            labelsToBeRemoved = labelsToRemove,
            labelsToBeAdded = labelsToAdd
        )

        // Then
        coVerify {
            localDataSource.relabelMessages(
                userId,
                listOf(message.messageId),
                labelIdsToRemove = emptySet(),
                labelIdsToAdd = labelsToAdd.toSet()
            )
        }
        coVerify {
            remoteDataSource.removeLabelsFromMessages(userId, listOf(message.messageId), emptyList())
            remoteDataSource.addLabelsToMessages(userId, listOf(message.messageId), labelsToAdd)
        }
    }

    @Test
    fun `Should return true if the message is read`() = runTest {
        // Given
        val messageId = MessageId(UUID.randomUUID().toString())
        coEvery { localDataSource.isMessageRead(userId, messageId) } returns true.right()

        // When
        val result = messageRepository.isMessageRead(userId, messageId).getOrNull()

        // Then
        assertEquals(true, result)
    }

    @Test
    fun `Should return false if the message is unread`() = runTest {
        // Given
        val messageId = MessageId(UUID.randomUUID().toString())
        coEvery { localDataSource.isMessageRead(userId, messageId) } returns false.right()

        // When
        val result = messageRepository.isMessageRead(userId, messageId).getOrNull()

        // Then
        assertEquals(false, result)
    }

    @Test
    fun `Should return error if the data source returns error`() = runTest {
        // Given
        val messageId = MessageId(UUID.randomUUID().toString())
        coEvery { localDataSource.isMessageRead(userId, messageId) } returns DataError.Local.NoDataCached.left()

        // When
        val result = messageRepository.isMessageRead(userId, messageId)

        // Then
        assertEquals(DataError.Local.NoDataCached.left(), result)
    }

    @Test
    fun `returns true when page is valid on local data source`() = runTest {
        // Given
        val pageKey = PageKey()
        val items = listOf(getMessage(id = "1", time = 1000))
        coEvery { localDataSource.isLocalPageValid(userId, pageKey, items) } returns true

        // When
        val actual = messageRepository.isLocalPageValid(userId, pageKey, items)

        // Then
        assertTrue(actual)
    }

    @Test
    fun `returns false when page is not valid on local data source`() = runTest {
        // Given
        val pageKey = PageKey()
        val items = listOf(getMessage(id = "1", time = 1000))
        coEvery { localDataSource.isLocalPageValid(userId, pageKey, items) } returns false

        // When
        val actual = messageRepository.isLocalPageValid(userId, pageKey, items)

        // Then
        assertFalse(actual)
    }

    @Test
    fun `should upsert the message with body`() = runTest {
        // Given
        val expectedUserId = UserIdSample.Primary
        val expectedMessageWithBody = MessageWithBodySample.EmptyDraft

        // When
        val messageSaved = messageRepository.upsertMessageWithBody(expectedUserId, expectedMessageWithBody)

        // Then
        assertTrue(messageSaved)
    }

    @Test
    fun `should return false when saving draft fails with IO exception`() = runTest {
        // Given
        val expectedUserId = UserIdSample.Primary
        val expectedMessageWithBody = MessageWithBodySample.EmptyDraft
        coEvery { localDataSource.upsertMessageWithBody(expectedUserId, expectedMessageWithBody) } throws IOException()

        // When
        val messageSaved = messageRepository.upsertMessageWithBody(expectedUserId, expectedMessageWithBody)

        // Then
        assertFalse(messageSaved)
    }

    @Test
    fun `should return false when saving draft fails while writing to file`() = runTest {
        // Given
        val expectedUserId = UserIdSample.Primary
        val expectedMessageWithBody = MessageWithBodySample.EmptyDraft
        coEvery {
            localDataSource.upsertMessageWithBody(expectedUserId, expectedMessageWithBody)
        } throws MessageBodyFileWriteException

        // When
        val messageSaved = messageRepository.upsertMessageWithBody(expectedUserId, expectedMessageWithBody)

        // Then
        assertFalse(messageSaved)
    }

    @Test
    fun `should read the message with body from local storage`() = runTest {
        // Given
        val userId = UserIdSample.Primary
        val expectedMessageWithBody = MessageWithBodySample.EmptyDraft
        val expectedMessageId = expectedMessageWithBody.message.messageId
        coEvery {
            localDataSource.observeMessageWithBody(
                userId,
                expectedMessageId
            )
        } returns flowOf(expectedMessageWithBody)

        // When
        val actualMessageWithBody = messageRepository.getLocalMessageWithBody(userId, expectedMessageId)

        // Then
        assertEquals(expectedMessageWithBody, actualMessageWithBody)
    }

    @Test
    fun `should return a null when reading message with body from local storage fails`() = runTest {
        // Given
        val userId = UserIdSample.Primary
        val messageId = MessageWithBodySample.EmptyDraft.message.messageId
        coEvery {
            localDataSource.observeMessageWithBody(
                userId,
                messageId
            )
        } returns emptyFlow()

        // When
        val actualMessageWithBody = messageRepository.getLocalMessageWithBody(userId, messageId)

        // Then
        assertNull(actualMessageWithBody)
    }

    @Test
    fun `when get refreshed message with body from remote storage is successful update local cache and return it`() =
        runTest {
            // Given
            val userId = UserIdSample.Primary
            val expectedMessageWithBody = MessageWithBodySample.RemoteDraft
            val expectedRefreshedMessageWithBody = RefreshedMessageWithBody(expectedMessageWithBody, isRefreshed = true)
            val expectedMessageId = MessageIdSample.RemoteDraft
            coEvery { remoteDataSource.getMessage(userId, expectedMessageId) } returns expectedMessageWithBody.right()

            // When
            val actual = messageRepository.getRefreshedMessageWithBody(userId, expectedMessageId)

            // Then
            assertEquals(expectedRefreshedMessageWithBody, actual)
            coVerify { localDataSource.upsertMessageWithBody(userId, expectedMessageWithBody) }
        }

    @Test
    fun `when get refreshed message with body from remote storage fails, return cached one`() = runTest {
        // Given
        val userId = UserIdSample.Primary
        val expectedMessageWithBody = MessageWithBodySample.RemoteDraft
        val expectedRefreshedMessageWithBody = RefreshedMessageWithBody(expectedMessageWithBody, isRefreshed = false)
        val expectedMessageId = MessageIdSample.RemoteDraft
        val expectedError = DataError.Remote.Http(NetworkError.Unreachable).left()
        coEvery { remoteDataSource.getMessage(userId, expectedMessageId) } returns expectedError
        coEvery { localDataSource.observeMessageWithBody(userId, expectedMessageId) } returns
            flowOf(expectedMessageWithBody)

        // When
        val actual = messageRepository.getRefreshedMessageWithBody(userId, expectedMessageId)

        // Then
        assertEquals(expectedRefreshedMessageWithBody, actual)
        coVerify(exactly = 0) { localDataSource.upsertMessageWithBody(userId, expectedMessageWithBody) }
    }

    @Test
    fun `verify delete messages calls local and remote data sources`() = runTest {
        // Given
        val messageIds = listOf(MessageId("1"), MessageId("2"))
        val expectedLabel = SystemLabelId.Trash.labelId
        coEvery { localDataSource.deleteMessagesWithId(userId, messageIds) } returns Unit.right()
        coJustRun { remoteDataSource.deleteMessages(userId, messageIds, expectedLabel) }

        // When
        val actual = messageRepository.deleteMessages(userId, messageIds, expectedLabel)

        // Then
        assertEquals(Unit.right(), actual)
        coVerify { localDataSource.deleteMessagesWithId(userId, messageIds) }
        coVerify { remoteDataSource.deleteMessages(userId, messageIds, expectedLabel) }
    }

    @Test
    fun `verify delete messages returns local error when deleting fails locally`() = runTest {
        // Given
        val messageIds = listOf(MessageId("1"), MessageId("2"))
        val expectedLabel = SystemLabelId.Trash.labelId
        val expected = DataError.Local.DeletingFailed.left()
        coEvery { localDataSource.deleteMessagesWithId(userId, messageIds) } returns expected

        // When
        val actual = messageRepository.deleteMessages(userId, messageIds, expectedLabel)

        // Then
        assertEquals(expected, actual)
        coVerify { localDataSource.deleteMessagesWithId(userId, messageIds) }
        coVerify { remoteDataSource wasNot Called }
    }

    @Test
    fun `verify delete messages with label calls local and remote data sources`() = runTest {
        // Given
        val expectedLabel = SystemLabelId.Trash.labelId
        coJustRun { remoteDataSource.clearLabel(userId, expectedLabel) }

        // When
        messageRepository.deleteMessages(userId, expectedLabel)

        // Then
        coVerify {
            remoteDataSource.clearLabel(userId, expectedLabel)
            localDataSource wasNot Called
        }
    }

    @Test
    fun `observe clear worker state returns worker state from remote data source`() = runTest {
        // Given
        val expected = true
        val expectedLabel = SystemLabelId.Spam.labelId
        val expectedFlow = MutableStateFlow(expected)
        coEvery { remoteDataSource.observeClearWorkerIsEnqueuedOrRunning(userId, expectedLabel) } returns expectedFlow

        // When
        messageRepository.observeClearLabelOperation(userId, expectedLabel).test {
            // Then
            assertTrue { awaitItem() }
        }
    }

    @Test
    fun `when report phishing is called then remote layer is called and returns Unit if successful`() = runTest {
        // Given
        val decryptedMessage = DecryptedMessageBodyTestData.MessageWithAttachments
        val expected = Unit.right()
        coEvery { remoteDataSource.reportPhishing(userId, decryptedMessage) } returns expected

        // When
        val actual = messageRepository.reportPhishing(userId, decryptedMessage)

        // Then
        assertEquals(expected, actual)
        coVerify { remoteDataSource.reportPhishing(userId, decryptedMessage) }
    }

    @Test
    fun `when report phishing is called then remote layer is called and returns error if unsuccessful`() = runTest {
        // Given
        val decryptedMessage = DecryptedMessageBodyTestData.MessageWithAttachments
        val expected = DataError.Remote.Unknown.left()
        coEvery { remoteDataSource.reportPhishing(userId, decryptedMessage) } returns expected

        // When
        val actual = messageRepository.reportPhishing(userId, decryptedMessage)

        // Then
        assertEquals(expected, actual)
        coVerify { remoteDataSource.reportPhishing(userId, decryptedMessage) }
    }
}
