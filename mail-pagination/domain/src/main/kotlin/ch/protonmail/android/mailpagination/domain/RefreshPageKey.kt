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

import ch.protonmail.android.mailpagination.domain.model.OrderDirection
import ch.protonmail.android.mailpagination.domain.model.PageItem
import ch.protonmail.android.mailpagination.domain.model.PageKey
import kotlin.math.max

/**
 * Return a [PageKey] to refresh current items.
 */
fun List<PageItem>.getRefreshPageKey(initial: PageKey): PageKey {
    // Return the key corresponding to all current items/pages, top to bottom.
    val topItem = firstOrNull()
    val bottomItem = lastOrNull()
    val (minItem, maxItem) = when (initial.orderDirection) {
        OrderDirection.Ascending -> topItem to bottomItem
        OrderDirection.Descending -> bottomItem to topItem
    }
    return initial.copy(
        filter = initial.filter.copy(
            minTime = minItem?.time ?: Long.MIN_VALUE,
            minOrder = minItem?.order ?: Long.MIN_VALUE,
            minId = null,
            maxTime = maxItem?.time ?: Long.MAX_VALUE,
            maxOrder = maxItem?.order ?: Long.MAX_VALUE,
            maxId = null
        ),
        size = max(size, initial.size)
    )
}
