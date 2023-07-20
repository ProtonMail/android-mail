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

package ch.protonmail.android.mailmessage.data.remote

import arrow.core.right
import ch.protonmail.android.mailcommon.data.worker.Enqueuer
import ch.protonmail.android.mailmessage.data.getMessage
import ch.protonmail.android.mailmessage.data.getMessageResource
import ch.protonmail.android.mailmessage.data.remote.response.GetMessagesResponse
import ch.protonmail.android.mailmessage.data.remote.worker.AddLabelMessageWorker
import ch.protonmail.android.mailmessage.data.remote.worker.MarkMessageAsReadWorker
import ch.protonmail.android.mailmessage.data.remote.worker.MarkMessageAsUnreadWorker
import ch.protonmail.android.mailmessage.data.remote.worker.RemoveLabelMessageWorker
import ch.protonmail.android.mailmessage.domain.entity.MessageId
import ch.protonmail.android.mailmessage.domain.sample.MessageIdSample
import ch.protonmail.android.mailpagination.domain.model.OrderBy
import ch.protonmail.android.mailpagination.domain.model.OrderDirection
import ch.protonmail.android.mailpagination.domain.model.PageFilter
import ch.protonmail.android.mailpagination.domain.model.PageKey
import ch.protonmail.android.mailpagination.domain.model.ReadStatus
import ch.protonmail.android.testdata.message.MessageTestData
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifySequence
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import me.proton.core.label.domain.entity.LabelId
import me.proton.core.network.data.ApiManagerFactory
import me.proton.core.network.data.ApiProvider
import me.proton.core.network.domain.session.SessionId
import me.proton.core.network.domain.session.SessionProvider
import me.proton.core.test.android.api.TestApiManager
import me.proton.core.util.kotlin.DefaultDispatcherProvider
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class MessageRemoteDataSourceImplTest {

    private val userId = UserId("1")

    private val sessionProvider = mockk<SessionProvider> {
        coEvery { getSessionId(userId) } returns SessionId("testSessionId")
    }

    private val messageApi = mockk<MessageApi> {
        coEvery { getMessages(allAny()) } returns GetMessagesResponse(
            code = 1000,
            total = 0,
            messages = emptyList(),
            stale = 0
        )
    }

    private val apiManagerFactory = mockk<ApiManagerFactory> {
        every { create(any(), MessageApi::class) } returns TestApiManager(messageApi)
    }

    private val enqueuer: Enqueuer = mockk {
        every { this@mockk.enqueue(any(), any()) } returns mockk()
    }

    private val apiProvider = ApiProvider(
        apiManagerFactory = apiManagerFactory,
        sessionProvider = sessionProvider,
        dispatcherProvider = DefaultDispatcherProvider()
    )

    private val messageRemoteDataSource = MessageRemoteDataSourceImpl(
        apiProvider = apiProvider,
        enqueuer = enqueuer
    )

    @Test(expected = IllegalArgumentException::class)
    fun `pageKey size greater than MessageApi maxPageSize throw Exception`() = runTest {
        // Given
        val pageKey = PageKey(size = 200)
        // When
        messageRemoteDataSource.getMessages(userId, pageKey)
    }

    @Test
    fun `pageKey size less or equal MessageApi maxPageSize`() = runTest {
        // Given
        val pageKey = PageKey(size = MessageApi.maxPageSize)
        // When
        val messages = messageRemoteDataSource.getMessages(userId, pageKey)
        // Then
        assertNotNull(messages)
    }

    @Test
    fun `return correctly mapped messages`() = runTest {
        // Given
        coEvery { messageApi.getMessages(allAny()) } returns GetMessagesResponse(
            code = 1000,
            total = 2,
            messages = listOf(
                getMessageResource("2", time = 2000),
                getMessageResource("1", time = 1000)
            ),
            stale = 0
        )
        // When
        val messages = messageRemoteDataSource.getMessages(userId, PageKey())
        // Then
        assertEquals(
            expected = listOf(
                getMessage(userId, "2", time = 2000),
                getMessage(userId, "1", time = 1000)
            ).right(),
            actual = messages
        )
    }

    @Test
    fun `call messageApi with correct parameters All Ascending`() = runTest {
        // When
        messageRemoteDataSource.getMessages(
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
            messageApi.getMessages(
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
                unread = null,
                conversationsIds = emptyList()
            )
        }
    }

    @Test
    fun `call messageApi with correct parameters Unread Descending`() = runTest {
        // When
        messageRemoteDataSource.getMessages(
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
            messageApi.getMessages(
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
                unread = 1,
                conversationsIds = emptyList()
            )
        }
    }

    @Test
    fun `call messageApi with correct parameters between 1000 and 3000 no keyword`() = runTest {
        // When
        messageRemoteDataSource.getMessages(
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
            messageApi.getMessages(
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
                unread = 1,
                conversationsIds = emptyList()
            )
        }
    }

    @Test
    fun `enqueues worker to perform add label API call when add label is called for message`() {
        // Given
        val messageId = MessageId(MessageTestData.RAW_MESSAGE_ID)
        val messageId2 = MessageIdSample.Invoice
        val labelId = LabelId("10")


        // When
        messageRemoteDataSource.addLabelsToMessages(userId, listOf(messageId, messageId2), listOf(labelId))

        // Then
        verify {
            enqueuer.enqueue<AddLabelMessageWorker>(
                AddLabelMessageWorker.params(
                    userId,
                    listOf(messageId, messageId2),
                    labelId
                )
            )
        }
    }

    @Test
    fun `enqueues worker to perform add label API call twice when add label is called for massages above the limit`() =
        runTest {
            // Given
            val messageIds = List(Enqueuer.MAX_PARAMETER_COUNT + 1) { MessageIdSample.Invoice }

            // When
            messageRemoteDataSource.addLabelsToMessages(userId, messageIds, listOf(LabelId("10")))

            // Then
            verifySequence {
                enqueuer.enqueue<AddLabelMessageWorker>(
                    AddLabelMessageWorker.params(userId, messageIds.take(Enqueuer.MAX_PARAMETER_COUNT), LabelId("10"))
                )
                enqueuer.enqueue<AddLabelMessageWorker>(
                    AddLabelMessageWorker.params(userId, messageIds.drop(Enqueuer.MAX_PARAMETER_COUNT), LabelId("10"))
                )
            }
        }

    @Test
    fun `enqueues workers to perform multiple add label API call when add labels is called for message`() {
        // Given
        val messageId = MessageId(MessageTestData.RAW_MESSAGE_ID)
        val labelId = LabelId("10")
        val labelId2 = LabelId("11")
        // When
        messageRemoteDataSource.addLabelsToMessages(userId, listOf(messageId), listOf(labelId, labelId2))
        // Then
        verify {
            enqueuer.enqueue<AddLabelMessageWorker>(
                AddLabelMessageWorker.params(userId, listOf(messageId), labelId)
            )
        }
        verify {
            enqueuer.enqueue<AddLabelMessageWorker>(
                AddLabelMessageWorker.params(userId, listOf(messageId), labelId2)
            )
        }
    }

    @Test
    fun `enqueues worker to perform remove label API call when add label is called for message`() {
        // Given
        val messageId = MessageId(MessageTestData.RAW_MESSAGE_ID)
        val labelId = LabelId("10")
        // When
        messageRemoteDataSource.removeLabelsFromMessages(userId, listOf(messageId), listOf(labelId))
        // Then
        verify {
            enqueuer.enqueue<RemoveLabelMessageWorker>(
                RemoveLabelMessageWorker.params(userId, listOf(messageId), labelId)
            )
        }
    }

    @Test
    fun `enqueues workers to perform multiple remove label API call when remove labels is called for message`() {
        // Given
        val messageId = MessageId(MessageTestData.RAW_MESSAGE_ID)
        val labelId = LabelId("10")
        val labelId2 = LabelId("11")
        // When
        messageRemoteDataSource.removeLabelsFromMessages(userId, listOf(messageId), listOf(labelId, labelId2))
        // Then
        verifySequence {
            enqueuer.enqueue<RemoveLabelMessageWorker>(
                RemoveLabelMessageWorker.params(userId, listOf(messageId), labelId)
            )
            enqueuer.enqueue<RemoveLabelMessageWorker>(
                RemoveLabelMessageWorker.params(userId, listOf(messageId), labelId2)
            )
        }
    }

    @Test
    fun `enqueues worker to mark message as unread`() {
        // given
        val messageId = listOf(MessageIdSample.Invoice)

        // when
        messageRemoteDataSource.markUnread(userId, messageId)

        // then
        verify { enqueuer.enqueue<MarkMessageAsUnreadWorker>(MarkMessageAsUnreadWorker.params(userId, messageId)) }
    }

    @Test
    fun `enqueues worker to mark messages as unread twice if message id count exceeds limit`() {
        // Given
        val messageIds = List(Enqueuer.MAX_PARAMETER_COUNT + 1) { MessageIdSample.Invoice }

        // When
        messageRemoteDataSource.markUnread(userId, messageIds)

        // Then
        verifySequence {
            enqueuer.enqueue<MarkMessageAsUnreadWorker>(
                MarkMessageAsUnreadWorker.params(userId, messageIds.take(Enqueuer.MAX_PARAMETER_COUNT))
            )
            enqueuer.enqueue<MarkMessageAsUnreadWorker>(
                MarkMessageAsUnreadWorker.params(userId, messageIds.drop(Enqueuer.MAX_PARAMETER_COUNT))
            )
        }
    }

    @Test
    fun `enqueues worker to mark message as read`() {
        // given
        val messageId = MessageIdSample.Invoice

        // when
        messageRemoteDataSource.markRead(userId, messageId)

        // then
        verify { enqueuer.enqueue<MarkMessageAsReadWorker>(MarkMessageAsReadWorker.params(userId, messageId)) }
    }
}
