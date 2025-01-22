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

package ch.protonmail.android.mailnotifications.domain

sealed class NotificationInteraction {

    data class SingleTap(
        val userId: String,
        val messageId: String
    ) : NotificationInteraction()

    data class GroupTap(
        val userId: String
    ) : NotificationInteraction()

    data object NoAction : NotificationInteraction()
}

fun resolveNotificationInteraction(
    userId: String?,
    messageId: String?,
    action: String?
): NotificationInteraction {
    return when {
        messageId != null && userId != null && action == null -> {
            NotificationInteraction.SingleTap(userId, messageId)
        }

        messageId == null && userId != null && action == null -> {
            NotificationInteraction.GroupTap(userId)
        }

        else -> NotificationInteraction.NoAction
    }
}
