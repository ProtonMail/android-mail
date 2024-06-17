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

package ch.protonmail.android.mailcontact.domain

import java.util.regex.Pattern

fun String.extractProperty(property: String): String? {
    // Regex to match the property with possible line folding
    val regex = Pattern.compile(
        "$property(?:;[^:]+)*:((?:[^\\r\\n]*(?:\\r\\n|\\n)[ \\t])*(?:[^\\r\\n]+))",
        Pattern.CASE_INSENSITIVE
    )

    val matcher = regex.matcher(this)
    return if (matcher.find()) {
        // Replace folded line continuations with a single space
        matcher
            .group(1)
            ?.replace("\r\n ", "")
            ?.replace("\n ", "")
            ?.replace("\r\n\t", "")
            ?.replace("\n\t", "")
            ?.trim()
    } else null
}
