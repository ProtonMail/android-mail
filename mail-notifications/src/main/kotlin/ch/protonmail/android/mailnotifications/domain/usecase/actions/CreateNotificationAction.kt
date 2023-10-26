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

package ch.protonmail.android.mailnotifications.domain.usecase.actions

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat.Action
import ch.protonmail.android.mailnotifications.R
import ch.protonmail.android.mailnotifications.data.local.PushNotificationActionsBroadcastReceiver
import ch.protonmail.android.mailnotifications.domain.NotificationsDeepLinkHelper
import ch.protonmail.android.mailnotifications.domain.model.LocalNotificationAction
import ch.protonmail.android.mailnotifications.domain.model.PushNotificationPendingIntentPayloadData
import dagger.hilt.android.qualifiers.ApplicationContext
import me.proton.core.util.kotlin.serialize
import javax.inject.Inject

internal class CreateNotificationAction @Inject constructor(
    @ApplicationContext private val context: Context,
    private val notificationsDeepLinkHelper: NotificationsDeepLinkHelper
) {

    operator fun invoke(payload: PushNotificationPendingIntentPayloadData): Action {
        val pendingIntent = when (payload.action) {
            is LocalNotificationAction.MoveTo -> createMoveToPendingIntent(payload)
            is LocalNotificationAction.Reply -> createReplyPendingIntent(payload)
        }

        // Icon is always going to be 0, as it is unused.
        return Action.Builder(0, payload.action.title, pendingIntent).apply {
            // Force device to be unlocked on API >= 31 when interacting with the notification action.
            setAuthenticationRequired(true)
        }.build()
    }

    private fun createMoveToPendingIntent(payload: PushNotificationPendingIntentPayloadData): PendingIntent {
        val intent = Intent(context, PushNotificationActionsBroadcastReceiver::class.java).apply {
            putExtra(NotificationActionIntentExtraKey, payload.serialize())
        }

        return PendingIntent.getBroadcast(
            context,
            payload.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun createReplyPendingIntent(payload: PushNotificationPendingIntentPayloadData): PendingIntent {
        val intent = notificationsDeepLinkHelper.buildReplyToDeepLinkIntent(
            payload.messageId,
            payload.userId
        )

        // Do not use BroadcastReceiver to handle this Intent.
        return PendingIntent.getActivities(
            context,
            payload.hashCode(),
            arrayOf(intent),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    @Suppress("ClassOrdering")
    private val LocalNotificationAction.title: String
        get() {
            return when (this) {
                LocalNotificationAction.MoveTo.Archive ->
                    context.getString(R.string.notification_actions_archive_description)

                LocalNotificationAction.MoveTo.Trash ->
                    context.getString(R.string.notification_actions_trash_description)

                LocalNotificationAction.Reply ->
                    context.getString(R.string.notification_actions_reply_description)
            }
        }

    companion object {

        const val NotificationActionIntentExtraKey = "notificationAction"
    }
}
