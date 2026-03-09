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

package ch.protonmail.android.mailsession.data.wrapper

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailsession.domain.model.LoginError
import uniffi.mail_account_uniffi.LoginFlow
import uniffi.mail_account_uniffi.LoginFlowMigrateResult
import uniffi.mail_account_uniffi.MigrationData
import uniffi.mail_account_uniffi.LoginError as UniffiLoginError

class LoginFlowWrapper(private val loginFlow: LoginFlow) {

    suspend fun migrate(data: MigrationData): Either<LoginError, Unit> = when (val result = loginFlow.migrate(data)) {
        is LoginFlowMigrateResult.Error -> result.v1.toLoginError().left()
        is LoginFlowMigrateResult.Ok -> Unit.right()
    }
}

internal fun UniffiLoginError.toLoginError(): LoginError = when (this) {
    // 1. API-related failures
    is uniffi.mail_account_uniffi.LoginError.AddressFetch,
    is uniffi.mail_account_uniffi.LoginError.ApiError,
    is uniffi.mail_account_uniffi.LoginError.FlowLogin,
    is uniffi.mail_account_uniffi.LoginError.FlowTotp,
    is uniffi.mail_account_uniffi.LoginError.FlowFido,
    is uniffi.mail_account_uniffi.LoginError.UserFetch,
    is uniffi.mail_account_uniffi.LoginError.KeySecretSaltFetch,
    is uniffi.mail_account_uniffi.LoginError.WithCodePollFlowFailed ->
        LoginError.ApiFailure

    // 2. Internal data/state issues
    is uniffi.mail_account_uniffi.LoginError.AddressKeySetup,
    is uniffi.mail_account_uniffi.LoginError.AddressSetup,
    is uniffi.mail_account_uniffi.LoginError.AuthStore,
    is uniffi.mail_account_uniffi.LoginError.CantUnlockUserKey,
    is uniffi.mail_account_uniffi.LoginError.InvalidState,
    is uniffi.mail_account_uniffi.LoginError.UserKeySetup,
    is uniffi.mail_account_uniffi.LoginError.Other,
    is uniffi.mail_account_uniffi.LoginError.QrLoginEncoding ->
        LoginError.InternalError

    // 3. Login-specific validation failures
    is uniffi.mail_account_uniffi.LoginError.InvalidCredentials ->
        LoginError.AuthenticationFailure

    // 4. Fallback
    else -> LoginError.Unknown
}
