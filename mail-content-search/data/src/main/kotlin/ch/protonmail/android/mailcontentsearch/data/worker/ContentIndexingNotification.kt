/*
 * Copyright (c) 2025 Proton Technologies AG
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

package ch.protonmail.android.mailcontentsearch.data.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import ch.protonmail.android.mailcommon.domain.system.NotificationChannelId
import ch.protonmail.android.mailcontentsearch.data.R
import ch.protonmail.android.mailcontentsearch.data.receiver.ContentIndexingCancelReceiver

internal object ContentIndexingNotification {

    const val NotificationId = 0x437E534C // "CSrch"

    fun build(
        context: Context,
        accountLabel: String?,
        progress: Double?
    ): NotificationCompat.Builder {
        ensureChannel(context)
        val title = context.getString(R.string.content_search_notification_title)
        val titleWithAccount = accountLabel
            ?.takeIf { it.isNotBlank() }
            ?.let { "$title — $it" }
            ?: title
        val contentText = progress?.let {
            context.getString(R.string.content_search_notification_progress, it.coerceAtLeast(0.0))
        } ?: context.getString(R.string.content_search_notification_preparing)
        return NotificationCompat.Builder(context, NotificationChannelId.ContentSearch)
            .setContentTitle(titleWithAccount)
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            // ET-6209: switch to a determinate bar once the SDK's estimatedFraction is reliable.
            .setProgress(0, 0, true)
            .addAction(
                NotificationCompat.Action.Builder(
                    0,
                    context.getString(R.string.content_search_notification_cancel),
                    cancelPendingIntent(context)
                ).build()
            )
    }

    private fun cancelPendingIntent(context: Context): PendingIntent {
        // The sweep runs as a single unique work item, so cancelling stops the whole sweep.
        val intent = Intent(context, ContentIndexingCancelReceiver::class.java)
            .setAction(ContentIndexingCancelReceiver.ActionCancel)
        return PendingIntent.getBroadcast(
            context,
            CANCEL_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun ensureChannel(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (nm.getNotificationChannel(NotificationChannelId.ContentSearch) != null) return
        val channelName = context.getString(R.string.content_search_notification_channel_name)
        val channelDescription = context.getString(R.string.content_search_notification_channel_description)
        val channel =
            NotificationChannel(NotificationChannelId.ContentSearch, channelName, NotificationManager.IMPORTANCE_LOW)
                .apply {
                    description = channelDescription
                    setShowBadge(false)
                }
        nm.createNotificationChannel(channel)
    }
}

private const val CANCEL_REQUEST_CODE = 1001
