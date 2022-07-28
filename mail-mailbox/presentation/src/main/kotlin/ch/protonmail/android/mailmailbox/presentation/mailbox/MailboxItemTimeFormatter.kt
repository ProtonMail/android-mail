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

package ch.protonmail.android.mailmailbox.presentation.mailbox

import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import androidx.annotation.StringRes
import ch.protonmail.android.mailcommon.domain.usecase.GetDefaultLocale
import ch.protonmail.android.mailmailbox.presentation.R
import javax.inject.Inject
import kotlin.time.Duration

class MailboxItemTimeFormatter @Inject constructor(
    private val currentTime: Calendar,
    private val getDefaultLocale: GetDefaultLocale
) {

    operator fun invoke(itemTime: Duration): FormattedTime {
        if (itemTime.isToday()) {
            return FormattedTime.Localized(itemTime.toHourLocalised())
        }

        if (itemTime.isYesterday()) {
            return FormattedTime.Localizable(R.string.yesterday)
        }

        if (itemTime.isThisWeek()) {
            return FormattedTime.Localized(itemTime.toWeekDay())
        }
        return FormattedTime.Localized("foo")
    }

    private fun Duration.toHourLocalised() = DateFormat.getTimeInstance(DateFormat.SHORT, getDefaultLocale())
        .format(Date(this.inWholeMilliseconds))

    private fun Duration.toWeekDay() = SimpleDateFormat("EEEE", getDefaultLocale())
        .format(Date(this.inWholeMilliseconds))

    private fun isYesterday(itemCalendar: Calendar) = isCurrentYear(itemCalendar) &&
        currentTime.get(Calendar.DAY_OF_YEAR) - itemCalendar.get(Calendar.DAY_OF_YEAR) == 1

    private fun isToday(itemCalendar: Calendar) = isCurrentYear(itemCalendar) &&
        currentTime.get(Calendar.DAY_OF_YEAR) == itemCalendar.get(Calendar.DAY_OF_YEAR)

    private fun isCurrentWeek(itemCalendar: Calendar) = isCurrentYear(itemCalendar) &&
        currentTime.get(Calendar.WEEK_OF_YEAR) == itemCalendar.get(Calendar.WEEK_OF_YEAR)

    private fun isCurrentYear(itemCalendar: Calendar) =
        currentTime.get(Calendar.YEAR) == itemCalendar.get(Calendar.YEAR)

    private fun Duration.isToday(): Boolean = isToday(toCalendar())

    private fun Duration.isYesterday(): Boolean = isYesterday(toCalendar())

    private fun Duration.isThisWeek(): Boolean = isCurrentWeek(toCalendar())

    private fun Duration.toCalendar(): Calendar {
        val itemCalendar = Calendar.getInstance()
        itemCalendar.time = Date(this.inWholeMilliseconds)
        return itemCalendar
    }

    sealed interface FormattedTime {
        data class Localizable(@StringRes val stringId: Int) : FormattedTime
        data class Localized(val value: String) : FormattedTime
    }
}

