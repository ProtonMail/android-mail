/*
 * Copyright (C) 2024 Proton AG
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

package me.proton.android.core.auth.presentation.login

import android.content.Context
import me.proton.android.core.auth.presentation.LogTag
import me.proton.android.core.auth.presentation.R
import me.proton.core.util.kotlin.CoreLogger
import uniffi.mail_account_uniffi.LoginError
import uniffi.mail_account_uniffi.PostLoginValidationError
import uniffi.mail_uniffi.OtherErrorReason
import uniffi.mail_uniffi.OtherErrorReason.InvalidParameter
import uniffi.mail_uniffi.OtherErrorReason.Other
import uniffi.mail_uniffi.OtherErrorReason.TaskCancelled
import uniffi.mail_uniffi.ProtonError
import uniffi.mail_uniffi.ProtonError.Network
import uniffi.mail_uniffi.ProtonError.NonProcessableActions
import uniffi.mail_uniffi.ProtonError.OtherReason
import uniffi.mail_uniffi.ProtonError.ServerError
import uniffi.mail_uniffi.ProtonError.Unexpected
import uniffi.mail_uniffi.SessionReason
import uniffi.mail_uniffi.UnexpectedError
import uniffi.mail_uniffi.UserSessionError
import uniffi.mail_uniffi_common.UserApiServiceError
import uniffi.mail_uniffi_common.UserApiServiceError.BadGateway
import uniffi.mail_uniffi_common.UserApiServiceError.BadRequest
import uniffi.mail_uniffi_common.UserApiServiceError.Forbidden
import uniffi.mail_uniffi_common.UserApiServiceError.Internal
import uniffi.mail_uniffi_common.UserApiServiceError.InternalServerError
import uniffi.mail_uniffi_common.UserApiServiceError.NetworkFailure
import uniffi.mail_uniffi_common.UserApiServiceError.NotFound
import uniffi.mail_uniffi_common.UserApiServiceError.NotImplemented
import uniffi.mail_uniffi_common.UserApiServiceError.OtherHttpError
import uniffi.mail_uniffi_common.UserApiServiceError.ServiceUnavailable
import uniffi.mail_uniffi_common.UserApiServiceError.TooManyRequests
import uniffi.mail_uniffi_common.UserApiServiceError.Unauthorized
import uniffi.mail_uniffi_common.UserApiServiceError.UnprocessableEntity

fun LoginError.getErrorMessage(context: Context): String = when (this) {
    is LoginError.FlowLogin -> v1.getErrorMessage(context)
    is LoginError.FlowTotp -> v1.getErrorMessage(context)
    is LoginError.FlowFido -> v1.getErrorMessage(context)
    is LoginError.UserFetch -> v1.getErrorMessage(context)
    is LoginError.KeySecretSaltFetch -> v1.getErrorMessage(context)
    is LoginError.AuthStore -> v1
    is LoginError.Other -> v1
    is LoginError.InvalidState -> "LoginError.InvalidState"
    is LoginError.AddressFetch -> "LoginError.AddressFetch"
    is LoginError.AddressKeySetup -> v1
    is LoginError.AddressSetup -> context.getString(R.string.auth_login_error_invalid_action_cannot_setup_address)
    is LoginError.UserKeySetup -> context.getString(R.string.auth_login_error_invalid_action_cannot_setup_user)
    is LoginError.ApiError -> v1
    is LoginError.CantUnlockUserKey -> context.getString(R.string.auth_login_error_invalid_action_cannot_unlock_keys)
    is LoginError.InvalidCredentials -> context.getString(R.string.auth_login_error_invalid_action_invalid_credentials)
    is LoginError.QrLoginEncoding -> "LoginError.QrLoginEncoding"
    is LoginError.WithCodePollFlowFailed -> v1
    is LoginError.Incorrect2FaCode -> context.getString(R.string.auth_second_factor_incorrect_code)
    is LoginError.AddressKeySetupAborted -> "LoginError.AddressKeySetupAborted"
    is LoginError.NoAddress -> "LoginError.NoAddress"
    is LoginError.NoLogin -> "LoginError.NoLogin"
    is LoginError.UserKeySetupAborted -> "LoginError.UserKeySetupAborted"
    is LoginError.SettingsFetch -> "LoginError.SettingsFetch"
    is LoginError.DuplicateSession -> v1
    is LoginError.MissingSession -> context.getString(R.string.auth_login_error_invalid_action_cannot_unlock_keys)

    is LoginError.PostLoginValidationFailed -> when (val loginError = this.v1) {
        is PostLoginValidationError.FreeAccountLimitExceeded -> context.resources.getQuantityString(
            R.plurals.auth_user_check_max_free_error,
            loginError.v1.toInt(), loginError.v1.toInt()
        )
    }
}

fun UserApiServiceError.getErrorMessage(context: Context) = when (this) {
    is BadRequest -> v1
    is OtherHttpError -> v2
    is BadGateway -> v1
    is InternalServerError -> v1
    is NotFound -> v1
    is NotImplemented -> v1
    is ServiceUnavailable -> v1
    is Unauthorized -> v1
    is UnprocessableEntity -> v1
    is Internal -> v1
    is TooManyRequests -> v1
    is Forbidden -> v1
    is NetworkFailure -> context.getString(R.string.presentation_general_connection_error)
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
    val error = IllegalStateException("UnexpectedError: $it")
    CoreLogger.e(LogTag.LOGIN, error)
}

private fun OtherErrorReason.getErrorMessage(context: Context) = when (this) {
    is TaskCancelled -> context.getString(R.string.presentation_error_general)
    is InvalidParameter -> context.getString(R.string.auth_login_error_invalid_action_invalid_credentials)
    is Other -> this.v1
}

fun ProtonError.getErrorMessage(context: Context) = when (this) {
    is OtherReason -> v1.getErrorMessage(context)
    is ServerError -> v1.getErrorMessage(context)
    is Unexpected -> v1.getErrorMessage()
    is Network -> context.getString(R.string.presentation_general_connection_error)
    is NonProcessableActions -> context.getString(R.string.proton_error_non_processable_actions)
}

fun UserSessionError.getErrorMessage(context: Context) = when (this) {
    is UserSessionError.Other -> this.v1.getErrorMessage(context)
    is UserSessionError.Reason -> when (this.v1) {
        is SessionReason.DuplicateSession -> "DUPLICATE_SESSION"
        is SessionReason.MethodCalledInWrongOrigin -> "METHOD_CALLED_IN_WRONG_ORIGIN"
        is SessionReason.UnknownLabel -> "UNKNOWN_LABEL"
        is SessionReason.UserSessionNotInitialized -> "USER_SESSION_NOT_INITIALIZED"
    }
}
