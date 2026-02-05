/*
 * Copyright (C) 2025 Proton AG
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package me.proton.android.core.payment.data.extension

import android.content.Context
import me.proton.android.core.payment.presentation.R
import uniffi.proton_mail_uniffi.OtherErrorReason
import uniffi.proton_mail_uniffi.OtherErrorReason.InvalidParameter
import uniffi.proton_mail_uniffi.OtherErrorReason.Other
import uniffi.proton_mail_uniffi.OtherErrorReason.TaskCancelled
import uniffi.proton_mail_uniffi.ProtonError
import uniffi.proton_mail_uniffi.ProtonError.Network
import uniffi.proton_mail_uniffi.ProtonError.NonProcessableActions
import uniffi.proton_mail_uniffi.ProtonError.OtherReason
import uniffi.proton_mail_uniffi.ProtonError.ServerError
import uniffi.proton_mail_uniffi.ProtonError.Unexpected
import uniffi.proton_mail_uniffi.SessionReason
import uniffi.proton_mail_uniffi.UnexpectedError
import uniffi.proton_mail_uniffi.UserSessionError
import uniffi.uniffi_common.UserApiServiceError
import uniffi.uniffi_common.UserApiServiceError.BadGateway
import uniffi.uniffi_common.UserApiServiceError.Forbidden
import uniffi.uniffi_common.UserApiServiceError.Internal
import uniffi.uniffi_common.UserApiServiceError.InternalServerError
import uniffi.uniffi_common.UserApiServiceError.NetworkFailure
import uniffi.uniffi_common.UserApiServiceError.NotFound
import uniffi.uniffi_common.UserApiServiceError.NotImplemented
import uniffi.uniffi_common.UserApiServiceError.OtherHttpError
import uniffi.uniffi_common.UserApiServiceError.ServiceUnavailable
import uniffi.uniffi_common.UserApiServiceError.TooManyRequests
import uniffi.uniffi_common.UserApiServiceError.Unauthorized
import uniffi.uniffi_common.UserApiServiceError.UnprocessableEntity

fun UserSessionError.getErrorMessage(context: Context) = when (this) {
    is UserSessionError.Other -> this.v1.getErrorMessage(context)
    is UserSessionError.Reason -> this.v1.getErrorMessage()
}

fun UserSessionError.isForbiddenError(): Boolean {
    val serverError = (this as? UserSessionError.Other)?.v1 as? ServerError
    val forbiddenError = serverError?.v1 as? Forbidden
    return forbiddenError != null
}

fun SessionReason.getErrorMessage() = when (this) {
    is SessionReason.DuplicateSession -> "DUPLICATE_SESSION"
    is SessionReason.MethodCalledInWrongOrigin -> "METHOD_CALLED_IN_WRONG_ORIGIN"
    is SessionReason.UnknownLabel -> "UNKNOWN_LABEL"
    is SessionReason.UserSessionNotInitialized -> "USER_SESSION_NOT_INITIALIZED "
}

fun ProtonError.getErrorMessage(context: Context) = when (this) {
    is OtherReason -> v1.getErrorMessage()
    is ServerError -> v1.getErrorMessage()
    is Unexpected -> v1.getErrorMessage()
    is Network -> context.getString(R.string.presentation_general_connection_error)
    is NonProcessableActions -> context.getString(R.string.proton_error_non_processable_actions)
}

fun UserApiServiceError.getErrorMessage() = when (this) {
    is UserApiServiceError.BadRequest -> v1
    is OtherHttpError -> v2
    is BadGateway -> v1
    is InternalServerError -> v1
    is NotFound -> v1
    is NotImplemented -> v1
    is ServiceUnavailable -> v1
    is Unauthorized -> v1
    is UnprocessableEntity -> v1
    is Internal -> v1
    is NetworkFailure -> v1
    is TooManyRequests -> v1
    is Forbidden -> v1
}

fun UnexpectedError.getErrorMessage() = when (this) {
    UnexpectedError.CRYPTO -> "CRYPTO"
    UnexpectedError.DATABASE -> "DATABASE"
    UnexpectedError.FILE_SYSTEM -> "FILE_SYSTEM"
    UnexpectedError.INTERNAL -> "INTERNAL"
    UnexpectedError.INVALID_ARGUMENT -> "INVALID_ARGUMENT"
    UnexpectedError.MEMORY -> "MEMORY"
    UnexpectedError.NETWORK -> "NETWORK"
    UnexpectedError.OS -> "OS"
    UnexpectedError.QUEUE -> "QUEUE"
    UnexpectedError.UNKNOWN -> "UNKNOWN"
    UnexpectedError.API -> "API"
    UnexpectedError.DRAFT -> "DRAFT"
    UnexpectedError.ERROR_MAPPING -> "ERROR_MAPPING"
    UnexpectedError.CONFIG -> "CONFIG"
}.also {
    throw IllegalStateException("UnexpectedError: $it")
}

fun OtherErrorReason.getErrorMessage() = when (this) {
    is TaskCancelled -> "TaskCancelled"
    is InvalidParameter -> "InvalidParameter"
    is Other -> this.v1
}
