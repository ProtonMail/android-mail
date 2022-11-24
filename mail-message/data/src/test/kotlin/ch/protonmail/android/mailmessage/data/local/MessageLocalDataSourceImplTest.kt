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

import app.cash.turbine.test
import arrow.core.left
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailmessage.data.getMessage
import ch.protonmail.android.mailmessage.data.getMessageWithLabels
import ch.protonmail.android.mailmessage.data.local.dao.MessageDao
import ch.protonmail.android.mailmessage.data.local.dao.MessageLabelDao
import ch.protonmail.android.mailmessage.data.local.entity.MessageLabelEntity
import ch.protonmail.android.mailmessage.data.local.relation.MessageWithLabelIds
import ch.protonmail.android.mailmessage.domain.entity.MessageId
import ch.protonmail.android.mailpagination.data.local.dao.PageIntervalDao
import ch.protonmail.android.mailpagination.data.local.upsertPageInterval
import ch.protonmail.android.mailpagination.domain.model.OrderDirection
import ch.protonmail.android.mailpagination.domain.model.PageItemType
import ch.protonmail.android.mailpagination.domain.model.PageKey
import ch.protonmail.android.testdata.message.MessageTestData
import ch.protonmail.android.testdata.user.UserIdTestData
import io.mockk.coEvery
import io.mockk.coInvoke
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.coVerifySequence
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import me.proton.core.label.domain.entity.LabelId
import org.junit.Before
import org.junit.Test
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
    private val labelDao = mockk<MessageLabelDao>(relaxUnitFun = true)
    private val pageIntervalDao = mockk<PageIntervalDao>(relaxUnitFun = true)

    private val db = mockk<MessageDatabase>(relaxed = true) {
        every { messageDao() } returns messageDao
        every { messageLabelDao() } returns labelDao
        every { pageIntervalDao() } returns pageIntervalDao
        coEvery { inTransaction(captureCoroutine<suspend () -> Any>()) } coAnswers {
            coroutine<suspend () -> Any>().coInvoke()
        }
    }

    private lateinit var messageLocalDataSource: MessageLocalDataSourceImpl

    @Before
    fun setUp() {
        mockkStatic(PageIntervalDao::upsertPageInterval)
        messageLocalDataSource = MessageLocalDataSourceImpl(db)
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
    fun `deleteAllMessages call messageDao and pageIntervalDao deleteAll`() = runTest {
        // When
        messageLocalDataSource.deleteAllMessages(userId1)

        // Then
        coVerify(exactly = 1) { db.inTransaction(any()) }
        coVerify(exactly = 1) { messageDao.deleteAll(userId1) }
        coVerify(exactly = 1) { pageIntervalDao.deleteAll(userId1, PageItemType.Message) }
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
        val message = MessageTestData.message
        val labelId = LabelId("0")
        // When
        messageLocalDataSource.addLabel(UserIdTestData.userId, MessageId(message.id), labelId)
        // Then
        coVerifySequence {
            labelDao.deleteAll(UserIdTestData.userId, listOf(message).map { MessageId(it.id) })
            labelDao.insertOrUpdate(MessageLabelEntity(UserIdTestData.userId, LabelId("0"), MessageId(message.id)))
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
}
