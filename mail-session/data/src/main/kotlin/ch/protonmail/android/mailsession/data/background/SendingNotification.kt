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

package ch.protonmail.android.mailsession.data.background

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import ch.protonmail.android.mailcommon.domain.system.NotificationChannelId
import ch.protonmail.android.mailsession.data.R

/**
 * The minimal, silent ongoing notification WorkManager needs when it runs the expedited send drain
 * as a foreground service (Android 11 and lower). Kept as unobtrusive as the platform allows.
 */
internal object SendingNotification {

    const val NotificationId = 0x53656E64 // "Send"

    fun build(context: Context): NotificationCompat.Builder {
        ensureChannel(context)
        return NotificationCompat.Builder(context, NotificationChannelId.Sending)
            .setContentTitle(context.getString(R.string.sending_notification_title))
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setOngoing(true)
            .setSilent(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
    }

    private fun ensureChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(NotificationChannelId.Sending) != null) return
        val channel = NotificationChannel(
            NotificationChannelId.Sending,
            context.getString(R.string.sending_notification_channel_name),
            NotificationManager.IMPORTANCE_MIN
        ).apply {
            setShowBadge(false)
            enableVibration(false)
            enableLights(false)
        }
        manager.createNotificationChannel(channel)
    }
}
