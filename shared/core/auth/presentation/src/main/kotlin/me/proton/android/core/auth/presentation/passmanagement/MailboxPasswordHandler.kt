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
import me.proton.android.core.auth.presentation.passmanagement.PasswordManagementAction.UserInputAction.UpdateMailboxPassword
import me.proton.android.core.auth.presentation.passmanagement.PasswordManagementAction.UserInputAction.UpdateMailboxPassword.SaveMailboxPassword
import me.proton.android.core.auth.presentation.passmanagement.PasswordManagementAction.UserInputAction.UpdateMailboxPassword.TwoFaComplete
import me.proton.android.core.auth.presentation.passmanagement.PasswordManagementState.Awaiting2faForMailbox
import me.proton.android.core.auth.presentation.passmanagement.PasswordManagementState.Error
import me.proton.android.core.auth.presentation.passmanagement.PasswordManagementState.UserInput
import me.proton.core.passvalidator.domain.entity.PasswordValidatorToken
import uniffi.mail_account_uniffi.PasswordFlow
import uniffi.mail_account_uniffi.PasswordFlowChangeMboxPassResult
import uniffi.mail_account_uniffi.SimplePasswordState.COMPLETE
import uniffi.mail_account_uniffi.SimplePasswordState.INVALID
import uniffi.mail_account_uniffi.SimplePasswordState.WANT_CHANGE
import uniffi.mail_account_uniffi.SimplePasswordState.WANT_PASS
import uniffi.mail_account_uniffi.SimplePasswordState.WANT_TFA

class MailboxPasswordHandler private constructor(
    private val context: Context,
    private val getUserId: () -> CoreUserId,
    private val getFlow: suspend () -> FlowManager.CurrentFlow,
    private val uiEventFlow: UiEventFlow<PasswordManagementEvent>
) : ErrorHandler {

    fun handleAction(action: UpdateMailboxPassword, currentState: UserInput): Flow<PasswordManagementState> = flow {
        when (val currentFlow = getFlow()) {
            is FlowManager.CurrentFlow.ChangingPassword -> {
                handleChangingPasswordFlow(currentFlow.flow, action, currentState)
            }

            is FlowManager.CurrentFlow.LoggingIn -> Unit // No actions supported for LoggingIn flow and changing MBPass
        }
    }

    private suspend fun FlowCollector<PasswordManagementState>.handleChangingPasswordFlow(
        passwordFlow: PasswordFlow,
        action: UpdateMailboxPassword,
        currentState: UserInput
    ) {
        when (action) {
            is SaveMailboxPassword -> {
                val updatedState = currentState.updateMailboxPassword(
                    current = action.currentLoginPassword,
                    new = action.newPassword,
                    confirmNew = action.confirmPassword
                )
                emitAll(saveMailboxPassword(passwordFlow, updatedState, action.token))
            }

            is TwoFaComplete -> {
                emitAll(handleTwoFaResult(passwordFlow, action.result, currentState, action.token))
            }
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
            emitAll(submitMailboxPassChange(passwordFlow, currentState, token))
        } else {
            emit(currentState.copyWithMailboxPassword { it.copy(loading = false) })
            passwordFlow.stepBack()
        }
    }

    private fun saveMailboxPassword(
        passwordFlow: PasswordFlow,
        currentState: UserInput,
        token: PasswordValidatorToken?
    ): Flow<PasswordManagementState> = flow {
        emit(currentState.copyWithMailboxPassword { it.copy(loading = true) })

        // Early validation
        val validationError = validateMailboxPasswordInputs(currentState.mailboxPassword.current, token)
        if (validationError != null) {
            emit(currentState.setMailboxPasswordValidationError(validationError))
            return@flow
        }

        emitAll(submitMailboxPassChange(passwordFlow, currentState, token))
    }

    private fun validateMailboxPasswordInputs(
        currentPassword: String,
        token: PasswordValidatorToken?
    ): ValidationError? {
        return when {
            currentPassword.isBlank() -> ValidationError.CurrentPasswordEmpty
            token == null -> ValidationError.PasswordInvalid
            else -> null
        }
    }

    private fun submitMailboxPassChange(
        passwordFlow: PasswordFlow,
        currentState: UserInput,
        token: PasswordValidatorToken?
    ): Flow<PasswordManagementState> = flow {
        emit(currentState.copyWithMailboxPassword { it.copy(loading = true) })

        val mailboxPasswordState = currentState.mailboxPassword
        val changeResult = safeExecute(currentState) {
            passwordFlow.changeMboxPass(
                currentPassword = mailboxPasswordState.current,
                newMboxPass = mailboxPasswordState.new,
                confirmPassword = mailboxPasswordState.confirmNew,
                token = (token as? PasswordValidatorTokenWrapper)?.toRust()
            )
        } ?: return@flow

        handleMailboxPasswordChangeResult(passwordFlow, changeResult, currentState, token)
    }

    private suspend fun FlowCollector<PasswordManagementState>.handleMailboxPasswordChangeResult(
        passwordFlow: PasswordFlow,
        changeResult: PasswordFlowChangeMboxPassResult,
        currentState: UserInput,
        token: PasswordValidatorToken?
    ) {
        when (changeResult) {
            is PasswordFlowChangeMboxPassResult.Error -> {
                val validationError = changeResult.v1.mapToValidationError()
                if (validationError != null) {
                    emit(currentState.setMailboxPasswordValidationError(validationError))
                } else {
                    emit(Error.General(changeResult.v1.getErrorMessage(context), currentState))
                }

                goToInitState(passwordFlow)
            }

            is PasswordFlowChangeMboxPassResult.Ok -> {
                when (changeResult.v1) {
                    WANT_PASS -> Unit // Not used in Rust
                    WANT_TFA -> emit(Awaiting2faForMailbox(getUserId(), currentState, token))
                    WANT_CHANGE -> emitAll(submitMailboxPassChange(passwordFlow, currentState, token))
                    COMPLETE -> uiEventFlow.emit(PasswordManagementEvent.MailboxPasswordSaved)
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
            getFlow: suspend () -> FlowManager.CurrentFlow,
            getUserId: () -> CoreUserId,
            uiEventFlow: UiEventFlow<PasswordManagementEvent>
        ): MailboxPasswordHandler = MailboxPasswordHandler(context, getUserId, getFlow, uiEventFlow)
    }
}

private fun UserInput.copyWithMailboxPassword(transform: (MailboxPasswordState) -> MailboxPasswordState): UserInput =
    copy(mailboxPassword = transform(mailboxPassword))

private fun UserInput.updateMailboxPassword(
    current: String,
    new: String,
    confirmNew: String
): UserInput = copy(
    mailboxPassword = mailboxPassword.copy(
        current = current,
        new = new,
        confirmNew = confirmNew
    )
)

private fun UserInput.setMailboxPasswordValidationError(error: ValidationError): UserInput =
    copyWithMailboxPassword { it.copy(validationError = error, loading = false) }
