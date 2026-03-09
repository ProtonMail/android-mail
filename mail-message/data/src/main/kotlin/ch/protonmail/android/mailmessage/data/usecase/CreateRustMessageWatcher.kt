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
import arrow.core.right
import ch.protonmail.android.mailcommon.data.mapper.LocalMessageId
import ch.protonmail.android.mailcommon.data.mapper.toDataError
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailsession.domain.wrapper.MailUserSessionWrapper
import uniffi.mail_uniffi.LiveQueryCallback
import uniffi.mail_uniffi.WatchMessageResult
import uniffi.mail_uniffi.WatchedMessage
import uniffi.mail_uniffi.watchMessage
import javax.inject.Inject

class CreateRustMessageWatcher @Inject constructor() {

    suspend operator fun invoke(
        session: MailUserSessionWrapper,
        messageId: LocalMessageId,
        callback: LiveQueryCallback
    ): Either<DataError, WatchedMessage> =
        when (val result = watchMessage(session.getRustUserSession(), messageId, callback)) {
            is WatchMessageResult.Error -> result.v1.toDataError().left()
            is WatchMessageResult.Ok -> {
                when (val watcher = result.v1) {
                    null -> DataError.Local.NotFound.left()
                    else -> watcher.right()
                }
            }
        }
}
