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

package ch.protonmail.android.mailpagination.data.local

import ch.protonmail.android.mailpagination.data.local.dao.PageIntervalDao
import ch.protonmail.android.mailpagination.data.local.entity.PageIntervalEntity
import ch.protonmail.android.mailpagination.domain.model.OrderBy
import ch.protonmail.android.mailpagination.domain.model.OrderDirection
import ch.protonmail.android.mailpagination.domain.model.PageFilter
import ch.protonmail.android.mailpagination.domain.model.PageItem
import ch.protonmail.android.mailpagination.domain.model.PageItemType
import ch.protonmail.android.mailpagination.domain.model.PageKey
import ch.protonmail.android.mailpagination.domain.model.ReadStatus
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.spyk
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import me.proton.core.label.domain.entity.LabelId
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

class PageIntervalUpdaterTest {

    private val userId = UserId("1")
    private val type = PageItemType.Message
    private val orderBy = OrderBy.Time
    private val labelId = LabelId("0")
    private val keyword = ""
    private val read = ReadStatus.All

    private val items = mutableSetOf<PageIntervalEntity>()

    @RelaxedMockK
    private val dao: PageIntervalDao = spyk {
        val inserted = mutableListOf<PageIntervalEntity>()
        coEvery {
            insertOrIgnore(*varargAll { inserted.add(it) })
        } answers {
            items.addAll(inserted)
        }

        val updated = mutableListOf<PageIntervalEntity>()
        coEvery {
            update(*varargAll { updated.add(it) })
        } answers {
            items.addAll(updated)
            updated.size
        }

        val deleted = mutableListOf<PageIntervalEntity>()
        coEvery {
            delete(*varargAll { deleted.add(it) })
        } answers {
            items.removeAll(deleted)
        }

        coEvery {
            getAll(any(), any(), any(), any(), any(), any())
        } answers {
            items.toList()
        }
    }

    data class FakeItem(
        override val userId: UserId = UserId("1"),
        override val id: String,
        override val time: Long,
        override val size: Long = 1000,
        override val order: Long = time,
        override val read: Boolean = false,
        override val labelIds: List<LabelId> = emptyList(),
        override val keywords: String = ""
    ) : PageItem

    private fun getInterval(
        minValue: Long,
        maxValue: Long,
        minId: String? = null,
        maxId: String? = null
    ) = PageIntervalEntity(
        userId = userId,
        type = type,
        orderBy = orderBy,
        labelId = labelId,
        keyword = keyword,
        read = read,
        minValue = minValue,
        maxValue = maxValue,
        minOrder = minValue,
        maxOrder = maxValue,
        minId = minId,
        maxId = maxId
    )

    private fun getPageKey(
        size: Int,
        orderDirection: OrderDirection = OrderDirection.Ascending,
        minTime: Long = Long.MIN_VALUE,
        maxTime: Long = Long.MAX_VALUE
    ) = PageKey(
        size = size,
        orderDirection = orderDirection,
        filter = PageFilter(
            minTime = minTime,
            minOrder = minTime,
            maxTime = maxTime,
            maxOrder = maxTime
        )
    )

    @Before
    fun setup() {
        MockKAnnotations.init(this)
    }

    @Test
    fun `page contains 2 on 10 items, interval is Long MIN to Long MAX`() = runTest {
        // When
        dao.upsertPageInterval(
            userId = userId,
            type = type,
            pageKey = getPageKey(size = 10, minTime = Long.MIN_VALUE, maxTime = Long.MAX_VALUE),
            items = listOf(
                FakeItem(id = "1", time = 1000),
                FakeItem(id = "2", time = 2000)
            )
        )

        val actual = dao.getAll(userId, type, orderBy, labelId, keyword, read)

        val expected = listOf(
            getInterval(minValue = Long.MIN_VALUE, maxValue = Long.MAX_VALUE)
        )

        // Then
        assertEquals(expected, actual)
    }

    @Test
    fun `page contains 10 on 10 items, interval is Long MIN to last time`() = runTest {
        // When
        dao.upsertPageInterval(
            userId = userId,
            type = type,
            pageKey = getPageKey(size = 10, minTime = Long.MIN_VALUE, maxTime = Long.MAX_VALUE),
            items = listOf(
                FakeItem(id = "1", time = 1000),
                FakeItem(id = "2", time = 2000),
                FakeItem(id = "3", time = 3000),
                FakeItem(id = "4", time = 4000),
                FakeItem(id = "5", time = 5000),
                FakeItem(id = "6", time = 6000),
                FakeItem(id = "7", time = 7000),
                FakeItem(id = "8", time = 8000),
                FakeItem(id = "9", time = 9000),
                FakeItem(id = "10", time = 10_000)
            )
        )

        val actual = dao.getAll(userId, type, orderBy, labelId, keyword, read)

        val expected = listOf(
            getInterval(minValue = Long.MIN_VALUE, maxValue = 10_000, maxId = "10")
        )

        // Then
        assertEquals(expected, actual)
    }

    @Test
    fun `page contains 10 on 10 items, interval is first time to last time`() = runTest {
        // When
        dao.upsertPageInterval(
            userId = userId,
            type = type,
            pageKey = getPageKey(size = 10, minTime = 1000, maxTime = Long.MAX_VALUE),
            items = listOf(
                FakeItem(id = "1", time = 1000),
                FakeItem(id = "2", time = 2000),
                FakeItem(id = "3", time = 3000),
                FakeItem(id = "4", time = 4000),
                FakeItem(id = "5", time = 5000),
                FakeItem(id = "6", time = 6000),
                FakeItem(id = "7", time = 7000),
                FakeItem(id = "8", time = 8000),
                FakeItem(id = "9", time = 9000),
                FakeItem(id = "10", time = 10_000)
            )
        )

        val actual = dao.getAll(userId, type, orderBy, labelId, keyword, read)

        val expected = listOf(
            getInterval(minValue = 1000, maxValue = 10_000, maxId = "10")
        )

        // Then
        assertEquals(expected, actual)
    }

    @Test
    fun `page contains 10 on 10 items, intervals are merged`() = runTest {
        // Given: already exiting interval.
        items.addAll(
            listOf(
                getInterval(minValue = 0, maxValue = 1000, maxId = "1")
            )
        )
        // When
        dao.upsertPageInterval(
            userId = userId,
            type = type,
            pageKey = getPageKey(size = 10, minTime = 1000, maxTime = Long.MAX_VALUE),
            items = listOf(
                FakeItem(id = "1", time = 1000),
                FakeItem(id = "2", time = 2000),
                FakeItem(id = "3", time = 3000),
                FakeItem(id = "4", time = 4000),
                FakeItem(id = "5", time = 5000),
                FakeItem(id = "6", time = 6000),
                FakeItem(id = "7", time = 7000),
                FakeItem(id = "8", time = 8000),
                FakeItem(id = "9", time = 9000),
                FakeItem(id = "10", time = 10_000)
            )
        )

        val actual = dao.getAll(userId, type, orderBy, labelId, keyword, read)

        val expected = listOf(
            getInterval(minValue = 0L, maxValue = 10_000L, maxId = "10")
        )

        // Then
        assertEquals(expected, actual)
    }

    @Test
    fun `page contains 2 on 10 items, intervals are merged`() = runTest {
        // Given: already exiting interval.
        items.addAll(
            listOf(
                getInterval(minValue = 0, maxValue = 1000, maxId = "1")
            )
        )
        // When
        dao.upsertPageInterval(
            userId = userId,
            type = type,
            pageKey = getPageKey(size = 10, minTime = 1000, maxTime = Long.MAX_VALUE),
            items = listOf(
                FakeItem(id = "1", time = 1000),
                FakeItem(id = "2", time = 2000)
            )
        )

        val actual = dao.getAll(userId, type, orderBy, labelId, keyword, read)

        val expected = listOf(
            getInterval(minValue = 0, maxValue = Long.MAX_VALUE, maxId = null)
        )

        // Then
        assertEquals(expected, actual)
    }

    @Test
    fun `page contains 2 on 10 items, all existing intervals are merged`() = runTest {
        // Given: already exiting intervals.
        items.addAll(
            listOf(
                getInterval(minValue = 0, maxValue = 1000, maxId = "1"),
                getInterval(minValue = 2000, maxValue = 2000, maxId = "2")
            )
        )
        // When
        dao.upsertPageInterval(
            userId = userId,
            type = type,
            pageKey = getPageKey(size = 10, minTime = Long.MIN_VALUE, maxTime = Long.MAX_VALUE),
            items = listOf(
                FakeItem(id = "1", time = 1000),
                FakeItem(id = "2", time = 2000)
            )
        )

        val actual = dao.getAll(userId, type, orderBy, labelId, keyword, read)

        val expected = listOf(
            getInterval(minValue = Long.MIN_VALUE, maxValue = Long.MAX_VALUE)
        )

        // Then
        assertEquals(expected, actual)
    }
}
