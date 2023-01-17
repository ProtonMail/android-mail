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

package ch.protonmail.android.mailcommon.domain.util

/**
 * Throws an [IllegalArgumentException] if the value is `null` or a blank String.
 *  Otherwise returns the not null value.
 *
 * @param fieldName Optional name of the field
 *
 * @sample `requireNotBlank(userId, "User id")` -> "User id was null" / "User id was blank"
 */
fun requireNotBlank(value: String?, fieldName: String? = null): String {
    requireNotNull(value) { "${fieldName ?: "Required value"} was null." }
    require(value.isNotBlank()) { "${fieldName ?: "Required value"} was blank." }
    return value
}

/**
 * Throws an [IllegalArgumentException] if the value is `null` or an empty String.
 *  Otherwise returns the not null value.
 *
 * @param fieldName Optional name of the field
 *
 * @sample `requireNotEmpty(userId, "User id")` -> "User id was null." / "User id was empty."
 */
fun requireNotEmpty(value: String?, fieldName: String? = null): String {
    requireNotNull(value) { "${fieldName ?: "Required value"} was null." }
    require(value.isNotEmpty()) { "${fieldName ?: "Required value"} was empty." }
    return value
}
