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

import ch.protonmail.android.mailcommon.presentation.R
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import kotlin.time.Duration

private const val MINUTES_IN_HOUR: Int = 60
private const val HOURS_IN_DAY: Int = 24

/**
 * Returns formatted duration as an array of maximum 3 TextUiModels or null.
 *
 * Example: [X days, Y hours, Z minutes]
 */
fun Duration.toExpirationTextUiModels(): List<TextUiModel>? {

    return if (this.isPositive()) {

        if (this.inWholeMinutes < MINUTES_IN_HOUR) {
            listOf(
                TextUiModel.PluralisedText(
                    R.plurals.expiration_minutes_full_word,
                    this.inWholeMinutes.toInt()
                )
            )
        } else if (this.inWholeHours < HOURS_IN_DAY) {
            listOf(
                TextUiModel.PluralisedText(
                    R.plurals.expiration_hours_full_word,
                    this.inWholeHours.toInt()
                ),
                TextUiModel.PluralisedText(
                    R.plurals.expiration_minutes_full_word,
                    (this.inWholeMinutes - this.inWholeHours * MINUTES_IN_HOUR).toInt()
                )
            )
        } else {
            listOf(
                TextUiModel.PluralisedText(
                    R.plurals.expiration_days_full_word,
                    this.inWholeDays.toInt()
                ),
                TextUiModel.PluralisedText(
                    R.plurals.expiration_hours_full_word,
                    (this.inWholeHours - this.inWholeDays * HOURS_IN_DAY).toInt()
                ),
                TextUiModel.PluralisedText(
                    R.plurals.expiration_minutes_full_word,
                    (this.inWholeMinutes - this.inWholeHours * MINUTES_IN_HOUR).toInt()
                )
            )
        }

    } else null
}


