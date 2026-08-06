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
        progress: AccountProgress?
    ): NotificationCompat.Builder {
        ensureChannel(context)
        val title = context.getString(R.string.content_search_notification_title)
        val titleWithAccount = accountLabel
            ?.takeIf { it.isNotBlank() }
            ?.let { "$title — $it" }
            ?: title
        val contentText = progress?.let {
            context.getString(R.string.content_search_notification_progress_accounts, it.completed, it.total)
        } ?: context.getString(R.string.content_search_notification_preparing)
        return NotificationCompat.Builder(context, NotificationChannelId.ContentSearch)
            .setContentTitle(titleWithAccount)
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            // Determinate over accounts, not messages: the orchestrator's overall percentage is
            // summed across every account's totals, so it jumps when an account is added or removed.
            .setProgress(progress?.total ?: 0, progress?.completed ?: 0, progress == null)
            .addAction(
                NotificationCompat.Action.Builder(
                    0,
                    context.getString(R.string.content_search_notification_pause),
                    cancelPendingIntent(context)
                ).build()
            )
    }

    /** Accounts finished out of the accounts the orchestrator set out to index. */
    data class AccountProgress(val completed: Int, val total: Int)

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
