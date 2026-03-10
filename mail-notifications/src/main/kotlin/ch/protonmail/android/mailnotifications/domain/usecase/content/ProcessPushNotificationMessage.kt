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

package ch.protonmail.android.mailnotifications.domain.usecase.content

import ch.protonmail.android.mailcommon.data.worker.Enqueuer
import ch.protonmail.android.mailnotifications.data.local.ProcessPushNotificationDataWorker
import ch.protonmail.android.mailsession.domain.repository.UserSessionRepository
import me.proton.core.network.domain.session.SessionId
import timber.log.Timber
import javax.inject.Inject

internal class ProcessPushNotificationMessage @Inject constructor(
    private val enqueuer: Enqueuer,
    private val userSessionRepository: UserSessionRepository
) {

    suspend operator fun invoke(sessionId: SessionId, encryptedMessage: String) {
        val userId = userSessionRepository.getUserId(sessionId) ?: run {
            Timber.w(
                "Notification: Push received but userId could not be resolved for sessionId=%s",
                sessionId.id
            )
            return
        }

        enqueuer.enqueue<ProcessPushNotificationDataWorker>(
            userId,
            ProcessPushNotificationDataWorker.params(userId.id, sessionId.id, encryptedMessage)
        )
    }
}
