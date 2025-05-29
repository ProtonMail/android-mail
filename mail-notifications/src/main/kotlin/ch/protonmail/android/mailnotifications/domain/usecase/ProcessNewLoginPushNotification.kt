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

import java.time.Instant
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import androidx.work.ListenableWorker
import ch.protonmail.android.mailcommon.presentation.system.NotificationProvider
import ch.protonmail.android.mailnotifications.R
import ch.protonmail.android.mailnotifications.data.local.ProcessPushNotificationDataWorkerUtils
import ch.protonmail.android.mailnotifications.domain.model.LocalPushNotificationData
import ch.protonmail.android.mailnotifications.domain.model.PushNotificationDismissPendingIntentData
import ch.protonmail.android.mailnotifications.domain.proxy.NotificationManagerCompatProxy
import ch.protonmail.android.mailnotifications.domain.usecase.actions.CreateNotificationAction
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

internal class ProcessNewLoginPushNotification @Inject constructor(
    @ApplicationContext private val context: Context,
    private val notificationProvider: NotificationProvider,
    private val notificationManagerCompatProxy: NotificationManagerCompatProxy,
    private val createNotificationAction: CreateNotificationAction
) {

    operator fun invoke(notificationData: LocalPushNotificationData.Login): ListenableWorker.Result {
        val userData = notificationData.userData
        val pushData = notificationData.pushData

        val notificationTitle = pushData.sender
        val notificationUserAddress = userData.userEmail
        val notificationContent = pushData.content
        val notificationUrl = pushData.url
        val notificationGroup = userData.userId
        val groupNotificationId = ProcessPushNotificationDataWorkerUtils.getNewLoginNotificationGroupForUserId(
            userData.userId
        ).hashCode()

        val intent = Intent(Intent.ACTION_VIEW, notificationUrl.toUri())

        val contentPendingIntent = PendingIntent.getActivities(
            context,
            notificationUrl.hashCode(),
            arrayOf(intent),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = notificationProvider.provideLoginNotificationBuilder(
            context = context,
            userAddress = notificationUserAddress,
            contentTitle = notificationTitle,
            contentText = notificationContent,
            group = notificationGroup,
            autoCancel = true
        ).apply { setContentIntent(contentPendingIntent) }.build()

        val groupNotification = notificationProvider.provideLoginNotificationBuilder(
            context = context,
            userAddress = notificationUserAddress,
            contentTitle = notificationTitle,
            contentText = context.getString(R.string.notification_summary_text_new_login_alerts),
            group = notificationGroup,
            isGroupSummary = true,
            autoCancel = true
        ).apply {
            setContentIntent(contentPendingIntent)

            val actionIntent = PushNotificationDismissPendingIntentData.GroupNotification(notificationGroup)
            setDeleteIntent(createNotificationAction(actionIntent))
        }.build()

        notificationManagerCompatProxy.run {
            showNotification(Instant.now().hashCode(), notification)
            showNotification(groupNotificationId, groupNotification)
        }

        return ListenableWorker.Result.success()
    }
}
