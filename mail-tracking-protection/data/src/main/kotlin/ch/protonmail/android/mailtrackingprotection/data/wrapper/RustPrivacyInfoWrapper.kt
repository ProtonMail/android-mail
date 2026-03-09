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

package ch.protonmail.android.mailtrackingprotection.data.wrapper

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.data.mapper.LocalMessageId
import ch.protonmail.android.mailcommon.data.mapper.toDataError
import ch.protonmail.android.mailcommon.domain.model.DataError
import uniffi.mail_uniffi.MailUserSession
import uniffi.mail_uniffi.WatchPrivacyInfoStreamResult
import uniffi.mail_uniffi.watchPrivacyInfoStream

class RustPrivacyInfoWrapper(private val rustSession: MailUserSession) {

    suspend fun watchTrackerInfoStream(messageId: LocalMessageId): Either<DataError, PrivacyInfoStreamWrapper> {
        return when (val result = watchPrivacyInfoStream(rustSession, messageId)) {
            is WatchPrivacyInfoStreamResult.Error -> result.v1.toDataError().left()
            is WatchPrivacyInfoStreamResult.Ok -> PrivacyInfoStreamWrapper(result.v1).right()
        }
    }
}
