/*
 * Copyright (c) 2021 Proton Technologies AG
 * This file is part of Proton Technologies AG and ProtonMail.
 *
 * ProtonMail is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ProtonMail is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with ProtonMail.  If not, see <https://www.gnu.org/licenses/>.
 */

package ch.protonmail.android.mailpagination.data.local

import ch.protonmail.android.mailpagination.data.local.entity.PageIntervalEntity

/**
 * Return the merged list of all overlapping intervals.
 */
fun List<PageIntervalEntity>.merge(): List<PageIntervalEntity> {
    val result = mutableListOf<PageIntervalEntity>()
    val sortedIntervals = sortedBy { it.minValue }
    for (current in sortedIntervals) {
        when {
            result.isEmpty() || !current.overlapWith(result.last()) -> result.add(current)
            else -> {
                // Merge current maxValue into last, if greater.
                val last = result.removeLast()
                val merged = if (current.maxValue > last.maxValue) last.copy(
                    maxValue = current.maxValue,
                    maxOrder = current.maxOrder,
                    maxId = current.maxId
                ) else last
                result.add(merged)
            }
        }
    }
    return result
}

fun PageIntervalEntity.overlapWith(other: PageIntervalEntity) = when {
    this.minValue > other.maxValue -> false
    this.minValue == other.maxValue && this.minOrder - other.maxOrder > 1 -> false
    else -> true // this.minValue < interval.maxValue
}
