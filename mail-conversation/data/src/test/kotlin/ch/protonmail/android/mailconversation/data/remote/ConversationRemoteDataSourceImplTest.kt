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

import ch.protonmail.android.mailconversation.data.getConversation
import ch.protonmail.android.mailconversation.data.getConversationResource
import ch.protonmail.android.mailconversation.data.remote.response.GetConversationsResponse
import ch.protonmail.android.mailpagination.domain.entity.OrderBy
import ch.protonmail.android.mailpagination.domain.entity.OrderDirection
import ch.protonmail.android.mailpagination.domain.entity.PageFilter
import ch.protonmail.android.mailpagination.domain.entity.PageKey
import ch.protonmail.android.mailpagination.domain.entity.ReadStatus
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import me.proton.core.label.domain.entity.LabelId
import me.proton.core.network.data.ApiManagerFactory
import me.proton.core.network.data.ApiProvider
import me.proton.core.network.domain.session.SessionId
import me.proton.core.network.domain.session.SessionProvider
import me.proton.core.test.android.api.TestApiManager
import me.proton.core.util.kotlin.DefaultDispatcherProvider
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class ConversationRemoteDataSourceImplTest {

    private val userId = UserId("1")

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

    private lateinit var apiProvider: ApiProvider
    private lateinit var conversationRemoteDataSource: ConversationRemoteDataSourceImpl

    @Before
    fun setUp() {
        apiProvider = ApiProvider(apiManagerFactory, sessionProvider, DefaultDispatcherProvider())
        conversationRemoteDataSource = ConversationRemoteDataSourceImpl(apiProvider)
    }

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
                getConversation(userId, "2", order = 2000),
                getConversation(userId, "1", order = 1000)
            ),
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
}
