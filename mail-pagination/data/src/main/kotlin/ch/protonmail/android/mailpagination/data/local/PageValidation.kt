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
import ch.protonmail.android.mailpagination.domain.model.PageItem
import ch.protonmail.android.mailpagination.domain.model.PageItemType
import ch.protonmail.android.mailpagination.domain.model.PageKey
import me.proton.core.domain.entity.UserId

/**
 * Return true if all [items] are considered locally up-to-date according the given [pageKey].
 */
suspend fun PageIntervalDao.isLocalPageValid(
    userId: UserId,
    type: PageItemType,
    pageKey: PageKey,
    items: List<PageItem>
): Boolean {
    val intervals = getAll(
        userId = userId,
        type = type,
        orderBy = pageKey.orderBy,
        labelId = pageKey.filter.labelId,
        keyword = pageKey.filter.keyword,
        read = pageKey.filter.read
    )
    return when {
        // No interval have been cached previously.
        intervals.isEmpty() -> false
        // No items, let's check pageKey min/max value.
        items.isEmpty() -> when (pageKey.orderBy) {
            // Any existing intervals that cover the current pageKey?
            OrderBy.Time -> intervals.anyContainsTimeIntervalOf(pageKey)
        }
        else -> when (pageKey.orderBy) {
            // Any single interval cover all items?
            OrderBy.Time -> intervals.anyContainsAllTimeOf(items)
        }
    }
}

private fun List<PageIntervalEntity>.anyContainsAllTimeOf(items: List<PageItem>): Boolean =
    any { interval -> items.all { interval.containsTimeOf(it) } }

private fun List<PageIntervalEntity>.anyContainsTimeIntervalOf(pageKey: PageKey): Boolean =
    any { it.containsTimeIntervalOf(pageKey) }

private fun PageIntervalEntity.containsTimeOf(item: PageItem): Boolean =
    item.isTimeGreaterOrEqual(this) && item.isTimeLessOrEqual(this)

private fun PageIntervalEntity.containsTimeIntervalOf(pageKey: PageKey): Boolean =
    pageKey.isMinTimeGreaterOrEqual(this) && pageKey.isMaxTimeLessOrEqual(this)

private fun PageItem.isTimeLessOrEqual(interval: PageIntervalEntity) =
    (time to order).lessOrEqual(interval.maxValue to interval.maxOrder)

private fun PageItem.isTimeGreaterOrEqual(interval: PageIntervalEntity) =
    (time to order).greaterOrEqual(interval.minValue to interval.minOrder)

private fun PageKey.isMaxTimeLessOrEqual(interval: PageIntervalEntity) =
    (filter.maxTime to filter.maxOrder).lessOrEqual(interval.maxValue to interval.maxOrder)

private fun PageKey.isMinTimeGreaterOrEqual(interval: PageIntervalEntity) =
    (filter.minTime to filter.minOrder).greaterOrEqual(interval.minValue to interval.minOrder)

private fun Pair<Long, Long>.greaterOrEqual(other: Pair<Long, Long>) =
    first > other.first || first == other.first && second >= other.second

private fun Pair<Long, Long>.lessOrEqual(other: Pair<Long, Long>) =
    first < other.first || first == other.first && second <= other.second
