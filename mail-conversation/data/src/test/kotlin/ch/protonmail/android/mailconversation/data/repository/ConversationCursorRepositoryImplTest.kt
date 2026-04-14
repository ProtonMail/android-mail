/*
 * Copyright (c) 2026 Proton Technologies AG
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

package ch.protonmail.android.mailconversation.data.repository

import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.ConversationCursorError
import ch.protonmail.android.mailcommon.domain.model.ConversationId
import ch.protonmail.android.mailcommon.domain.model.CursorId
import ch.protonmail.android.mailcommon.domain.model.CursorResult
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcommon.domain.sample.UserIdSample
import ch.protonmail.android.mailconversation.data.local.RustConversationsQuery
import ch.protonmail.android.mailconversation.data.usecase.CreateRustConversationPaginator
import ch.protonmail.android.mailconversation.data.wrapper.ConversationCursorWrapper
import ch.protonmail.android.mailconversation.data.wrapper.ConversationPaginatorWrapper
import ch.protonmail.android.maillabel.data.local.RustMailboxFactory
import ch.protonmail.android.maillabel.data.mapper.toLocalLabelId
import ch.protonmail.android.maillabel.data.wrapper.MailboxWrapper
import ch.protonmail.android.maillabel.domain.model.SystemLabelId
import ch.protonmail.android.mailmessage.data.mapper.toLocalConversationId
import ch.protonmail.android.mailpagination.domain.model.PaginationError
import ch.protonmail.android.test.utils.rule.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals
import io.mockk.Runs
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.slot
import org.junit.Rule
import uniffi.mail_uniffi.ConversationScrollerLiveQueryCallback
import kotlin.test.assertTrue

internal class ConversationCursorRepositoryImplTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val rustMailboxFactory = mockk<RustMailboxFactory>()
    private val createRustConversationPaginator = mockk<CreateRustConversationPaginator>()
    private val rustConversationsQuery = mockk<RustConversationsQuery>()

    private val repository = ConversationCursorRepositoryImpl(
        rustMailboxFactory = rustMailboxFactory,
        createRustConversationPaginator = createRustConversationPaginator,
        rustConversationsQuery = rustConversationsQuery
    )

    private val cursorWrapper = mockk<ConversationCursorWrapper> {
        coEvery { nextPage() } returns CursorResult.Cursor(ConversationId("101"))
        every { previousPage() } returns CursorResult.Cursor(ConversationId("99"))
        every { goForwards() } just Runs
        every { goBackwards() } just Runs
        every { disconnect() } just Runs
    }

    private val secondCursorWrapper = mockk<ConversationCursorWrapper> {
        coEvery { nextPage() } returns CursorResult.Cursor(ConversationId("201"))
        every { previousPage() } returns CursorResult.Cursor(ConversationId("299"))
        every { goForwards() } just Runs
        every { goBackwards() } just Runs
        every { disconnect() } just Runs
    }


    @Test
    fun `getCursor reuses active mailbox paginator cursor when available`() = runTest {
        // Given
        val userId = UserIdSample.Primary
        val labelId = SystemLabelId.Inbox.labelId
        val anchorItem = CursorId(ConversationId("100"), null)
        val anchorConversationId = anchorItem.conversationId.toLocalConversationId()

        coEvery {
            rustConversationsQuery.getCursorFromActivePaginator(
                userId = userId,
                labelId = labelId,
                anchorConversationId = anchorConversationId
            )
        } returns cursorWrapper.right()

        // When
        val actual = repository.getCursor(
            anchorItemId = anchorItem,
            userId = userId,
            labelId = labelId
        )

        // Then
        assertTrue(actual.isRight())
        assertEquals(
            ConversationId("100"),
            (actual.getOrNull()?.current as? CursorResult.Cursor)?.conversationId
        )
        coVerify(exactly = 1) {
            rustConversationsQuery.getCursorFromActivePaginator(
                userId = userId,
                labelId = labelId,
                anchorConversationId = anchorConversationId
            )
        }
        coVerify(exactly = 0) { rustMailboxFactory.create(any(), any()) }
        coVerify(exactly = 0) { createRustConversationPaginator(any(), any()) }
    }

    @Test
    fun `getCursor falls back to dedicated cursor paginator when active mailbox paginator returns null`() = runTest {
        // Given
        val userId = UserIdSample.Primary
        val labelId = SystemLabelId.Inbox.labelId
        val anchorItem = CursorId(ConversationId("100"), null)
        val anchorConversationId = anchorItem.conversationId.toLocalConversationId()
        val mailbox = mockk<MailboxWrapper>()
        val callbackSlot = slot<ConversationScrollerLiveQueryCallback>()

        val paginator = mockk<ConversationPaginatorWrapper> {
            every { getScrollerId() } returns "cursor-scroller-id"
            coEvery { disconnect() } just Runs
            coEvery { getCursor(anchorConversationId) } returns cursorWrapper.right()
        }

        coEvery {
            rustConversationsQuery.getCursorFromActivePaginator(
                userId = userId,
                labelId = labelId,
                anchorConversationId = anchorConversationId
            )
        } returns null

        coEvery {
            rustMailboxFactory.create(userId, labelId.toLocalLabelId())
        } returns mailbox.right()

        coEvery {
            createRustConversationPaginator(
                mailbox = mailbox,
                callback = capture(callbackSlot)
            )
        } returns paginator.right()

        // When
        val actual = repository.getCursor(
            anchorItemId = anchorItem,
            userId = userId,
            labelId = labelId
        )

        // Then
        assertTrue(actual.isRight())
        assertEquals(
            ConversationId("100"),
            (actual.getOrNull()?.current as? CursorResult.Cursor)?.conversationId
        )
        coVerify(exactly = 1) {
            rustMailboxFactory.create(userId, labelId.toLocalLabelId())
        }
        coVerify(exactly = 1) {
            createRustConversationPaginator(
                mailbox = mailbox,
                callback = any()
            )
        }
        coVerify(exactly = 1) { paginator.getCursor(anchorConversationId) }
    }

    @Test
    fun `getCursor falls back to dedicated cursor paginator when active mailbox paginator returns error`() = runTest {
        // Given
        val userId = UserIdSample.Primary
        val labelId = SystemLabelId.Inbox.labelId
        val anchorItem = CursorId(ConversationId("100"), null)
        val anchorConversationId = anchorItem.conversationId.toLocalConversationId()
        val mailbox = mockk<MailboxWrapper>()
        val callbackSlot = slot<ConversationScrollerLiveQueryCallback>()

        val paginator = mockk<ConversationPaginatorWrapper> {
            every { getScrollerId() } returns "cursor-scroller-id"
            coEvery { disconnect() } just Runs
            coEvery { getCursor(anchorConversationId) } returns cursorWrapper.right()
        }

        coEvery {
            rustConversationsQuery.getCursorFromActivePaginator(
                userId = userId,
                labelId = labelId,
                anchorConversationId = anchorConversationId
            )
        } returns PaginationError.Other(DataError.Local.IllegalStateError).left()

        coEvery {
            rustMailboxFactory.create(userId, labelId.toLocalLabelId())
        } returns mailbox.right()

        coEvery {
            createRustConversationPaginator(
                mailbox = mailbox,
                callback = capture(callbackSlot)
            )
        } returns paginator.right()

        // When
        val actual = repository.getCursor(
            anchorItemId = anchorItem,
            userId = userId,
            labelId = labelId
        )

        // Then
        assertTrue(actual.isRight())
        coVerify(exactly = 1) {
            rustMailboxFactory.create(userId, labelId.toLocalLabelId())
        }
        coVerify(exactly = 1) { paginator.getCursor(anchorConversationId) }
    }

    @Test
    fun `getCursor returns InvalidState when fallback mailbox cannot be created`() = runTest {
        // Given
        val userId = UserIdSample.Primary
        val labelId = SystemLabelId.Inbox.labelId
        val anchorItem = CursorId(ConversationId("100"), null)
        val anchorConversationId = anchorItem.conversationId.toLocalConversationId()

        coEvery {
            rustConversationsQuery.getCursorFromActivePaginator(
                userId = userId,
                labelId = labelId,
                anchorConversationId = anchorConversationId
            )
        } returns null

        coEvery {
            rustMailboxFactory.create(userId, labelId.toLocalLabelId())
        } returns DataError.Local.Unknown.left()

        // When
        val actual = repository.getCursor(
            anchorItemId = anchorItem,
            userId = userId,
            labelId = labelId
        )

        // Then
        assertEquals(ConversationCursorError.InvalidState.left(), actual)
        coVerify(exactly = 0) { createRustConversationPaginator(any(), any()) }
    }

    @Test
    fun `getCursor reuses existing dedicated cursor paginator for same user and label`() = runTest {
        // Given
        val userId = UserIdSample.Primary
        val labelId = SystemLabelId.Inbox.labelId
        val anchorItem = CursorId(ConversationId("100"), null)
        val secondAnchorItem = CursorId(ConversationId("200"), null)
        val firstConversationId = anchorItem.conversationId.toLocalConversationId()
        val secondConversationId = secondAnchorItem.conversationId.toLocalConversationId()
        val mailbox = mockk<MailboxWrapper>()
        val callbackSlot = slot<ConversationScrollerLiveQueryCallback>()

        val paginator = mockk<ConversationPaginatorWrapper> {
            every { getScrollerId() } returns "cursor-scroller-id"
            coEvery { disconnect() } just Runs
            coEvery { getCursor(firstConversationId) } returns cursorWrapper.right()
            coEvery { getCursor(secondConversationId) } returns secondCursorWrapper.right()
        }

        coEvery {
            rustConversationsQuery.getCursorFromActivePaginator(
                userId = userId,
                labelId = labelId,
                anchorConversationId = any()
            )
        } returns null

        coEvery {
            rustMailboxFactory.create(userId, labelId.toLocalLabelId())
        } returns mailbox.right()

        coEvery {
            createRustConversationPaginator(
                mailbox = mailbox,
                callback = capture(callbackSlot)
            )
        } returns paginator.right()

        // When
        val firstResult = repository.getCursor(
            anchorItemId = anchorItem,
            userId = userId,
            labelId = labelId
        )
        val secondResult = repository.getCursor(
            anchorItemId = secondAnchorItem,
            userId = userId,
            labelId = labelId
        )

        // Then
        assertTrue(firstResult.isRight())
        assertTrue(secondResult.isRight())
        coVerify(exactly = 1) {
            createRustConversationPaginator(
                mailbox = mailbox,
                callback = any()
            )
        }
        coVerify(exactly = 1) { paginator.getCursor(firstConversationId) }
        coVerify(exactly = 1) { paginator.getCursor(secondConversationId) }
    }

    @Test
    fun `getCursor recreates dedicated cursor paginator when label changes`() = runTest {
        // Given
        val userId = UserIdSample.Primary
        val firstLabelId = SystemLabelId.Inbox.labelId
        val secondLabelId = SystemLabelId.Archive.labelId
        val anchorItem = CursorId(ConversationId("100"), null)
        val secondAnchorItem = CursorId(ConversationId("200"), null)
        val firstConversationId = anchorItem.conversationId.toLocalConversationId()
        val secondConversationId = secondAnchorItem.conversationId.toLocalConversationId()

        val firstMailbox = mockk<MailboxWrapper>()
        val secondMailbox = mockk<MailboxWrapper>()
        val firstCallbackSlot = slot<ConversationScrollerLiveQueryCallback>()
        val secondCallbackSlot = slot<ConversationScrollerLiveQueryCallback>()

        val firstPaginator = mockk<ConversationPaginatorWrapper> {
            every { getScrollerId() } returns "cursor-scroller-id-1"
            coEvery { disconnect() } just Runs
            coEvery { getCursor(firstConversationId) } returns cursorWrapper.right()
        }

        val secondPaginator = mockk<ConversationPaginatorWrapper> {
            every { getScrollerId() } returns "cursor-scroller-id-2"
            coEvery { disconnect() } just Runs
            coEvery { getCursor(secondConversationId) } returns secondCursorWrapper.right()
        }

        coEvery {
            rustConversationsQuery.getCursorFromActivePaginator(
                userId = userId,
                labelId = any(),
                anchorConversationId = any()
            )
        } returns null

        coEvery {
            rustMailboxFactory.create(userId, firstLabelId.toLocalLabelId())
        } returns firstMailbox.right()
        coEvery {
            rustMailboxFactory.create(userId, secondLabelId.toLocalLabelId())
        } returns secondMailbox.right()

        coEvery {
            createRustConversationPaginator(
                mailbox = firstMailbox,
                callback = capture(firstCallbackSlot)
            )
        } returns firstPaginator.right()
        coEvery {
            createRustConversationPaginator(
                mailbox = secondMailbox,
                callback = capture(secondCallbackSlot)
            )
        } returns secondPaginator.right()

        // When
        val firstResult = repository.getCursor(
            anchorItemId = anchorItem,
            userId = userId,
            labelId = firstLabelId
        )
        val secondResult = repository.getCursor(
            anchorItemId = secondAnchorItem,
            userId = userId,
            labelId = secondLabelId
        )

        // Then
        assertTrue(firstResult.isRight())
        assertTrue(secondResult.isRight())
        coVerify(exactly = 1) { firstPaginator.disconnect() }
        coVerify(exactly = 1) { firstPaginator.getCursor(firstConversationId) }
        coVerify(exactly = 1) { secondPaginator.getCursor(secondConversationId) }
    }
}
