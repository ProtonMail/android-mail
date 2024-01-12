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

package ch.protonmail.android.mailmessage.data.local.usecase

import javax.inject.Inject

class SanitizeFullFileName @Inject constructor() {

    /**
     * Sanitizes the file name by replacing illegal/unicode/reserved characters with an underscore.
     *
     * If the [fileName] is fine as is, the original string is returned.
     */
    operator fun invoke(fileName: String): String {
        return fileName
            .replace(IllegalCharactersRegex, BaseReplacement)
            .replace(UnicodeControlCharactersRegex, BaseReplacement)
            .replace(ReservedCharactersRegex, BaseReplacement)
            .replace(TrailingPeriodAndSpacesRegex, BaseReplacement)
    }

    private companion object {

        val IllegalCharactersRegex = Regex("""[/?<>\\:*|"]""")
        val UnicodeControlCharactersRegex = Regex("""[\x00-\x1f\x80-\x9f]""")
        val ReservedCharactersRegex = Regex("""^\.+$""")
        val TrailingPeriodAndSpacesRegex = Regex("""[. ]+${'$'}""")
        const val BaseReplacement = "_"
    }
}
