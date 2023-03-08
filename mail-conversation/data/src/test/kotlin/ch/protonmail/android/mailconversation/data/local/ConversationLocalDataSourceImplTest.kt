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
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.ConversationId
import ch.protonmail.android.mailcommon.domain.sample.ConversationIdSample
import ch.protonmail.android.mailcommon.domain.sample.DataErrorSample
import ch.protonmail.android.mailconversation.data.TestConversationLabel
import ch.protonmail.android.mailconversation.data.getConversation
import ch.protonmail.android.mailconversation.data.getConversationWithLabels
import ch.protonmail.android.mailconversation.data.local.dao.ConversationDao
import ch.protonmail.android.mailconversation.data.local.dao.ConversationLabelDao
import ch.protonmail.android.mailconversation.data.local.entity.ConversationLabelEntity
import ch.protonmail.android.mailconversation.data.sample.ConversationEntitySample
import ch.protonmail.android.mailconversation.data.sample.ConversationWithLabelsSample
import ch.protonmail.android.mailconversation.domain.sample.ConversationLabelSample
import ch.protonmail.android.mailconversation.domain.sample.ConversationSample
import ch.protonmail.android.maillabel.domain.model.MailLabelId
import ch.protonmail.android.mailpagination.data.local.dao.PageIntervalDao
import ch.protonmail.android.mailpagination.data.local.upsertPageInterval
import ch.protonmail.android.mailpagination.domain.model.OrderDirection
import ch.protonmail.android.mailpagination.domain.model.PageItemType
import ch.protonmail.android.mailpagination.domain.model.PageKey
import ch.protonmail.android.testdata.conversation.ConversationTestData
import ch.protonmail.android.testdata.conversation.ConversationWithContextTestData
import ch.protonmail.android.testdata.conversation.ConversationWithLabelTestData
import ch.protonmail.android.testdata.user.UserIdTestData.userId
import ch.protonmail.android.testdata.user.UserIdTestData.userId1
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coInvoke
import io.mockk.coVerify
import io.mockk.coVerifySequence
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import me.proton.core.label.domain.entity.LabelId
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ConversationLocalDataSourceImplTest {

    private val contextLabelId = MailLabelId.System.Archive.labelId

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
            // userId
            getConversation(userId, "1", time = 1000, labelIds = emptyList()),
            getConversation(userId, "2", time = 2000, labelIds = listOf("4")),
            getConversation(userId, "3", time = 3000, labelIds = listOf("0", "1")),
            // userId1
            getConversation(userId1, "1", time = 1000, labelIds = listOf("3"))
        )
        val user1conversationIds =
            listOf(ConversationId("1"), ConversationId("2"), ConversationId("3"))
        val user2conversationIds = listOf(ConversationId("1"))

        // When
        conversationLocalDataSource.upsertConversations(conversations)

        // Then
        coVerify { db.inTransaction(any()) }
        coVerify { labelDao.deleteAll(userId, user1conversationIds) }
        coVerify { labelDao.deleteAll(userId1, user2conversationIds) }
        coVerify(exactly = 1) { conversationDao.insertOrUpdate(entities = anyVararg()) }
        coVerify(exactly = 4) { labelDao.insertOrUpdate(entities = anyVararg()) }
        coVerify(exactly = 0) { pageIntervalDao.upsertPageInterval(any(), any(), any(), any()) }
    }

    @Test
    fun `with userId and pageKey, upsert conversations and corresponding interval`() = runTest {
        // Given
        val pageKey = PageKey(orderDirection = OrderDirection.Ascending, size = 3)
        val conversations = listOf(
            // userId
            ConversationWithContextTestData.conversation1NoLabels,
            ConversationWithContextTestData.conversation2Labeled,
            ConversationWithContextTestData.conversation3Labeled
        )
        val user1conversationIds =
            listOf(ConversationId("1"), ConversationId("2"), ConversationId("3"))

        // When
        conversationLocalDataSource.upsertConversations(userId, pageKey, conversations)

        // Then
        coVerify { db.inTransaction(any()) }
        coVerify { labelDao.deleteAll(userId, user1conversationIds) }
        coVerify(exactly = 1) { conversationDao.insertOrUpdate(entities = anyVararg()) }
        coVerify(exactly = 3) { labelDao.insertOrUpdate(entities = anyVararg()) }
        coVerify(exactly = 1) { pageIntervalDao.upsertPageInterval(any(), any(), any(), any()) }
    }

    @Test
    fun `deleteAllConversations call conversationDao and pageIntervalDao deleteAll`() = runTest {
        // When
        conversationLocalDataSource.deleteAllConversations(userId)

        // Then
        coVerify(exactly = 1) { db.inTransaction(any()) }
        coVerify(exactly = 1) { conversationDao.deleteAll(userId) }
        coVerify(exactly = 1) { pageIntervalDao.deleteAll(userId, PageItemType.Conversation) }
    }

    @Test
    fun `markAsStale call pageIntervalDao deleteAll`() = runTest {
        // Given
        val labelId = LabelId("1")

        // When
        conversationLocalDataSource.markAsStale(userId, labelId)

        // Then
        coVerify(exactly = 1) {
            pageIntervalDao.deleteAll(
                userId,
                PageItemType.Conversation,
                labelId
            )
        }
    }

    @Test
    fun `observe conversation returns conversation from db when existing`() = runTest {
        // Given
        val conversationId = ConversationId("convId1")
        val conversationWithLabels = getConversationWithLabels(userId, conversationId.id)
        val conversation = conversationWithLabels.toConversation()
        coEvery { conversationDao.observe(userId, conversationId) } returns flowOf(conversationWithLabels)
        // When
        conversationLocalDataSource.observeConversation(userId, conversationId).test {
            // Then
            assertEquals(conversation, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `observe conversation returns null conversation when not existing in db`() = runTest {
        // Given
        val conversationId = ConversationId("convId1")
        coEvery { conversationDao.observe(userId, conversationId) } returns flowOf(null)
        // When
        conversationLocalDataSource.observeConversation(userId, conversationId).test {
            // Then
            assertNull(awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `upsert conversation inserts or updates conversation and labels in the DB`() = runTest {
        // Given
        val conversationId = ConversationId("convId1")
        val conversation = getConversation(userId, conversationId.id, labelIds = listOf("0", "10"))

        // When
        conversationLocalDataSource.upsertConversation(userId, conversation)

        // Then
        val expectedLabels = listOf(
            TestConversationLabel.createConversationLabel(conversationId, conversation, LabelId("0")),
            TestConversationLabel.createConversationLabel(conversationId, conversation, LabelId("10"))
        )
        coVerify { conversationDao.insertOrUpdate(conversation.toEntity()) }
        coVerify { labelDao.deleteAll(userId, listOf(conversation.conversationId)) }
        coVerify { labelDao.insertOrUpdate(expectedLabels.first()) }
        coVerify { labelDao.insertOrUpdate(expectedLabels.last()) }
    }

    @Test
    fun `add label insert conversation labels locally`() = runTest {
        // Given
        coEvery {
            conversationDao.observe(
                userId,
                ConversationId(ConversationTestData.RAW_CONVERSATION_ID)
            )
        } returns flowOf(
            ConversationWithLabelTestData.conversationWithLabel(
                userId = userId,
                conversationId = ConversationId(ConversationTestData.RAW_CONVERSATION_ID)
            )
        )
        val conversation = ConversationTestData.conversation
        val labelId = LabelId("10")

        // When
        conversationLocalDataSource.addLabel(userId, conversation.conversationId, labelId)

        // Then
        val slot = slot<ConversationLabelEntity>()
        coVerifySequence {
            labelDao.deleteAll(userId, listOf(conversation.conversationId))
            labelDao.insertOrUpdate(capture(slot))
            labelDao.insertOrUpdate(capture(slot))
        }
    }

    @Test
    fun `add labels insert all passed conversation labels locally`() = runTest {
        // Given
        coEvery {
            conversationDao.observe(
                userId,
                ConversationId(ConversationTestData.RAW_CONVERSATION_ID)
            )
        } returns flowOf(
            ConversationWithLabelTestData.conversationWithLabel(
                userId = userId,
                conversationId = ConversationId(ConversationTestData.RAW_CONVERSATION_ID)
            )
        )
        val conversation = ConversationTestData.conversation
        val labelList = listOf(LabelId("10"), LabelId("11"))

        // When
        conversationLocalDataSource.addLabels(userId, conversation.conversationId, labelList)

        // Then
        val list = mutableListOf<ConversationLabelEntity>()
        coVerifySequence {
            labelDao.deleteAll(userId, listOf(conversation.conversationId))
            labelDao.insertOrUpdate(capture(list))
            labelDao.insertOrUpdate(capture(list))
            labelDao.insertOrUpdate(capture(list))
        }
        assertEquals(3, list.size)
        assertEquals("0", list[0].labelId.id)
        assertEquals("10", list[1].labelId.id)
        assertEquals("11", list[2].labelId.id)
    }

    @Test
    fun `remove label removes conversation labels locally`() = runTest {
        // Given
        coEvery {
            conversationDao.observe(
                userId,
                ConversationId(ConversationTestData.RAW_CONVERSATION_ID)
            )
        } returns flowOf(
            ConversationWithLabelTestData.conversationWithLabel(
                userId = userId,
                conversationId = ConversationId(ConversationTestData.RAW_CONVERSATION_ID)
            )
        )
        val conversation = ConversationTestData.conversation
        val labelId = LabelId("0")

        // When
        conversationLocalDataSource.removeLabel(userId, conversation.conversationId, labelId)

        // Then
        coVerifySequence {
            labelDao.deleteAll(userId, listOf(conversation.conversationId))
        }
    }

    @Test
    fun `remove labels removes all passed conversation labels locally`() = runTest {
        // Given
        coEvery {
            conversationDao.observe(
                userId,
                ConversationId(ConversationTestData.RAW_CONVERSATION_ID)
            )
        } returns flowOf(
            ConversationWithLabelTestData.conversationWithMultipleLabels(
                userId = userId,
                conversationId = ConversationId(ConversationTestData.RAW_CONVERSATION_ID)
            )
        )
        val conversation = ConversationTestData.conversation
        val labelList = listOf(
            LabelId("0"),
            LabelId("1")
        )

        // When
        conversationLocalDataSource.removeLabels(userId, conversation.conversationId, labelList)

        // Then
        coVerifySequence {
            labelDao.deleteAll(userId, listOf(conversation.conversationId))
            labelDao.insertOrUpdate(
                entities = arrayOf(
                    ConversationLabelEntity(
                        userId = userId,
                        conversationId = conversation.conversationId,
                        labelId = LabelId("2"),
                        contextTime = 1000,
                        contextSize = 0,
                        contextNumMessages = 0,
                        contextNumUnread = 0,
                        contextNumAttachments = 0
                    )
                )
            )
        }
    }

    @Test
    fun `mark unread increments the conversation's overall unread count`() = runTest {
        // given
        val conversationId = ConversationIdSample.WeatherForecast
        val conversation = ConversationWithLabelsSample.WeatherForecast.copy(
            conversation = ConversationEntitySample.WeatherForecast.copy(
                numMessages = 3,
                numUnread = 2
            )
        )
        val updatedConversation = ConversationSample.WeatherForecast.copy(
            numMessages = 3,
            numUnread = 3
        )
        every { conversationDao.observe(userId, conversationId) } returns flowOf(conversation)

        // when
        val result = conversationLocalDataSource.markUnread(
            userId,
            ConversationIdSample.WeatherForecast,
            contextLabelId
        )

        // then
        assertEquals(updatedConversation.right(), result)
    }

    @Test
    fun `mark unread increments the unread count for the given context label only`() = runTest {
        // given
        val contextLabelId = MailLabelId.System.Inbox.labelId
        val conversationId = ConversationIdSample.AlphaAppFeedback
        val conversation = ConversationWithLabelsSample.AlphaAppFeedback.copy(
            conversation = ConversationEntitySample.AlphaAppFeedback.copy(
                numMessages = 2,
                numUnread = 0
            )
        )
        val updatedLabels = listOf(
            ConversationLabelSample.AlphaAppFeedback.AllMail,
            ConversationLabelSample.AlphaAppFeedback.Inbox.copy(contextNumUnread = 1),
            ConversationLabelSample.AlphaAppFeedback.Archive
        )
        val updatedConversation = ConversationSample.AlphaAppFeedback.copy(
            numMessages = 2,
            numUnread = 1,
            labels = updatedLabels
        )
        every { conversationDao.observe(userId, conversationId) } returns flowOf(conversation)

        // when
        val result = conversationLocalDataSource.markUnread(
            userId,
            ConversationIdSample.AlphaAppFeedback,
            contextLabelId
        )

        // then
        assertEquals(updatedConversation.right(), result)
    }

    @Test
    fun `mark unread doesn't increase unread count when inconsistent`() = runTest {
        // given
        val conversationId = ConversationIdSample.WeatherForecast
        val conversation = ConversationWithLabelsSample.WeatherForecast.copy(
            conversation = ConversationEntitySample.WeatherForecast.copy(
                numMessages = 2,
                numUnread = 2
            )
        )
        val updatedConversation = ConversationSample.WeatherForecast.copy(
            numMessages = 2,
            numUnread = 2
        )
        every { conversationDao.observe(userId, conversationId) } returns flowOf(conversation)

        // when
        val result = conversationLocalDataSource.markUnread(
            userId,
            ConversationIdSample.WeatherForecast,
            contextLabelId
        )

        // then
        assertEquals(updatedConversation.right(), result)
    }

    @Test
    fun `mark unread returns error if conversation not found`() = runTest {
        // given
        val conversationId = ConversationIdSample.WeatherForecast
        val error = DataErrorSample.NoCache.left()
        every { conversationLocalDataSource.observeConversation(userId, conversationId) } returns flowOf(null)

        // when
        val result = conversationLocalDataSource.markUnread(userId, conversationId, contextLabelId)

        // then
        assertEquals(error, result)
    }

    @Test
    fun `rollback mark unread decrements the conversation's overall unread count`() = runTest {
        // given
        val conversationId = ConversationIdSample.WeatherForecast
        val conversation = ConversationWithLabelsSample.WeatherForecast.copy(
            conversation = ConversationEntitySample.WeatherForecast.copy(
                numUnread = 9
            )
        )
        val updatedConversation = conversation.copy(
            conversation = ConversationEntitySample.WeatherForecast.copy(
                numUnread = 8
            )
        )
        every { conversationDao.observe(userId, conversationId) } returns flowOf(conversation)

        // when
        val result = conversationLocalDataSource.rollbackMarkUnread(userId, conversationId, contextLabelId)

        // then
        assertEquals(updatedConversation.toConversation().right(), result)
    }

    @Test
    fun `rollback mark unread decrements the unread count for the given context label only`() = runTest {
        // given
        val contextLabelId = MailLabelId.System.Inbox.labelId
        val conversationId = ConversationIdSample.AlphaAppFeedback
        val initialLabels = listOf(
            ConversationLabelSample.AlphaAppFeedback.AllMail,
            ConversationLabelSample.AlphaAppFeedback.Inbox.copy(contextNumUnread = 1),
            ConversationLabelSample.AlphaAppFeedback.Archive.copy(contextNumUnread = 1)
        )
        val conversation = ConversationWithLabelsSample.AlphaAppFeedback.copy(
            conversation = ConversationEntitySample.AlphaAppFeedback.copy(
                numMessages = 2,
                numUnread = 2
            ),
            labels = initialLabels
        )
        val updatedLabels = listOf(
            ConversationLabelSample.AlphaAppFeedback.AllMail,
            ConversationLabelSample.AlphaAppFeedback.Inbox.copy(contextNumUnread = 0),
            ConversationLabelSample.AlphaAppFeedback.Archive.copy(contextNumUnread = 1)
        )
        val updatedConversation = ConversationSample.AlphaAppFeedback.copy(
            numMessages = 2,
            numUnread = 1,
            labels = updatedLabels
        )
        every { conversationDao.observe(userId, conversationId) } returns flowOf(conversation)

        // when
        val result = conversationLocalDataSource.rollbackMarkUnread(
            userId,
            ConversationIdSample.AlphaAppFeedback,
            contextLabelId
        )

        // then
        assertEquals(updatedConversation.right(), result)
    }

    @Test
    fun `rollback mark unread returns error if conversation not found`() = runTest {
        // given
        val conversationId = ConversationIdSample.WeatherForecast
        val error = DataErrorSample.NoCache.left()
        every { conversationDao.observe(userId, conversationId) } returns flowOf(null)

        // when
        val result = conversationLocalDataSource.rollbackMarkUnread(userId, conversationId, contextLabelId)

        // then
        assertEquals(error, result)
    }
}
