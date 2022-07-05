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

import java.io.IOException
import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.room.InvalidationTracker
import androidx.room.RoomDatabase
import androidx.room.getQueryDispatcher
import ch.protonmail.android.maillabel.domain.model.MailLabelId
import ch.protonmail.android.maillabel.domain.model.MailLabelId.System.Inbox
import ch.protonmail.android.mailmailbox.domain.model.MailboxItem
import ch.protonmail.android.mailmailbox.domain.model.MailboxItemType
import ch.protonmail.android.mailmailbox.domain.model.MailboxPageKey
import ch.protonmail.android.mailmailbox.domain.usecase.GetMultiUserMailboxItems
import ch.protonmail.android.mailmailbox.presentation.getMailboxItem
import ch.protonmail.android.mailpagination.domain.AdjacentPageKeys
import ch.protonmail.android.mailpagination.domain.GetAdjacentPageKeys
import ch.protonmail.android.mailpagination.domain.entity.OrderBy
import ch.protonmail.android.mailpagination.domain.entity.OrderDirection
import ch.protonmail.android.mailpagination.domain.entity.PageFilter
import ch.protonmail.android.mailpagination.domain.entity.PageKey
import ch.protonmail.android.mailpagination.domain.entity.ReadStatus
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import me.proton.core.label.domain.entity.LabelId
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MailboxItemPagingSourceTest {

    private val userId = UserId("1")

    private val mockInvalidationTracker = mockk<InvalidationTracker> {
        every { this@mockk.addObserver(any()) } just Runs
        every { this@mockk.addWeakObserver(any()) } just Runs
        every { this@mockk.removeObserver(any()) } just Runs
    }

    private val roomDatabase = mockk<RoomDatabase> {
        mockkStatic(RoomDatabase::getQueryDispatcher)
        every { this@mockk.getQueryDispatcher() } returns UnconfinedTestDispatcher()
        every { this@mockk.invalidationTracker } returns mockInvalidationTracker
    }

    private val getMailboxItems = mockk<GetMultiUserMailboxItems> {
        coEvery { this@mockk.invoke(type = any(), pageKey = any()) } returns emptyList()
    }

    private val getAdjacentPageKeys = mockk<GetAdjacentPageKeys> {
        every { this@mockk.invoke(any(), any(), any()) } returns AdjacentPageKeys(PageKey(), PageKey(), PageKey())
    }

    private var isFilterUnreadEnabled = false
    private var selectedLabelId: MailLabelId = Inbox

    private val pagingSource by lazy {
        MailboxItemPagingSource(
            roomDatabase = roomDatabase,
            getMailboxItems = getMailboxItems,
            getAdjacentPageKeys = getAdjacentPageKeys,
            userIds = listOf(userId),
            selectedMailLabelId = selectedLabelId,
            filterUnread = isFilterUnreadEnabled,
            type = MailboxItemType.Message
        )
    }

    @Test
    fun `pagingSource load emptyList`() = runTest {
        // Given
        coEvery { getMailboxItems.invoke(type = any(), pageKey = any()) } returns emptyList()

        assertEquals(
            // When
            actual = pagingSource.load(
                PagingSource.LoadParams.Refresh(key = null, loadSize = 25, false)
            ),
            // Then
            expected = PagingSource.LoadResult.Page<MailboxPageKey, MailboxItem>(
                data = emptyList(),
                prevKey = null,
                nextKey = null
            ),
        )
        coVerify { mockInvalidationTracker.addWeakObserver(any()) }
    }

    @Test
    fun `pagingSource load error`() = runTest {
        // Given
        val exception = IOException("test")
        coEvery { getMailboxItems.invoke(type = any(), pageKey = any()) } throws exception

        assertEquals(
            // When
            actual = pagingSource.load(
                PagingSource.LoadParams.Refresh(key = null, loadSize = 25, false)
            ),
            // Then
            expected = PagingSource.LoadResult.Error(
                throwable = exception
            ),
        )
    }

    @Test
    fun `pagingSource invalidate return LoadResult Invalid`() = runTest {
        // Given
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
    fun `pagingSource load return LoadResult Page with items and adjacent keys`() = runTest {
        // Given
        assertFalse(pagingSource.keyReuseSupported)
        coEvery { getMailboxItems.invoke(type = any(), pageKey = any()) } returns listOf(
            getMailboxItem(userId, "5", time = 5000),
            getMailboxItem(userId, "4", time = 4000),
            getMailboxItem(userId, "3", time = 3000),
            getMailboxItem(userId, "2", time = 2000),
            getMailboxItem(userId, "1", time = 1000),
        )

        // When
        val loadResult = pagingSource.load(
            PagingSource.LoadParams.Refresh(key = null, loadSize = 5, false)
        )

        // Then
        assertIs<PagingSource.LoadResult.Page<MailboxPageKey, MailboxItem>>(loadResult)
        assertEquals(5, loadResult.data.size)
        assertNotNull(loadResult.prevKey)
        assertNotNull(loadResult.nextKey)
    }

    @Test
    fun `pagingSource getRefreshKey return null key`() = runTest {
        // Given

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
    fun `pagingSource getRefreshKey return non null key`() = runTest {
        // Given

        // When
        val refreshKey = pagingSource.getRefreshKey(
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
    fun `pagingSource getRefreshKey returns key which maintains existing PageFilter`() = runTest {
        // Given
        isFilterUnreadEnabled = true
        selectedLabelId = MailLabelId.System.Archive

        // When
        val refreshKey = pagingSource.getRefreshKey(
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
    fun `adjacent page keys are loaded using initial page size`() = runTest {
        // Paging implementation params.loadSize is by default 3x the (initial) pageSize,
        // for the first load. We take the max to respect this value.
        // For adjacent pages, we just want to normal pageSize.

        // Given
        val items = listOf(
            getMailboxItem(userId, "2", time = 2000),
            getMailboxItem(userId, "1", time = 1000)
        )
        coEvery { getMailboxItems.invoke(type = any(), pageKey = any()) } returns items

        // When
        pagingSource.loadPage(
            PagingSource.LoadParams.Refresh(key = null, loadSize = 100, false)
        )

        // Then
        val initialPageKeySize = 25 // PageKey.defaultPageSize
        verify { getAdjacentPageKeys(items, any(), initialPageKeySize) }
    }

    private fun buildMockPages(): List<PagingSource.LoadResult.Page<MailboxPageKey, MailboxItem>> =
        listOf(
            PagingSource.LoadResult.Page(
                data = listOf(
                    getMailboxItem(userId, "5", time = 5000),
                    getMailboxItem(userId, "4", time = 4000),
                    getMailboxItem(userId, "3", time = 3000),
                    getMailboxItem(userId, "2", time = 2000),
                    getMailboxItem(userId, "1", time = 1000),
                ),
                prevKey = null,
                nextKey = null
            )
        )
}
