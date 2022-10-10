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
import ch.protonmail.android.mailpagination.domain.model.PageItem
import ch.protonmail.android.mailpagination.domain.model.PageItemType
import ch.protonmail.android.mailpagination.domain.model.PageKey
import me.proton.core.domain.entity.UserId

/**
 * Update or insert a page interval according [items] and [pageKey].
 *
 * Overlapping interval are merged.
 *
 * Note: Typically used to update intervals after fetching some [PageItem].
 */
suspend fun PageIntervalDao.upsertPageInterval(
    userId: UserId,
    type: PageItemType,
    pageKey: PageKey,
    items: List<PageItem>
) {
    // If no more page for this query -> use pageKey interval.
    // If potentially more pages for this query -> use pageKey + lastItem.

    // Let lastItem be null if messages.size < pageKey.size.
    val lastItem = items.lastOrNull().takeIf { items.size >= pageKey.size }
    val (minItem, maxItem) = when (pageKey.orderDirection) {
        OrderDirection.Ascending -> null to lastItem
        OrderDirection.Descending -> lastItem to null
    }
    val minValue = when (pageKey.orderBy) {
        OrderBy.Time -> minItem?.time ?: pageKey.filter.minTime
    }
    val maxValue = when (pageKey.orderBy) {
        OrderBy.Time -> maxItem?.time ?: pageKey.filter.maxTime
    }
    val minOrder = minItem?.order ?: pageKey.filter.minOrder
    val maxOrder = maxItem?.order ?: pageKey.filter.maxOrder
    val minId = minItem?.id ?: pageKey.filter.minId
    val maxId = maxItem?.id ?: pageKey.filter.maxId
    insertOrMerge(
        PageIntervalEntity(
            userId = userId,
            type = type,
            orderBy = pageKey.orderBy,
            labelId = pageKey.filter.labelId,
            keyword = pageKey.filter.keyword,
            read = pageKey.filter.read,
            minValue = minValue,
            maxValue = maxValue,
            minOrder = minOrder,
            maxOrder = maxOrder,
            minId = minId,
            maxId = maxId
        )
    )
}
