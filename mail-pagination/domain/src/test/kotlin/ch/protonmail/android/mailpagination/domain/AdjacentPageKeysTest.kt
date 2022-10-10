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
import ch.protonmail.android.mailpagination.domain.model.PageFilter
import ch.protonmail.android.mailpagination.domain.model.PageItem
import ch.protonmail.android.mailpagination.domain.model.PageKey
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import me.proton.core.label.domain.entity.LabelId
import org.junit.Test
import kotlin.test.assertEquals

class AdjacentPageKeysTest {

    val getAdjacentPageKeys = GetAdjacentPageKeys()

    @Test
    fun `return correct prev, current and next keys`() = runTest {
        // Given
        val current = PageKey(
            orderBy = OrderBy.Time,
            orderDirection = OrderDirection.Ascending,
            size = 5,
            filter = PageFilter(
                minTime = 1000L,
                maxTime = 5000L
            )
        )
        val items = listOf<PageItem>(
            FakeItem(id = "1", time = 1000L),
            FakeItem(id = "2", time = 2000L),
            FakeItem(id = "3", time = 3000L),
            FakeItem(id = "4", time = 4000L),
            FakeItem(id = "5", time = 5000L)
        )
        val first = items.first()
        val last = items.last()
        val expected = AdjacentPageKeys(
            prev = current.copy(
                filter = current.filter.copy(
                    minTime = Long.MIN_VALUE,
                    minOrder = Long.MIN_VALUE,
                    minId = null,
                    maxTime = first.time,
                    maxOrder = first.order - 1,
                    maxId = first.id
                )
            ),
            current = current,
            next = current.copy(
                filter = current.filter.copy(
                    minTime = last.time,
                    minOrder = last.order + 1,
                    minId = last.id,
                    maxTime = Long.MAX_VALUE,
                    maxOrder = Long.MAX_VALUE,
                    maxId = null
                )
            )
        )

        // When
        val actual = getAdjacentPageKeys(items, current, current.size)
        // Then
        assertEquals(expected, actual)
    }

    @Test
    fun `return correct prev, current and next keys, with unbounded pageKey`() = runTest {
        // Given
        val current = PageKey(
            orderBy = OrderBy.Time,
            orderDirection = OrderDirection.Ascending,
            size = 5,
            filter = PageFilter(
                minTime = Long.MIN_VALUE,
                maxTime = Long.MAX_VALUE
            )
        )
        val items = listOf<PageItem>(
            FakeItem(id = "1", time = 1000L),
            FakeItem(id = "2", time = 2000L),
            FakeItem(id = "3", time = 3000L),
            FakeItem(id = "4", time = 4000L),
            FakeItem(id = "5", time = 5000L)
        )
        val first = items.first()
        val last = items.last()
        val expected = AdjacentPageKeys(
            prev = current.copy(
                filter = current.filter.copy(
                    minTime = Long.MIN_VALUE,
                    minOrder = Long.MIN_VALUE,
                    minId = null,
                    maxTime = first.time,
                    maxOrder = first.order - 1,
                    maxId = first.id
                )
            ),
            current = current,
            next = current.copy(
                filter = current.filter.copy(
                    minTime = last.time,
                    minOrder = last.order + 1,
                    minId = last.id,
                    maxTime = Long.MAX_VALUE,
                    maxOrder = Long.MAX_VALUE,
                    maxId = null
                )
            )
        )

        // When
        val actual = getAdjacentPageKeys(items, current, current.size)
        // Then
        assertEquals(expected, actual)
    }

    @Test
    fun `return correct prev, current and next keys, with Descending pageKey`() = runTest {
        // Given
        val current = PageKey(
            orderBy = OrderBy.Time,
            orderDirection = OrderDirection.Descending,
            size = 5,
            filter = PageFilter(
                minTime = Long.MIN_VALUE,
                maxTime = Long.MAX_VALUE
            )
        )
        val items = listOf<PageItem>(
            FakeItem(id = "5", time = 5000L),
            FakeItem(id = "4", time = 4000L),
            FakeItem(id = "3", time = 3000L),
            FakeItem(id = "2", time = 2000L),
            FakeItem(id = "1", time = 1000L)
        )
        val first = items.first()
        val last = items.last()
        val expected = AdjacentPageKeys(
            prev = current.copy(
                filter = current.filter.copy(
                    minTime = first.time,
                    minOrder = first.order + 1,
                    minId = first.id,
                    maxTime = Long.MAX_VALUE,
                    maxOrder = Long.MAX_VALUE,
                    maxId = null
                )
            ),
            current = current,
            next = current.copy(
                filter = current.filter.copy(
                    minTime = Long.MIN_VALUE,
                    minOrder = Long.MIN_VALUE,
                    minId = null,
                    maxTime = last.time,
                    maxOrder = last.order - 1,
                    maxId = last.id
                )
            )
        )

        // When
        val actual = getAdjacentPageKeys(items, current, current.size)
        // Then
        assertEquals(expected, actual)
    }

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
}
