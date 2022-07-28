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
            return itemTime.format(ItemTimeFormat.Today)
        }

        if (itemTime.isYesterday()) {
            return FormattedTime.Localizable(R.string.yesterday)
        }
        return FormattedTime.Date("foo")
    }

    private fun Duration.format(timeFormat: ItemTimeFormat) = FormattedTime.Date(
        DateFormat.getTimeInstance(
            timeFormat.dateFormatConst,
            getDefaultLocale()
        ).format(
            Date(this.inWholeMilliseconds)
        )
    )

    private fun Duration.isToday(): Boolean {
        val itemCalendar = Calendar.getInstance()
        itemCalendar.time = Date(this.inWholeMilliseconds)

        return isCurrentYear(itemCalendar) && isCurrentDayOfYear(itemCalendar)
    }

    private fun Duration.isYesterday(): Boolean {
        val itemCalendar = Calendar.getInstance()
        itemCalendar.time = Date(this.inWholeMilliseconds)

        return isCurrentYear(itemCalendar) && isDayOfYearYesterday(itemCalendar)
    }

    private fun isDayOfYearYesterday(itemCalendar: Calendar) =
        currentTime.get(Calendar.DAY_OF_YEAR) - itemCalendar.get(Calendar.DAY_OF_YEAR) == 1

    private fun isCurrentDayOfYear(itemCalendar: Calendar) =
        currentTime.get(Calendar.DAY_OF_YEAR) == itemCalendar.get(Calendar.DAY_OF_YEAR)

    private fun isCurrentYear(itemCalendar: Calendar) =
        currentTime.get(Calendar.YEAR) == itemCalendar.get(Calendar.YEAR)


    private enum class ItemTimeFormat(val dateFormatConst: Int) {
        Today(DateFormat.SHORT)
    }

    sealed interface FormattedTime {
        data class Localizable(@StringRes val stringId: Int) : FormattedTime
        data class Date(val value: String) : FormattedTime
    }
}

