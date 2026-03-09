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

package ch.protonmail.android.mailnotifications.data.usecase

import arrow.core.Either
import arrow.core.raise.either
import ch.protonmail.android.mailnotifications.data.model.DecryptionError
import ch.protonmail.android.mailnotifications.domain.mapper.PushNotificationMapper
import ch.protonmail.android.mailnotifications.domain.model.LocalPushNotification
import ch.protonmail.android.mailnotifications.domain.model.LocalPushNotificationData
import ch.protonmail.android.mailsession.domain.repository.UserSessionRepository
import me.proton.core.domain.entity.UserId
import me.proton.core.network.domain.session.SessionId
import uniffi.mail_uniffi.EncryptedPushNotification
import uniffi.mail_uniffi.MailUserSessionUserResult
import javax.inject.Inject

internal class DecryptPushNotificationContent @Inject constructor(
    private val userSessionRepository: UserSessionRepository,
    private val decryptPushNotification: DecryptPushNotification
) {

    suspend operator fun invoke(
        userId: UserId,
        sessionId: SessionId,
        encryptedContent: String
    ): Either<DecryptionError, LocalPushNotification> = either {

        val encryptedNotification = EncryptedPushNotification(sessionId.id, encryptedContent)

        val userSession = userSessionRepository.getUserSession(userId = userId) ?: raise(DecryptionError.UnknownUser)
        val userEmail = when (val user = userSession.getRustUserSession().user()) {
            is MailUserSessionUserResult.Error -> raise(DecryptionError.FailedToDetermineUserEmail)
            is MailUserSessionUserResult.Ok -> user.v1.email
        }

        val userData = LocalPushNotificationData.UserPushData(userId, userEmail)

        return tryDecrypt(userData, encryptedNotification)
    }

    private suspend fun tryDecrypt(
        userPushData: LocalPushNotificationData.UserPushData,
        encryptedPushNotification: EncryptedPushNotification
    ) = either {
        val decryptedData = decryptPushNotification(encryptedPushNotification).bind()

        PushNotificationMapper.toLocalPushNotification(userPushData, decryptedData)
    }
}
