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

package ch.protonmail.android.mailpagination.domain

import ch.protonmail.android.mailpagination.domain.model.OrderBy
import ch.protonmail.android.mailpagination.domain.model.OrderDirection
import ch.protonmail.android.mailpagination.domain.model.PageItem
import ch.protonmail.android.mailpagination.domain.model.PageKey
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import me.proton.core.label.domain.entity.LabelId
import org.junit.Test
import kotlin.test.assertEquals

class RefreshPageKeyKtTest {

    data class FakeItem(
        override val userId: UserId = UserId("userId"),
        override val id: String,
        override val time: Long,
        override val size: Long = 1000,
        override val order: Long = time,
        override val read: Boolean = false,
        override val labelIds: List<LabelId> = emptyList(),
        override val keywords: String = ""
    ) : PageItem

    @Test
    fun `return ascending refresh key from first to last item`() = runTest {
        // Given
        val initial = PageKey(
            orderBy = OrderBy.Time,
            orderDirection = OrderDirection.Ascending,
            size = 10
        )
        val items = listOf<PageItem>(
            FakeItem(id = "1", time = 1000L),
            FakeItem(id = "2", time = 2000L),
            FakeItem(id = "3", time = 3000L),
            FakeItem(id = "4", time = 4000L),
            FakeItem(id = "5", time = 5000L),
            FakeItem(id = "6", time = 6000L),
            FakeItem(id = "7", time = 7000L),
            FakeItem(id = "8", time = 8000L),
            FakeItem(id = "9", time = 9000L),
            FakeItem(id = "10", time = 10_000L)
        )
        val lowest = items.first()
        val highest = items.last()
        val expected = initial.copy(
            filter = initial.filter.copy(
                minTime = lowest.time,
                minOrder = lowest.order,
                maxTime = highest.time,
                maxOrder = highest.order
            )
        )

        // When
        val actual = items.getRefreshPageKey(initial)
        // Then
        assertEquals(expected, actual)
    }

    @Test
    fun `return descending refresh key from first to last item`() = runTest {
        // Given
        val initial = PageKey(
            orderBy = OrderBy.Time,
            orderDirection = OrderDirection.Descending,
            size = 10
        )
        val items = listOf<PageItem>(
            FakeItem(id = "10", time = 10_000L),
            FakeItem(id = "9", time = 9000L),
            FakeItem(id = "8", time = 8000L),
            FakeItem(id = "7", time = 7000L),
            FakeItem(id = "6", time = 6000L),
            FakeItem(id = "5", time = 5000L),
            FakeItem(id = "4", time = 4000L),
            FakeItem(id = "3", time = 3000L),
            FakeItem(id = "2", time = 2000L),
            FakeItem(id = "1", time = 1000L)
        )
        val lowest = items.last()
        val highest = items.first()
        val expected = initial.copy(
            filter = initial.filter.copy(
                minTime = lowest.time,
                minOrder = lowest.order,
                maxTime = highest.time,
                maxOrder = highest.order
            )
        )

        // When
        val actual = items.getRefreshPageKey(initial)
        // Then
        assertEquals(expected, actual)
    }

    @Test
    fun `return refresh key for all items size`() = runTest {
        // Given
        val initial = PageKey(
            orderBy = OrderBy.Time,
            orderDirection = OrderDirection.Ascending,
            size = 5
        )
        val items = listOf<PageItem>(
            FakeItem(id = "1", time = 1000L),
            FakeItem(id = "2", time = 2000L),
            FakeItem(id = "3", time = 3000L),
            FakeItem(id = "4", time = 4000L),
            FakeItem(id = "5", time = 5000L),
            FakeItem(id = "6", time = 6000L),
            FakeItem(id = "7", time = 7000L),
            FakeItem(id = "8", time = 8000L),
            FakeItem(id = "9", time = 9000L),
            FakeItem(id = "10", time = 10_000L)
        )
        val lowest = items.first()
        val highest = items.last()
        val expected = initial.copy(
            filter = initial.filter.copy(
                minTime = lowest.time,
                minOrder = lowest.order,
                maxTime = highest.time,
                maxOrder = highest.order
            ),
            size = 10
        )

        // When
        val actual = items.getRefreshPageKey(initial)
        // Then
        assertEquals(expected, actual)
    }

    @Test
    fun `return refresh key for 1 item`() = runTest {
        // Given
        val initial = PageKey(
            orderBy = OrderBy.Time,
            orderDirection = OrderDirection.Descending,
            size = 10
        )
        val items = listOf<PageItem>(
            FakeItem(id = "1", time = 1000L)
        )
        val single = items.first()
        val expected = initial.copy(
            filter = initial.filter.copy(
                minTime = single.time,
                minOrder = single.order,
                maxTime = single.time,
                maxOrder = single.order
            )
        )

        // When
        val actual = items.getRefreshPageKey(initial)
        // Then
        assertEquals(expected, actual)
    }

    @Test
    fun `return refresh key for 0 item`() = runTest {
        // Given
        val initial = PageKey(
            orderBy = OrderBy.Time,
            orderDirection = OrderDirection.Ascending,
            size = 10
        )
        val items = listOf<PageItem>()
        val expected = initial.copy(
            filter = initial.filter.copy(
                minTime = Long.MIN_VALUE,
                minOrder = Long.MIN_VALUE,
                maxTime = Long.MAX_VALUE,
                maxOrder = Long.MAX_VALUE
            )
        )

        // When
        val actual = items.getRefreshPageKey(initial)
        // Then
        assertEquals(expected, actual)
    }
}
