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

package me.proton.android.core.auth.presentation.signup.viewmodel

import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import me.proton.android.core.auth.data.entity.PasswordValidatorTokenWrapper
import me.proton.android.core.auth.presentation.signup.CreatePasswordAction
import me.proton.android.core.auth.presentation.signup.CreatePasswordAction.CreatePasswordClosed
import me.proton.android.core.auth.presentation.signup.CreatePasswordAction.LoadData
import me.proton.android.core.auth.presentation.signup.CreatePasswordAction.Perform
import me.proton.android.core.auth.presentation.signup.CreatePasswordState.Closed
import me.proton.android.core.auth.presentation.signup.CreatePasswordState.Creating
import me.proton.android.core.auth.presentation.signup.CreatePasswordState.Error
import me.proton.android.core.auth.presentation.signup.CreatePasswordState.Idle
import me.proton.android.core.auth.presentation.signup.CreatePasswordState.Success
import me.proton.android.core.auth.presentation.signup.CreatePasswordState.ValidationError.ConfirmPasswordMissMatch
import me.proton.android.core.auth.presentation.signup.CreatePasswordState.ValidationError.PasswordEmpty
import me.proton.android.core.auth.presentation.signup.CreatePasswordState.ValidationError.PasswordInvalid
import me.proton.android.core.auth.presentation.signup.SignUpState
import me.proton.android.core.auth.presentation.signup.mapToNavigationRoute
import me.proton.core.passvalidator.domain.entity.PasswordValidatorToken
import uniffi.mail_account_uniffi.PasswordValidatorServiceToken
import uniffi.mail_account_uniffi.SignupException
import uniffi.mail_account_uniffi.SignupFlow
import uniffi.mail_account_uniffi.SignupFlowSubmitPasswordResult

/**
 * Handler responsible for password-related actions during signup process.
 */
class PasswordHandler private constructor(
    private val getFlow: suspend () -> SignupFlow,
    private val getString: (resId: Int) -> String,
    private val getQuantityString: (resId: Int, quantity: Int, args: Int) -> String
) : ErrorHandler {

    fun handleAction(action: CreatePasswordAction) = when (action) {
        is LoadData -> flowOf(Idle)
        is Perform -> handlePasswordSubmit(
            password = action.password,
            confirmPassword = action.confirmPassword,
            token = action.token
        )

        is CreatePasswordClosed -> handleClose(action.back)
    }

    private fun handlePasswordSubmit(
        password: String,
        confirmPassword: String,
        token: PasswordValidatorToken?
    ) = flow {
        emit(Creating)

        when (val result = getFlow().submitPassword(password, confirmPassword, token?.toRust())) {
            is SignupFlowSubmitPasswordResult.Error -> {
                val state = when (result.v1) {
                    is SignupException.PasswordEmpty -> PasswordEmpty
                    is SignupException.PasswordsNotMatching -> ConfirmPasswordMissMatch
                    is SignupException.PasswordNotValidated -> PasswordInvalid
                    is SignupException.PasswordValidationMismatch -> PasswordInvalid
                    else -> Error(message = result.v1.getErrorMessage(getString, getQuantityString))
                }
                emit(state)
            }
            is SignupFlowSubmitPasswordResult.Ok -> {
                val route = result.v1.mapToNavigationRoute()
                emit(Success(route))
            }
        }
    }

    private fun handleClose(back: Boolean) = flow {
        if (back) {
            getFlow().stepBack()
        }
        emit(Closed)
    }

    override fun handleError(throwable: Throwable): SignUpState = Error(message = throwable.message)

    companion object {

        fun create(
            getFlow: suspend () -> SignupFlow,
            getString: (resId: Int) -> String,
            getQuantityString: (resId: Int, quantity: Int, args: Int) -> String
        ) = PasswordHandler(getFlow, getString, getQuantityString)
    }
}

private fun PasswordValidatorToken.toRust(): PasswordValidatorServiceToken? =
    (this as? PasswordValidatorTokenWrapper)?.toRust()
