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

import android.app.PendingIntent
import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import ch.protonmail.android.mailcommon.presentation.system.NotificationProvider
import ch.protonmail.android.mailnotifications.R
import ch.protonmail.android.mailnotifications.domain.AppInBackgroundState
import ch.protonmail.android.mailnotifications.domain.NotificationsDeepLinkHelper
import ch.protonmail.android.mailnotifications.domain.model.NotificationAction
import ch.protonmail.android.mailnotifications.domain.model.NotificationType
import ch.protonmail.android.mailnotifications.domain.model.PushNotification
import ch.protonmail.android.mailnotifications.domain.model.PushNotificationData
import ch.protonmail.android.mailnotifications.domain.model.PushNotificationSender
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

    override suspend fun doWork(): Result = if (appInBackgroundState.isAppInBackground()) {
        processNotification(super.getApplicationContext())
    } else {
        Result.success()
    }

    private suspend fun processNotification(context: Context): Result {
        val sessionId = inputData.getString(KEY_PUSH_NOTIFICATION_UID)
        val encryptedNotification = inputData.getString(KEY_PUSH_NOTIFICATION_ENCRYPTED_MESSAGE)

        return if (sessionId.isNullOrEmpty() || encryptedNotification.isNullOrEmpty()) {
            Result.failure(
                workDataOf(KEY_PROCESS_PUSH_NOTIFICATION_DATA_ERROR to "Input data is missing")
            )
        } else {
            processNotification(context, SessionId(sessionId), encryptedNotification)
        }
    }

    private suspend fun processNotification(
        context: Context,
        sessionId: SessionId,
        encryptedNotification: String
    ): Result {
        val notificationUserId = sessionManager.getUserId(sessionId)
            ?: return Result.failure(
                workDataOf(
                    KEY_PROCESS_PUSH_NOTIFICATION_DATA_ERROR to "User is unknown or inactive"
                )
            )

        val userId = UserId(notificationUserId.id)
        val user = userManager.getUser(userId)
        val decryptedNotification = decryptNotificationContent(userId, encryptedNotification).getOrNull()

        Timber.d("Decrypted data: $decryptedNotification")

        return when {
            decryptedNotification == null -> Result.failure(
                workDataOf(
                    KEY_PROCESS_PUSH_NOTIFICATION_DATA_ERROR to "Error decrypting the notification content."
                )
            )

            !isCreatedMessageNotification(decryptedNotification.value) ||
                decryptedNotification.value.data == null ||
                decryptedNotification.value.data.sender == null -> Result.success()

            else -> processNotification(
                context,
                user,
                decryptedNotification.value.data.sender,
                decryptedNotification.value.data
            )
        }
    }

    private fun processNotification(
        context: Context,
        user: User,
        sender: PushNotificationSender,
        notificationData: PushNotificationData
    ): Result {
        val notification = notificationProvider.provideEmailNotificationBuilder(
            context = context,
            contentTitle = sender.senderName.ifEmpty { sender.senderAddress },
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
            contentTitle = sender.senderName,
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

    private fun isCreatedMessageNotification(pushNotification: PushNotification): Boolean {
        return NotificationType.fromStringOrNull(pushNotification.type) == NotificationType.EMAIL &&
            pushNotification.data?.action == NotificationAction.CREATED
    }

    companion object {

        private const val KEY_PUSH_NOTIFICATION_UID = "UID"
        private const val KEY_PUSH_NOTIFICATION_ENCRYPTED_MESSAGE = "encryptedMessage"
        private const val KEY_PROCESS_PUSH_NOTIFICATION_DATA_ERROR = "ProcessPushNotificationDataError"

        fun params(uid: String, encryptedNotification: String) = mapOf(
            KEY_PUSH_NOTIFICATION_UID to uid,
            KEY_PUSH_NOTIFICATION_ENCRYPTED_MESSAGE to encryptedNotification
        )
    }
}
