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

package ch.protonmail.android.mailcommon.presentation.usecase

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import ch.protonmail.android.mailcommon.domain.usecase.GetAppLocale
import javax.inject.Inject

class FormatLocalDate @Inject constructor(
    private val getAppLocale: GetAppLocale
) {

    operator fun invoke(date: LocalDate): String {
        return date.format(
            DateTimeFormatter.ofLocalizedDate(
                FormatStyle.MEDIUM
            ).withLocale(
                getAppLocale()
            )
        )
    }
}
