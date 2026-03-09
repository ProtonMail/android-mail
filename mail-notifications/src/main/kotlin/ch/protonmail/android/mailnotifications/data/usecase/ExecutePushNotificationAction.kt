/*
 * Copyright (c) 2025 Proton Technologies AG
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

package ch.protonmail.android.mailnotifications.data.usecase

import arrow.core.raise.either
import ch.protonmail.android.mailcommon.data.mapper.toDataError
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailnotifications.data.model.QuickActionPayloadData
import ch.protonmail.android.mailnotifications.domain.model.LocalNotificationAction
import ch.protonmail.android.mailsession.data.mapper.toLocalUserId
import ch.protonmail.android.mailsession.data.repository.MailSessionRepository
import me.proton.core.domain.entity.UserId
import timber.log.Timber
import uniffi.mail_uniffi.MailSession
import uniffi.mail_uniffi.MailSessionGetSessionsResult
import uniffi.mail_uniffi.PushNotificationQuickAction
import uniffi.mail_uniffi.RemoteId
import uniffi.mail_uniffi.StoredSession
import uniffi.mail_uniffi.VoidActionResult
import javax.inject.Inject

internal class ExecutePushNotificationAction @Inject constructor(
    private val mailSessionRepository: MailSessionRepository
) {

    suspend operator fun invoke(payload: QuickActionPayloadData) = either {
        val mailSession = mailSessionRepository.getMailSession().getRustMailSession()

        val remoteId = RemoteId(payload.remoteId)

        val action = when (payload.action) {
            LocalNotificationAction.MarkAsRead -> PushNotificationQuickAction.MarkAsRead(remoteId)
            LocalNotificationAction.MoveTo.Archive -> PushNotificationQuickAction.MoveToArchive(remoteId)
            LocalNotificationAction.MoveTo.Trash -> PushNotificationQuickAction.MoveToTrash(remoteId)
        }

        val session = getStoredSessionForUser(mailSession, payload.userId)
            ?: raise(QuickActionPushError.NoMailSession)

        when (val result = mailSession.executeNotificationQuickAction(session, action, timeLeftMs = null)) {
            is VoidActionResult.Error -> raise(QuickActionPushError.Error(result.v1.toDataError()))
            VoidActionResult.Ok -> Unit
        }
    }

    private suspend fun getStoredSessionForUser(mailSession: MailSession, userId: UserId): StoredSession? =
        when (val result = mailSession.getSessions()) {
            is MailSessionGetSessionsResult.Error -> {
                Timber.e("execute-push-action failed due to no user session. Error: $result")
                null
            }
            is MailSessionGetSessionsResult.Ok -> result.v1.firstOrNull { it.userId() == userId.toLocalUserId() }
        }
}

sealed interface QuickActionPushError {
    data object NoMailSession : QuickActionPushError
    data class Error(val reason: DataError)
}
