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

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import ch.protonmail.android.mailcommon.domain.coroutines.AppScope
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailmessage.domain.repository.MessageRepository
import ch.protonmail.android.mailnotifications.domain.model.LocalNotificationAction
import ch.protonmail.android.mailnotifications.domain.model.PushNotificationPendingIntentPayloadData
import ch.protonmail.android.mailnotifications.domain.proxy.NotificationManagerCompatProxy
import ch.protonmail.android.mailnotifications.domain.usecase.intents.CreateNotificationActionPendingIntent.Companion.NotificationActionIntentExtraKey
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import me.proton.core.domain.entity.UserId
import me.proton.core.util.kotlin.deserialize
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
internal class PushNotificationActionsBroadcastReceiver @Inject constructor() : BroadcastReceiver() {

    @Inject
    lateinit var messageRepository: MessageRepository

    @Inject
    lateinit var notificationManagerCompatProxy: NotificationManagerCompatProxy

    @Inject
    @AppScope
    lateinit var coroutineScope: CoroutineScope

    @Inject
    @ApplicationContext
    lateinit var applicationContext: Context

    override fun onReceive(context: Context?, intent: Intent?) {
        val rawAction = intent?.extras?.getString(NotificationActionIntentExtraKey)

        if (rawAction == null) {
            Timber.d("Unable to extract action from intent $intent.")
            return
        }

        val actionData = rawAction.deserialize<PushNotificationPendingIntentPayloadData>()

        coroutineScope.launch {
            val result = when (val action = actionData.action) {
                is LocalNotificationAction.MoveTo -> {
                    messageRepository.moveTo(
                        userId = UserId(actionData.userId),
                        messageId = MessageId(actionData.messageId),
                        fromLabel = null,
                        toLabel = action.destinationLabel
                    )
                }

                is LocalNotificationAction.Reply -> TODO("MAILANDR-1135")
            }

            result.onLeft {
                Timber.d("Error moving message from notification action: $it")
            }.onRight {
                Timber.d("Message moved successfully from notification action: $it")
            }
        }

        notificationManagerCompatProxy.run {
            dismissNotification(actionData.notificationId)
            dismissNotificationGroupIfEmpty(actionData.notificationGroup)
        }
    }
}
