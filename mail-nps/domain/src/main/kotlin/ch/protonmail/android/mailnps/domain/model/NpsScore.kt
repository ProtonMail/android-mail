/*
 * Copyright (c) 2026 Proton Technologies AG
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

package ch.protonmail.android.mailnps.domain.model

/**
 * A Net Promoter Score, an integer from [MIN]..[MAX] (0 = "very unlikely", 10 = "very likely").
 * The value is validated on construction so an out-of-range score can never reach telemetry.
 */
@JvmInline
value class NpsScore(val value: Int) {

    init {
        require(value in MIN..MAX) { "NPS score must be in $MIN..$MAX, was $value" }
    }

    companion object {

        const val MIN = 0
        const val MAX = 10

        /** All valid scores, low to high, for rendering the 0-10 button row. */
        val all: List<NpsScore> = (MIN..MAX).map { NpsScore(it) }
    }
}
