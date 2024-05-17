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

package ch.protonmail.android.mailupselling.domain.model.telemetry.data

@JvmInline
value class AccountAge(val days: Int)

fun AccountAge.toUpsellingTelemetryDimensionValue(): String {
    @Suppress("MagicNumber")
    return when (this.days) {
        in Int.MIN_VALUE..<0 -> "n/a"
        0 -> "0"
        in 1..3 -> "01-03"
        in 4..10 -> "04-10"
        in 11..30 -> "11-30"
        in 31..60 -> "31-60"
        else -> ">60"
    }
}
