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
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import me.proton.core.label.domain.entity.LabelId
import org.junit.Before
import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PageValidationKtTest {

    private val userId = UserId("1")
    private val type = PageItemType.Message
    private val orderBy = OrderBy.Time
    private val labelId = LabelId("0")
    private val keyword = ""
    private val read = ReadStatus.All

    private val localIntervals = mutableSetOf<PageIntervalEntity>()

    private val dao: PageIntervalDao = mockk {
        // Using "answers" to allow tests to modify localIntervals list
        coEvery { getAll(userId, type, orderBy, labelId, keyword, read) } answers { localIntervals.toList() }
    }

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        localIntervals.clear()
    }

    @Test
    fun `when no interval exist, return false`() = runTest {
        // Given
        localIntervals.clear()

        // When
        val actual = dao.isLocalPageValid(
            userId = userId,
            type = type,
            pageKey = getPageKey(minTime = 1000, maxTime = Long.MAX_VALUE),
            items = listOf()
        )

        // Then
        assertFalse(actual)
    }

    @Test
    fun `when no interval exist, even with items, return false`() = runTest {
        // Given
        localIntervals.clear()

        // When
        val actual = dao.isLocalPageValid(
            userId = userId,
            type = type,
            pageKey = getPageKey(minTime = 1000, maxTime = Long.MAX_VALUE),
            items = listOf(
                FakeItem(id = "1", time = 1000),
                FakeItem(id = "2", time = 2000),
                FakeItem(id = "3", time = 3000)
            )
        )

        // Then
        assertFalse(actual)
    }

    @Test
    fun `when interval exist and cover the pageKey, without items, return true`() = runTest {
        // Given
        localIntervals.add(
            getInterval(minValue = 1000, maxValue = 5000)
        )

        // When
        val actual = dao.isLocalPageValid(
            userId = userId,
            type = type,
            pageKey = getPageKey(minTime = 2000, maxTime = 4000),
            items = emptyList()
        )

        // Then
        assertTrue(actual)
    }

    @Test
    fun `when a single interval cover all items, return true`() = runTest {
        // Given
        localIntervals.add(
            getInterval(minValue = 1000, maxValue = 5000)
        )

        // When
        val actual = dao.isLocalPageValid(
            userId = userId,
            type = type,
            pageKey = getPageKey(minTime = 0, maxTime = 6000),
            items = listOf(
                FakeItem(id = "1", time = 1000),
                FakeItem(id = "2", time = 2000),
                FakeItem(id = "3", time = 3000),
                FakeItem(id = "4", time = 4000),
                FakeItem(id = "5", time = 5000)
            )
        )

        // Then
        assertTrue(actual)
    }

    @Test
    fun `when a single interval doesn't cover all items, return false`() = runTest {
        // Given
        localIntervals.add(
            getInterval(minValue = 1000, maxValue = 4000)
        )

        // When
        val actual = dao.isLocalPageValid(
            userId = userId,
            type = type,
            pageKey = getPageKey(minTime = 0, maxTime = 6000),
            items = listOf(
                FakeItem(id = "1", time = 1000),
                FakeItem(id = "2", time = 2000),
                FakeItem(id = "3", time = 3000),
                FakeItem(id = "4", time = 4000),
                FakeItem(id = "5", time = 5000) // <- Not valid
            )
        )

        // Then
        assertFalse(actual)
    }

    @Test
    fun `when two single interval doesn't cover all items, return false`() = runTest {
        // Given
        localIntervals.addAll(
            listOf(
                getInterval(minValue = 1000, maxValue = 2000),
                // 2000 -> 4000 no covered.
                getInterval(minValue = 4000, maxValue = 5000)
            )
        )

        // When
        val actual = dao.isLocalPageValid(
            userId = userId,
            type = type,
            pageKey = getPageKey(minTime = 0, maxTime = 6000),
            items = listOf(
                FakeItem(id = "1", time = 1000),
                FakeItem(id = "2", time = 2000),
                FakeItem(id = "3", time = 3000), // <- Not valid
                FakeItem(id = "4", time = 4000),
                FakeItem(id = "5", time = 5000)
            )
        )

        // Then
        assertFalse(actual)
    }

    @Test
    fun `when two intervals are needed to cover all items, return false`() = runTest {
        // Given
        localIntervals.addAll(
            listOf(
                getInterval(minValue = 1000, maxValue = 4000),
                // Potential gap in the middle.
                getInterval(minValue = 4500, maxValue = 5000)
            )
        )

        // When
        val actual = dao.isLocalPageValid(
            userId = userId,
            type = type,
            pageKey = getPageKey(minTime = 0, maxTime = 20_000),
            items = listOf(
                FakeItem(id = "1", time = 1000),
                FakeItem(id = "2", time = 2000),
                FakeItem(id = "3", time = 3000),
                FakeItem(id = "4", time = 4000),
                // Potential gap between 4000 -> 4500.
                FakeItem(id = "5", time = 5000)
            )
        )

        // Then
        assertFalse(actual)
    }

    @Test
    fun `when two intervals without gaps are needed to cover all items, return false`() = runTest {
        // This scenario is here to document the actual behavior of `isLocalPageValid` extension function in isolation.
        // The same case is not expected to happen in production as the collaboration with `PageIntervalMerger`
        // should merge two consecutive intervals into one
        // Given
        localIntervals.addAll(
            listOf(
                getInterval(minValue = 1000, maxValue = 4000),
                getInterval(minValue = 4000, maxValue = 5000)
            )
        )

        // When
        val actual = dao.isLocalPageValid(
            userId = userId,
            type = type,
            pageKey = getPageKey(minTime = 0, maxTime = 20_000),
            items = listOf(
                FakeItem(id = "1", time = 1000),
                FakeItem(id = "2", time = 2000),
                FakeItem(id = "3", time = 3000),
                FakeItem(id = "4", time = 4000),
                FakeItem(id = "5", time = 5000)
            )
        )

        // Then
        assertFalse(actual)
    }

    @Test
    fun `when two intervals each cover all items, return true`() = runTest {
        // Given
        localIntervals.addAll(
            listOf(
                // Should not happen as overlapping intervals must be merged.
                getInterval(minValue = 0, maxValue = 10_000),
                getInterval(minValue = 1000, maxValue = 6000)
            )
        )

        // When
        val actual = dao.isLocalPageValid(
            userId = userId,
            type = type,
            pageKey = getPageKey(minTime = 0, maxTime = 20_000),
            items = listOf(
                FakeItem(id = "1", time = 1000),
                FakeItem(id = "2", time = 2000),
                FakeItem(id = "3", time = 3000),
                FakeItem(id = "4", time = 4000),
                FakeItem(id = "5", time = 5000)
            )
        )

        // Then
        assertTrue(actual)
    }

    @Test
    fun `when same time, valid order, return true`() = runTest {
        // Given
        localIntervals.addAll(
            listOf(
                getInterval(minValue = 0, maxValue = 2000, maxOrder = 2001) // <- 2001 !
            )
        )

        // When
        val actual = dao.isLocalPageValid(
            userId = userId,
            type = type,
            pageKey = getPageKey(minTime = 0, maxTime = 20_000),
            items = listOf(
                FakeItem(id = "1", time = 1000),
                FakeItem(id = "2", time = 2000, order = 2000), // <- Same time, ok order.
                FakeItem(id = "3", time = 2000, order = 2001) // <- Same time, ok order.
            )
        )

        // Then
        assertTrue(actual)
    }

    @Test
    fun `when same time, invalid order, return false`() = runTest {
        // Given
        localIntervals.addAll(
            listOf(
                getInterval(minValue = 0, maxValue = 2000, maxOrder = 2000) // <- 2000 !
            )
        )

        // When
        val actual = dao.isLocalPageValid(
            userId = userId,
            type = type,
            pageKey = getPageKey(minTime = 0, maxTime = 20_000),
            items = listOf(
                FakeItem(id = "1", time = 1000),
                FakeItem(id = "2", time = 2000, order = 2000), // <- Same time, ok order.
                FakeItem(id = "3", time = 2000, order = 2001) // <- Same time, ko order.
            )
        )

        // Then
        assertFalse(actual)
    }

    private fun getInterval(
        minValue: Long,
        maxValue: Long,
        minOrder: Long = minValue,
        maxOrder: Long = maxValue,
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
        minOrder = minOrder,
        maxOrder = maxOrder,
        minId = minId,
        maxId = maxId
    )

    private fun getPageKey(
        orderDirection: OrderDirection = OrderDirection.Ascending,
        minTime: Long = Long.MIN_VALUE,
        maxTime: Long = Long.MAX_VALUE
    ) = PageKey(
        orderDirection = orderDirection,
        filter = PageFilter(
            minTime = minTime,
            minOrder = minTime,
            maxTime = maxTime,
            maxOrder = maxTime
        )
    )

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

}
