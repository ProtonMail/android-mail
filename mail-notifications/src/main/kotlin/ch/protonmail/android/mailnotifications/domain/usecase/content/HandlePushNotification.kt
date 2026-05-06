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
import ch.protonmail.android.mailfeatureflags.domain.annotation.IsPushProcessingWithoutWorkerEnabled
import ch.protonmail.android.mailfeatureflags.domain.model.FeatureFlag
import ch.protonmail.android.mailnotifications.data.local.ProcessPushNotificationDataWorker
import ch.protonmail.android.mailnotifications.domain.usecase.ProcessPushNotification
import ch.protonmail.android.mailsession.data.mapper.toUserId
import ch.protonmail.android.mailsession.data.repository.MailSessionRepository
import ch.protonmail.android.mailsession.data.repository.runInRustBackground
import ch.protonmail.android.mailsession.data.wrapper.MailSessionWrapper
import me.proton.core.domain.entity.UserId
import me.proton.core.network.domain.session.SessionId
import timber.log.Timber
import javax.inject.Inject

internal class HandlePushNotification @Inject constructor(
    private val enqueuer: Enqueuer,
    private val processPushNotification: ProcessPushNotification,
    private val mailSessionRepository: MailSessionRepository,
    @IsPushProcessingWithoutWorkerEnabled private val processingWithoutWorkerEnabled: FeatureFlag<Boolean>
) {

    suspend operator fun invoke(sessionId: SessionId, encryptedMessage: String) {
        mailSessionRepository.runInRustBackground { mailSession ->
            val userId = resolveUserId(mailSession, sessionId) ?: run {
                Timber.w(
                    "Notification: Push received but userId could not be resolved for sessionId=%s",
                    sessionId.id
                )
                return@runInRustBackground
            }

            Timber.d("Notification: Resolved userId for the given session")

            if (processingWithoutWorkerEnabled.get()) {
                processPushNotification(
                    userId,
                    sessionId,
                    encryptedMessage
                )
            } else {
                enqueuer.enqueue<ProcessPushNotificationDataWorker>(
                    userId,
                    ProcessPushNotificationDataWorker.params(userId.id, sessionId.id, encryptedMessage)
                )
            }
        }
    }

    private suspend fun resolveUserId(mailSession: MailSessionWrapper, sessionId: SessionId): UserId? =
        mailSession.getSessionById(sessionId).getOrNull()
            ?.userId()
            ?.toUserId()
}
