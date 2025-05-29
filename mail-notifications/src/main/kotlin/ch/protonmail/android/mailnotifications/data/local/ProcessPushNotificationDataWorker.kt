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

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import ch.protonmail.android.mailcommon.domain.AppInBackgroundState
import ch.protonmail.android.mailnotifications.R
import ch.protonmail.android.mailnotifications.data.local.ProcessPushNotificationDataWorkerUtils.isMessageReadNotification
import ch.protonmail.android.mailnotifications.data.local.ProcessPushNotificationDataWorkerUtils.isNewLoginNotification
import ch.protonmail.android.mailnotifications.data.local.ProcessPushNotificationDataWorkerUtils.isNewMessageNotification
import ch.protonmail.android.mailnotifications.data.remote.resource.PushNotificationData
import ch.protonmail.android.mailnotifications.data.remote.resource.PushNotificationSender
import ch.protonmail.android.mailnotifications.domain.model.LocalPushNotificationData
import ch.protonmail.android.mailnotifications.domain.model.MessageReadPushData
import ch.protonmail.android.mailnotifications.domain.model.NewLoginPushData
import ch.protonmail.android.mailnotifications.domain.model.NewMessagePushData
import ch.protonmail.android.mailnotifications.domain.model.UserPushData
import ch.protonmail.android.mailnotifications.domain.usecase.ProcessMessageReadPushNotification
import ch.protonmail.android.mailnotifications.domain.usecase.ProcessNewLoginPushNotification
import ch.protonmail.android.mailnotifications.domain.usecase.ProcessNewMessagePushNotification
import ch.protonmail.android.mailnotifications.domain.usecase.content.DecryptNotificationContent
import ch.protonmail.android.mailsettings.domain.usecase.notifications.GetExtendedNotificationsSetting
import ch.protonmail.android.mailsettings.domain.usecase.privacy.ObserveBackgroundSyncSetting
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import me.proton.core.accountmanager.domain.SessionManager
import me.proton.core.domain.entity.UserId
import me.proton.core.network.domain.session.SessionId
import me.proton.core.user.domain.UserManager

@HiltWorker
internal class ProcessPushNotificationDataWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParameters: WorkerParameters,
    private val sessionManager: SessionManager,
    private val decryptNotificationContent: DecryptNotificationContent,
    private val appInBackgroundState: AppInBackgroundState,
    private val userManager: UserManager,
    private val getNotificationsExtendedPreference: GetExtendedNotificationsSetting,
    private val observeBackgroundSyncSetting: ObserveBackgroundSyncSetting,
    private val processNewMessagePushNotification: ProcessNewMessagePushNotification,
    private val processNewLoginPushNotification: ProcessNewLoginPushNotification,
    private val processMessageReadPushNotification: ProcessMessageReadPushNotification
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

        val hasBackgroundSyncEnabled = observeBackgroundSyncSetting().first().getOrNull()?.isEnabled ?: true

        return when {
            isNewMessageNotification(decryptedNotification.value) &&
                appInBackgroundState.isAppInBackground() &&
                hasBackgroundSyncEnabled -> {
                val notificationData = data.toLocalEmailNotificationData(context, userId.id, user.email ?: "")
                processNewMessagePushNotification(notificationData)
            }

            isMessageReadNotification(decryptedNotification.value) -> {
                val notificationData = data.toLocalMessageReadNotificationData(userId.id, user.email ?: "")
                processMessageReadPushNotification(notificationData)
            }

            isNewLoginNotification(decryptedNotification.value) -> {
                val notificationData = data.toLocalNewLoginNotificationData(context, userId.id, user.email ?: "")
                processNewLoginPushNotification(notificationData)
            }

            else -> Result.success()
        }
    }

    private suspend fun PushNotificationData.toLocalEmailNotificationData(
        context: Context,
        userId: String,
        userEmail: String
    ): LocalPushNotificationData.NewMessage {
        val userData = UserPushData(userId, userEmail)
        val sender = getSenderForNewMessage(context, sender)
        val pushData = NewMessagePushData(sender, messageId, body)

        return LocalPushNotificationData.NewMessage(userData, pushData)
    }

    private fun PushNotificationData.toLocalMessageReadNotificationData(
        userId: String,
        userEmail: String
    ): LocalPushNotificationData.MessageRead {
        val userData = UserPushData(userId, userEmail)
        val pushData = MessageReadPushData(messageId)
        return LocalPushNotificationData.MessageRead(userData, pushData)
    }

    private fun PushNotificationData.toLocalNewLoginNotificationData(
        context: Context,
        userId: String,
        userEmail: String
    ): LocalPushNotificationData.Login {
        val userData = UserPushData(userId, userEmail)
        val sender = sender?.senderName?.ifEmpty { sender.senderAddress }
            ?: context.getString(R.string.notification_title_text_new_login_alerts_fallback)
        val pushData = NewLoginPushData(sender, body, url)

        return LocalPushNotificationData.Login(userData, pushData)
    }

    private suspend fun getSenderForNewMessage(context: Context, sender: PushNotificationSender?): String {
        val hasNotificationsExtended = getNotificationsExtendedPreference().getOrNull()?.enabled ?: true

        if (hasNotificationsExtended) {
            return sender?.senderName?.ifEmpty { sender.senderAddress }
                ?: context.getString(R.string.notification_title_text_new_message_fallback)
        }

        return context.getString(R.string.notification_title_text_new_message_fallback)
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
