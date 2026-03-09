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

import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcommon.domain.sample.UserIdSample
import ch.protonmail.android.maillabel.data.local.RustMailboxFactory
import ch.protonmail.android.maillabel.data.wrapper.MailboxWrapper
import ch.protonmail.android.maillabel.domain.model.SystemLabelId
import ch.protonmail.android.mailmessage.data.local.RustMessageListQueryImpl.Companion.NONE_FOLLOWUP_GRACE_MS
import ch.protonmail.android.mailmessage.data.usecase.CreateRustMessagesPaginator
import ch.protonmail.android.mailmessage.data.usecase.CreateRustSearchPaginator
import ch.protonmail.android.mailmessage.data.wrapper.MessagePaginatorWrapper
import ch.protonmail.android.mailpagination.domain.model.PageInvalidationEvent
import ch.protonmail.android.mailpagination.domain.model.PageKey
import ch.protonmail.android.mailpagination.domain.model.PageToLoad
import ch.protonmail.android.mailpagination.domain.model.PaginationError
import ch.protonmail.android.mailpagination.domain.repository.PageInvalidationRepository
import ch.protonmail.android.test.utils.rule.MainDispatcherRule
import ch.protonmail.android.testdata.message.rust.LocalMessageTestData
import io.mockk.Called
import io.mockk.CapturingSlot
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import uniffi.proton_mail_uniffi.Message
import uniffi.proton_mail_uniffi.MessageScrollerListUpdate
import uniffi.proton_mail_uniffi.MessageScrollerLiveQueryCallback
import uniffi.proton_mail_uniffi.MessageScrollerUpdate
import kotlin.test.Test

class RustMessageListQueryImplTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val mailbox = mockk<MailboxWrapper>()

    private val rustMailboxFactory = mockk<RustMailboxFactory>()
    private val createRustMessagesPaginator = mockk<CreateRustMessagesPaginator>()
    private val createRustSearchPaginator = mockk<CreateRustSearchPaginator>()
    private val invalidationRepository = mockk<PageInvalidationRepository>()

    private val rustMessageListQuery = RustMessageListQueryImpl(
        rustMailboxFactory = rustMailboxFactory,
        createRustMessagesPaginator = createRustMessagesPaginator,
        createRustSearchPaginator = createRustSearchPaginator,
        coroutineScope = CoroutineScope(mainDispatcherRule.testDispatcher),
        invalidationRepository = invalidationRepository
    )

    private val expectedMessages = listOf(
        LocalMessageTestData.AugWeatherForecast,
        LocalMessageTestData.SepWeatherForecast,
        LocalMessageTestData.OctWeatherForecast
    )

    private val userId = UserIdSample.Primary
    private val inboxLabelId = SystemLabelId.Inbox.labelId

    private fun paginatorWrapperWithNextEmitting(
        callback: CapturingSlot<MessageScrollerLiveQueryCallback>,
        items: List<Message>,
        filterUnread: Boolean = false,
        showSpamTrash: Boolean = false
    ): MessagePaginatorWrapper = mockk {
        coEvery { nextPage() } answers {
            callback.captured.onUpdate(
                MessageScrollerUpdate.List(
                    MessageScrollerListUpdate.Append(
                        items = items,
                        scrollerId = DefaultScrollerId
                    )
                )
            )
            Unit.right()
        }
        coEvery { disconnect() } just Runs
        coEvery { filterUnread(filterUnread) } just Runs
        coEvery { showSpamAndTrash(showSpamTrash) } just Runs
        every { getScrollerId() } returns DefaultScrollerId
    }

    private fun paginatorWrapperWithReloadEmittingReplaceFrom(
        callback: CapturingSlot<MessageScrollerLiveQueryCallback>,
        items: List<Message>,
        idx: ULong = 0u,
        filterUnread: Boolean = false,
        showSpamTrash: Boolean = false
    ): MessagePaginatorWrapper = mockk {
        coEvery { reload() } answers {
            callback.captured.onUpdate(
                MessageScrollerUpdate.List(
                    MessageScrollerListUpdate.ReplaceFrom(
                        idx = idx, items = items, scrollerId = DefaultScrollerId
                    )
                )
            )
            Unit.right()
        }
        coEvery { disconnect() } just Runs
        coEvery { filterUnread(filterUnread) } just Runs
        coEvery { showSpamAndTrash(showSpamTrash) } just Runs
        every { getScrollerId() } returns DefaultScrollerId
    }

    @Test
    fun `returns IllegalStateError when mailbox is null`() = runTest {
        // Given
        val pageKey = PageKey.DefaultPageKey()
        coEvery { rustMailboxFactory.create(userId) } returns DataError.Local.Unknown.left()
        val expected = PaginationError.Other(DataError.Local.IllegalStateError).left()

        // When
        val actual = rustMessageListQuery.getMessages(userId, pageKey)

        // Then
        assertEquals(expected, actual)
        verify { createRustMessagesPaginator wasNot Called }
        verify { createRustSearchPaginator wasNot Called }
    }

    @Test
    fun `returns first page when called with PageToLoad First`() = runTest {
        // Given
        val pageKey = PageKey.DefaultPageKey(labelId = inboxLabelId, pageToLoad = PageToLoad.First)
        val callback = slot<MessageScrollerLiveQueryCallback>()
        val paginator = paginatorWrapperWithNextEmitting(callback, expectedMessages)

        coEvery { rustMailboxFactory.create(userId) } returns mailbox.right()
        coEvery {
            createRustMessagesPaginator(
                mailbox = mailbox,
                callback = capture(callback)
            )
        } returns paginator.right()

        // When
        val actual = rustMessageListQuery.getMessages(userId, pageKey)

        // Then
        assertEquals(expectedMessages.right(), actual)
    }

    @Test
    fun `returns next page when called with PageToLoad Next`() = runTest {
        // Given
        val pageKey = PageKey.DefaultPageKey(labelId = inboxLabelId, pageToLoad = PageToLoad.Next)
        val callback = slot<MessageScrollerLiveQueryCallback>()
        val paginator = paginatorWrapperWithNextEmitting(callback, expectedMessages)

        coEvery { rustMailboxFactory.create(userId) } returns mailbox.right()
        coEvery {
            createRustMessagesPaginator(
                mailbox = mailbox,
                callback = capture(callback)
            )
        } returns paginator.right()

        // When
        val actual = rustMessageListQuery.getMessages(userId, pageKey)

        // Then
        assertEquals(expectedMessages.right(), actual)
    }

    @Test
    fun `returns all loaded items when called with PageToLoad All`() = runTest {
        // Given
        val pageKey = PageKey.DefaultPageKey(labelId = inboxLabelId, pageToLoad = PageToLoad.All)
        val callback = slot<MessageScrollerLiveQueryCallback>()
        val paginator = paginatorWrapperWithReloadEmittingReplaceFrom(callback, expectedMessages, idx = 0u)

        coEvery { rustMailboxFactory.create(userId) } returns mailbox.right()
        coEvery {
            createRustMessagesPaginator(
                mailbox = mailbox,
                callback = capture(callback)
            )
        } returns paginator.right()

        // When
        val actual = rustMessageListQuery.getMessages(userId, pageKey)

        // Then
        assertEquals(expectedMessages.right(), actual)
    }

    @Test
    fun `initialises paginator only once for the same descriptor`() = runTest {
        // Given
        val firstKey = PageKey.DefaultPageKey(labelId = inboxLabelId, pageToLoad = PageToLoad.First)
        val nextKey = firstKey.copy(pageToLoad = PageToLoad.Next)
        val callback = slot<MessageScrollerLiveQueryCallback>()
        val paginator = paginatorWrapperWithNextEmitting(callback, expectedMessages)

        coEvery { rustMailboxFactory.create(userId) } returns mailbox.right()
        coEvery {
            createRustMessagesPaginator(
                mailbox = mailbox,
                callback = capture(callback)
            )
        } returns paginator.right()

        // When
        rustMessageListQuery.getMessages(userId, firstKey)
        rustMessageListQuery.getMessages(userId, nextKey)

        // Then
        coVerify(exactly = 1) {
            createRustMessagesPaginator(
                mailbox = mailbox,
                callback = any()
            )
        }
    }

    @Test
    fun `reinitialises paginator when labelId changes`() = runTest {
        // Given
        val archive = SystemLabelId.Archive.labelId
        val firstKey = PageKey.DefaultPageKey(labelId = inboxLabelId)
        val secondKey = PageKey.DefaultPageKey(labelId = archive)

        val callback = slot<MessageScrollerLiveQueryCallback>()
        val paginator = paginatorWrapperWithNextEmitting(callback, expectedMessages)

        coEvery { rustMailboxFactory.create(userId) } returns mailbox.right()
        coEvery {
            createRustMessagesPaginator(
                mailbox = mailbox,
                callback = capture(callback)
            )
        } returns paginator.right()

        // When
        rustMessageListQuery.getMessages(userId, firstKey)
        rustMessageListQuery.getMessages(userId, secondKey)

        // Then
        coVerify(exactly = 2) {
            createRustMessagesPaginator(
                mailbox = mailbox,
                callback = any()
            )
        }
        coVerify { paginator.disconnect() }
    }

    @Test
    fun `updates flag on paginator without re-initializing it when unread filter is applied`() = runTest {
        // Given
        val firstKey = PageKey.DefaultPageKey(labelId = inboxLabelId)
        val secondKey = firstKey.copy(pageToLoad = PageToLoad.Next)

        val callback = slot<MessageScrollerLiveQueryCallback>()
        val paginator = paginatorWrapperWithNextEmitting(callback, expectedMessages)

        coEvery { paginator.filterUnread(true) } just Runs
        coEvery { rustMailboxFactory.create(userId) } returns mailbox.right()
        coEvery {
            createRustMessagesPaginator(
                mailbox = mailbox,
                callback = capture(callback)
            )
        } returns paginator.right()

        // When
        rustMessageListQuery.getMessages(userId, firstKey)
        rustMessageListQuery.updateUnreadFilter(true)

        // Then
        coVerify(exactly = 1) {
            createRustMessagesPaginator(
                mailbox = mailbox,
                callback = any()
            )
        }
        coVerify { paginator.filterUnread(true) }
    }

    @Test
    fun `updates flag on paginator without re-initializing when includeSpamTrash filter is applied`() = runTest {
        // Given
        val firstKey = PageKey.DefaultPageKey(labelId = inboxLabelId)

        val callback = slot<MessageScrollerLiveQueryCallback>()
        val paginator = paginatorWrapperWithNextEmitting(callback, expectedMessages)

        coEvery { paginator.showSpamAndTrash(true) } just Runs
        coEvery { rustMailboxFactory.create(userId) } returns mailbox.right()
        coEvery {
            createRustMessagesPaginator(
                mailbox = mailbox,
                callback = capture(callback)
            )
        } returns paginator.right()

        // When
        rustMessageListQuery.getMessages(userId, firstKey)
        rustMessageListQuery.updateShowSpamTrashFilter(true)

        // Then
        coVerify(exactly = 1) {
            createRustMessagesPaginator(
                mailbox = mailbox,
                callback = any()
            )
        }
        coVerify { paginator.showSpamAndTrash(true) }
    }

    @Test
    fun `Search - uses search paginator and returns first page`() = runTest {
        // Given
        val key = PageKey.PageKeyForSearch(keyword = "invoice", pageToLoad = PageToLoad.First)
        val callback = slot<MessageScrollerLiveQueryCallback>()
        val paginator = paginatorWrapperWithNextEmitting(callback, expectedMessages)

        coEvery { rustMailboxFactory.create(userId) } returns mailbox.right()
        coEvery {
            createRustSearchPaginator(
                mailbox = mailbox,
                keyword = "invoice",
                callback = capture(callback)
            )
        } returns paginator.right()

        // When
        val actual = rustMessageListQuery.getMessages(userId, key)

        // Then
        assertEquals(expectedMessages.right(), actual)
    }

    @Test
    fun `Search - reinitialises paginator when keyword changes`() = runTest {
        // Given
        val key1 = PageKey.PageKeyForSearch(keyword = "invoice")
        val key2 = PageKey.PageKeyForSearch(keyword = "report")

        val callback = slot<MessageScrollerLiveQueryCallback>()
        val paginator = paginatorWrapperWithNextEmitting(callback, expectedMessages)

        coEvery { rustMailboxFactory.create(userId) } returns mailbox.right()
        coEvery {
            createRustSearchPaginator(
                mailbox, "invoice", capture(callback)
            )
        } returns paginator.right()
        coEvery {
            createRustSearchPaginator(mailbox, "report", capture(callback))
        } returns paginator.right()

        // When
        rustMessageListQuery.getMessages(userId, key1)
        rustMessageListQuery.getMessages(userId, key2)

        // Then
        coVerify(exactly = 1) { createRustSearchPaginator(mailbox, "invoice", any()) }
        coVerify { paginator.disconnect() }
        coVerify(exactly = 1) { createRustSearchPaginator(mailbox, "report", any()) }
    }

    @Test
    fun `submits invalidation when ReplaceBefore is received when no pending request`() = runTest {
        // Given
        val labelId = SystemLabelId.Inbox.labelId
        val pageKey = PageKey.DefaultPageKey(labelId = labelId)

        val callback = slot<MessageScrollerLiveQueryCallback>()
        val paginator = paginatorWrapperWithNextEmitting(callback, expectedMessages)

        coEvery { rustMailboxFactory.create(userId) } returns mailbox.right()
        coEvery {
            createRustMessagesPaginator(
                mailbox = mailbox,
                callback = capture(callback)
            )
        } returns paginator.right()
        coEvery { invalidationRepository.submit(PageInvalidationEvent.MessagesInvalidated(id = 1)) } just Runs

        // When
        rustMessageListQuery.getMessages(userId, pageKey)

        // Then
        callback.captured.onUpdate(
            MessageScrollerUpdate.List(
                MessageScrollerListUpdate.ReplaceBefore(
                    idx = 1u, items = emptyList(), scrollerId = DefaultScrollerId
                )
            )
        )
        advanceUntilIdle()

        coVerify { invalidationRepository.submit(PageInvalidationEvent.MessagesInvalidated(id = 1)) }
    }

    @Test
    fun `Append None followed by ReplaceBefore(0) within grace returns follow-up items`() = runTest {
        // Given
        val pageKey = PageKey.DefaultPageKey(labelId = inboxLabelId, pageToLoad = PageToLoad.First)
        val expectedFollowUp = listOf(LocalMessageTestData.OctWeatherForecast)

        val callback = slot<MessageScrollerLiveQueryCallback>()
        val paginator = mockk<MessagePaginatorWrapper> {
            coEvery { nextPage() } answers {
                CoroutineScope(mainDispatcherRule.testDispatcher).launch {
                    callback.captured.onUpdate(
                        MessageScrollerUpdate.List(
                            MessageScrollerListUpdate.None(DefaultScrollerId)
                        )
                    )
                    delay(NONE_FOLLOWUP_GRACE_MS - 100)
                    callback.captured.onUpdate(
                        MessageScrollerUpdate.List(
                            MessageScrollerListUpdate.ReplaceBefore(
                                idx = 0u, items = expectedFollowUp, scrollerId = DefaultScrollerId
                            )
                        )
                    )
                }
                Unit.right()
            }
            coEvery { disconnect() } just Runs
            coEvery { filterUnread(false) } just Runs
            coEvery { showSpamAndTrash(false) } just Runs
            every { getScrollerId() } returns DefaultScrollerId
        }

        coEvery { rustMailboxFactory.create(userId) } returns mailbox.right()
        coEvery {
            createRustMessagesPaginator(
                mailbox = mailbox,
                callback = capture(callback)
            )
        } returns paginator.right()

        // When
        val actual = rustMessageListQuery.getMessages(userId, pageKey)

        // Then: we should get the follow-up items rather than an empty list
        assertEquals(expectedFollowUp.right(), actual)
    }

    @Test
    fun `Append None then follow-up arrives after grace returns empty list and clears pending request`() = runTest {
        // Given
        val pageKey = PageKey.DefaultPageKey(labelId = inboxLabelId, pageToLoad = PageToLoad.First)

        val callback = slot<MessageScrollerLiveQueryCallback>()
        val paginator = mockk<MessagePaginatorWrapper> {
            coEvery { nextPage() } answers {
                CoroutineScope(mainDispatcherRule.testDispatcher).launch {
                    callback.captured.onUpdate(
                        MessageScrollerUpdate.List(
                            MessageScrollerListUpdate.None(DefaultScrollerId)
                        )
                    )
                    delay(NONE_FOLLOWUP_GRACE_MS + 100)
                    callback.captured.onUpdate(
                        MessageScrollerUpdate.List(
                            MessageScrollerListUpdate.ReplaceBefore(
                                idx = 0u,
                                items = listOf(
                                    LocalMessageTestData.OctWeatherForecast
                                ),
                                scrollerId = DefaultScrollerId
                            )
                        )
                    )
                }
                Unit.right()
            }
            coEvery { disconnect() } just Runs
            coEvery { filterUnread(false) } just Runs
            coEvery { showSpamAndTrash(false) } just Runs
            every { getScrollerId() } returns DefaultScrollerId
        }
        coEvery {
            invalidationRepository.submit(
                match { it is PageInvalidationEvent.MessagesInvalidated }
            )
        } just Runs

        coEvery { rustMailboxFactory.create(userId) } returns mailbox.right()
        coEvery {
            createRustMessagesPaginator(
                mailbox = mailbox,
                callback = capture(callback)
            )
        } returns paginator.right()

        // When
        val actual = rustMessageListQuery.getMessages(userId, pageKey)

        // Then
        assertEquals(emptyList<Message>().right(), actual)
    }

    companion object {

        private const val DefaultScrollerId = "scroller-id"
    }
}
