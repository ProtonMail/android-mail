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

import androidx.compose.ui.graphics.Color
import arrow.core.Either
import arrow.core.left
import arrow.core.right
import javax.inject.Inject

class ColorMapper @Inject constructor() {

    fun toColor(string: String): Either<String, Color> {
        @Suppress("MagicNumber")
        with(string.substringAfter("#")) {
            val (a, r, g, b) = when (length) {
                3 -> listOf(NoTransparency, substring(0, 1), substring(1, 2), substring(2, 3))
                6 -> listOf(NoTransparency, substring(0, 2), substring(2, 4), substring(4, 6))
                4 -> listOf(substring(0, 1), substring(1, 2), substring(2, 3), substring(3, 4))
                8 -> listOf(substring(0, 2), substring(2, 4), substring(4, 6), substring(6, 8))
                else -> return string.left()
            }
            return Color(
                alpha = a.toColorInt(),
                red = r.toColorInt(),
                green = g.toColorInt(),
                blue = b.toColorInt()
            ).right()
        }
    }

    private fun String.toColorInt() = toInt(radix = 16)

    companion object {

        const val NoTransparency = "FF"
    }
}
