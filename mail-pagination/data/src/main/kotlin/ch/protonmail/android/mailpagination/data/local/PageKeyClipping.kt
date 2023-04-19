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
import ch.protonmail.android.mailpagination.domain.model.OrderBy
import ch.protonmail.android.mailpagination.domain.model.PageItemType
import ch.protonmail.android.mailpagination.domain.model.PageKey
import me.proton.core.domain.entity.UserId

/**
 * Return clipped [PageKey] according already persisted intervals.
 *
 * Note: Usually used to trim unnecessary interval from the [PageKey] before fetching.
 * @return clipped key or null if persisted intervals already include [pageKey].
 */
suspend fun PageIntervalDao.getClippedPageKey(
    userId: UserId,
    type: PageItemType,
    pageKey: PageKey
): PageKey? {
    val intervals = getAll(
        userId = userId,
        type = type,
        orderBy = pageKey.orderBy,
        labelId = pageKey.filter.labelId,
        keyword = pageKey.filter.keyword,
        read = pageKey.filter.read
    )
    val (minValue, maxValue) = when (pageKey.orderBy) {
        OrderBy.Time -> pageKey.filter.minTime to pageKey.filter.maxTime
    }
    val minInterval = intervals.firstOrNull { interval ->
        minValue in interval.minValue..interval.maxValue
    }
    val maxInterval = intervals.firstOrNull { interval ->
        maxValue in interval.minValue..interval.maxValue
    }
    return pageKey.copy(
        filter = pageKey.filter.copy(
            minTime = minInterval?.maxValue ?: pageKey.filter.minTime,
            minOrder = minInterval?.maxOrder ?: pageKey.filter.minOrder,
            minId = minInterval?.maxId ?: pageKey.filter.minId,
            maxTime = maxInterval?.minValue ?: pageKey.filter.maxTime,
            maxOrder = maxInterval?.minOrder ?: pageKey.filter.maxOrder,
            maxId = maxInterval?.minId ?: pageKey.filter.maxId
        )
    ).takeIf { it.filter.minTime <= it.filter.maxTime }
}
