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

package ch.protonmail.android.mailmailbox.presentation.paging

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcommon.domain.model.NetworkError
import ch.protonmail.android.mailcommon.domain.sample.UserIdSample
import ch.protonmail.android.mailconversation.domain.repository.ConversationRepository
import ch.protonmail.android.mailmailbox.domain.model.MailboxItem
import ch.protonmail.android.mailmailbox.domain.model.MailboxItemType
import ch.protonmail.android.mailmailbox.domain.model.MailboxPageKey
import ch.protonmail.android.mailmailbox.presentation.paging.exception.DataErrorException
import ch.protonmail.android.mailmessage.domain.repository.MessageRepository
import ch.protonmail.android.mailpagination.domain.AdjacentPageKeys
import ch.protonmail.android.mailpagination.domain.GetAdjacentPageKeys
import ch.protonmail.android.mailpagination.domain.model.OrderDirection
import ch.protonmail.android.mailpagination.domain.model.PageKey
import ch.protonmail.android.mailpagination.presentation.paging.EmptyLabelId
import ch.protonmail.android.mailpagination.presentation.paging.EmptyLabelInProgressSignal
import ch.protonmail.android.testdata.conversation.ConversationWithContextTestData
import ch.protonmail.android.testdata.mailbox.MailboxTestData
import ch.protonmail.android.testdata.message.MessageTestData
import io.mockk.Called
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verifySequence
import kotlinx.coroutines.test.runTest
import org.junit.Test
import javax.inject.Provider
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

@OptIn(ExperimentalPagingApi::class)
class MailboxItemRemoteMediatorTest {

    private val userId = UserIdSample.Primary
    private val mailboxPageKey = MailboxPageKey(
        userIds = listOf(userId),
        pageKey = PageKey()
    )
    private var mailboxItemType = MailboxItemType.Message

    private val messageRepository = mockk<MessageRepository>(relaxUnitFun = true)
    private val conversationRepository = mockk<ConversationRepository>(relaxUnitFun = true)
    private val getAdjacentPageKeys = mockk<GetAdjacentPageKeys>()
    private val emptyLabelInProgressSignal = mockk<EmptyLabelInProgressSignal>()
    private val skipInitialMediatorRefresh = mockk<Provider<Boolean>> {
        every { this@mockk.get() } returns false
    }

    private val mailboxItemRemoteMediator by lazy {
        MailboxItemRemoteMediator(
            messageRepository = messageRepository,
            conversationRepository = conversationRepository,
            getAdjacentPageKeys = getAdjacentPageKeys,
            mailboxPageKey = mailboxPageKey,
            type = mailboxItemType,
            emptyLabelInProgressSignal = emptyLabelInProgressSignal,
            skipInitialMediatorRefresh = skipInitialMediatorRefresh.get()
        )
    }

    @Test
    fun `returns success when mediator is called with refresh for message, then data is marked as stale`() = runTest {
        // Given
        val emptyLabelId = EmptyLabelId(mailboxPageKey.pageKey.filter.labelId.id)

        coEvery {
            messageRepository.getRemoteMessages(userId, mailboxPageKey.pageKey)
        } returns listOf(MessageTestData.message).right()
        every { emptyLabelInProgressSignal.isEmptyLabelInProgress(emptyLabelId) } returns false

        // When
        val result = mailboxItemRemoteMediator.load(
            loadType = LoadType.REFRESH,
            state = buildPagingState()
        )

        // Then
        coVerify { messageRepository.markAsStale(userId, mailboxPageKey.pageKey.filter.labelId) }
        coVerify(exactly = 0) { conversationRepository.markAsStale(any(), any()) }
        assertIs<RemoteMediator.MediatorResult.Success>(result)
        assertFalse(result.endOfPaginationReached)
    }

    @Test
    fun `returns success when mediator is called with refresh for conversation, then data is marked as stale`() =
        runTest {
            // Given
            mailboxItemType = MailboxItemType.Conversation
            val emptyLabelId = EmptyLabelId(mailboxPageKey.pageKey.filter.labelId.id)

            coEvery {
                conversationRepository.getRemoteConversations(userId, mailboxPageKey.pageKey)
            } returns listOf(ConversationWithContextTestData.conversation1).right()
            every { emptyLabelInProgressSignal.isEmptyLabelInProgress(emptyLabelId) } returns false

            // When
            val result = mailboxItemRemoteMediator.load(
                loadType = LoadType.REFRESH,
                state = buildPagingState()
            )

            // Then
            coVerify { conversationRepository.markAsStale(userId, mailboxPageKey.pageKey.filter.labelId) }
            coVerify(exactly = 0) { messageRepository.markAsStale(any(), any()) }
            assertIs<RemoteMediator.MediatorResult.Success>(result)
            assertFalse(result.endOfPaginationReached)
        }

    @Test
    fun `given the mediator is called with append, then data is not marked as stale`() = runTest {
        // Given
        val pageKey = mailboxPageKey.pageKey.copy(
            orderDirection = OrderDirection.Ascending
        )
        val pages = emptyList<PagingSource.LoadResult.Page<MailboxPageKey, MailboxItem>>()
        val items = pages.flatMap { it.data }
        val pageSize = PageKey.defaultPageSize
        val emptyLabelId = EmptyLabelId(mailboxPageKey.pageKey.filter.labelId.id)

        coEvery { getAdjacentPageKeys(items, mailboxPageKey.pageKey, pageSize) } returns AdjacentPageKeys(
            prev = PageKey(),
            current = mailboxPageKey.pageKey,
            next = pageKey
        )
        coEvery {
            messageRepository.getRemoteMessages(
                userId,
                pageKey
            )
        } returns listOf(MessageTestData.message).right()
        every { emptyLabelInProgressSignal.isEmptyLabelInProgress(emptyLabelId) } returns false

        // When
        mailboxItemRemoteMediator.load(
            loadType = LoadType.APPEND,
            state = buildPagingState(pages, pageSize)
        )

        // Then
        coVerify(exactly = 0) { messageRepository.markAsStale(any(), any()) }
        coVerify { messageRepository.getRemoteMessages(userId, pageKey) }
    }

    @Test
    fun `returns end of pagination with ongoing empty label action, and resets the empty label action state`() =
        runTest {
            // Given
            mailboxItemType = MailboxItemType.Conversation
            val emptyLabelId = EmptyLabelId(mailboxPageKey.pageKey.filter.labelId.id)
            every { emptyLabelInProgressSignal.isEmptyLabelInProgress(emptyLabelId) } returns true
            every { emptyLabelInProgressSignal.resetOperationSignal() } just runs

            // When
            val result = mailboxItemRemoteMediator.load(
                loadType = LoadType.REFRESH,
                state = buildPagingState()
            )

            // Then
            coVerify(exactly = 0) { conversationRepository.getRemoteConversations(userId, mailboxPageKey.pageKey) }
            coVerify(exactly = 0) { messageRepository.getRemoteMessages(userId, mailboxPageKey.pageKey) }
            coVerify(exactly = 0) { conversationRepository.markAsStale(userId, mailboxPageKey.pageKey.filter.labelId) }
            coVerify(exactly = 0) { messageRepository.markAsStale(any(), any()) }
            verifySequence {
                emptyLabelInProgressSignal.isEmptyLabelInProgress(emptyLabelId)
                emptyLabelInProgressSignal.resetOperationSignal()
            }
            assertIs<RemoteMediator.MediatorResult.Success>(result)
            assertTrue(result.endOfPaginationReached, "End of pagination is not reached")
            confirmVerified(conversationRepository, messageRepository, emptyLabelInProgressSignal)
        }

    @Test
    fun `returns error when mediator is called with append and message api call fails`() = runTest {
        // Given
        val expectedRemoteError = DataError.Remote.Http(NetworkError.Unreachable)
        val pageKey = mailboxPageKey.pageKey.copy(
            orderDirection = OrderDirection.Ascending
        )
        val pages = emptyList<PagingSource.LoadResult.Page<MailboxPageKey, MailboxItem>>()
        val items = pages.flatMap { it.data }
        val pageSize = PageKey.defaultPageSize
        val emptyLabelId = EmptyLabelId(mailboxPageKey.pageKey.filter.labelId.id)

        coEvery { getAdjacentPageKeys(items, mailboxPageKey.pageKey, pageSize) } returns AdjacentPageKeys(
            prev = PageKey(),
            current = mailboxPageKey.pageKey,
            next = pageKey
        )
        coEvery {
            messageRepository.getRemoteMessages(userId, pageKey)
        } returns expectedRemoteError.left()
        every { emptyLabelInProgressSignal.isEmptyLabelInProgress(emptyLabelId) } returns false

        // When
        val result = mailboxItemRemoteMediator.load(
            loadType = LoadType.APPEND,
            state = buildPagingState(pages, pageSize)
        )

        // Then
        coVerify(exactly = 0) { messageRepository.markAsStale(any(), any()) }
        coVerify { messageRepository.getRemoteMessages(userId, pageKey) }
        assertIs<RemoteMediator.MediatorResult.Error>(result)
        val actualException = result.throwable
        assertIs<DataErrorException>(actualException)
        assertEquals(expectedRemoteError, actualException.error)
    }

    @Test
    fun `returns error when mediator is called with append and conversation api call fails`() = runTest {
        // Given
        val expectedRemoteError = DataError.Remote.Http(NetworkError.NoNetwork)
        val pageKey = mailboxPageKey.pageKey.copy(
            orderDirection = OrderDirection.Ascending
        )
        val pages = emptyList<PagingSource.LoadResult.Page<MailboxPageKey, MailboxItem>>()
        val items = pages.flatMap { it.data }
        val pageSize = PageKey.defaultPageSize
        val emptyLabelId = EmptyLabelId(mailboxPageKey.pageKey.filter.labelId.id)

        coEvery { getAdjacentPageKeys(items, mailboxPageKey.pageKey, pageSize) } returns AdjacentPageKeys(
            prev = PageKey(),
            current = mailboxPageKey.pageKey,
            next = pageKey
        )
        mailboxItemType = MailboxItemType.Conversation
        coEvery {
            conversationRepository.getRemoteConversations(userId, pageKey)
        } returns expectedRemoteError.left()
        every { emptyLabelInProgressSignal.isEmptyLabelInProgress(emptyLabelId) } returns false

        // When
        val result = mailboxItemRemoteMediator.load(
            loadType = LoadType.APPEND,
            state = buildPagingState(pages, pageSize)
        )

        // Then
        coVerify(exactly = 0) { conversationRepository.markAsStale(any(), any()) }
        coVerify { conversationRepository.getRemoteConversations(userId, pageKey) }
        assertIs<RemoteMediator.MediatorResult.Error>(result)
        val actualException = result.throwable
        assertIs<DataErrorException>(actualException)
        assertEquals(expectedRemoteError, actualException.error)
    }

    @Test
    fun `given the remote mediator is called with append and and state contains next key, then this key is used`() =
        runTest {
            // Given
            val nextKey = mailboxPageKey.pageKey.copy(
                orderDirection = OrderDirection.Ascending
            )
            val pages = listOf(
                PagingSource.LoadResult.Page(
                    data = listOf(
                        MailboxTestData.readMailboxItem,
                        MailboxTestData.unreadMailboxItem
                    ),
                    prevKey = null,
                    nextKey = MailboxPageKey(
                        pageKey = nextKey,
                        userIds = listOf()
                    )
                )
            )
            val pageSize = PageKey.defaultPageSize
            mailboxItemType = MailboxItemType.Conversation
            val emptyLabelId = EmptyLabelId(mailboxPageKey.pageKey.filter.labelId.id)

            coEvery {
                conversationRepository.getRemoteConversations(userId, nextKey)
            } returns listOf(ConversationWithContextTestData.conversation1).right()
            every { emptyLabelInProgressSignal.isEmptyLabelInProgress(emptyLabelId) } returns false

            // When
            val result = mailboxItemRemoteMediator.load(
                loadType = LoadType.APPEND,
                state = buildPagingState(pages, pageSize)
            )

            // Then
            coVerify { conversationRepository.getRemoteConversations(userId, nextKey) }
            coVerify { getAdjacentPageKeys wasNot Called }
            assertIs<RemoteMediator.MediatorResult.Success>(result)
        }

    @Test
    fun `given the remote mediator is called with prepend and and state contains prev key, then this key is used`() =
        runTest {
            // Given
            val prevKey = mailboxPageKey.pageKey.copy(
                orderDirection = OrderDirection.Ascending
            )
            val pages = listOf(
                PagingSource.LoadResult.Page(
                    data = listOf(
                        MailboxTestData.readMailboxItem,
                        MailboxTestData.unreadMailboxItem
                    ),
                    prevKey = MailboxPageKey(
                        pageKey = prevKey,
                        userIds = listOf()
                    ),
                    nextKey = null
                )
            )
            val pageSize = PageKey.defaultPageSize
            mailboxItemType = MailboxItemType.Conversation
            val emptyLabelId = EmptyLabelId(mailboxPageKey.pageKey.filter.labelId.id)

            coEvery {
                conversationRepository.getRemoteConversations(userId, prevKey)
            } returns listOf(ConversationWithContextTestData.conversation1).right()
            every { emptyLabelInProgressSignal.isEmptyLabelInProgress(emptyLabelId) } returns false

            // When
            val result = mailboxItemRemoteMediator.load(
                loadType = LoadType.PREPEND,
                state = buildPagingState(pages, pageSize)
            )

            // Then
            coVerify { conversationRepository.getRemoteConversations(userId, prevKey) }
            coVerify { getAdjacentPageKeys wasNot Called }
            assertIs<RemoteMediator.MediatorResult.Success>(result)
        }

    private fun buildPagingState(
        pages: List<PagingSource.LoadResult.Page<MailboxPageKey, MailboxItem>> = emptyList(),
        pageSize: Int = PageKey.defaultPageSize
    ) = PagingState(
        pages = pages,
        config = PagingConfig(
            pageSize = pageSize,
            enablePlaceholders = false,
            initialLoadSize = pageSize,
            prefetchDistance = 1
        ),
        anchorPosition = 0,
        leadingPlaceholderCount = 0
    )
}
