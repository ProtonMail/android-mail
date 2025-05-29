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

package ch.protonmail.android.mailnotifications.domain.usecase

import android.content.Context
import androidx.work.ListenableWorker
import ch.protonmail.android.mailcommon.domain.coroutines.AppScope
import ch.protonmail.android.mailcommon.presentation.system.NotificationProvider
import ch.protonmail.android.mailnotifications.R
import ch.protonmail.android.mailnotifications.domain.model.LocalNotificationAction
import ch.protonmail.android.mailnotifications.domain.model.LocalPushNotificationData
import ch.protonmail.android.mailnotifications.domain.model.PushNotificationDismissPendingIntentData
import ch.protonmail.android.mailnotifications.domain.model.PushNotificationPendingIntentPayloadData
import ch.protonmail.android.mailnotifications.domain.proxy.NotificationManagerCompatProxy
import ch.protonmail.android.mailnotifications.domain.usecase.actions.CreateNotificationAction
import ch.protonmail.android.mailnotifications.domain.usecase.intents.CreateNewMessageNavigationIntent
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import me.proton.core.domain.entity.UserId
import me.proton.core.eventmanager.domain.EventManagerConfig
import me.proton.core.eventmanager.domain.EventManagerProvider
import javax.inject.Inject

internal class ProcessNewMessagePushNotification @Inject constructor(
    @ApplicationContext private val context: Context,
    private val notificationProvider: NotificationProvider,
    private val notificationManagerCompatProxy: NotificationManagerCompatProxy,
    private val createNewMessageNavigationIntent: CreateNewMessageNavigationIntent,
    private val createNotificationAction: CreateNotificationAction,
    private val eventManager: EventManagerProvider,
    @AppScope private val coroutineScope: CoroutineScope
) {

    @Suppress("LongMethod")
    operator fun invoke(notificationData: LocalPushNotificationData.NewMessage): ListenableWorker.Result {

        val userData = notificationData.userData
        val pushData = notificationData.pushData

        coroutineScope.launch {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastExecutionTime >= DEBOUNCE_INTERVAL_MS) {
                lastExecutionTime = currentTime
                val config = EventManagerConfig.Core(UserId(userData.userId))
                eventManager.get(config).resume()
            }
        }

        val notificationTitle = pushData.sender
        val notificationUserAddress = userData.userEmail
        val notificationContent = pushData.content
        val notificationGroup = userData.userId
        val notificationId = pushData.messageId.hashCode()
        val groupNotificationId = notificationGroup.hashCode()

        val notification = notificationProvider.provideEmailNotificationBuilder(
            context = context,
            contentTitle = notificationTitle,
            subText = notificationUserAddress,
            contentText = notificationContent,
            group = notificationGroup,
            autoCancel = true
        ).apply {
            setContentIntent(
                createNewMessageNavigationIntent(notificationId, pushData.messageId, userData.userId)
            )

            val archiveAction = PushNotificationPendingIntentPayloadData(
                notificationId,
                notificationGroup,
                userData.userId,
                pushData.messageId,
                LocalNotificationAction.MoveTo.Archive
            )

            val trashAction = archiveAction.copy(action = LocalNotificationAction.MoveTo.Trash)
            val markAsReadAction = archiveAction.copy(action = LocalNotificationAction.MarkAsRead)

            addAction(createNotificationAction(archiveAction))
            addAction(createNotificationAction(trashAction))
            addAction(createNotificationAction(markAsReadAction))

            val dismissalAction = PushNotificationDismissPendingIntentData.SingleNotification(
                userData.userId,
                notificationId
            )

            setDeleteIntent(createNotificationAction(dismissalAction))
        }.build()

        val groupNotification = notificationProvider.provideEmailNotificationBuilder(
            context = context,
            contentTitle = notificationTitle,
            subText = notificationUserAddress,
            contentText = context.getString(R.string.notification_summary_text_new_messages),
            group = notificationGroup,
            isGroupSummary = true,
            autoCancel = true
        ).apply {
            setContentIntent(createNewMessageNavigationIntent(notificationId, userData.userId))

            val dismissalAction = PushNotificationDismissPendingIntentData.GroupNotification(userData.userId)
            setDeleteIntent(createNotificationAction(dismissalAction))
        }.build()

        notificationManagerCompatProxy.run {
            showNotification(notificationId, notification)
            showNotification(groupNotificationId, groupNotification)
        }

        return ListenableWorker.Result.success()
    }

    companion object {

        private var lastExecutionTime: Long = 0
        private const val DEBOUNCE_INTERVAL_MS = 10_000
    }
}
