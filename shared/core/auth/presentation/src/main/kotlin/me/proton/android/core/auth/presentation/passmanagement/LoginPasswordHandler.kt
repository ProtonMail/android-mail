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
import ch.protonmail.android.design.compose.viewmodel.UiEventFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import me.proton.android.core.account.domain.model.CoreUserId
import me.proton.android.core.auth.data.entity.PasswordValidatorTokenWrapper
import me.proton.android.core.auth.presentation.flow.FlowManager
import me.proton.android.core.auth.presentation.passmanagement.PasswordManagementAction.UserInputAction.UpdateLoginPassword
import me.proton.android.core.auth.presentation.passmanagement.PasswordManagementAction.UserInputAction.UpdateLoginPassword.SaveLoginPassword
import me.proton.android.core.auth.presentation.passmanagement.PasswordManagementAction.UserInputAction.UpdateLoginPassword.TwoFaComplete
import me.proton.android.core.auth.presentation.passmanagement.PasswordManagementState.Awaiting2faForLogin
import me.proton.android.core.auth.presentation.passmanagement.PasswordManagementState.Error
import me.proton.android.core.auth.presentation.passmanagement.PasswordManagementState.UserInput
import me.proton.core.passvalidator.domain.entity.PasswordValidatorToken
import uniffi.mail_account_uniffi.LoginFlow
import uniffi.mail_account_uniffi.LoginFlowSubmitNewPasswordResult
import uniffi.mail_account_uniffi.PasswordFlow
import uniffi.mail_account_uniffi.PasswordFlowChangePassResult
import uniffi.mail_account_uniffi.SimplePasswordState.COMPLETE
import uniffi.mail_account_uniffi.SimplePasswordState.INVALID
import uniffi.mail_account_uniffi.SimplePasswordState.WANT_CHANGE
import uniffi.mail_account_uniffi.SimplePasswordState.WANT_PASS
import uniffi.mail_account_uniffi.SimplePasswordState.WANT_TFA

class LoginPasswordHandler private constructor(
    private val context: Context,
    private val getUserId: () -> CoreUserId,
    private val getFlow: suspend () -> FlowManager.CurrentFlow,
    private val uiEventFlow: UiEventFlow<PasswordManagementEvent>
) : ErrorHandler {

    fun handleAction(action: UpdateLoginPassword, currentState: UserInput): Flow<PasswordManagementState> = flow {
        when (val currentFlow = getFlow()) {
            is FlowManager.CurrentFlow.ChangingPassword -> {
                handleChangingPasswordFlow(currentFlow.flow, action, currentState)
            }

            is FlowManager.CurrentFlow.LoggingIn -> {
                handleLoggingInFlow(currentFlow.flow, action, currentState)
            }
        }
    }

    private suspend fun FlowCollector<PasswordManagementState>.handleChangingPasswordFlow(
        passwordFlow: PasswordFlow,
        action: UpdateLoginPassword,
        currentState: UserInput
    ) {
        when (action) {
            is SaveLoginPassword -> {
                val updatedState = currentState.updateLoginPassword(
                    current = action.currentPassword,
                    new = action.newPassword,
                    confirmNew = action.confirmPassword
                )
                emitAll(saveLoginPassword(passwordFlow, updatedState, action.token))
            }

            is TwoFaComplete -> {
                emitAll(handleTwoFaResult(passwordFlow, action.result, currentState, action.token))
            }
        }
    }

    private suspend fun FlowCollector<PasswordManagementState>.handleLoggingInFlow(
        loginFlow: LoginFlow,
        action: UpdateLoginPassword,
        currentState: UserInput
    ) {
        if (action !is SaveLoginPassword) return

        // Early validation
        if (action.token == null) {
            emit(currentState.setPasswordValidationError(ValidationError.PasswordEmpty))
            return
        }

        emit(currentState.copyWithLoginPassword { it.copy(loading = true) })

        val result = safeExecute(currentState) {
            loginFlow.submitNewPassword(action.newPassword)
        } ?: return

        when (result) {
            is LoginFlowSubmitNewPasswordResult.Error -> {
                emit(Error.General(result.v1.toString(), currentState))
            }

            is LoginFlowSubmitNewPasswordResult.Ok -> uiEventFlow.emit(PasswordManagementEvent.LoginPasswordSaved)
        }
    }

    private fun handleTwoFaResult(
        passwordFlow: PasswordFlow,
        result: Boolean,
        currentState: UserInput,
        token: PasswordValidatorToken?
    ): Flow<PasswordManagementState> = flow {
        if (passwordFlow.getState() == COMPLETE) {
            uiEventFlow.emit(PasswordManagementEvent.LoginPasswordSaved)
        } else if (result) {
            emitAll(submitPassChange(passwordFlow, currentState, token))
        } else {
            emit(currentState.copyWithLoginPassword { it.copy(loading = false) })
            passwordFlow.stepBack()
        }
    }

    private fun saveLoginPassword(
        passwordFlow: PasswordFlow,
        currentState: UserInput,
        token: PasswordValidatorToken?
    ): Flow<PasswordManagementState> = flow {
        emit(currentState.copyWithLoginPassword { it.copy(loading = true) })

        // Early validation
        val validationError = validatePasswordInputs(currentState.loginPassword.current, token)
        if (validationError != null) {
            emit(currentState.setPasswordValidationError(validationError))
            return@flow
        }

        emitAll(submitPassChange(passwordFlow, currentState, token))
    }

    private fun validatePasswordInputs(currentPassword: String, token: PasswordValidatorToken?): ValidationError? {
        return when {
            currentPassword.isBlank() -> ValidationError.CurrentPasswordEmpty
            token == null -> ValidationError.PasswordEmpty
            else -> null
        }
    }

    private fun submitPassChange(
        passwordFlow: PasswordFlow,
        currentState: UserInput,
        token: PasswordValidatorToken?
    ): Flow<PasswordManagementState> = flow {
        emit(currentState.copyWithLoginPassword { it.copy(loading = true) })

        val loginPasswordState = currentState.loginPassword
        val changeResult = safeExecute(currentState) {
            passwordFlow.changePass(
                currentPassword = loginPasswordState.current,
                newPass = loginPasswordState.new,
                confirmPassword = loginPasswordState.confirmNew,
                token = (token as? PasswordValidatorTokenWrapper)?.toRust()
            )
        } ?: return@flow

        handlePasswordChangeResult(passwordFlow, changeResult, currentState, token)
    }

    private suspend fun FlowCollector<PasswordManagementState>.handlePasswordChangeResult(
        passwordFlow: PasswordFlow,
        changeResult: PasswordFlowChangePassResult,
        currentState: UserInput,
        token: PasswordValidatorToken?
    ) {
        when (changeResult) {
            is PasswordFlowChangePassResult.Error -> {
                val validationError = changeResult.v1.mapToValidationError()
                if (validationError != null) {
                    emit(currentState.setPasswordValidationError(validationError))
                } else {
                    emit(Error.General(changeResult.v1.getErrorMessage(context), currentState))
                }
                goToInitState(passwordFlow)
            }

            is PasswordFlowChangePassResult.Ok -> {
                when (changeResult.v1) {
                    WANT_PASS -> Unit // Not used in Rust
                    WANT_TFA -> emit(Awaiting2faForLogin(getUserId(), currentState, token))
                    WANT_CHANGE -> emitAll(submitPassChange(passwordFlow, currentState, token))
                    COMPLETE -> uiEventFlow.emit(PasswordManagementEvent.LoginPasswordSaved)
                    INVALID -> emit(Error.InvalidState(currentState))
                }
            }
        }
    }

    private suspend fun goToInitState(passwordFlow: PasswordFlow) {
        while (passwordFlow.getState() != WANT_PASS) {
            passwordFlow.stepBack()
        }
    }

    private suspend inline fun <T> FlowCollector<PasswordManagementState>.safeExecute(
        currentState: UserInput,
        operation: () -> T
    ): T? {
        return runCatching { operation() }.getOrElse { exception ->
            emit(Error.General(exception.message, currentState))
            null
        }
    }

    override fun handleError(throwable: Throwable, currentState: UserInput): PasswordManagementState =
        Error.General(error = throwable.message, currentState)

    companion object {

        fun create(
            context: Context,
            getUserId: () -> CoreUserId,
            getPasswordFlow: suspend () -> FlowManager.CurrentFlow,
            uiEventFlow: UiEventFlow<PasswordManagementEvent>
        ): LoginPasswordHandler = LoginPasswordHandler(context, getUserId, getPasswordFlow, uiEventFlow)
    }
}

private fun UserInput.copyWithLoginPassword(transform: (LoginPasswordState) -> LoginPasswordState): UserInput =
    copy(loginPassword = transform(loginPassword))

private fun UserInput.updateLoginPassword(
    current: String,
    new: String,
    confirmNew: String
): UserInput = copy(
    loginPassword = loginPassword.copy(
        current = current,
        new = new,
        confirmNew = confirmNew
    )
)

private fun UserInput.setPasswordValidationError(error: ValidationError): UserInput =
    copyWithLoginPassword { it.copy(validationError = error, loading = false) }
