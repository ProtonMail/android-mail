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

package ch.protonmail.android.mailnotifications.data.remote.resource

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal enum class NotificationType(val type: String) {

    @SerialName(NOTIFICATION_TYPE_EMAIL)
    EMAIL("email"),

    @SerialName(NOTIFICATION_TYPE_OPEN_URL)
    OPEN_URL("open_url");

    companion object {

        private const val NOTIFICATION_TYPE_EMAIL = "email"
        private const val NOTIFICATION_TYPE_OPEN_URL = "open_url"

        fun fromStringOrNull(type: String): NotificationType? {
            return values().find {
                it.type == type
            }
        }
    }
}
