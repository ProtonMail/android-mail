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

class PageKeyClippingKtTest {

    private val userId = UserId("1")
    private val type = PageItemType.Message
    private val orderBy = OrderBy.Time
    private val labelId = LabelId("0")
    private val keyword = ""
    private val read = ReadStatus.All

    private val localIntervals = mutableSetOf<PageIntervalEntity>()

    @RelaxedMockK
    private val dao: PageIntervalDao = spyk {
        coEvery {
            getAll(any(), any(), any(), any(), any(), any())
        } answers {
            localIntervals.toList()
        }
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
        maxTime: Long = Long.MAX_VALUE,
        minOrder: Long = minTime,
        maxOrder: Long = maxTime
    ) = PageKey(
        orderDirection = orderDirection,
        filter = PageFilter(
            minTime = minTime,
            minOrder = minOrder,
            maxTime = maxTime,
            maxOrder = maxOrder
        )
    )

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        localIntervals.clear()
    }

    @Test
    fun `when no interval exist, return same key`() = runTest {
        // Given
        localIntervals.clear()

        // When
        val actual = dao.getClippedPageKey(
            userId = userId,
            type = type,
            pageKey = getPageKey(minTime = 1000, maxTime = Long.MAX_VALUE)
        )

        val expected = getPageKey(minTime = 1000, maxTime = Long.MAX_VALUE)

        // Then
        assertEquals(expected, actual)
    }

    @Test
    fun `when interval exist, not overlapping, return same key`() = runTest {
        // Given
        localIntervals.addAll(
            listOf(
                getInterval(minValue = Long.MIN_VALUE, maxValue = 1000)
            )
        )

        // When
        val actual = dao.getClippedPageKey(
            userId = userId,
            type = type,
            pageKey = getPageKey(minTime = 2000, maxTime = Long.MAX_VALUE)
        )

        val expected = getPageKey(minTime = 2000, maxTime = Long.MAX_VALUE)

        // Then
        assertEquals(expected, actual)
    }

    @Test
    fun `when interval exist, overlapping, return clipped key`() = runTest {
        // Given
        localIntervals.addAll(
            listOf(
                getInterval(minValue = Long.MIN_VALUE, maxValue = 1000)
            )
        )

        // When
        val actual = dao.getClippedPageKey(
            userId = userId,
            type = type,
            pageKey = getPageKey(minTime = 0, maxTime = Long.MAX_VALUE)
        )

        val expected = getPageKey(minTime = 1000, maxTime = Long.MAX_VALUE)

        // Then
        assertEquals(expected, actual)
    }

    @Test
    fun `when min in one interval, return clipped min`() = runTest {
        // Given
        localIntervals.addAll(
            listOf(
                getInterval(minValue = Long.MIN_VALUE, maxValue = 1000),
                getInterval(minValue = 2000, maxValue = 3000)
            )
        )

        // When
        val actual = dao.getClippedPageKey(
            userId = userId,
            type = type,
            pageKey = getPageKey(minTime = 0, maxTime = Long.MAX_VALUE)
        )

        val expected = getPageKey(minTime = 1000, maxTime = Long.MAX_VALUE)

        // Then
        assertEquals(expected, actual)
    }

    @Test
    fun `when min in one interval and max in another, return clipped min & max`() = runTest {
        // Given
        localIntervals.addAll(
            listOf(
                getInterval(minValue = Long.MIN_VALUE, maxValue = 1000),
                getInterval(minValue = 2000, maxValue = 3000)
            )
        )

        // When
        val actual = dao.getClippedPageKey(
            userId = userId,
            type = type,
            pageKey = getPageKey(minTime = 0, maxTime = 3000)
        )

        val expected = getPageKey(minTime = 1000, maxTime = 2000)

        // Then
        assertEquals(expected, actual)
    }

    @Test
    fun `when min & max in same interval return null`() = runTest {
        // Given
        localIntervals.addAll(
            listOf(
                getInterval(minValue = Long.MIN_VALUE, maxValue = 1000),
                getInterval(minValue = 2000, maxValue = 3000)
            )
        )

        // When
        val actual = dao.getClippedPageKey(
            userId = userId,
            type = type,
            pageKey = getPageKey(minTime = 0, maxTime = 1000)
        )

        val expected = null

        // Then
        assertEquals(expected, actual)
    }

    @Test
    fun `when interval contain maxOrder, return clipped correct order`() = runTest {
        // Given
        localIntervals.addAll(
            listOf(
                getInterval(minValue = Long.MIN_VALUE, maxValue = 1000, maxOrder = 1001)
            )
        )

        // When
        val actual = dao.getClippedPageKey(
            userId = userId,
            type = type,
            pageKey = getPageKey(minTime = 0, maxTime = 3000)
        )

        val expected = getPageKey(minTime = 1000, minOrder = 1001, maxTime = 3000)

        // Then
        assertEquals(expected, actual)
    }

    @Test
    fun `when interval contain minOrder, return clipped correct order`() = runTest {
        // Given
        localIntervals.addAll(
            listOf(
                getInterval(minValue = 1000, minOrder = 1001, maxValue = Long.MAX_VALUE)
            )
        )

        // When
        val actual = dao.getClippedPageKey(
            userId = userId,
            type = type,
            pageKey = getPageKey(minTime = 0, maxTime = 3000)
        )

        val expected = getPageKey(minTime = 0, maxTime = 1000, maxOrder = 1001)

        // Then
        assertEquals(expected, actual)
    }

    @Test
    fun `when intervals contain minOrder and maxOrder, return clipped correct order`() = runTest {
        // Given
        localIntervals.addAll(
            listOf(
                getInterval(minValue = 0, minOrder = 1, maxValue = 1000, maxOrder = 1001),
                getInterval(minValue = 2000, minOrder = 2001, maxValue = 3000, maxOrder = 3001)
            )
        )

        // When
        val actual = dao.getClippedPageKey(
            userId = userId,
            type = type,
            pageKey = getPageKey(minTime = 0, maxTime = 3000)
        )

        val expected = getPageKey(minTime = 1000, minOrder = 1001, maxTime = 2000, maxOrder = 2001)

        // Then
        assertEquals(expected, actual)
    }
}
