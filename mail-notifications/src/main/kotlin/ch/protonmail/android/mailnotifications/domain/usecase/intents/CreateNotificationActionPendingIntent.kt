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

package ch.protonmail.android.mailnotifications.domain.usecase.intents

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import ch.protonmail.android.mailnotifications.data.local.PushNotificationActionsBroadcastReceiver
import ch.protonmail.android.mailnotifications.domain.model.PushNotificationPendingIntentPayloadData
import dagger.hilt.android.qualifiers.ApplicationContext
import me.proton.core.util.kotlin.serialize
import javax.inject.Inject

internal class CreateNotificationActionPendingIntent @Inject constructor(
    @ApplicationContext private val context: Context
) {

    operator fun invoke(payload: PushNotificationPendingIntentPayloadData): PendingIntent {
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

    companion object {

        const val NotificationActionIntentExtraKey = "notificationAction"
    }
}
