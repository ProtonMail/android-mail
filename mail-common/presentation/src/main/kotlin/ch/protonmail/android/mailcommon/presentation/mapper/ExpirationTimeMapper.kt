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

package ch.protonmail.android.mailcommon.presentation.mapper

import ch.protonmail.android.mailcommon.domain.usecase.GetCurrentEpochTimeDuration
import ch.protonmail.android.mailcommon.presentation.R.string
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import javax.inject.Inject
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds

class ExpirationTimeMapper @Inject constructor(
    private val getCurrentEpochTimeDuration: GetCurrentEpochTimeDuration
) {

    fun toUiModel(epochTime: Duration): TextUiModel {
        val timeDiff = epochTime - getCurrentEpochTimeDuration()
        return when {
            timeDiff <= 0.seconds -> TextUiModel(string.expiration_expired)
            timeDiff < 1.hours -> TextUiModel(string.expiration_minutes_arg, timeDiff.inWholeMinutes)
            timeDiff < 1.days -> TextUiModel(string.expiration_hours_arg, timeDiff.inWholeHours)
            else -> TextUiModel(string.expiration_days_arg, timeDiff.inWholeDays)
        }
    }
}
