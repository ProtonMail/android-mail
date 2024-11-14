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

package ch.protonmail.android.maildetail.presentation.util

import android.content.res.Resources
import ch.protonmail.android.mailcommon.presentation.R
import kotlin.time.Duration

private const val MINUTES_IN_HOUR: Int = 60
private const val HOURS_IN_DAY: Int = 24

/**
 * Returns formatted duration as an array of maximum 3 Strings or empty list.
 *
 * Example: [X days, Y hours, Z minutes]
 */
fun Duration.toFormattedDurationParts(resources: Resources): List<String> {

    return if (this.isPositive() && this.inWholeMinutes >= 1) {

        if (this.inWholeMinutes < MINUTES_IN_HOUR) {
            listOf(
                resources.getQuantityString(
                    R.plurals.expiration_minutes_full_word,
                    this.inWholeMinutes.toInt(),
                    this.inWholeMinutes.toInt()
                )
            )
        } else if (this.inWholeHours < HOURS_IN_DAY) {
            listOf(
                resources.getQuantityString(
                    R.plurals.expiration_hours_full_word,
                    this.inWholeHours.toInt(),
                    this.inWholeHours.toInt()
                ),
                resources.getQuantityString(
                    R.plurals.expiration_minutes_full_word,
                    (this.inWholeMinutes - this.inWholeHours * MINUTES_IN_HOUR).toInt(),
                    (this.inWholeMinutes - this.inWholeHours * MINUTES_IN_HOUR).toInt()
                )
            )
        } else {
            listOf(
                resources.getQuantityString(
                    R.plurals.expiration_days_full_word,
                    this.inWholeDays.toInt(),
                    this.inWholeDays.toInt()
                ),
                resources.getQuantityString(
                    R.plurals.expiration_hours_full_word,
                    (this.inWholeHours - this.inWholeDays * HOURS_IN_DAY).toInt(),
                    (this.inWholeHours - this.inWholeDays * HOURS_IN_DAY).toInt()
                ),
                resources.getQuantityString(
                    R.plurals.expiration_minutes_full_word,
                    (this.inWholeMinutes - this.inWholeHours * MINUTES_IN_HOUR).toInt(),
                    (this.inWholeMinutes - this.inWholeHours * MINUTES_IN_HOUR).toInt()
                )
            )
        }
    } else emptyList()
}

/**
 * Returns formatted duration as a string according to Auto-Delete expiration requirements.
 *
 * Examples:
 * "This message will be automatically deleted in less than an hour".
 * "This message will be automatically deleted in less than a day".
 * "This message will be automatically deleted in {days rounded down} days".
 */
fun Duration.toFormattedAutoDeleteExpiration(resources: Resources): String? {

    return if (this.isPositive() && this.inWholeMinutes >= 1) {

        if (this.inWholeMinutes < MINUTES_IN_HOUR) {
            resources.getString(R.string.expiration_auto_delete_hour)
        } else if (this.inWholeHours < HOURS_IN_DAY) {
            resources.getString(R.string.expiration_auto_delete_day)
        } else {
            val numberOfDays = resources.getQuantityString(
                R.plurals.expiration_days_full_word,
                this.inWholeDays.toInt(),
                this.inWholeDays.toInt()
            )
            resources.getString(R.string.expiration_auto_delete_days, numberOfDays)
        }
    } else null
}
