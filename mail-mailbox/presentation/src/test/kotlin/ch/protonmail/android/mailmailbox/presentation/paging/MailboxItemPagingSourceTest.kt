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

import android.util.Log
import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.room.InvalidationTracker
import androidx.room.RoomDatabase
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.maillabel.domain.model.MailLabelId
import ch.protonmail.android.maillabel.domain.model.MailLabelId.System.Inbox
import ch.protonmail.android.mailmailbox.domain.model.MailboxItem
import ch.protonmail.android.mailmailbox.domain.model.MailboxItemType
import ch.protonmail.android.mailmailbox.domain.model.MailboxPageKey
import ch.protonmail.android.mailmailbox.domain.usecase.GetMultiUserMailboxItems
import ch.protonmail.android.mailmailbox.domain.usecase.IsMultiUserLocalPageValid
import ch.protonmail.android.mailpagination.domain.AdjacentPageKeys
import ch.protonmail.android.mailpagination.domain.GetAdjacentPageKeys
import ch.protonmail.android.mailpagination.domain.model.OrderBy
import ch.protonmail.android.mailpagination.domain.model.OrderDirection
import ch.protonmail.android.mailpagination.domain.model.PageFilter
import ch.protonmail.android.mailpagination.domain.model.PageKey
import ch.protonmail.android.mailpagination.domain.model.ReadStatus
import ch.protonmail.android.testdata.mailbox.MailboxTestData.buildMailboxItem
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import me.proton.core.label.domain.entity.LabelId
import org.junit.Test
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MailboxItemPagingSourceTest {

    private val userId = UserId("1")
    private val pageKey = buildPageKey()
    private val prevKey = buildPageKey(filter = pageKey.filter.copy(minTime = 1234, maxTime = Long.MAX_VALUE))
    private val nextKey = buildPageKey(filter = pageKey.filter.copy(maxTime = 1234, minTime = Long.MIN_VALUE))
    private val mailboxPageKey = MailboxPageKey(listOf(userId), pageKey)
    private val type = MailboxItemType.Message
    private val mailboxItems = listOf(
        buildMailboxItem(userId, "5", time = 5000),
        buildMailboxItem(userId, "4", time = 4000),
        buildMailboxItem(userId, "3", time = 3000),
        buildMailboxItem(userId, "2", time = 2000),
        buildMailboxItem(userId, "1", time = 1000)
    )

    private val mockInvalidationTracker = mockk<InvalidationTracker> {
        every { this@mockk.addObserver(any()) } just Runs
        every { this@mockk.addWeakObserver(any()) } just Runs
        every { this@mockk.removeObserver(any()) } just Runs
    }

    private val roomDatabase = mockk<RoomDatabase> {
        every { this@mockk.invalidationTracker } returns mockInvalidationTracker
    }

    private val getMailboxItems = mockk<GetMultiUserMailboxItems> {
        coEvery { this@mockk.invoke(type = any(), pageKey = mailboxPageKey) } returns emptyList<MailboxItem>().right()
    }

    private val getAdjacentPageKeys = mockk<GetAdjacentPageKeys> {
        every { this@mockk.invoke(any(), any(), any()) } returns AdjacentPageKeys(prevKey, pageKey, nextKey)
    }
    private val isMultiUserLocalPageValid = mockk<IsMultiUserLocalPageValid> {
        coEvery { this@mockk.invoke(type, any()) } returns true
    }

    @BeforeTest
    fun setUp() {
        mockkStatic(Log::class)
        every { Log.isLoggable(any(), any()) } returns false
    }

    @AfterTest
    fun tearDown() {
        unmockkStatic(Log::class)
    }

    @Test
    fun `when local data is empty but valid then result page with next and prev key is returned`() = runTest {
        // Given
        coEvery {
            getMailboxItems.invoke(type = type, pageKey = mailboxPageKey)
        } returns emptyList<MailboxItem>().right()

        assertEquals(
            // When
            actual = buildPagingSource().load(
                PagingSource.LoadParams.Refresh(key = mailboxPageKey, loadSize = 25, false)
            ),
            // Then
            expected = PagingSource.LoadResult.Page<MailboxPageKey, MailboxItem>(
                data = emptyList(),
                prevKey = buildMailboxPageKey(prevKey),
                nextKey = buildMailboxPageKey(nextKey)
            )
        )
        coVerify { mockInvalidationTracker.addWeakObserver(any()) }
    }

    @Test
    fun `when local data is empty and not valid then result page without next and prev key is returned`() = runTest {
        // Given
        coEvery {
            getMailboxItems.invoke(type = type, pageKey = mailboxPageKey)
        } returns emptyList<MailboxItem>().right()
        coEvery {
            isMultiUserLocalPageValid.invoke(type = type, pageKey = buildMailboxPageKey(prevKey))
        } returns false
        coEvery {
            isMultiUserLocalPageValid.invoke(type = type, pageKey = buildMailboxPageKey(nextKey))
        } returns false

        val expected = PagingSource.LoadResult.Page<MailboxPageKey, MailboxItem>(
            data = emptyList(),
            prevKey = null,
            nextKey = null
        )

        // When
        val actual =
            buildPagingSource().load(PagingSource.LoadParams.Refresh(key = mailboxPageKey, loadSize = 25, false))

        // Then
        assertEquals(actual, expected)
        coVerify { mockInvalidationTracker.addWeakObserver(any()) }
    }

    @Test
    fun `when appending and local data is empty, then load result page without next key is returned`() = runTest {
        // Given
        coEvery {
            getMailboxItems.invoke(type = type, pageKey = mailboxPageKey)
        } returns emptyList<MailboxItem>().right()

        val expected = PagingSource.LoadResult.Page<MailboxPageKey, MailboxItem>(
            data = emptyList(),
            prevKey = buildMailboxPageKey(prevKey),
            nextKey = null
        )

        // When
        val actual =
            buildPagingSource().load(PagingSource.LoadParams.Append(key = mailboxPageKey, loadSize = 25, false))

        // Then
        assertEquals(actual, expected)
    }

    @Test
    fun `when appending, and local data is not empty, then load result page with next and prev key is returned`() =
        runTest {
            // Given
            coEvery {
                getMailboxItems.invoke(type = type, pageKey = mailboxPageKey)
            } returns mailboxItems.right()

            val expected = PagingSource.LoadResult.Page(
                data = mailboxItems,
                prevKey = buildMailboxPageKey(prevKey),
                nextKey = buildMailboxPageKey(nextKey)
            )

            // When
            val actual =
                buildPagingSource().load(PagingSource.LoadParams.Append(key = mailboxPageKey, loadSize = 25, false))

            // Then
            assertEquals(actual, expected)
        }

    @Test
    fun `when prepending, and local data is empty, then load result page without prev key is returned`() = runTest {
        // Given
        coEvery {
            getMailboxItems.invoke(type = type, pageKey = mailboxPageKey)
        } returns emptyList<MailboxItem>().right()

        val expected = PagingSource.LoadResult.Page<MailboxPageKey, MailboxItem>(
            data = emptyList(),
            prevKey = null,
            nextKey = buildMailboxPageKey(nextKey)
        )

        // When
        val actual =
            buildPagingSource().load(PagingSource.LoadParams.Prepend(key = mailboxPageKey, loadSize = 25, false))

        // Then
        assertEquals(actual, expected)
    }

    @Test
    fun `when prepending, and local data is not empty, then load result page with prev and next key is returned`() =
        runTest {
            // Given
            coEvery {
                getMailboxItems.invoke(type = type, pageKey = mailboxPageKey)
            } returns mailboxItems.right()

            val expected = PagingSource.LoadResult.Page(
                data = mailboxItems,
                prevKey = buildMailboxPageKey(prevKey),
                nextKey = buildMailboxPageKey(nextKey)
            )

            // When
            val actual =
                buildPagingSource().load(PagingSource.LoadParams.Prepend(key = mailboxPageKey, loadSize = 25, false))

            // Then
            assertEquals(actual, expected)
        }

    @Test
    fun `when loading items fails, result page without items and null for next and prev key is returned`() = runTest {
        // Given
        coEvery { getMailboxItems.invoke(type = any(), pageKey = any()) } returns DataError.Local.Unknown.left()

        // when
        val result = buildPagingSource()
            .load(PagingSource.LoadParams.Refresh(key = null, loadSize = 25, false))

        // then
        assertIs<PagingSource.LoadResult.Page<MailboxPageKey, MailboxItem>>(result)
        assertEquals(emptyList(), result.data)
        assertNull(result.nextKey)
        assertNull(result.prevKey)
    }

    @Test
    fun `paging source invalidate returns invalid load result`() = runTest {
        // Given
        val pagingSource = buildPagingSource()
        assertFalse(pagingSource.invalid)
        pagingSource.invalidate()
        assertTrue(pagingSource.invalid)

        // When
        val loadResult = pagingSource.load(
            PagingSource.LoadParams.Refresh(key = null, loadSize = 5, false)
        )

        // Then
        assertIs<PagingSource.LoadResult.Invalid<MailboxPageKey, MailboxItem>>(loadResult)
    }

    @Test
    fun `given the paging state contains no pages, then getRefreshKey returns null`() = runTest {
        // Given
        val pagingSource = buildPagingSource()

        // When
        val refreshKey = pagingSource.getRefreshKey(
            PagingState(
                pages = emptyList(),
                anchorPosition = null,
                config = PagingConfig(pageSize = 5, initialLoadSize = 15),
                leadingPlaceholderCount = 0
            )
        )

        // Then
        assertNull(refreshKey)
    }

    @Test
    fun `given the paging state contains pages with data, then getRefreshKey returns non null key`() = runTest {
        // When
        val refreshKey = buildPagingSource().getRefreshKey(
            PagingState(
                pages = buildMockPages(),
                anchorPosition = null,
                config = PagingConfig(pageSize = 5, initialLoadSize = 15),
                leadingPlaceholderCount = 0
            )
        )

        // Then
        assertEquals(
            expected = MailboxPageKey(
                userIds = listOf(userId),
                pageKey = PageKey(
                    filter = PageFilter(
                        labelId = LabelId("0"),
                        keyword = "",
                        read = ReadStatus.All,
                        minTime = 1000,
                        maxTime = 5000,
                        minOrder = 1000,
                        maxOrder = 1000,
                        minId = null,
                        maxId = null
                    ),
                    orderBy = OrderBy.Time,
                    orderDirection = OrderDirection.Descending,
                    size = 25
                )
            ),
            actual = refreshKey
        )
    }

    @Test
    fun `paging source get refresh key returns key which maintains existing page filter`() = runTest {
        // Given
        val archiveUnreadPageKey = buildPageKey(ReadStatus.Unread, MailLabelId.System.Archive)

        // When
        val refreshKey = buildPagingSource(archiveUnreadPageKey).getRefreshKey(
            PagingState(
                pages = buildMockPages(),
                anchorPosition = null,
                config = PagingConfig(pageSize = 5, initialLoadSize = 15),
                leadingPlaceholderCount = 0
            )
        )

        // Then
        assertEquals(
            expected = MailboxPageKey(
                userIds = listOf(userId),
                pageKey = PageKey(
                    filter = PageFilter(
                        labelId = LabelId("6"),
                        keyword = "",
                        read = ReadStatus.Unread,
                        minTime = 1000,
                        maxTime = 5000,
                        minOrder = 1000,
                        maxOrder = 1000,
                        minId = null,
                        maxId = null
                    ),
                    orderBy = OrderBy.Time,
                    orderDirection = OrderDirection.Descending,
                    size = 25
                )
            ),
            actual = refreshKey
        )
    }

    @Test
    fun `given paging state contains one item only, then getRefreshKey returns key with max time to INF`() = runTest {
        /*
         * MAILANDR-854: This prevents a crash when refreshing Drafts location after updating a draft.
         * Issue was caused by the refresh key relying on the time of the item in the list which
         * is not valid anymore after the draft was updated on the backend (as time is updated).
         * Keeping the interval open on "maxTime" side allows finding the item after the time was updated.
         */
        // When
        val refreshKey = buildPagingSource().getRefreshKey(
            PagingState(
                pages = buildMockPages(listOf(buildMailboxItem(userId, "1", time = 1000))),
                anchorPosition = null,
                config = PagingConfig(pageSize = 5, initialLoadSize = 15),
                leadingPlaceholderCount = 0
            )
        )

        // Then
        assertEquals(
            expected = MailboxPageKey(
                userIds = listOf(userId),
                pageKey = PageKey(
                    filter = PageFilter(
                        labelId = LabelId("0"),
                        keyword = "",
                        read = ReadStatus.All,
                        minTime = 1000,
                        maxTime = Long.MAX_VALUE,
                        minOrder = 1000,
                        maxOrder = 1000,
                        minId = null,
                        maxId = null
                    ),
                    orderBy = OrderBy.Time,
                    orderDirection = OrderDirection.Descending,
                    size = 25
                )
            ),
            actual = refreshKey
        )
    }

    @Test
    fun `adjacent page keys are loaded using initial page size`() = runTest {
        // Paging implementation params.loadSize is by default 3x the (initial) pageSize,
        // for the first load. We take the max to respect this value.
        // For adjacent pages, we just want to normal pageSize.

        // Given
        val items = listOf(
            buildMailboxItem(userId, "2", time = 2000),
            buildMailboxItem(userId, "1", time = 1000)
        )
        coEvery {
            getMailboxItems.invoke(type = type, pageKey = buildMailboxPageKey(pageKey.copy(size = 100)))
        } returns items.right()

        // When
        buildPagingSource().loadPage(
            PagingSource.LoadParams.Refresh(key = mailboxPageKey, loadSize = 100, false)
        )

        // Then
        val initialPageKeySize = 25 // PageKey.defaultPageSize
        verify { getAdjacentPageKeys(items, mailboxPageKey.pageKey.copy(size = 100), initialPageKeySize) }
    }

    @Test
    fun `when loading page with equal prev and next keys, then load result page with next key only is returned`() =
        runTest {
            // Given
            val sameKey = nextKey.copy(filter = PageFilter(labelId = LabelId("5")))
            coEvery { getMailboxItems.invoke(type = type, pageKey = mailboxPageKey) } returns mailboxItems.right()
            every { getAdjacentPageKeys(any(), any(), any()) } returns AdjacentPageKeys(sameKey, pageKey, sameKey)

            val expected = PagingSource.LoadResult.Page(
                data = mailboxItems,
                prevKey = null,
                nextKey = buildMailboxPageKey(sameKey)
            )

            // When
            val actual = buildPagingSource().load(
                PagingSource.LoadParams.Prepend(key = mailboxPageKey, loadSize = 25, false)
            )

            // Then
            assertEquals(actual, expected)
        }


    private fun buildPagingSource(pageKey: PageKey = buildPageKey()) = MailboxItemPagingSource(
        roomDatabase = roomDatabase,
        getMailboxItems = getMailboxItems,
        getAdjacentPageKeys = getAdjacentPageKeys,
        isMultiUserLocalPageValid = isMultiUserLocalPageValid,
        mailboxPageKey = mailboxPageKey.copy(pageKey = pageKey),
        type = type
    )

    private fun buildMockPages(
        items: List<MailboxItem> = mailboxItems
    ): List<PagingSource.LoadResult.Page<MailboxPageKey, MailboxItem>> = listOf(
        PagingSource.LoadResult.Page(
            data = items,
            prevKey = null,
            nextKey = null
        )
    )

    private fun buildPageKey(
        readState: ReadStatus = ReadStatus.All,
        selectedLabelId: MailLabelId = Inbox,
        filter: PageFilter = PageFilter(labelId = selectedLabelId.labelId, read = readState)
    ) = PageKey(filter = filter)

    private fun buildMailboxPageKey(pageKey: PageKey) = mailboxPageKey.copy(pageKey = pageKey)
}
