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

package ch.protonmail.android.mailnotifications.domain.proxy

import android.annotation.SuppressLint
import android.app.Notification
import androidx.core.app.NotificationManagerCompat
import javax.inject.Inject

internal class NotificationManagerCompatProxyImpl @Inject constructor(
    private val notificationManagerCompat: NotificationManagerCompat
) : NotificationManagerCompatProxy {

    override fun dismissNotification(notificationId: Int) {
        notificationManagerCompat.cancel(notificationId)
    }

    override fun dismissNotificationGroupIfEmpty(groupKey: String) {
        val notifications = notificationManagerCompat.activeNotifications.filter {
            it.groupKey.contains(groupKey) // groupKey here has a different format here.
        }

        if (notifications.size == 1) dismissNotification(notifications.first().id)
    }

    @SuppressLint("MissingPermission")
    override fun showNotification(notificationId: Int, notification: Notification) {
        notificationManagerCompat.notify(notificationId, notification)
    }

    override fun areNotificationsEnabled() = notificationManagerCompat.areNotificationsEnabled()
}
