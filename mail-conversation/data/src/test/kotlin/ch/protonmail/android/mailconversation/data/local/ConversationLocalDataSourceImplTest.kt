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

package ch.protonmail.android.mailconversation.data.local

import app.cash.turbine.test
import ch.protonmail.android.mailcommon.domain.model.ConversationId
import ch.protonmail.android.mailconversation.data.getConversation
import ch.protonmail.android.mailconversation.data.getConversationWithLabels
import ch.protonmail.android.mailconversation.data.local.dao.ConversationDao
import ch.protonmail.android.mailconversation.data.local.dao.ConversationLabelDao
import ch.protonmail.android.mailpagination.data.local.dao.PageIntervalDao
import ch.protonmail.android.mailpagination.data.local.upsertPageInterval
import ch.protonmail.android.mailpagination.domain.entity.OrderDirection
import ch.protonmail.android.mailpagination.domain.entity.PageItemType
import ch.protonmail.android.mailpagination.domain.entity.PageKey
import ch.protonmail.android.testdata.conversation.ConversationWithContextTestData.getConversationWithContext
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coInvoke
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
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

class ConversationLocalDataSourceImplTest {

    private val userId1 = UserId("1")
    private val userId2 = UserId("2")

    private val conversationDao = mockk<ConversationDao> {
        coEvery { this@mockk.insertOrUpdate(entities = anyVararg()) } just Runs
        coEvery { this@mockk.insertOrIgnore(entities = anyVararg()) } just Runs
        coEvery { this@mockk.deleteAll(any()) } just Runs
    }
    private val labelDao = mockk<ConversationLabelDao> {
        coEvery { this@mockk.insertOrUpdate(entities = anyVararg()) } just Runs
        coEvery { this@mockk.insertOrIgnore(entities = anyVararg()) } just Runs
        coEvery { this@mockk.deleteAll(any(), any()) } just Runs
    }
    private val pageIntervalDao = mockk<PageIntervalDao>(relaxed = true)

    private val db = mockk<ConversationDatabase>(relaxed = true) {
        every { conversationDao() } returns conversationDao
        every { conversationLabelDao() } returns labelDao
        every { pageIntervalDao() } returns pageIntervalDao
        coEvery { inTransaction(captureCoroutine<suspend () -> Any>()) } coAnswers {
            coroutine<suspend () -> Any>().coInvoke()
        }
    }

    private lateinit var conversationLocalDataSource: ConversationLocalDataSourceImpl

    @Before
    fun setUp() {
        mockkStatic(PageIntervalDao::upsertPageInterval)
        conversationLocalDataSource = ConversationLocalDataSourceImpl(db)
    }

    @Test
    fun `upsert conversations and corresponding labels, from arbitrary users`() = runTest {
        // Given
        val conversations = listOf(
            // userId1
            getConversation(userId1, "1", time = 1000, labelIds = emptyList()),
            getConversation(userId1, "2", time = 2000, labelIds = listOf("4")),
            getConversation(userId1, "3", time = 3000, labelIds = listOf("0", "1")),
            // userId2
            getConversation(userId2, "1", time = 1000, labelIds = listOf("3"))
        )
        val user1conversationIds =
            listOf(ConversationId("1"), ConversationId("2"), ConversationId("3"))
        val user2conversationIds = listOf(ConversationId("1"))

        // When
        conversationLocalDataSource.upsertConversations(conversations)

        // Then
        coVerify { db.inTransaction(any()) }
        coVerify { labelDao.deleteAll(userId1, user1conversationIds) }
        coVerify { labelDao.deleteAll(userId2, user2conversationIds) }
        coVerify(exactly = 1) { conversationDao.insertOrUpdate(entities = anyVararg()) }
        coVerify(exactly = 4) { labelDao.insertOrUpdate(entities = anyVararg()) }
        coVerify(exactly = 0) { pageIntervalDao.upsertPageInterval(any(), any(), any(), any()) }
    }

    @Test
    fun `with userId1 and pageKey, upsert conversations and corresponding interval`() = runTest {
        // Given
        val pageKey = PageKey(orderDirection = OrderDirection.Ascending, size = 3)
        val conversations = listOf(
            // userId1
            getConversationWithContext(userId1, "1", time = 1000, labelIds = emptyList()),
            getConversationWithContext(userId1, "2", time = 2000, labelIds = listOf("4")),
            getConversationWithContext(userId1, "3", time = 3000, labelIds = listOf("0", "1"))
        )
        val user1conversationIds =
            listOf(ConversationId("1"), ConversationId("2"), ConversationId("3"))

        // When
        conversationLocalDataSource.upsertConversations(userId1, pageKey, conversations)

        // Then
        coVerify { db.inTransaction(any()) }
        coVerify { labelDao.deleteAll(userId1, user1conversationIds) }
        coVerify(exactly = 1) { conversationDao.insertOrUpdate(entities = anyVararg()) }
        coVerify(exactly = 3) { labelDao.insertOrUpdate(entities = anyVararg()) }
        coVerify(exactly = 1) { pageIntervalDao.upsertPageInterval(any(), any(), any(), any()) }
    }

    @Test
    fun `deleteAllConversations call conversationDao and pageIntervalDao deleteAll`() = runTest {
        // When
        conversationLocalDataSource.deleteAllConversations(userId1)

        // Then
        coVerify(exactly = 1) { db.inTransaction(any()) }
        coVerify(exactly = 1) { conversationDao.deleteAll(userId1) }
        coVerify(exactly = 1) { pageIntervalDao.deleteAll(userId1, PageItemType.Conversation) }
    }

    @Test
    fun `markAsStale call pageIntervalDao deleteAll`() = runTest {
        // Given
        val labelId = LabelId("1")

        // When
        conversationLocalDataSource.markAsStale(userId1, labelId)

        // Then
        coVerify(exactly = 1) {
            pageIntervalDao.deleteAll(
                userId1,
                PageItemType.Conversation,
                labelId
            )
        }
    }

    @Test
    fun `observe conversation returns conversation from db when existing`() = runTest {
        // Given
        val conversationId = ConversationId("convId1")
        val conversationWithLabels = getConversationWithLabels(userId1, conversationId.id)
        val conversation = conversationWithLabels.toConversation()
        coEvery { conversationDao.observe(userId1, conversationId) } returns flowOf(conversationWithLabels)
        // When
        conversationLocalDataSource.observeConversation(userId1, conversationId).test {
            // Then
            assertEquals(conversation, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `observe conversation returns null conversation when not existing in db`() = runTest {
        // Given
        val conversationId = ConversationId("convId1")
        coEvery { conversationDao.observe(userId1, conversationId) } returns flowOf(null)
        // When
        conversationLocalDataSource.observeConversation(userId1, conversationId).test {
            // Then
            assertNull(awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `upsert conversation inserts or updates conversation in the DB`() = runTest {
        // Given
        val conversationId = ConversationId("convId1")
        val conversation = getConversation(userId1, conversationId.id)
        // When
        conversationLocalDataSource.upsertConversation(userId1, conversation)
        // Then
        coVerify { conversationDao.insertOrUpdate(conversation.toEntity()) }
    }
}
