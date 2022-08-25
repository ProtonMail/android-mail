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

package ch.protonmail.android.mailmailbox.presentation.mailbox.usecase

import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import ch.protonmail.android.mailcommon.domain.usecase.GetAppLocale
import ch.protonmail.android.mailcommon.domain.usecase.GetDefaultCalendar
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailmailbox.presentation.R
import javax.inject.Inject
import kotlin.time.Duration

class FormatMailboxItemTime @Inject constructor(
    private val getDefaultCalendar: GetDefaultCalendar,
    private val getAppLocale: GetAppLocale
) {

    private val currentTime by lazy { getDefaultCalendar() }

    operator fun invoke(itemTime: Duration): TextUiModel {
        if (itemTime.isToday()) {
            return TextUiModel.Text(itemTime.toHourAndMinutes())
        }
        if (itemTime.isYesterday()) {
            return TextUiModel.TextRes(R.string.yesterday)
        }
        if (itemTime.isThisWeek()) {
            return TextUiModel.Text(itemTime.toWeekDay())
        }
        if (itemTime.isThisYear()) {
            return TextUiModel.Text(itemTime.toDayAndMonth())
        }
        return TextUiModel.Text(itemTime.toFullDate())
    }

    private fun Duration.toFullDate() = DateFormat.getDateInstance(DateFormat.MEDIUM, getAppLocale())
        .format(Date(this.inWholeMilliseconds))

    private fun Duration.toDayAndMonth() = this.toFullDate()

    private fun Duration.toHourAndMinutes() = DateFormat.getTimeInstance(DateFormat.SHORT, getAppLocale())
        .format(Date(this.inWholeMilliseconds))

    private fun Duration.toWeekDay() = SimpleDateFormat("EEEE", getAppLocale())
        .format(Date(this.inWholeMilliseconds))

    private fun isYesterday(itemCalendar: Calendar) = isCurrentYear(itemCalendar) &&
        currentTime.get(Calendar.DAY_OF_YEAR) - itemCalendar.get(Calendar.DAY_OF_YEAR) == 1 ||
        isYesterdayAcrossYearChange(itemCalendar)

    private fun isToday(itemCalendar: Calendar) = isCurrentYear(itemCalendar) &&
        currentTime.get(Calendar.DAY_OF_YEAR) == itemCalendar.get(Calendar.DAY_OF_YEAR)

    private fun isCurrentWeek(itemCalendar: Calendar) =
        currentTime.get(Calendar.WEEK_OF_YEAR) == itemCalendar.get(Calendar.WEEK_OF_YEAR)

    private fun isCurrentYear(itemCalendar: Calendar) =
        currentTime.get(Calendar.YEAR) == itemCalendar.get(Calendar.YEAR)

    private fun isYesterdayAcrossYearChange(itemCalendar: Calendar) = isPreviousYear(itemCalendar) &&
        itemCalendar.get(Calendar.DAY_OF_YEAR) - currentTime.get(Calendar.DAY_OF_YEAR) == 364

    private fun isPreviousYear(itemCalendar: Calendar) =
        currentTime.get(Calendar.YEAR) - itemCalendar.get(Calendar.YEAR) == 1

    private fun Duration.isToday() = isToday(toCalendar())
    private fun Duration.isYesterday() = isYesterday(toCalendar())
    private fun Duration.isThisWeek() = isCurrentWeek(toCalendar())
    private fun Duration.isThisYear() = isCurrentYear(toCalendar())

    private fun Duration.toCalendar(): Calendar {
        val itemCalendar = Calendar.getInstance(getAppLocale())
        itemCalendar.time = Date(this.inWholeMilliseconds)
        return itemCalendar
    }

}

