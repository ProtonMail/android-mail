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

package me.proton.android.core.auth.presentation.passmanagement

import android.content.Context
import me.proton.android.core.auth.presentation.R
import uniffi.mail_account_uniffi.PasswordException

fun PasswordException.getErrorMessage(context: Context): String = when (this) {
    is PasswordException.Api,
    is PasswordException.Crypto,
    is PasswordException.InvalidState,
    is PasswordException.KeyUnlock,
    is PasswordException.Internal -> context.getString(R.string.settings_password_general_error)

    is PasswordException.Invalid2FaCode -> context.getString(R.string.settings_password_invalid_2fa)
    is PasswordException.InvalidCredentials ->
        context.getString(R.string.auth_login_error_invalid_action_invalid_credentials)

    is PasswordException.PasswordEmpty -> context.getString(R.string.settings_password_error_empty)
    is PasswordException.PasswordNotValidated,
    is PasswordException.PasswordValidationMismatch,
    is PasswordException.PasswordsNotMatching -> context.getString(R.string.settings_password_error)
    is PasswordException.InvalidRecoveryCode -> context.getString(R.string.settings_password_error_invalid_recovery)
    is PasswordException.Reused2FaCode -> context.getString(R.string.settings_password_error_2fa_reused)
}

fun PasswordException.mapToValidationError(): ValidationError? = when (this) {
    is PasswordException.PasswordEmpty -> ValidationError.PasswordEmpty
    is PasswordException.PasswordsNotMatching -> ValidationError.ConfirmPasswordMissMatch
    is PasswordException.PasswordNotValidated,
    is PasswordException.PasswordValidationMismatch -> ValidationError.PasswordInvalid

    else -> null
}
