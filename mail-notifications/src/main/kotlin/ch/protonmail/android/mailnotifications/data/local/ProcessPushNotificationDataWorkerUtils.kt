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

package ch.protonmail.android.mailnotifications.data.local

import ch.protonmail.android.mailnotifications.data.remote.resource.NotificationAction
import ch.protonmail.android.mailnotifications.data.remote.resource.NotificationType
import ch.protonmail.android.mailnotifications.data.remote.resource.PushNotification

internal object ProcessPushNotificationDataWorkerUtils {

    fun isNewMessageNotification(pushNotification: PushNotification): Boolean {
        return NotificationType.fromStringOrNull(pushNotification.type) == NotificationType.EMAIL &&
            pushNotification.data?.action == NotificationAction.CREATED
    }

    fun isNewLoginNotification(pushNotification: PushNotification): Boolean {
        return NotificationType.fromStringOrNull(pushNotification.type) == NotificationType.OPEN_URL &&
            pushNotification.data?.action == NotificationAction.CREATED
    }

    fun isMessageReadNotification(pushNotification: PushNotification): Boolean {
        return NotificationType.fromStringOrNull(pushNotification.type) == NotificationType.EMAIL &&
            pushNotification.data?.action == NotificationAction.TOUCHED
    }

    fun getNewLoginNotificationGroupForUserId(userId: String) = "$userId-openurl"

    fun getNewLoginNotificationIdForUserId(userId: String) = getNewLoginNotificationGroupForUserId(userId).hashCode()
}
