/*
 * Copyright (c) 2021 Proton Technologies AG
 * This file is part of Proton Technologies AG and ProtonMail.
 *
 * ProtonMail is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ProtonMail is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with ProtonMail.  If not, see <https://www.gnu.org/licenses/>.
 */

package ch.protonmail.android.mailmessage.data.local;

import ch.protonmail.android.mailpagination.data.local.dao.PageIntervalDao
import ch.protonmail.android.mailpagination.data.local.upsertPageInterval
import ch.protonmail.android.mailpagination.domain.entity.OrderDirection
import ch.protonmail.android.mailpagination.domain.entity.PageItemType
import ch.protonmail.android.mailpagination.domain.entity.PageKey
import ch.protonmail.android.mailmessage.data.getMessage
import ch.protonmail.android.mailmessage.data.local.dao.MessageDao
import ch.protonmail.android.mailmessage.data.local.dao.MessageLabelDao
import ch.protonmail.android.mailmessage.domain.entity.MessageId
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coInvoke
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import me.proton.core.label.domain.entity.LabelId
import org.junit.Before
import org.junit.Test

class MessageLocalDataSourceImplTest {

    private val userId1 = UserId("1")
    private val userId2 = UserId("2")

    private val messageDao = mockk<MessageDao> {
        coEvery { this@mockk.insertOrUpdate(entities = anyVararg()) } just Runs
        coEvery { this@mockk.insertOrIgnore(entities = anyVararg()) } just Runs
        coEvery { this@mockk.deleteAll(any()) } just Runs
    }
    private val labelDao = mockk<MessageLabelDao>{
        coEvery { this@mockk.insertOrUpdate(entities = anyVararg()) } just Runs
        coEvery { this@mockk.insertOrIgnore(entities = anyVararg()) } just Runs
        coEvery { this@mockk.deleteAll(any(), any()) } just Runs
    }
    private val pageIntervalDao = mockk<PageIntervalDao>(relaxed = true)

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
            getMessage(userId2, "1", time = 1000, labelIds = listOf("3")),
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
            getMessage(userId1, "3", time = 3000, labelIds = listOf("0", "1")),
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
}
