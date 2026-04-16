/*
 * Copyright (c) 2026 Proton Technologies AG
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

import androidx.work.ListenableWorker
import androidx.work.workDataOf
import arrow.core.getOrElse
import ch.protonmail.android.mailnotifications.data.usecase.DecryptPushNotificationContent
import ch.protonmail.android.mailnotifications.domain.model.LocalPushNotification
import me.proton.core.domain.entity.UserId
import me.proton.core.network.domain.session.SessionId
import timber.log.Timber
import javax.inject.Inject

internal class ProcessPushNotification @Inject constructor(
    private val decryptPushNotificationContent: DecryptPushNotificationContent,
    private val processNewMessagePushNotification: ProcessNewMessagePushNotification,
    private val processNewLoginPushNotification: ProcessNewLoginPushNotification,
    private val processMessageReadPushNotification: ProcessMessageReadPushNotification
) {

    suspend operator fun invoke(
        userId: UserId,
        sessionId: SessionId,
        encryptedPayload: String
    ): ListenableWorker.Result {

        val decryptedNotification =
            decryptPushNotificationContent(userId, sessionId, encryptedPayload)
                .getOrElse {
                    Timber.w(
                        "Notification: Failed to decrypt push for userId=%s, sessionId=%s error=%s",
                        userId,
                        sessionId,
                        it.message
                    )
                    return ListenableWorker.Result.failure(
                        workDataOf(KeyProcessPushNotificationDataError to it.message)
                    )
                }

        Timber.d(
            "Notification: Process push notification of type=%s for userId=%s, sessionId=%s",
            decryptedNotification.typeName(),
            userId,
            sessionId
        )

        return when (decryptedNotification) {
            is LocalPushNotification.Message.NewMessage ->
                processNewMessagePushNotification(decryptedNotification)

            is LocalPushNotification.Message.MessageRead ->
                processMessageReadPushNotification(decryptedNotification)

            is LocalPushNotification.Login ->
                processNewLoginPushNotification(decryptedNotification)

            is LocalPushNotification.Message.UnsupportedMessageAction -> {
                val error = IllegalStateException(
                    "Unsupported push action - ${decryptedNotification.actionType?.action}"
                )
                Timber.w("Notification: %s", error.message)
                ListenableWorker.Result.failure(
                    workDataOf(KeyProcessPushNotificationDataError to error.message)
                )
            }
        }
    }

    companion object {
        const val KeyProcessPushNotificationDataError = "ProcessPushNotificationDataError"
    }
}

internal fun LocalPushNotification.typeName(): String = when (this) {
    is LocalPushNotification.Message.MessageRead -> "MessageRead"
    is LocalPushNotification.Message.NewMessage -> "NewMessage"
    is LocalPushNotification.Message.UnsupportedMessageAction ->
        "UnsupportedMessageAction"

    is LocalPushNotification.Login -> "Login"
}
