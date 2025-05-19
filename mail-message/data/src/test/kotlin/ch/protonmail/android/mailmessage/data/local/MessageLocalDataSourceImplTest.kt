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

package ch.protonmail.android.mailmessage.data.local

import java.util.UUID
import android.database.sqlite.SQLiteConstraintException
import app.cash.turbine.test
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.ConversationId
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcommon.domain.sample.ConversationIdSample
import ch.protonmail.android.mailcommon.domain.sample.DataErrorSample
import ch.protonmail.android.mailcommon.domain.sample.LabelIdSample
import ch.protonmail.android.mailcommon.domain.sample.UserIdSample
import ch.protonmail.android.maillabel.domain.model.MailLabelId
import ch.protonmail.android.maillabel.domain.model.SystemLabelId
import ch.protonmail.android.mailmessage.data.getMessage
import ch.protonmail.android.mailmessage.data.getMessageWithLabels
import ch.protonmail.android.mailmessage.data.local.dao.MessageAttachmentDao
import ch.protonmail.android.mailmessage.data.local.dao.MessageBodyDao
import ch.protonmail.android.mailmessage.data.local.dao.MessageDao
import ch.protonmail.android.mailmessage.data.local.dao.MessageLabelDao
import ch.protonmail.android.mailmessage.data.local.entity.MessageLabelEntity
import ch.protonmail.android.mailmessage.data.local.relation.MessageWithBodyEntity
import ch.protonmail.android.mailmessage.data.local.relation.MessageWithLabelIds
import ch.protonmail.android.mailmessage.data.mapper.MessageAttachmentEntityMapper
import ch.protonmail.android.mailmessage.data.mapper.MessageWithBodyEntityMapper
import ch.protonmail.android.mailmessage.data.sample.MessageEntitySample
import ch.protonmail.android.mailmessage.data.sample.MessageWithLabelIdsSample
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailmessage.domain.model.MessageWithBody
import ch.protonmail.android.mailmessage.domain.sample.MessageAttachmentSample
import ch.protonmail.android.mailmessage.domain.sample.MessageIdSample
import ch.protonmail.android.mailmessage.domain.sample.MessageSample
import ch.protonmail.android.mailpagination.data.local.dao.PageIntervalDao
import ch.protonmail.android.mailpagination.data.local.upsertPageInterval
import ch.protonmail.android.mailpagination.domain.model.OrderDirection
import ch.protonmail.android.mailpagination.domain.model.PageFilter
import ch.protonmail.android.mailpagination.domain.model.PageItemType
import ch.protonmail.android.mailpagination.domain.model.PageKey
import ch.protonmail.android.testdata.message.MessageAttachmentEntityTestData
import ch.protonmail.android.testdata.message.MessageBodyEntityTestData
import ch.protonmail.android.testdata.message.MessageBodyTestData
import ch.protonmail.android.testdata.message.MessageEntityTestData
import ch.protonmail.android.testdata.message.MessageTestData
import ch.protonmail.android.testdata.user.UserIdTestData
import io.mockk.Called
import io.mockk.called
import io.mockk.coEvery
import io.mockk.coInvoke
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.coVerifySequence
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import me.proton.core.label.domain.entity.LabelId
import org.junit.Before
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MessageLocalDataSourceImplTest {

    private val userId1 = UserId("1")
    private val userId2 = UserId("2")

    private val messageDao = mockk<MessageDao>(relaxUnitFun = true) {
        coEvery {
            observe(
                userId = any(),
                messageId = any()
            )
        } returns flowOf(MessageWithLabelIds(MessageTestData.message.toEntity(), listOf(LabelId("0"))))
    }
    private val messageBodyDao = mockk<MessageBodyDao>(relaxUnitFun = true) {
        every {
            observeMessageWithBodyEntity(userId = any(), messageId = any())
        } returns flowOf(
            MessageWithBodyEntity(
                MessageEntityTestData.messageEntity,
                MessageBodyEntityTestData.messageBodyEntity,
                listOf(LabelIdSample.Inbox)
            )
        )
    }
    private val labelDao = mockk<MessageLabelDao>(relaxUnitFun = true)
    private val pageIntervalDao = mockk<PageIntervalDao>(relaxUnitFun = true)
    private val attachmentDao = mockk<MessageAttachmentDao>(relaxUnitFun = true) {
        every { observeMessageAttachmentEntities(any(), any()) } returns flowOf(
            listOf(MessageAttachmentEntityTestData.invoice())
        )
    }

    private val db = mockk<MessageDatabase>(relaxed = true) {
        every { messageDao() } returns messageDao
        every { messageBodyDao() } returns messageBodyDao
        every { messageLabelDao() } returns labelDao
        every { pageIntervalDao() } returns pageIntervalDao
        every { messageAttachmentDao() } returns attachmentDao
        coEvery { inTransaction(captureCoroutine<suspend () -> Any>()) } coAnswers {
            coroutine<suspend () -> Any>().coInvoke()
        }
    }
    private val messageBodyFileStorage = mockk<MessageBodyFileStorage>(relaxUnitFun = true)
    private val attachmentFileStorage = mockk<AttachmentFileStorage>(relaxUnitFun = true)
    private val searchResultsLocalDataSource = mockk<SearchResultsLocalDataSource>(relaxUnitFun = true)
    private val messageWithBodyEntityMapper = MessageWithBodyEntityMapper()
    private val attachmentEntityMapper = MessageAttachmentEntityMapper()

    private lateinit var messageLocalDataSource: MessageLocalDataSourceImpl

    @Before
    fun setUp() {
        mockkStatic(PageIntervalDao::upsertPageInterval)
        messageLocalDataSource = MessageLocalDataSourceImpl(
            db = db,
            messageBodyFileStorage = messageBodyFileStorage,
            messageWithBodyEntityMapper = messageWithBodyEntityMapper,
            messageAttachmentEntityMapper = attachmentEntityMapper,
            attachmentFileStorage = attachmentFileStorage,
            searchResultsLocalDataSource = searchResultsLocalDataSource
        )
    }

    @Test
    fun `upsert messages and corresponding labels, from arbitrary users`() = runTest {
        // Given
        val messages = listOf(
            // userId1
            getMessage(userId1, "1", time = 1000, labelIds = emptyList()),
            getMessage(userId1, "2", time = 2000, labelIds = listOf("4")),
            getMessage(userId1, "3", time = 3000, labelIds = listOf("0", "1")),
            // userId2
            getMessage(userId2, "1", time = 1000, labelIds = listOf("3"))
        )
        val user1MessageIds = listOf(MessageId("1"), MessageId("2"), MessageId("3"))
        val user2MessageIds = listOf(MessageId("1"))

        // When
        messageLocalDataSource.upsertMessages(messages)

        // Then
        coVerify { db.inTransaction(any()) }
        coVerify { labelDao.deleteAll(userId1, user1MessageIds) }
        coVerify { labelDao.deleteAll(userId2, user2MessageIds) }
        coVerify(exactly = 1) { messageDao.insertOrUpdate(entities = anyVararg()) }
        coVerify(exactly = 4) { labelDao.insertOrUpdate(entities = anyVararg()) }
        coVerify(exactly = 0) { pageIntervalDao.upsertPageInterval(any(), any(), any(), any()) }
    }

    @Test
    fun `with userId1 and pageKey, upsert messages and corresponding interval`() = runTest {
        // Given
        val pageKey = PageKey(orderDirection = OrderDirection.Ascending, size = 3)
        val messages = listOf(
            // userId1
            getMessage(userId1, "1", time = 1000, labelIds = emptyList()),
            getMessage(userId1, "2", time = 2000, labelIds = listOf("4")),
            getMessage(userId1, "3", time = 3000, labelIds = listOf("0", "1"))
        )
        val user1MessageIds = listOf(MessageId("1"), MessageId("2"), MessageId("3"))

        // When
        messageLocalDataSource.upsertMessages(userId1, pageKey, messages)

        // Then
        coVerify { db.inTransaction(any()) }
        coVerify { labelDao.deleteAll(userId1, user1MessageIds) }
        coVerify(exactly = 1) { messageDao.insertOrUpdate(entities = anyVararg()) }
        coVerify(exactly = 3) { labelDao.insertOrUpdate(entities = anyVararg()) }
        coVerify(exactly = 1) { pageIntervalDao.upsertPageInterval(any(), any(), any(), any()) }
    }

    @Test
    fun `in search mode upsert messages to search results data source`() = runTest {
        // Given
        val keyword = "search query"
        val pageKey = PageKey(
            orderDirection = OrderDirection.Ascending, size = 3,
            filter = PageFilter(keyword = keyword)
        )
        val messages = listOf(
            // userId1
            getMessage(userId1, "1", time = 1000, labelIds = emptyList()),
            getMessage(userId1, "2", time = 2000, labelIds = listOf("4")),
            getMessage(userId1, "3", time = 3000, labelIds = listOf("0", "1"))
        )
        val user1MessageIds = listOf(MessageId("1"), MessageId("2"), MessageId("3"))

        // When
        messageLocalDataSource.upsertMessages(userId1, pageKey, messages)

        // Then
        coVerify { db.inTransaction(any()) }
        coVerify { labelDao.deleteAll(userId1, user1MessageIds) }
        coVerify(exactly = 1) { messageDao.insertOrUpdate(entities = anyVararg()) }
        coVerify(exactly = 3) { labelDao.insertOrUpdate(entities = anyVararg()) }
        coVerify(exactly = 1) { pageIntervalDao.upsertPageInterval(any(), any(), any(), any()) }
        coVerify(exactly = 1) {
            searchResultsLocalDataSource.upsertResults(
                userId1,
                keyword,
                messages
            )
        }
    }

    @Test
    fun `in search mode get messages should return search results`() = runTest {
        // Given
        val keyword = "search query"
        val pageKey = PageKey(
            orderDirection = OrderDirection.Ascending, size = 3,
            filter = PageFilter(keyword = keyword)
        )
        val messages = listOf(
            getMessageWithLabels(userId1, "1"),
            getMessageWithLabels(userId1, "2"),
            getMessageWithLabels(userId1, "3")
        )
        val searchResults = listOf(
            getMessageWithLabels(userId1, "2"),
            getMessageWithLabels(userId1, "3")
        )
        coEvery { messageDao.observeAll(userId1, pageKey) } returns flowOf(messages)
        coEvery { messageDao.observeSearchResults(userId1, pageKey) } returns flowOf(searchResults)

        // When
        messageLocalDataSource.getMessages(userId1, pageKey)

        // Then
        coVerify(exactly = 0) { messageDao.observeAll(any(), any()) }
        coVerify(exactly = 1) { messageDao.observeSearchResults(userId1, pageKey) }
    }

    @Test
    fun `in non-search mode get messages should return messages in local db`() = runTest {
        // Given
        val pageKey = PageKey(
            orderDirection = OrderDirection.Ascending, size = 3
        )

        val messages = listOf(
            getMessageWithLabels(userId1, "1"),
            getMessageWithLabels(userId1, "2"),
            getMessageWithLabels(userId1, "3")
        )
        val searchResults = listOf(
            getMessageWithLabels(userId1, "2"),
            getMessageWithLabels(userId1, "3")
        )
        coEvery { messageDao.observeAll(userId1, pageKey) } returns flowOf(messages)
        coEvery { messageDao.observeSearchResults(userId1, pageKey) } returns flowOf(searchResults)

        // When
        messageLocalDataSource.getMessages(userId1, pageKey)

        // Then
        coVerify(exactly = 1) { messageDao.observeAll(userId1, pageKey) }
        coVerify(exactly = 0) { messageDao.observeSearchResults(any(), any()) }
    }

    @Test
    fun `should delete all messages and page interval from the db and message body files`() = runTest {
        // Given
        coEvery { messageBodyFileStorage.deleteAllMessageBodies(userId1) } returns true

        // When
        messageLocalDataSource.deleteAllMessages(userId1)

        // Then
        coVerify(exactly = 1) { db.inTransaction(any()) }
        coVerify(exactly = 1) { messageDao.deleteAll(userId1) }
        coVerify(exactly = 1) { pageIntervalDao.deleteAll(userId1, PageItemType.Message) }
        coVerify(exactly = 1) { messageBodyFileStorage.deleteAllMessageBodies(userId1) }
    }

    @Test
    fun `should delete all messages except draft ones from the db and message body files`() = runTest {
        // Given
        val draftMessages = listOf(MessageIdSample.LocalDraft, MessageIdSample.EmptyDraft)
        coEvery { messageBodyFileStorage.deleteAllMessageBodies(userId1) } returns true

        // When
        messageLocalDataSource.deleteAllMessagesExcept(userId1, draftMessages)

        // Then
        coVerify(exactly = 1) { db.inTransaction(any()) }
        coVerify(exactly = 1) { messageDao.deleteAllExcept(userId1, draftMessages) }
        coVerify(exactly = 1) { pageIntervalDao.deleteAll(userId1, PageItemType.Message) }
        coVerify(exactly = 1) { messageBodyFileStorage.deleteAllMessageBodies(userId1) }
    }

    @Test
    fun `should delete messages from the db and corresponding message body files`() = runTest {
        // Given
        val deletedIds = listOf(
            MessageIdSample.Invoice,
            MessageIdSample.EmptyDraft,
            MessageIdSample.OctWeatherForecast
        )
        val deletedRawIds = deletedIds.map { it.id }
        coEvery { messageBodyFileStorage.deleteMessageBody(userId1, MessageIdSample.Invoice) } returns true
        coEvery { messageBodyFileStorage.deleteMessageBody(userId1, MessageIdSample.EmptyDraft) } returns true
        coEvery { messageBodyFileStorage.deleteMessageBody(userId1, MessageIdSample.OctWeatherForecast) } returns true

        // When
        messageLocalDataSource.deleteMessages(userId1, deletedIds)

        // Then
        coVerify { messageDao.delete(userId1, deletedRawIds) }
        coVerify { messageBodyFileStorage.deleteMessageBody(userId1, MessageIdSample.Invoice) }
        coVerify { messageBodyFileStorage.deleteMessageBody(userId1, MessageIdSample.EmptyDraft) }
        coVerify { messageBodyFileStorage.deleteMessageBody(userId1, MessageIdSample.OctWeatherForecast) }
    }

    @Test
    fun `should delete messages from the db chunked to not exceed defined maximum`() = runTest {
        // Given
        val deletedIds = (1..MessageLocalDataSourceImpl.SQL_CHUNK_SIZE * 3).map { MessageId("$it") }
        val chunkedDeletedRawIds = deletedIds.map { it.id }.chunked(MessageLocalDataSourceImpl.SQL_CHUNK_SIZE)
        coEvery { messageBodyFileStorage.deleteMessageBody(userId1, any()) } returns true

        // When
        messageLocalDataSource.deleteMessages(userId1, deletedIds)

        // Then
        coVerify { messageDao.delete(userId1, chunkedDeletedRawIds[0]) }
        coVerify { messageDao.delete(userId1, chunkedDeletedRawIds[1]) }
        coVerify { messageDao.delete(userId1, chunkedDeletedRawIds[2]) }
    }

    @Test
    fun `returns Unit when delete messages from the db and corresponding message body files is successful`() = runTest {
        // Given
        val deletedIds = listOf(
            MessageIdSample.Invoice,
            MessageIdSample.EmptyDraft,
            MessageIdSample.OctWeatherForecast
        )
        val deletedRawIds = deletedIds.map { it.id }
        coEvery { messageBodyFileStorage.deleteMessageBody(userId1, MessageIdSample.Invoice) } returns true
        coEvery { messageBodyFileStorage.deleteMessageBody(userId1, MessageIdSample.EmptyDraft) } returns true
        coEvery { messageBodyFileStorage.deleteMessageBody(userId1, MessageIdSample.OctWeatherForecast) } returns true

        // When
        val result = messageLocalDataSource.deleteMessagesWithId(userId1, deletedIds)

        // Then
        coVerify { messageDao.delete(userId1, deletedRawIds) }
        coVerify { messageBodyFileStorage.deleteMessageBody(userId1, MessageIdSample.Invoice) }
        coVerify { messageBodyFileStorage.deleteMessageBody(userId1, MessageIdSample.EmptyDraft) }
        coVerify { messageBodyFileStorage.deleteMessageBody(userId1, MessageIdSample.OctWeatherForecast) }
        assertEquals(Unit.right(), result)
    }

    @Test
    fun `returns data error deleting failed when delete messages from the db failed`() = runTest {
        // Given
        val deletedIds = listOf(
            MessageIdSample.Invoice,
            MessageIdSample.EmptyDraft,
            MessageIdSample.OctWeatherForecast
        )
        val deletedRawIds = deletedIds.map { it.id }
        coEvery { messageDao.delete(userId1, deletedRawIds) } throws SQLiteConstraintException()

        // When
        val result = messageLocalDataSource.deleteMessagesWithId(userId1, deletedIds)

        // Then
        coVerify { messageDao.delete(userId1, deletedRawIds) }
        coVerify { messageBodyFileStorage wasNot Called }
        assertEquals(DataError.Local.DeletingFailed.left(), result)
    }

    @Test
    fun `should delete only messages with given label from db and corresponding message body files`() = runTest {
        // Given
        val deletedConversationIds = listOf(
            ConversationIdSample.Invoices,
            ConversationIdSample.AlphaAppFeedback,
            ConversationIdSample.WeatherForecast
        )
        val expectedRawIds = listOf(MessageIdSample.Invoice.id)
        val expectedMessages = listOf(
            MessageWithLabelIdsSample.Invoice,
            MessageWithLabelIdsSample.AugWeatherForecast,
            MessageWithLabelIdsSample.SepWeatherForecast
        )
        coEvery {
            messageDao.getMessageWithLabelsInConversations(userId1, deletedConversationIds)
        } returns expectedMessages
        coEvery { messageBodyFileStorage.deleteMessageBody(userId1, MessageIdSample.Invoice) } returns true

        // When
        val result = messageLocalDataSource
            .deleteMessagesInConversations(userId1, deletedConversationIds, LabelIdSample.Document)

        // Then
        coVerify { messageDao.delete(userId1, expectedRawIds) }
        coVerifySequence { messageBodyFileStorage.deleteMessageBody(userId1, MessageIdSample.Invoice) }
        assertEquals(Unit.right(), result)
    }

    @Test
    fun `should return data error when delete only messages with given label fails`() = runTest {
        // Given
        val deletedConversationIds = listOf(
            ConversationIdSample.Invoices,
            ConversationIdSample.AlphaAppFeedback,
            ConversationIdSample.WeatherForecast
        )
        val expectedRawIds = listOf(MessageIdSample.Invoice.id)
        val expectedMessages = listOf(
            MessageWithLabelIdsSample.Invoice,
            MessageWithLabelIdsSample.AugWeatherForecast,
            MessageWithLabelIdsSample.SepWeatherForecast
        )
        coEvery {
            messageDao.getMessageWithLabelsInConversations(userId1, deletedConversationIds)
        } returns expectedMessages
        coEvery { messageDao.delete(userId1, expectedRawIds) } throws SQLiteConstraintException()

        // When
        val result = messageLocalDataSource
            .deleteMessagesInConversations(userId1, deletedConversationIds, LabelIdSample.Document)

        // Then
        coVerify { messageDao.delete(userId1, expectedRawIds) }
        coVerify { messageBodyFileStorage wasNot Called }
        assertEquals(DataError.Local.DeletingFailed.left(), result)
    }

    @Test
    fun `should delete messages with given label from db and corresponding message body `() = runTest {
        // Given
        val expectedLabel = LabelIdSample.Trash
        val expectedRawIds = listOf(MessageIdSample.AugWeatherForecast.id, MessageIdSample.SepWeatherForecast.id)
        val expectedMessages = listOf(
            MessageWithLabelIdsSample.AugWeatherForecast.copy(labelIds = listOf(expectedLabel)),
            MessageWithLabelIdsSample.SepWeatherForecast.copy(labelIds = listOf(expectedLabel))
        )
        coEvery { messageDao.observeMessages(userId1, expectedLabel) } returns flowOf(expectedMessages)
        coEvery { messageBodyFileStorage.deleteMessageBody(userId1, MessageIdSample.AugWeatherForecast) } returns true
        coEvery { messageBodyFileStorage.deleteMessageBody(userId1, MessageIdSample.SepWeatherForecast) } returns true
        coJustRun { messageDao.delete(userId1, expectedRawIds) }

        // When
        val result = messageLocalDataSource.deleteMessagesWithLabel(userId1, expectedLabel)

        // Then
        coVerify { messageDao.delete(userId1, expectedRawIds) }
        coVerifySequence {
            messageBodyFileStorage.deleteMessageBody(userId1, MessageIdSample.AugWeatherForecast)
            messageBodyFileStorage.deleteMessageBody(userId1, MessageIdSample.SepWeatherForecast)
        }
        assertEquals(Unit.right(), result)
    }

    @Test
    fun `markAsStale call pageIntervalDao deleteAll`() = runTest {
        // Given
        val labelId = LabelId("1")

        // When
        messageLocalDataSource.markAsStale(userId1, labelId)

        // Then
        coVerify(exactly = 1) { pageIntervalDao.deleteAll(userId1, PageItemType.Message, labelId) }
    }

    @Test
    fun `observe message returns local message when existing`() = runTest {
        // Given
        val messageId = MessageId("MessageId")
        val messageWithLabels = getMessageWithLabels(userId1, "1")
        val message = messageWithLabels.toMessage()
        coEvery { messageDao.observe(userId1, messageId) } returns flowOf(messageWithLabels)

        // When
        messageLocalDataSource.observeMessage(userId1, messageId).test {
            // Then
            assertEquals(message, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `observe message returns null when message does not exist locally`() = runTest {
        // Given
        val messageId = MessageId("MessageId")
        coEvery { messageDao.observe(userId1, messageId) } returns flowOf(null)

        // When
        messageLocalDataSource.observeMessage(userId1, messageId).test {
            // Then
            assertNull(awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `get messages for conversations returns returns local message when existing`() = runTest {
        // Given
        val conversationIds = listOf(ConversationId("ConversationId"))
        val expected = listOf(getMessage(userId1, "1"))

        val messageWithLabels = listOf(getMessageWithLabels(userId1, "1"))
        coEvery {
            messageDao.observeMessageWithLabelsInConversations(userId1, conversationIds)
        } returns flowOf(messageWithLabels)

        // When
        messageLocalDataSource.observeMessagesForConversation(userId1, conversationIds).test {
            // Then
            assertEquals(expected, awaitItem())
            awaitComplete()
        }

    }

    @Test
    fun `get messages for conversations returns local data error when no messages are stored`() = runTest {
        // Given
        val conversationIds = listOf(ConversationId("ConversationId"))

        coEvery {
            messageDao.observeMessageWithLabelsInConversations(userId1, conversationIds)
        } returns flowOf(emptyList())

        // When
        messageLocalDataSource.observeMessagesForConversation(userId1, conversationIds).test {
            // Then
            assertEquals(emptyList(), awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `upsert message inserts message and related labels locally`() = runTest {
        val message = MessageTestData.spamMessage
        // When
        messageLocalDataSource.upsertMessage(message)
        // Then
        coVerify { messageDao.insertOrUpdate(message.toEntity()) }
        coVerifyOrder {
            labelDao.deleteAll(message.userId, listOf(message.messageId))
            val spamLabelEntity = MessageLabelEntity(
                message.userId,
                message.labelIds.first(),
                message.messageId
            )
            labelDao.insertOrUpdate(spamLabelEntity)
        }
    }

    @Test
    fun `observe message with body locally returns a message with body`() = runTest {
        // Given
        val expected = MessageWithBody(MessageTestData.message, MessageBodyTestData.messageBodyWithAttachment)

        // When
        messageLocalDataSource.observeMessageWithBody(userId1, MessageId("messageId")).test {

            // Then
            assertEquals(expected, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `observe message with body locally returns null`() = runTest {
        // Given
        every {
            messageBodyDao.observeMessageWithBodyEntity(userId = any(), messageId = any())
        } returns flowOf(null)

        // When
        messageLocalDataSource.observeMessageWithBody(userId1, MessageId("messageId")).test {

            // Then
            assertNull(awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `observe message with body loads the body from file if the body is null in the db`() = runTest {
        // Given
        val observedMessageId = MessageEntityTestData.messageEntity.messageId
        val messageBodyFromFile = "I am a message body from a file"
        coEvery { messageBodyFileStorage.readMessageBody(userId1, observedMessageId) } returns messageBodyFromFile
        every { messageBodyDao.observeMessageWithBodyEntity(userId1, observedMessageId) } returns flowOf(
            MessageWithBodyEntity(
                MessageEntityTestData.messageEntity,
                MessageBodyEntityTestData.messageBodyEntity.copy(body = null),
                listOf(LabelIdSample.Inbox)
            )
        )
        val expected = MessageWithBody(
            MessageTestData.message,
            MessageBodyTestData.messageBodyWithAttachment.copy(body = messageBodyFromFile)
        )

        // When
        messageLocalDataSource.observeMessageWithBody(userId1, observedMessageId).test {

            // Then
            assertEquals(expected, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `observe message attachments returns a list of attachments`() = runTest {
        // Given
        val messageId = MessageIdSample.Invoice
        val expected = listOf(MessageAttachmentSample.invoice)

        // When
        messageLocalDataSource.observeMessageAttachments(userId1, messageId).test {

            // Then
            assertEquals(expected, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `upsert message with body inserts the message, message body, labels and attachment locally`() = runTest {
        // Given
        val messageWithBody = MessageWithBody(MessageTestData.message, MessageBodyTestData.messageBodyWithAttachment)

        // When
        messageLocalDataSource.upsertMessageWithBody(userId1, messageWithBody)

        // Then
        coVerify { messageDao.insertOrUpdate(messageWithBody.message.toEntity()) }
        verifyLabelsUpdatedFor(messageWithBody)
        coVerify { messageBodyDao.insertOrUpdate(MessageBodyEntityTestData.messageBodyEntity) }
        coVerify {
            attachmentDao.insertOrUpdate(
                MessageAttachmentEntityTestData.invoice(
                    userId1,
                    messageWithBody.message.messageId
                )
            )
        }
        coVerify { messageBodyFileStorage wasNot called }
    }

    @Test
    fun `upsert message with large body over the limit inserts message in db and body in a file`() = runTest {
        // Given
        val largeMessageBody = ByteArray(501 * 1024)
        val messageWithBody = MessageWithBody(
            MessageTestData.message,
            MessageBodyTestData.messageBody.copy(body = String(largeMessageBody))
        )
        val savedMessageEntity = MessageBodyEntityTestData.messageBodyEntity.copy(body = null)
        coEvery { messageBodyFileStorage.saveMessageBody(userId1, messageWithBody.messageBody) } just runs

        // When
        messageLocalDataSource.upsertMessageWithBody(userId1, messageWithBody)

        // Then
        coVerify { messageDao.insertOrUpdate(messageWithBody.message.toEntity()) }
        verifyLabelsUpdatedFor(messageWithBody)
        coVerify { messageBodyDao.insertOrUpdate(savedMessageEntity) }
        coVerify { messageBodyFileStorage.saveMessageBody(userId1, messageWithBody.messageBody) }
    }

    @Test
    fun `upsert message with large body under the limit inserts message in db and not in a file`() = runTest {
        // Given
        val largeMessageBody = ByteArray(499 * 1024)
        val messageWithBody = MessageWithBody(
            MessageTestData.message,
            MessageBodyTestData.messageBody.copy(body = String(largeMessageBody))
        )
        val savedMessageEntity = MessageBodyEntityTestData.messageBodyEntity.copy(body = String(largeMessageBody))

        // When
        messageLocalDataSource.upsertMessageWithBody(userId1, messageWithBody)

        // Then
        coVerify { messageDao.insertOrUpdate(messageWithBody.message.toEntity()) }
        verifyLabelsUpdatedFor(messageWithBody)
        coVerify { messageBodyDao.insertOrUpdate(savedMessageEntity) }
        coVerify { messageBodyFileStorage wasNot called }
    }

    @Test(expected = MessageBodyFileWriteException::class)
    fun `should not write to the db when storing a large body in a file fails`() = runTest {
        // Given
        val largeMessageBody = ByteArray(501 * 1024)
        val messageWithBody = MessageWithBody(
            MessageTestData.message,
            MessageBodyTestData.messageBody.copy(body = String(largeMessageBody))
        )
        coEvery {
            messageBodyFileStorage.saveMessageBody(userId1, messageWithBody.messageBody)
        } throws MessageBodyFileWriteException

        // When
        messageLocalDataSource.upsertMessageWithBody(userId1, messageWithBody)

        // Then
        coVerify { messageDao wasNot called }
        coVerify { messageBodyDao wasNot called }
        coVerify { labelDao wasNot called }
    }

    @Test
    fun `add label insert labels locally`() = runTest {
        // Given
        val message = MessageTestData.message
        val labelId = LabelId("10")
        // When
        messageLocalDataSource.addLabel(UserIdTestData.userId, MessageId(message.id), labelId)
        // Then
        coVerifyOrder {
            labelDao.deleteAll(UserIdTestData.userId, listOf(message).map { MessageId(it.id) })
            labelDao.insertOrUpdate(MessageLabelEntity(UserIdTestData.userId, LabelId("0"), MessageId(message.id)))
            labelDao.insertOrUpdate(MessageLabelEntity(UserIdTestData.userId, LabelId("10"), MessageId(message.id)))
        }
    }

    @Test
    fun `add labels insert labels locally`() = runTest {
        // Given
        val message = MessageTestData.message
        val labelId = LabelId("10")
        val labelId2 = LabelId("11")
        // When
        messageLocalDataSource.addLabels(UserIdTestData.userId, MessageId(message.id), listOf(labelId, labelId2))
        // Then
        coVerifyOrder {
            labelDao.deleteAll(UserIdTestData.userId, listOf(message).map { MessageId(it.id) })
            labelDao.insertOrUpdate(MessageLabelEntity(UserIdTestData.userId, LabelId("0"), MessageId(message.id)))
            labelDao.insertOrUpdate(MessageLabelEntity(UserIdTestData.userId, LabelId("10"), MessageId(message.id)))
            labelDao.insertOrUpdate(MessageLabelEntity(UserIdTestData.userId, LabelId("11"), MessageId(message.id)))
        }
    }

    @Test
    fun `add label emits no data cached error when message does not exist locally`() = runTest {
        // Given
        val message = MessageTestData.message
        val labelId = LabelId("10")
        coEvery { messageDao.observe(UserIdTestData.userId, messageId = MessageId(message.id)) } returns flowOf(null)
        // When
        val actual = messageLocalDataSource.addLabel(UserIdTestData.userId, MessageId(message.id), labelId)
        // Then
        assertEquals(DataError.Local.NoDataCached.left(), actual)
    }

    @Test
    fun `add label ignores inserting existing labels`() = runTest {
        // Given
        val labelId = LabelId("0")
        val message = MessageTestData.message.copy(
            labelIds = listOf(labelId)
        )
        // When
        messageLocalDataSource.addLabel(UserIdTestData.userId, MessageId(message.id), labelId)
        // Then
        coVerifySequence {
            labelDao.deleteAll(UserIdTestData.userId, listOf(message.messageId))
            labelDao.insertOrUpdate(MessageLabelEntity(UserIdTestData.userId, labelId, MessageId(message.id)))
        }
    }

    @Test
    fun `remove label removes labels locally`() = runTest {
        // Given
        val message = MessageTestData.starredMessage
        val labelId = LabelId("10")
        coEvery { messageDao.observe(userId = any(), messageId = any()) } returns flowOf(
            MessageWithLabelIds(
                MessageTestData.message.toEntity(),
                listOf(LabelId("0"), LabelId("10"))
            )
        )
        // When
        messageLocalDataSource.removeLabel(UserIdTestData.userId, MessageId(message.id), labelId)
        // Then
        coVerifySequence {
            labelDao.deleteAll(UserIdTestData.userId, listOf(message).map { MessageId(it.id) })
            labelDao.insertOrUpdate(MessageLabelEntity(UserIdTestData.userId, LabelId("0"), MessageId(message.id)))
        }
    }

    @Test
    fun `remove labels removes labels locally`() = runTest {
        // Given
        val message = MessageTestData.starredMessage
        val labelId = LabelId("10")
        val labelId2 = LabelId("11")
        coEvery { messageDao.observe(userId = any(), messageId = any()) } returns flowOf(
            MessageWithLabelIds(
                MessageTestData.message.toEntity(),
                listOf(LabelId("0"), LabelId("10"), LabelId("11"))
            )
        )
        // When
        messageLocalDataSource.removeLabels(UserIdTestData.userId, MessageId(message.id), listOf(labelId, labelId2))
        // Then
        coVerifySequence {
            labelDao.deleteAll(UserIdTestData.userId, listOf(message).map { MessageId(it.id) })
            labelDao.insertOrUpdate(MessageLabelEntity(UserIdTestData.userId, LabelId("0"), MessageId(message.id)))
        }
    }

    @Test
    fun `remove label emits no data cached error when message does not exist locally`() = runTest {
        // Given
        val message = MessageTestData.message
        val labelId = LabelId("10")
        coEvery { messageDao.observe(UserIdTestData.userId, messageId = MessageId(message.id)) } returns flowOf(null)
        // When
        val actual = messageLocalDataSource.removeLabel(UserIdTestData.userId, MessageId(message.id), labelId)
        // Then
        assertEquals(DataError.Local.NoDataCached.left(), actual)
    }

    @Test
    fun `relabel removes and adds labels locally`() = runTest {
        // Given
        val messages = MessageTestData.starredMessagesWithCustomLabel
        coEvery { messageDao.observeMessages(userId1, messages.map { it.messageId }) } returns flowOf(
            listOf(
                MessageWithLabelIds(messages[0].toEntity(), messages[0].labelIds),
                MessageWithLabelIds(messages[1].toEntity(), messages[1].labelIds),
                MessageWithLabelIds(messages[2].toEntity(), messages[2].labelIds)
            )
        )

        val labelsToBeRemoved = setOf(LabelId("10"), LabelId("11"))
        val labelsToBeAdded = setOf(LabelId("12"), LabelId("13"))
        val expected = messages.map {
            it.copy(labelIds = listOf(SystemLabelId.Inbox.labelId, LabelId("12"), LabelId("13")))
        }

        // When
        val actual = messageLocalDataSource.relabelMessages(
            userId = userId1,
            messageIds = messages.map { it.messageId },
            labelIdsToRemove = labelsToBeRemoved,
            labelIdsToAdd = labelsToBeAdded
        )

        // Then
        assertEquals(expected.right(), actual)
    }

    @Test
    fun `relabel removes and adds labels locally when not all affected messages have same labels applied`() = runTest {
        // Given
        val messages = MessageTestData.starredMessagesWithPartiallySetLabels
        coEvery { messageDao.observeMessages(userId1, messages.map { it.messageId }) } returns flowOf(
            listOf(
                MessageWithLabelIds(messages[0].toEntity(), messages[0].labelIds),
                MessageWithLabelIds(messages[1].toEntity(), messages[1].labelIds),
                MessageWithLabelIds(messages[2].toEntity(), messages[2].labelIds)
            )
        )

        val labelsToBeRemoved = setOf(LabelId("10"), LabelId("11"))
        val labelsToBeAdded = setOf(LabelId("12"), LabelId("13"))
        val expected = messages.map {
            it.copy(labelIds = listOf(SystemLabelId.Inbox.labelId, LabelId("12"), LabelId("13")))
        }

        // When
        val actual = messageLocalDataSource.relabelMessages(
            userId = userId1,
            messageIds = messages.map { it.messageId },
            labelIdsToRemove = labelsToBeRemoved,
            labelIdsToAdd = labelsToBeAdded
        )

        // Then
        assertEquals(expected.right(), actual)
    }

    @Test
    fun `mark unread returns updated messages`() = runTest {
        // given
        val userId = UserIdSample.Primary
        val messageId = MessageIdSample.Invoice
        val messageId2 = MessageIdSample.SepWeatherForecast
        val message = MessageWithLabelIdsSample.Invoice.copy(
            message = MessageEntitySample.Invoice.copy(
                unread = false
            )
        )
        val message2 = MessageWithLabelIdsSample.SepWeatherForecast.copy(
            message = MessageEntitySample.SepWeatherForecast.copy(
                unread = false
            )
        )
        val updatedMessage = message.copy(
            message = MessageEntitySample.Invoice.copy(
                unread = true
            )
        )
        val updatedMessage2 = message2.copy(
            message = MessageEntitySample.SepWeatherForecast.copy(
                unread = true
            )
        )
        val expected = listOf(updatedMessage, updatedMessage2).map { it.toMessage() }.right()
        every { messageDao.observeMessages(userId, listOf(messageId, messageId2)) } returns flowOf(
            listOf(message, message2)
        )

        // when
        val result = messageLocalDataSource.markUnread(userId, listOf(messageId, messageId2))

        // then
        assertEquals(expected, result)
    }

    @Test
    fun `mark unread returns error if message not found`() = runTest {
        // given
        val userId = UserIdSample.Primary
        val messageId = MessageIdSample.Invoice
        val error = DataErrorSample.NoCache.left()
        every { messageDao.observeMessages(userId, listOf(messageId)) } returns flowOf(emptyList())

        // when
        val result = messageLocalDataSource.markUnread(userId, listOf(messageId))

        // then
        assertEquals(error, result)
    }

    @Test
    fun `should mark unread last read messages from conversations with the current label`() = runTest {
        // Given
        val userId = userId1
        val contextLabelId = MailLabelId.System.Inbox.labelId
        val conversationId = ConversationIdSample.AlphaAppFeedback
        val messages = listOf(
            MessageWithLabelIds(
                MessageSample.AlphaAppArchivedFeedback.copy(unread = false).toEntity(), listOf(LabelIdSample.Archive)
            ),
            MessageWithLabelIds(
                MessageSample.AlphaAppInfoRequest.copy(unread = false).toEntity(), listOf(LabelIdSample.Inbox)
            ),
            MessageWithLabelIds(
                MessageSample.AlphaAppQAReport.copy(unread = false).toEntity(), listOf(LabelIdSample.Inbox)
            ),
            MessageWithLabelIds(
                MessageSample.AlphaAppArchivedFeedback.copy(unread = false).toEntity(), listOf(LabelIdSample.Archive)
            ),
            MessageWithLabelIds(
                MessageSample.AlphaAppArchivedFeedback.copy(unread = true).toEntity(), listOf(LabelIdSample.Archive)
            )
        )
        val expected = listOf(MessageSample.AlphaAppQAReport.copy(unread = true)).right()
        coEvery {
            messageDao.observeAllOrderByTimeAsc(userId, conversationId = conversationId)
        } returns flowOf(messages)
        coEvery {
            messageDao.observeMessages(userId, listOf(MessageSample.AlphaAppQAReport.messageId))
        } returns flowOf(
            listOf(
                MessageWithLabelIds(
                    MessageSample.AlphaAppQAReport.copy(unread = false).toEntity(), listOf(LabelIdSample.Inbox)
                )
            )
        )

        // When
        val actual = messageLocalDataSource.markUnreadLastReadMessageInConversations(
            userId,
            listOf(conversationId),
            contextLabelId
        )

        // Then
        assertEquals(expected, actual)
    }

    @Test
    fun `mark read returns updated message`() = runTest {
        // given
        val userId = UserIdSample.Primary
        val messageId = MessageIdSample.Invoice
        val message = MessageWithLabelIdsSample.Invoice.copy(
            message = MessageEntitySample.Invoice.copy(
                unread = true
            )
        )
        val updatedMessage = message.copy(
            message = MessageEntitySample.Invoice.copy(
                unread = false
            )
        )
        every { messageDao.observeMessages(userId, listOf(messageId)) } returns flowOf(listOf(message))

        // when
        val result = messageLocalDataSource.markRead(userId, listOf(messageId))

        // then
        assertEquals(listOf(updatedMessage.toMessage()).right(), result)
    }

    @Test
    fun `mark read returns error if message not found`() = runTest {
        // given
        val userId = UserIdSample.Primary
        val messageId = listOf(MessageIdSample.Invoice)
        val error = DataErrorSample.NoCache.left()
        every { messageDao.observeMessages(userId, messageId) } returns flowOf(emptyList())

        // when
        val result = messageLocalDataSource.markRead(userId, messageId)

        // then
        assertEquals(error, result)
    }

    @Test
    fun `should mark read all messages from given conversations`() = runTest {
        // Given
        val conversationId = ConversationIdSample.AlphaAppFeedback
        val messageIds = listOf(
            MessageSample.AlphaAppInfoRequest.messageId,
            MessageSample.AlphaAppQAReport.messageId,
            MessageSample.AlphaAppArchivedFeedback.messageId
        )
        val expected = listOf(
            MessageSample.AlphaAppInfoRequest.copy(unread = false),
            MessageSample.AlphaAppQAReport.copy(unread = false),
            MessageSample.AlphaAppArchivedFeedback.copy(unread = false)
        ).right()
        coEvery { messageDao.getMessageIdsInConversations(userId1, listOf(conversationId)) } returns messageIds
        coEvery { messageDao.observeMessages(userId1, messageIds) } returns flowOf(
            listOf(
                MessageWithLabelIds(
                    MessageSample.AlphaAppInfoRequest.copy(unread = true).toEntity(), listOf(LabelIdSample.Inbox)
                ),
                MessageWithLabelIds(
                    MessageSample.AlphaAppQAReport.copy(unread = false).toEntity(), listOf(LabelIdSample.Inbox)
                ),
                MessageWithLabelIds(
                    MessageSample.AlphaAppArchivedFeedback.copy(unread = true).toEntity(), listOf(LabelIdSample.Archive)
                )
            )
        )

        // When
        val actual = messageLocalDataSource.markMessagesInConversationsRead(userId1, listOf(conversationId))

        // Then
        assertEquals(expected, actual)
    }

    @Test
    fun `Should return true if the message is read`() = runTest {
        // Given
        val sample = MessageEntitySample.Invoice
        val messageId = sample.messageId
        every { messageDao.observe(userId1, messageId) } returns flowOf(
            MessageWithLabelIds(
                sample.copy(unread = false),
                listOf(LabelIdSample.Inbox)
            )
        )

        // When
        val result = messageLocalDataSource.isMessageRead(userId1, messageId).getOrNull()

        // Then
        assertEquals(true, result)
    }

    @Test
    fun `Should return false if the message is not read`() = runTest {
        // Given
        val sample = MessageEntitySample.Invoice
        val messageId = sample.messageId
        every { messageDao.observe(userId1, messageId) } returns flowOf(
            MessageWithLabelIds(
                sample.copy(unread = true),
                listOf(LabelIdSample.Inbox)
            )
        )

        // When
        val result = messageLocalDataSource.isMessageRead(userId1, messageId).getOrNull()

        // Then
        assertEquals(false, result)
    }

    @Test
    fun `Should return error if the message is not cached`() = runTest {
        // Given
        val messageId = MessageId(UUID.randomUUID().toString())
        every { messageDao.observe(userId1, messageId) } returns flowOf(null)

        // When
        val result = messageLocalDataSource.isMessageRead(userId1, messageId)

        // Then
        assertEquals(DataError.Local.NoDataCached.left(), result)
    }

    @Test
    fun `should add a label to all messages in the given conversations`() = runTest {
        // Given
        val conversationIds = listOf(ConversationId("conversation1"), ConversationId("conversation2"))
        val messageIds = listOf(MessageId("message1"), MessageId("message2"), MessageId("message3"))
        val labelToAdd = LabelId("labelToAdd")
        val messagesWithLabelIds = listOf(
            MessageWithLabelIdsSample.build(labelIds = listOf(LabelId("1"))),
            MessageWithLabelIdsSample.build(labelIds = emptyList()),
            MessageWithLabelIdsSample.build(labelIds = listOf(LabelId("1"), LabelId("2")))
        )
        val expected = messagesWithLabelIds.map { it.toMessage().copy(labelIds = it.labelIds + labelToAdd) }.right()
        coEvery {
            messageDao.getMessageIdsInConversations(userId1, conversationIds)
        } returns messageIds
        coEvery {
            messageDao.observeMessages(userId1, messageIds)
        } returns flowOf(messagesWithLabelIds)

        // When
        val actual = messageLocalDataSource.relabelMessagesInConversations(
            userId = userId1,
            conversationIds = conversationIds,
            labelIdsToAdd = setOf(labelToAdd)
        )

        // Then
        assertEquals(expected, actual)
    }

    @Test
    fun `should update message id of stored attachments when message id changes`() = runTest {
        // Given
        val oldMessageId = MessageIdSample.Invoice
        val newMessageId = MessageId(oldMessageId.id + "_new")
        val newConversationId = ConversationIdSample.build()

        // When
        messageLocalDataSource.updateDraftRemoteIds(userId1, oldMessageId, newMessageId, newConversationId)

        // Then
        coVerifyOrder {
            messageDao.updateDraftRemoteIds(userId1, oldMessageId, newMessageId, newConversationId)
            messageBodyFileStorage.updateFileNameForMessageBody(userId1, oldMessageId, newMessageId)
            attachmentFileStorage.updateParentFolderForAttachments(userId1, oldMessageId, newMessageId)
        }
    }

    @Test
    fun `should remove label from all messages in the given conversations`() = runTest {
        // Given
        val conversationIds = listOf(ConversationId("conversation1"), ConversationId("conversation2"))
        val messageIds = listOf(MessageId("message1"), MessageId("message2"), MessageId("message3"))
        val labelToRemove = LabelId("labelToRemove")
        val messagesWithLabelIds = listOf(
            MessageWithLabelIdsSample.build(labelIds = listOf(LabelId("1"), labelToRemove)),
            MessageWithLabelIdsSample.build(labelIds = listOf(labelToRemove)),
            MessageWithLabelIdsSample.build(labelIds = listOf(LabelId("1"), LabelId("2"), labelToRemove))
        )
        val expected = messagesWithLabelIds.map { it.toMessage().copy(labelIds = it.labelIds - labelToRemove) }.right()
        coEvery {
            messageDao.getMessageIdsInConversations(userId1, conversationIds)
        } returns messageIds
        coEvery {
            messageDao.observeMessages(userId1, messageIds)
        } returns flowOf(messagesWithLabelIds)

        // When
        val actual = messageLocalDataSource.relabelMessagesInConversations(
            userId = userId1,
            conversationIds = conversationIds,
            labelIdsToRemove = setOf(labelToRemove)
        )

        // Then
        assertEquals(expected, actual)
    }
    private fun verifyLabelsUpdatedFor(messageWithBody: MessageWithBody) {
        coVerifyOrder {
            labelDao.deleteAll(messageWithBody.message.userId, listOf(messageWithBody.message.messageId))
            val spamLabelEntity = MessageLabelEntity(
                messageWithBody.message.userId,
                messageWithBody.message.labelIds.first(),
                messageWithBody.message.messageId
            )
            labelDao.insertOrUpdate(spamLabelEntity)
        }
    }
}
