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
import ch.protonmail.android.mailnotifications.domain.usecase.ProcessPushNotification
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import me.proton.core.domain.entity.UserId
import me.proton.core.network.domain.session.SessionId
import timber.log.Timber

@HiltWorker
internal class ProcessPushNotificationDataWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParameters: WorkerParameters,
    private val processPushNotification: ProcessPushNotification
) : CoroutineWorker(context, workerParameters) {

    override suspend fun doWork(): Result {
        val userId = inputData.getString(KeyPushNotificationUserId)
        val sessionId = inputData.getString(KeyPushNotificationUid)
        val encryptedNotification = inputData.getString(KeyPushNotificationEncryptedMessage)

        Timber.d(
            "Notification: Worker - Process push notification for userId=%s, sessionId=%s",
            userId,
            sessionId
        )
        return if (userId.isNullOrEmpty() || sessionId.isNullOrEmpty() || encryptedNotification.isNullOrEmpty()) {
            Result.failure(workDataOf(KeyPushNotificationDataError to "Input data is missing"))
        } else {
            processPushNotification(
                UserId(id = userId), SessionId(id = sessionId), encryptedNotification
            )
        }
    }

    companion object {

        const val KeyPushNotificationUserId = "userId"
        const val KeyPushNotificationUid = "UID"
        const val KeyPushNotificationEncryptedMessage = "encryptedMessage"
        const val KeyPushNotificationDataError = "PushNotificationDataError"

        fun params(
            userId: String,
            uid: String,
            encryptedNotification: String
        ) = mapOf(
            KeyPushNotificationUserId to userId,
            KeyPushNotificationUid to uid,
            KeyPushNotificationEncryptedMessage to encryptedNotification
        )
    }
}
