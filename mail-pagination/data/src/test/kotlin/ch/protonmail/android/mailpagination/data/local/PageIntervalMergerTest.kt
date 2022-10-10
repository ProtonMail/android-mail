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

import ch.protonmail.android.mailpagination.data.local.entity.PageIntervalEntity
import ch.protonmail.android.mailpagination.domain.model.OrderBy
import ch.protonmail.android.mailpagination.domain.model.PageItemType
import ch.protonmail.android.mailpagination.domain.model.ReadStatus
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import me.proton.core.label.domain.entity.LabelId
import org.junit.Test
import kotlin.test.assertEquals

class PageIntervalMergerTest {

    private fun getInterval(
        minValue: Long,
        maxValue: Long,
        minOrder: Long = minValue,
        maxOrder: Long = maxValue
    ) = PageIntervalEntity(
        userId = UserId("1"),
        type = PageItemType.Message,
        orderBy = OrderBy.Time,
        labelId = LabelId("1"),
        keyword = "",
        read = ReadStatus.All,
        minValue = minValue,
        maxValue = maxValue,
        minOrder = minOrder,
        maxOrder = maxOrder,
        minId = "minId",
        maxId = "maxId"
    )

    @Test
    fun `merge 2 overlapping intervals`() = runTest {
        // Given
        val intervals = listOf(
            getInterval(1000, 2000),
            getInterval(1500, 3000)
        )
        val expected = listOf(
            getInterval(1000, 3000)
        )
        // When
        val actual = intervals.merge()
        // Then
        assertEquals(expected, actual)
    }

    @Test
    fun `merge 2 touching intervals`() = runTest {
        // Given
        val intervals = listOf(
            getInterval(1000, 2000),
            getInterval(2000, 3000)
        )
        val expected = listOf(
            getInterval(1000, 3000)
        )
        // When
        val actual = intervals.merge()
        // Then
        assertEquals(expected, actual)
    }

    @Test
    fun `don't merge 2 non-overlapping intervals`() = runTest {
        // Given
        val intervals = listOf(
            getInterval(1000, 2000),
            getInterval(2100, 3000)
        )
        val expected = listOf(
            getInterval(1000, 2000),
            getInterval(2100, 3000)
        )
        // When
        val actual = intervals.merge()
        // Then
        assertEquals(expected, actual)
    }

    @Test
    fun `merge many overlapping intervals`() = runTest {
        // Given
        val intervals = listOf(
            getInterval(1000, 2000),
            getInterval(2200, 3000),
            getInterval(2900, 3200),
            getInterval(1000, 2100)
        )
        val expected = listOf(
            getInterval(1000, 2100),
            getInterval(2200, 3200)
        )
        // When
        val actual = intervals.merge()
        // Then
        assertEquals(expected, actual)
    }

    @Test
    fun `merge many overlapping intervals, different starting order`() = runTest {
        // Given
        val intervals = listOf(
            getInterval(2200, 3000),
            getInterval(1000, 2100),
            getInterval(2900, 3200),
            getInterval(1000, 2000)
        )
        val expected = listOf(
            getInterval(1000, 2100),
            getInterval(2200, 3200)
        )
        // When
        val actual = intervals.merge()
        // Then
        assertEquals(expected, actual)
    }

    @Test
    fun `merge nothing`() = runTest {
        // Given
        val intervals = listOf(
            getInterval(1000, 2000),
            getInterval(2100, 3000),
            getInterval(3100, 4000),
            getInterval(4100, 5000)
        )
        val expected = listOf(
            getInterval(1000, 2000),
            getInterval(2100, 3000),
            getInterval(3100, 4000),
            getInterval(4100, 5000)
        )
        // When
        val actual = intervals.merge()
        // Then
        assertEquals(expected, actual)
    }

    @Test
    fun `merge only fully touching intervals, value are equals but order are not`() = runTest {
        // Given
        val intervals = listOf(
            getInterval(minValue = 1000, minOrder = 1001, maxValue = 2000, maxOrder = 2001),
            // 2001 & 2003 have more than 1 increment -> non-overlapping.
            getInterval(minValue = 2000, minOrder = 2003, maxValue = 3000, maxOrder = 3001)
        )
        val expected = listOf(
            getInterval(minValue = 1000, minOrder = 1001, maxValue = 2000, maxOrder = 2001),
            getInterval(minValue = 2000, minOrder = 2003, maxValue = 3000, maxOrder = 3001)
        )
        // When
        val actual = intervals.merge()
        // Then
        assertEquals(expected, actual)
    }

    @Test
    fun `merge only fully touching intervals, value are equals and order as well`() = runTest {
        // Given
        val intervals = listOf(
            getInterval(minValue = 1000, minOrder = 1001, maxValue = 2000, maxOrder = 2001),
            // 2001 & 2002 have only 1 increment -> overlapping.
            getInterval(minValue = 2000, minOrder = 2002, maxValue = 3000, maxOrder = 3001)
        )
        val expected = listOf(
            getInterval(minValue = 1000, minOrder = 1001, maxValue = 3000, maxOrder = 3001)
        )
        // When
        val actual = intervals.merge()
        // Then
        assertEquals(expected, actual)
    }

    @Test
    fun `merge only fully touching intervals, maxOrder greater than minOrder`() = runTest {
        // Given
        val intervals = listOf(
            getInterval(minValue = 1000, minOrder = 1001, maxValue = 2000, maxOrder = 2002),
            // 2002 > 2001 -> overlapping.
            getInterval(minValue = 2000, minOrder = 2000, maxValue = 3000, maxOrder = 3001)
        )
        val expected = listOf(
            getInterval(minValue = 1000, minOrder = 1001, maxValue = 3000, maxOrder = 3001)
        )
        // When
        val actual = intervals.merge()
        // Then
        assertEquals(expected, actual)
    }
}
