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

package ch.protonmail.android.mailconversation.data.remote

import androidx.work.ExistingWorkPolicy
import arrow.core.right
import ch.protonmail.android.mailcommon.data.worker.Enqueuer
import ch.protonmail.android.mailcommon.domain.benchmark.BenchmarkTracerImpl
import ch.protonmail.android.mailcommon.domain.model.ConversationId
import ch.protonmail.android.mailcommon.domain.sample.ConversationIdSample
import ch.protonmail.android.mailconversation.data.getConversationResource
import ch.protonmail.android.mailconversation.data.remote.ConversationRemoteDataSourceImpl.Companion.MAX_CONVERSATION_IDS_API_LIMIT
import ch.protonmail.android.mailconversation.data.remote.response.GetConversationsResponse
import ch.protonmail.android.mailconversation.data.remote.worker.AddLabelConversationWorker
import ch.protonmail.android.mailconversation.data.remote.worker.ClearConversationLabelWorker
import ch.protonmail.android.mailconversation.data.remote.worker.DeleteConversationsWorker
import ch.protonmail.android.mailconversation.data.remote.worker.MarkConversationAsReadWorker
import ch.protonmail.android.mailconversation.data.remote.worker.MarkConversationAsUnreadWorker
import ch.protonmail.android.mailconversation.data.remote.worker.RemoveLabelConversationWorker
import ch.protonmail.android.maillabel.domain.model.MailLabelId
import ch.protonmail.android.maillabel.domain.model.SystemLabelId
import ch.protonmail.android.mailpagination.domain.model.OrderBy
import ch.protonmail.android.mailpagination.domain.model.OrderDirection
import ch.protonmail.android.mailpagination.domain.model.PageFilter
import ch.protonmail.android.mailpagination.domain.model.PageKey
import ch.protonmail.android.mailpagination.domain.model.ReadStatus
import ch.protonmail.android.testdata.conversation.ConversationTestData
import ch.protonmail.android.testdata.conversation.ConversationWithContextTestData
import ch.protonmail.android.testdata.user.UserIdTestData.userId
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifySequence
import kotlinx.coroutines.test.runTest
import me.proton.core.label.domain.entity.LabelId
import me.proton.core.network.data.ApiManagerFactory
import me.proton.core.network.data.ApiProvider
import me.proton.core.network.domain.session.SessionId
import me.proton.core.network.domain.session.SessionProvider
import me.proton.core.test.android.api.TestApiManager
import me.proton.core.util.kotlin.DefaultDispatcherProvider
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class ConversationRemoteDataSourceImplTest {

    private val contextLabelId = MailLabelId.System.Archive.labelId

    private val sessionProvider = mockk<SessionProvider> {
        coEvery { getSessionId(userId) } returns SessionId("testSessionId")
    }

    private val conversationApi = mockk<ConversationApi> {
        coEvery { getConversations(allAny()) } returns GetConversationsResponse(
            code = 1000,
            total = 0,
            conversations = emptyList(),
            stale = 0
        )
    }

    private val apiManagerFactory = mockk<ApiManagerFactory> {
        every { create(any(), ConversationApi::class) } returns TestApiManager(conversationApi)
    }
    private val enqueuer: Enqueuer = mockk {
        every { this@mockk.enqueue(userId, any(), any()) } returns mockk()
    }

    private val apiProvider = ApiProvider(apiManagerFactory, sessionProvider, DefaultDispatcherProvider())
    private val benchmarkTracer = BenchmarkTracerImpl(false)
    private val conversationRemoteDataSource = ConversationRemoteDataSourceImpl(
        apiProvider,
        enqueuer,
        benchmarkTracer
    )

    @Test(expected = IllegalArgumentException::class)
    fun `pageKey size greater than ConversationApi maxPageSize throw Exception`() = runTest {
        // Given
        val pageKey = PageKey(size = 200)
        // When
        conversationRemoteDataSource.getConversations(userId, pageKey)
    }

    @Test
    fun `pageKey size less or equal ConversationApi maxPageSize`() = runTest {
        // Given
        val pageKey = PageKey(size = ConversationApi.maxPageSize)
        // When
        val conversations = conversationRemoteDataSource.getConversations(userId, pageKey)
        // Then
        assertNotNull(conversations)
    }

    @Test
    fun `return correctly mapped conversations`() = runTest {
        // Given
        coEvery { conversationApi.getConversations(allAny()) } returns GetConversationsResponse(
            code = 1000,
            total = 2,
            conversations = listOf(
                getConversationResource("2", order = 2000),
                getConversationResource("1", order = 1000)
            ),
            stale = 0
        )
        // When
        val conversations = conversationRemoteDataSource.getConversations(userId, PageKey())
        // Then
        assertEquals(
            expected = listOf(
                ConversationWithContextTestData.conversation2Ordered,
                ConversationWithContextTestData.conversation1Ordered
            ).right(),
            actual = conversations
        )
    }

    @Test
    fun `call conversationApi with correct parameters All Ascending`() = runTest {
        // When
        conversationRemoteDataSource.getConversations(
            userId = userId,
            pageKey = PageKey(
                size = 25,
                orderDirection = OrderDirection.Ascending,
                orderBy = OrderBy.Time,
                filter = PageFilter(
                    labelId = LabelId("0"),
                    keyword = "test",
                    read = ReadStatus.All,
                    minTime = Long.MIN_VALUE,
                    maxTime = Long.MAX_VALUE,
                    minOrder = Long.MIN_VALUE,
                    maxOrder = Long.MAX_VALUE,
                    minId = null,
                    maxId = null
                )
            )
        )
        // Then
        coVerify {
            conversationApi.getConversations(
                page = 0,
                pageSize = 25,
                limit = 25,
                labelIds = listOf("0"),
                sort = "Time",
                desc = 0,
                beginTime = null,
                beginId = null,
                endTime = null,
                endId = null,
                keyword = "test",
                unread = null
            )
        }
    }

    @Test
    fun `call conversationApi with correct parameters Unread Descending`() = runTest {
        // When
        conversationRemoteDataSource.getConversations(
            userId = userId,
            pageKey = PageKey(
                size = 25,
                orderDirection = OrderDirection.Descending,
                orderBy = OrderBy.Time,
                filter = PageFilter(
                    labelId = LabelId("0"),
                    keyword = "test",
                    read = ReadStatus.Unread,
                    minTime = Long.MIN_VALUE,
                    maxTime = Long.MAX_VALUE,
                    minOrder = Long.MIN_VALUE,
                    maxOrder = Long.MAX_VALUE,
                    minId = null,
                    maxId = null
                )
            )
        )
        // Then
        coVerify {
            conversationApi.getConversations(
                page = 0,
                pageSize = 25,
                limit = 25,
                labelIds = listOf("0"),
                sort = "Time",
                desc = 1,
                beginTime = null,
                beginId = null,
                endTime = null,
                endId = null,
                keyword = "test",
                unread = 1
            )
        }
    }

    @Test
    fun `call conversationApi with parameters between 1000 and 3000 no keyword`() = runTest {
        // When
        conversationRemoteDataSource.getConversations(
            userId = userId,
            pageKey = PageKey(
                size = 25,
                orderDirection = OrderDirection.Descending,
                orderBy = OrderBy.Time,
                filter = PageFilter(
                    labelId = LabelId("0"),
                    keyword = "",
                    read = ReadStatus.Unread,
                    minTime = 1000,
                    maxTime = 3000,
                    minOrder = Long.MIN_VALUE,
                    maxOrder = Long.MAX_VALUE,
                    minId = "1000",
                    maxId = "3000"
                )
            )
        )
        // Then
        coVerify {
            conversationApi.getConversations(
                page = 0,
                pageSize = 25,
                limit = 25,
                labelIds = listOf("0"),
                sort = "Time",
                desc = 1,
                beginTime = 1000,
                beginId = "1000",
                endTime = 3000,
                endId = "3000",
                keyword = null,
                unread = 1
            )
        }
    }

    @Test
    fun `enqueues worker to perform add label API call when add label is called for conversation`() {
        // Given
        val conversationId = ConversationId(ConversationTestData.RAW_CONVERSATION_ID)
        val labelId = LabelId("10")
        appendOrReplaceUniqueAddLabelWorkSucceeds()

        // When
        conversationRemoteDataSource.addLabel(userId, listOf(conversationId), labelId)

        // Then
        val expectedParams = AddLabelConversationWorker.params(
            userId,
            listOf(conversationId),
            labelId
        )
        verify {
            enqueuer.enqueueUniqueWork<AddLabelConversationWorker>(
                userId = userId,
                workerId = AddLabelConversationWorker.id(userId),
                params = match { mapDeepEquals(it, expectedParams) },
                existingWorkPolicy = ExistingWorkPolicy.APPEND_OR_REPLACE
            )
        }
    }

    @Test
    fun `enqueues worker to perform multiple add label API calls when add labels is called for conversation`() {
        // Given
        val conversationId = ConversationId(ConversationTestData.RAW_CONVERSATION_ID)
        val labelList = listOf(LabelId("10"), LabelId("11"))
        appendOrReplaceUniqueAddLabelWorkSucceeds()

        // When
        conversationRemoteDataSource.addLabels(userId, listOf(conversationId), labelList)

        // Then
        verifySequence {
            val expectedFirst = AddLabelConversationWorker.params(
                userId,
                listOf(conversationId),
                labelList.first()
            )
            enqueuer.enqueueUniqueWork<AddLabelConversationWorker>(
                userId = userId,
                workerId = AddLabelConversationWorker.id(userId),
                params = match { mapDeepEquals(it, expectedFirst) },
                existingWorkPolicy = ExistingWorkPolicy.APPEND_OR_REPLACE
            )
            val expectedLast = AddLabelConversationWorker.params(
                userId,
                listOf(conversationId),
                labelList.last()
            )
            enqueuer.enqueueUniqueWork<AddLabelConversationWorker>(
                userId = userId,
                workerId = AddLabelConversationWorker.id(userId),
                params = match { mapDeepEquals(it, expectedLast) },
                existingWorkPolicy = ExistingWorkPolicy.APPEND_OR_REPLACE
            )
        }
    }

    @Test
    fun `enqueues worker to perform add label API call twice when conversations number is above the limit`() = runTest {
        // Given
        val conversationIds = List(MAX_CONVERSATION_IDS_API_LIMIT + 1) { ConversationIdSample.Invoices }
        val labelId = LabelId("10")
        appendOrReplaceUniqueAddLabelWorkSucceeds()

        // When
        conversationRemoteDataSource.addLabels(userId, conversationIds, listOf(labelId))

        // Then
        verifySequence {
            val expectedFirst = AddLabelConversationWorker.params(
                userId,
                conversationIds.take(MAX_CONVERSATION_IDS_API_LIMIT),
                labelId
            )
            enqueuer.enqueueUniqueWork<AddLabelConversationWorker>(
                userId = userId,
                workerId = AddLabelConversationWorker.id(userId),
                params = match { mapDeepEquals(it, expectedFirst) },
                existingWorkPolicy = ExistingWorkPolicy.APPEND_OR_REPLACE
            )
            val expectedSecond = AddLabelConversationWorker.params(
                userId,
                conversationIds.drop(MAX_CONVERSATION_IDS_API_LIMIT),
                labelId
            )
            enqueuer.enqueueUniqueWork<AddLabelConversationWorker>(
                userId = userId,
                workerId = AddLabelConversationWorker.id(userId),
                params = match { mapDeepEquals(it, expectedSecond) },
                existingWorkPolicy = ExistingWorkPolicy.APPEND_OR_REPLACE
            )
        }
    }

    @Test
    fun `enqueues worker to perform mark read API call twice when conversations number is above the limit`() = runTest {
        // Given
        val conversationIds = List(MAX_CONVERSATION_IDS_API_LIMIT + 1) { ConversationIdSample.Invoices }

        // When
        conversationRemoteDataSource.markRead(userId, conversationIds)

        // Then
        verifySequence {
            val expectedFirst = MarkConversationAsReadWorker.params(
                userId,
                conversationIds.take(MAX_CONVERSATION_IDS_API_LIMIT)
            )
            enqueuer.enqueue<MarkConversationAsReadWorker>(userId, match { mapDeepEquals(it, expectedFirst) })
            val expectedSecond = MarkConversationAsReadWorker.params(
                userId,
                conversationIds.drop(MAX_CONVERSATION_IDS_API_LIMIT)
            )
            enqueuer.enqueue<MarkConversationAsReadWorker>(userId, match { mapDeepEquals(it, expectedSecond) })
        }
    }

    @Test
    fun `enqueues worker to perform mark unread API call twice when conversations number is above the limit`() =
        runTest {
            // Given
            val conversationIds = List(MAX_CONVERSATION_IDS_API_LIMIT + 1) { ConversationIdSample.Invoices }

            // When
            conversationRemoteDataSource.markUnread(userId, conversationIds, contextLabelId)

            // Then
            verifySequence {
                val expectedFirst = MarkConversationAsUnreadWorker.params(
                    userId,
                    conversationIds.take(MAX_CONVERSATION_IDS_API_LIMIT),
                    contextLabelId
                )
                enqueuer.enqueue<MarkConversationAsUnreadWorker>(userId, match { mapDeepEquals(it, expectedFirst) })
                val expectedSecond = MarkConversationAsUnreadWorker.params(
                    userId,
                    conversationIds.drop(MAX_CONVERSATION_IDS_API_LIMIT),
                    contextLabelId
                )
                enqueuer.enqueue<MarkConversationAsUnreadWorker>(userId, match { mapDeepEquals(it, expectedSecond) })
            }
        }

    @Test
    fun `enqueues worker to perform remove label API call when remove label is called for conversation`() {
        // Given
        val conversationId = ConversationId(ConversationTestData.RAW_CONVERSATION_ID)
        val labelId = LabelId("10")

        // When
        conversationRemoteDataSource.removeLabel(userId, listOf(conversationId), labelId)

        // Then
        val expected = RemoveLabelConversationWorker.params(
            userId,
            listOf(conversationId),
            labelId
        )
        verify {
            enqueuer.enqueue<RemoveLabelConversationWorker>(userId, match { mapDeepEquals(it, expected) })
        }
    }

    @Test
    fun `enqueues worker to perform multiple remove label API calls when remove labels is called for conversation`() {
        // Given
        val conversationId = ConversationId(ConversationTestData.RAW_CONVERSATION_ID)
        val labelList = listOf(LabelId("10"), LabelId("11"))

        // When
        conversationRemoteDataSource.removeLabels(userId, listOf(conversationId), labelList)

        // Then
        verifySequence {
            val expectedFirst = RemoveLabelConversationWorker.params(
                userId,
                listOf(conversationId),
                labelList.first()
            )
            enqueuer.enqueue<RemoveLabelConversationWorker>(userId, match { mapDeepEquals(it, expectedFirst) })
            val expectedLast = RemoveLabelConversationWorker.params(
                userId,
                listOf(conversationId),
                labelList.last()
            )
            enqueuer.enqueue<RemoveLabelConversationWorker>(userId, match { mapDeepEquals(it, expectedLast) })
        }
    }

    @Test
    fun `enqueues worker to perform remove label API call twice when conversations number is above the limit`() =
        runTest {
            // Given
            val conversationIds = List(MAX_CONVERSATION_IDS_API_LIMIT + 1) { ConversationIdSample.Invoices }
            val labelId = LabelId("10")

            // When
            conversationRemoteDataSource.removeLabels(userId, conversationIds, listOf(labelId))

            // Then
            verifySequence {
                val expectedFirst = RemoveLabelConversationWorker.params(
                    userId,
                    conversationIds.take(MAX_CONVERSATION_IDS_API_LIMIT),
                    labelId
                )
                enqueuer.enqueue<RemoveLabelConversationWorker>(userId, match { mapDeepEquals(it, expectedFirst) })
                val expectedSecond = RemoveLabelConversationWorker.params(
                    userId,
                    conversationIds.drop(MAX_CONVERSATION_IDS_API_LIMIT),
                    labelId
                )
                enqueuer.enqueue<RemoveLabelConversationWorker>(userId, match { mapDeepEquals(it, expectedSecond) })
            }
        }

    @Test
    fun `enqueues worker to perform mark unread API call when mark unread is called for conversation`() = runTest {
        // given
        val conversationId = ConversationIdSample.WeatherForecast

        // when
        conversationRemoteDataSource.markUnread(userId, listOf(conversationId), contextLabelId)

        // then
        val expected = MarkConversationAsUnreadWorker.params(
            userId,
            listOf(conversationId),
            contextLabelId
        )
        verify {
            enqueuer.enqueue<MarkConversationAsUnreadWorker>(userId, match { mapDeepEquals(it, expected) })
        }
    }

    @Test
    fun `enqueues worker to perform mark read API call when mark read is called for conversation`() = runTest {
        // given
        val conversationId = ConversationIdSample.WeatherForecast

        // when
        conversationRemoteDataSource.markRead(userId, listOf(conversationId))

        // then
        val expected = MarkConversationAsReadWorker.params(
            userId,
            listOf(conversationId)
        )
        verify {
            enqueuer.enqueue<MarkConversationAsReadWorker>(userId, match { mapDeepEquals(it, expected) })
        }
    }

    @Test
    fun `enqueues worker to delete conversation`() {
        // Given
        val conversationId = listOf(ConversationIdSample.Invoices)

        // When
        conversationRemoteDataSource.deleteConversations(userId, conversationId, SystemLabelId.Trash.labelId)

        // Then
        val expected = DeleteConversationsWorker.params(userId, conversationId, SystemLabelId.Trash.labelId)
        verify { enqueuer.enqueue<DeleteConversationsWorker>(userId, match { mapDeepEquals(it, expected) }) }
    }

    @Test
    fun `enqueues worker to delete conversations twice if message id count exceeds limit`() {
        // Given
        val conversationIds = List(MAX_CONVERSATION_IDS_API_LIMIT + 1) { ConversationIdSample.WeatherForecast }
        val currentLabelId = LabelId("10")

        // When
        conversationRemoteDataSource.deleteConversations(userId, conversationIds, currentLabelId)

        // Then
        verifySequence {
            val expectedFirst = DeleteConversationsWorker.params(
                userId, conversationIds.take(MAX_CONVERSATION_IDS_API_LIMIT), currentLabelId
            )
            enqueuer.enqueue<DeleteConversationsWorker>(userId, match { mapDeepEquals(it, expectedFirst) })

            val expectedSecond = DeleteConversationsWorker.params(
                userId, conversationIds.drop(MAX_CONVERSATION_IDS_API_LIMIT), currentLabelId
            )
            enqueuer.enqueue<DeleteConversationsWorker>(userId, match { mapDeepEquals(it, expectedSecond) })
        }
    }

    @Test
    fun `enqueues worker to clear label`() {
        // given
        val labelId = SystemLabelId.Trash.labelId
        every {
            enqueuer.enqueueUniqueWork<ClearConversationLabelWorker>(
                userId,
                ClearConversationLabelWorker.id(userId, labelId),
                ClearConversationLabelWorker.params(userId, labelId)
            )
        } returns mockk()

        // when
        conversationRemoteDataSource.clearLabel(userId, labelId)

        // then
        verify {
            enqueuer.enqueueUniqueWork<ClearConversationLabelWorker>(
                userId,
                ClearConversationLabelWorker.id(userId, labelId),
                ClearConversationLabelWorker.params(userId, labelId)
            )
        }
    }

    private fun appendOrReplaceUniqueAddLabelWorkSucceeds() {
        every {
            enqueuer.enqueueUniqueWork<AddLabelConversationWorker>(
                userId = userId,
                workerId = AddLabelConversationWorker.id(userId),
                params = any(),
                existingWorkPolicy = ExistingWorkPolicy.APPEND_OR_REPLACE
            )
        } returns mockk()
    }

    private fun mapDeepEquals(expected: Map<String, Any?>, actual: Map<String, Any?>): Boolean =
        expected.keys == actual.keys &&
            expected.values.toTypedArray().contentDeepEquals(actual.values.toTypedArray())
}
