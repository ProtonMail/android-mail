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

package ch.protonmail.android.mailpagination.domain.model

import ch.protonmail.android.mailpagination.domain.model.OrderBy.Time
import ch.protonmail.android.mailpagination.domain.model.OrderDirection.Descending
import ch.protonmail.android.mailpagination.domain.model.ReadStatus.All
import ch.protonmail.android.mailpagination.domain.model.ReadStatus.Read
import ch.protonmail.android.mailpagination.domain.model.ReadStatus.Unread
import me.proton.core.label.domain.entity.LabelId
import me.proton.core.util.kotlin.EMPTY_STRING

/**
 * Page Parameters needed to query/fetch/filter/sort/order a page.
 */
data class PageKey(
    val filter: PageFilter = PageFilter(),
    val orderBy: OrderBy = Time,
    val orderDirection: OrderDirection = Descending,
    val size: Int = defaultPageSize
) {
    companion object {
        const val defaultPageSize = 25
    }
}

/**
 * Page Filters needed to query a page.
 * @param labelId filters on [PageItem.labelIds], containing the given [labelId]
 * @param keyword filters on [PageItem.keywords], containing [keyword]. Supports wildcard
 * @param read filters on [PageItem.read]
 * @param minTime filters on [PageItem.time], greater or equal
 * @param maxTime filters on [PageItem.time], less or equal
 * @param minOrder filters on [PageItem.order], greater or equal, only if [PageItem.time] equal [minTime]
 * @param maxOrder filters on [PageItem.order], less or equal, only if [PageItem.time] equal [maxTime]
 * @param minId filters on [PageItem.id], excluded, only if [PageItem.time] equal [minTime]
 * @param maxId filters on [PageItem.id], excluded, only if [PageItem.time] equal [maxTime]
 */
data class PageFilter(
    val labelId: LabelId = LabelId("0"),
    val keyword: String = EMPTY_STRING,
    val read: ReadStatus = All,
    val minTime: Long = Long.MIN_VALUE,
    val maxTime: Long = Long.MAX_VALUE,
    val minOrder: Long = Long.MIN_VALUE,
    val maxOrder: Long = Long.MAX_VALUE,
    val minId: String? = null,
    val maxId: String? = null
)

/**
 * Order by property (e.g. [PageItem.time]).
 *
 * @see OrderDirection
 */
enum class OrderBy {
    /** Order by [PageItem.time] and if equal then by [PageItem.order].*/
    Time
    // Size is not supported, BE need to add BeginSize/EndSize parameters.
}

/**
 * Order direction (ascending, descending).
 *
 * @see OrderBy
 */
enum class OrderDirection {
    Ascending,
    Descending
}

/**
 * Filter only [Read], [Unread] or [All] items.
 *
 * @see [PageItem.read]
 */
enum class ReadStatus {
    All,
    Read,
    Unread
}
