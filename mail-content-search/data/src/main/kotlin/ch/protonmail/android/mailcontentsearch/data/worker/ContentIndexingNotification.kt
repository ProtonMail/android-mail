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
import kotlin.math.roundToInt

internal object ContentIndexingNotification {

    const val NotificationId = 0x437E534C // "CSrch"

    private const val PercentageScale = 100

    fun build(
        context: Context,
        accountLabel: String?,
        progress: IndexingProgress?
    ): NotificationCompat.Builder {
        ensureChannel(context)
        val title = context.getString(R.string.content_search_notification_title)
        val titleWithAccount = accountLabel
            ?.takeIf { it.isNotBlank() }
            ?.let { "$title — $it" }
            ?: title
        // Rust reports a total of zero until it has sized the backfill, which would render as
        // "0% · 0/0 messages" rather than as the wait it is.
        val sized = progress?.takeIf { it.totalMessages > 0 }
        val contentText = sized?.contentText(context)
            ?: context.getString(R.string.content_search_notification_preparing)
        return NotificationCompat.Builder(context, NotificationChannelId.ContentSearch)
            .setContentTitle(titleWithAccount)
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            // Scaled to the percentage rather than the raw counts: those are message totals summed
            // across accounts, so they can outgrow the Int the bar takes.
            .setProgress(PercentageScale, sized?.roundedPercentage ?: 0, sized == null)
            .addAction(
                NotificationCompat.Action.Builder(
                    0,
                    context.getString(R.string.content_search_notification_pause),
                    cancelPendingIntent(context)
                ).build()
            )
    }

    /**
     * What the orchestrator has indexed so far, session-wide.
     *
     * The message counts are summed across every account, so they are only meaningful next to the
     * account counts - hence both are shown as soon as there is more than one account to index.
     */
    data class IndexingProgress(
        val percentage: Double,
        val processedMessages: Long,
        val totalMessages: Long,
        val completedAccounts: Int,
        val totalAccounts: Int
    ) {

        val roundedPercentage: Int get() = percentage.roundToInt().coerceIn(0, PercentageScale)

        fun contentText(context: Context): String = if (totalAccounts > 1) {
            context.getString(
                R.string.content_search_notification_message_progress_multi_account,
                roundedPercentage,
                processedMessages,
                totalMessages,
                completedAccounts,
                totalAccounts
            )
        } else {
            context.getString(
                R.string.content_search_notification_message_progress,
                roundedPercentage,
                processedMessages,
                totalMessages
            )
        }
    }

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
