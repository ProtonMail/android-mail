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
import androidx.annotation.VisibleForTesting
import androidx.core.app.NotificationCompat
import ch.protonmail.android.mailcommon.domain.system.NotificationChannelId
import ch.protonmail.android.mailcontentsearch.data.R
import ch.protonmail.android.mailcontentsearch.data.receiver.ContentIndexingCancelReceiver
import kotlin.math.roundToInt

internal object ContentIndexingNotification {

    const val NotificationId = 0x437E534C // "CSrch"

    private const val PercentageScale = 100

    /**
     * [percentage] is how far the named account's own backfill has got, or null while there is
     * nothing determinate to report - never the orchestrator's session-wide figure, which is summed
     * across every account and so would not match the one account this notification names.
     */
    fun build(
        context: Context,
        accountAddress: String?,
        percentage: Double?
    ): NotificationCompat.Builder {
        ensureChannel(context)
        return NotificationCompat.Builder(context, NotificationChannelId.ContentSearch)
            .setContentTitle(context.getString(R.string.content_search_notification_title))
            .setContentText(contentText(context, accountAddress, percentage))
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            // Scaled to the percentage rather than the raw counts, which can outgrow the Int the bar
            // takes.
            .setProgress(PercentageScale, percentage?.rounded() ?: 0, percentage == null)
            .addAction(
                NotificationCompat.Action.Builder(
                    0,
                    context.getString(R.string.content_search_notification_pause),
                    cancelPendingIntent(context)
                ).build()
            )
    }

    /** The account the notification is for, next to how far its indexing has got. */
    @VisibleForTesting
    fun contentText(
        context: Context,
        accountAddress: String?,
        percentage: Double?
    ): String {
        val status = percentage
            ?.let { context.getString(R.string.content_search_notification_percentage, it.rounded()) }
            ?: context.getString(R.string.content_search_notification_preparing)
        return accountAddress
            ?.takeIf { it.isNotBlank() }
            ?.let { context.getString(R.string.content_search_notification_progress, it, status) }
            ?: status
    }

    // Rust revises its totals as it goes, so the percentage can overshoot what a bar takes.
    private fun Double.rounded(): Int = roundToInt().coerceIn(0, PercentageScale)

    private fun cancelPendingIntent(context: Context): PendingIntent {
        // Stops the orchestrator for every account. Nothing persists that, so the next foreground
        // transition resumes - hence "Pause" rather than "Cancel".
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
