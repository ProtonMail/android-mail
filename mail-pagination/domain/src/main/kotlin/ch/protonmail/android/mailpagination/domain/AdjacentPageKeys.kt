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
import javax.inject.Inject

class GetAdjacentPageKeys @Inject constructor() {

    /**
     * Return [AdjacentPageKeys] to get the adjacent previous and next pages.
     */
    operator fun invoke(
        items: List<PageItem>,
        current: PageKey,
        size: Int
    ): AdjacentPageKeys {
        // Get top and bottom items to compute prev/next keys.
        val topItem = items.firstOrNull()
        val bottomItem = items.lastOrNull()
        val (minItem, maxItem) = when (current.orderDirection) {
            OrderDirection.Ascending -> topItem to bottomItem
            OrderDirection.Descending -> bottomItem to topItem
        }
        val lessThanMinItemKey = current.copy(
            filter = current.filter.copy(
                minTime = Long.MIN_VALUE,
                minOrder = Long.MIN_VALUE,
                minId = null,
                maxTime = minItem?.time ?: Long.MAX_VALUE,
                // Make sure we don't get the same item (minus(1)) in the adjacent page.
                maxOrder = minItem?.order?.minus(1) ?: Long.MAX_VALUE,
                // Make sure we don't get the same item from BE.
                maxId = minItem?.id
            ),
            size = size
        )
        val greaterThenMaxItemKey = current.copy(
            filter = current.filter.copy(
                minTime = maxItem?.time ?: Long.MIN_VALUE,
                // Make sure we don't get the same item (plus(1)).
                minOrder = maxItem?.order?.plus(1) ?: Long.MIN_VALUE,
                // Make sure we don't get the same item from BE.
                minId = maxItem?.id,
                maxTime = Long.MAX_VALUE,
                maxOrder = Long.MAX_VALUE,
                maxId = null
            ),
            size = size
        )
        val (prev, next) = when (current.orderDirection) {
            OrderDirection.Ascending -> lessThanMinItemKey to greaterThenMaxItemKey
            OrderDirection.Descending -> greaterThenMaxItemKey to lessThanMinItemKey
        }
        return AdjacentPageKeys(
            prev = prev,
            current = current,
            next = next
        )
    }
}

data class AdjacentPageKeys(
    val prev: PageKey,
    val current: PageKey,
    val next: PageKey
)
