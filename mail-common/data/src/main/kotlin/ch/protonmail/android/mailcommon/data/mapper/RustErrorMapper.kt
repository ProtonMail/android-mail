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

package ch.protonmail.android.mailcommon.data.mapper

import ch.protonmail.android.mailcommon.domain.model.DataError
import uniffi.proton_mail_uniffi.ActionError
import uniffi.proton_mail_uniffi.ActionErrorReason
import uniffi.proton_mail_uniffi.OtherErrorReason
import uniffi.proton_mail_uniffi.ProtonError
import uniffi.proton_mail_uniffi.SessionReason
import uniffi.proton_mail_uniffi.UserSessionError

fun UserSessionError.toDataError(): DataError = when (this) {
    is UserSessionError.Other -> this.v1.toDataError()
    is UserSessionError.Reason -> when (val error = this.v1) {
        is SessionReason.MethodCalledInWrongOrigin -> DataError.Local.Other(
            "MethodCalledInWrongOrigin: expected: ${error.expected}, actual: ${error.actual}"
        )

        is SessionReason.UnknownLabel -> DataError.Local.NotFound
        is SessionReason.DuplicateSession,
        is SessionReason.UserSessionNotInitialized -> DataError.Local.NoUserSession
    }
}

fun ActionError.toDataError(): DataError = when (this) {
    is ActionError.Other -> this.v1.toDataError()
    is ActionError.Reason -> when (v1) {
        ActionErrorReason.UNKNOWN_LABEL,
        ActionErrorReason.UNKNOWN_MESSAGE,
        ActionErrorReason.UNKNOWN_CONTENT_ID -> DataError.Local.NotFound
    }
}

fun ProtonError.toDataError(): DataError = when (this) {
    is ProtonError.Network -> DataError.Remote.NoNetwork
    is ProtonError.OtherReason -> this.toDataError()
    is ProtonError.ServerError -> DataError.Remote.ServerError
    is ProtonError.Unexpected -> DataError.Local.Unknown
    is ProtonError.NonProcessableActions -> DataError.Local.UnsupportedOperation
}

fun ProtonError.OtherReason.toDataError() = when (val error = this.v1) {
    OtherErrorReason.InvalidParameter -> DataError.Local.InvalidRequest
    OtherErrorReason.TaskCancelled -> DataError.Local.TaskCancelled
    is OtherErrorReason.Other -> DataError.Local.Other(error.v1)
}
