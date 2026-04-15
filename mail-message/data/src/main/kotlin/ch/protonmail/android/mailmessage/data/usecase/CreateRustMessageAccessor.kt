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

package ch.protonmail.android.mailmessage.data.usecase

import arrow.core.Either
import arrow.core.left
import arrow.core.raise.either
import arrow.core.right
import ch.protonmail.android.mailcommon.data.mapper.LocalMessageId
import ch.protonmail.android.mailcommon.data.mapper.LocalMessageMetadata
import ch.protonmail.android.mailcommon.data.mapper.RemoteMessageId
import ch.protonmail.android.mailcommon.data.mapper.toDataError
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailsession.domain.wrapper.MailUserSessionWrapper
import uniffi.mail_uniffi.MessageResult
import uniffi.mail_uniffi.ResolveMessageFromPushNotificationResult
import uniffi.mail_uniffi.message
import uniffi.mail_uniffi.resolveMessageFromPushNotification
import javax.inject.Inject

class CreateRustMessageAccessor @Inject constructor() {

    suspend operator fun invoke(
        session: MailUserSessionWrapper,
        messageId: LocalMessageId
    ): Either<DataError, LocalMessageMetadata> = when (val result = message(session.getRustUserSession(), messageId)) {
        is MessageResult.Error -> result.v1.toDataError().left()
        is MessageResult.Ok -> when (val message = result.v1) {
            null -> DataError.Local.NotFound.left()
            else -> message.right()
        }
    }

    suspend operator fun invoke(
        session: MailUserSessionWrapper,
        remoteMessageId: RemoteMessageId
    ): Either<DataError, LocalMessageMetadata> = either {
        when (
            val result = resolveMessageFromPushNotification(
                session = session.getRustUserSession(),
                remoteId = remoteMessageId
            )
        ) {
            is ResolveMessageFromPushNotificationResult.Error -> raise(result.v1.toDataError())
            is ResolveMessageFromPushNotificationResult.Ok -> result.v1
        }
    }
}
