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

import java.time.Instant
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import ch.protonmail.android.mailcommon.presentation.system.NotificationProvider
import ch.protonmail.android.mailnotifications.R
import ch.protonmail.android.mailnotifications.data.local.ProcessPushNotificationDataWorkerUtils.getNewLoginNotificationGroupForUserId
import ch.protonmail.android.mailnotifications.data.local.ProcessPushNotificationDataWorkerUtils.getNewLoginNotificationIdForUserId
import ch.protonmail.android.mailnotifications.data.local.ProcessPushNotificationDataWorkerUtils.isNewLoginNotification
import ch.protonmail.android.mailnotifications.data.local.ProcessPushNotificationDataWorkerUtils.isNewMessageNotification
import ch.protonmail.android.mailnotifications.domain.AppInBackgroundState
import ch.protonmail.android.mailnotifications.domain.NotificationsDeepLinkHelper
import ch.protonmail.android.mailnotifications.domain.model.PushNotificationData
import ch.protonmail.android.mailnotifications.domain.proxy.NotificationManagerCompatProxy
import ch.protonmail.android.mailnotifications.domain.usecase.DecryptNotificationContent
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import me.proton.core.accountmanager.domain.SessionManager
import me.proton.core.domain.entity.UserId
import me.proton.core.network.domain.session.SessionId
import me.proton.core.user.domain.UserManager
import me.proton.core.user.domain.entity.User
import timber.log.Timber

@HiltWorker
class ProcessPushNotificationDataWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParameters: WorkerParameters,
    private val sessionManager: SessionManager,
    private val decryptNotificationContent: DecryptNotificationContent,
    private val appInBackgroundState: AppInBackgroundState,
    private val notificationProvider: NotificationProvider,
    private val userManager: UserManager,
    private val notificationsDeepLinkHelper: NotificationsDeepLinkHelper,
    private val notificationManagerCompatProxy: NotificationManagerCompatProxy
) : CoroutineWorker(context, workerParameters) {

    override suspend fun doWork(): Result {
        val sessionId = inputData.getString(KeyPushNotificationUid)
        val encryptedNotification = inputData.getString(KeyPushNotificationEncryptedMessage)

        return if (sessionId.isNullOrEmpty() || encryptedNotification.isNullOrEmpty()) {
            Result.failure(
                workDataOf(KeyProcessPushNotificationDataError to "Input data is missing")
            )
        } else {
            processNotification(applicationContext, SessionId(sessionId), encryptedNotification)
        }
    }

    private suspend fun processNotification(
        context: Context,
        sessionId: SessionId,
        encryptedNotification: String
    ): Result {
        val notificationUserId = sessionManager.getUserId(sessionId)
            ?: return Result.failure(
                workDataOf(KeyProcessPushNotificationDataError to "User is unknown or inactive")
            )

        val userId = UserId(notificationUserId.id)
        val user = userManager.getUser(userId)
        val decryptedNotification = decryptNotificationContent(userId, encryptedNotification).getOrNull()
            ?: return Result.failure(
                workDataOf(KeyProcessPushNotificationDataError to "Unable to decrypt notification content.")
            )

        val data = decryptedNotification.value.data ?: return Result.failure(
            workDataOf(KeyProcessPushNotificationDataError to "Push Notification data is null.")
        )

        Timber.d("Decrypted data: $decryptedNotification")

        return when {
            isNewMessageNotification(decryptedNotification.value) &&
                appInBackgroundState.isAppInBackground() -> processNewMessageNotification(
                context,
                user,
                data
            )

            isNewLoginNotification(decryptedNotification.value) -> {
                processNewLoginNotification(
                    context,
                    user,
                    data
                )
            }

            else -> Result.success()
        }
    }

    private fun processNewLoginNotification(
        context: Context,
        user: User,
        notificationData: PushNotificationData
    ): Result {
        val notificationTitle = notificationData.sender?.senderName
            ?: context.getString(R.string.notification_title_text_new_login_alerts_fallback)

        val notificationUrl = notificationData.url
        val notificationGroup = getNewLoginNotificationGroupForUserId(user.userId)
        val viewIntent = Intent(Intent.ACTION_VIEW, Uri.parse(notificationUrl))
        val contentPendingIntent = PendingIntent.getActivities(
            context,
            notificationUrl.hashCode(),
            arrayOf(viewIntent),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = notificationProvider.provideLoginNotificationBuilder(
            context = context,
            userAddress = user.email ?: "",
            contentTitle = notificationTitle,
            contentText = notificationData.body,
            group = notificationGroup,
            autoCancel = true
        ).apply { setContentIntent(contentPendingIntent) }.build()

        val groupNotification = notificationProvider.provideLoginNotificationBuilder(
            context = context,
            userAddress = user.email ?: "",
            contentTitle = notificationTitle,
            contentText = context.getString(R.string.notification_summary_text_new_login_alerts),
            group = notificationGroup,
            isGroupSummary = true,
            autoCancel = true
        ).apply { setContentIntent(contentPendingIntent) }.build()

        notificationManagerCompatProxy.run {
            showNotification(Instant.now().hashCode(), notification)
            showNotification(getNewLoginNotificationIdForUserId(user.userId), groupNotification)
        }

        return Result.success()
    }

    private fun processNewMessageNotification(
        context: Context,
        user: User,
        notificationData: PushNotificationData
    ): Result {
        val sender = notificationData.sender
        val notificationTitle = sender?.senderName?.ifEmpty { sender.senderAddress }
            ?: context.getString(R.string.notification_title_text_new_message_fallback)

        val notification = notificationProvider.provideEmailNotificationBuilder(
            context = context,
            contentTitle = notificationTitle,
            subText = user.email ?: "",
            contentText = notificationData.body,
            group = user.userId.id,
            autoCancel = true
        ).apply {
            setContentIntent(
                PendingIntent.getActivity(
                    context,
                    notificationData.messageId.hashCode(),
                    notificationsDeepLinkHelper.buildMessageDeepLinkIntent(
                        notificationData.messageId.hashCode().toString(),
                        notificationData.messageId,
                        user.userId.id
                    ),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
        }.build()

        val groupNotification = notificationProvider.provideEmailNotificationBuilder(
            context = context,
            contentTitle = notificationTitle,
            subText = user.email ?: "",
            contentText = context.getString(R.string.notification_summary_text_new_messages),
            group = user.userId.id,
            isGroupSummary = true,
            autoCancel = true
        ).apply {
            setContentIntent(
                PendingIntent.getActivity(
                    context,
                    notificationData.messageId.hashCode(),
                    notificationsDeepLinkHelper.buildMessageGroupDeepLinkIntent(
                        user.userId.id.hashCode().toString(),
                        user.userId.id
                    ),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
        }.build()

        notificationManagerCompatProxy.run {
            showNotification(notificationData.messageId.hashCode(), notification)
            showNotification(user.userId.id.hashCode(), groupNotification)
        }

        return Result.success()
    }

    companion object {

        const val KeyPushNotificationUid = "UID"
        const val KeyPushNotificationEncryptedMessage = "encryptedMessage"
        const val KeyProcessPushNotificationDataError = "ProcessPushNotificationDataError"

        fun params(uid: String, encryptedNotification: String) = mapOf(
            KeyPushNotificationUid to uid,
            KeyPushNotificationEncryptedMessage to encryptedNotification
        )
    }
}
