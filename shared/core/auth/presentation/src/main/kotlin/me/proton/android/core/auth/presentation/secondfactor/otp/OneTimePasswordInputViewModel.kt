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

package me.proton.android.core.auth.presentation.secondfactor.otp

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import me.proton.android.core.account.domain.model.CoreUserId
import me.proton.android.core.auth.presentation.R
import me.proton.android.core.auth.presentation.flow.FlowManager
import me.proton.android.core.auth.presentation.flow.FlowManager.CurrentFlow
import me.proton.android.core.auth.presentation.login.getErrorMessage
import me.proton.android.core.auth.presentation.passmanagement.getErrorMessage
import me.proton.android.core.auth.presentation.secondfactor.SecondFactorArg.getUserId
import me.proton.android.core.auth.presentation.secondfactor.otp.OneTimePasswordInputAction.Authenticate
import me.proton.android.core.auth.presentation.secondfactor.otp.OneTimePasswordInputAction.Load
import me.proton.android.core.auth.presentation.secondfactor.otp.OneTimePasswordInputState.Awaiting2Pass
import me.proton.android.core.auth.presentation.secondfactor.otp.OneTimePasswordInputState.Closed
import me.proton.android.core.auth.presentation.secondfactor.otp.OneTimePasswordInputState.Error
import me.proton.android.core.auth.presentation.secondfactor.otp.OneTimePasswordInputState.Idle
import me.proton.android.core.auth.presentation.secondfactor.otp.OneTimePasswordInputState.Loading
import me.proton.android.core.auth.presentation.secondfactor.otp.OneTimePasswordInputState.LoggedIn
import me.proton.core.compose.viewmodel.BaseViewModel
import uniffi.mail_account_uniffi.LoginError
import uniffi.mail_account_uniffi.LoginFlow
import uniffi.mail_account_uniffi.LoginFlowSubmitTotpResult
import uniffi.mail_account_uniffi.PasswordFlowSubmitTotpResult
import uniffi.mail_uniffi.MailSession
import uniffi.mail_uniffi.MailSessionToUserSessionResult
import javax.inject.Inject

@HiltViewModel
class OneTimePasswordInputViewModel @Inject constructor(
    @ApplicationContext
    private val context: Context,
    private val savedStateHandle: SavedStateHandle,
    private val sessionInterface: MailSession,
    private val flowManager: FlowManager
) : BaseViewModel<OneTimePasswordInputAction, OneTimePasswordInputState>(
    initialState = Idle,
    initialAction = Load()
) {

    private val userId by lazy { CoreUserId(savedStateHandle.getUserId()) }

    override suspend fun FlowCollector<OneTimePasswordInputState>.onError(throwable: Throwable) {
        emit(Error.LoginFlow(throwable.message))
    }

    override fun onAction(action: OneTimePasswordInputAction): Flow<OneTimePasswordInputState> {
        return when (action) {
            is Load -> onLoad()
            is Authenticate -> onValidateAndAuthenticate(action)
        }
    }

    private fun onLoad(): Flow<OneTimePasswordInputState> = flow {
        emit(Idle)
    }

    private fun onValidateAndAuthenticate(action: Authenticate) = flow {
        emit(Loading)
        when (action.code.isBlank()) {
            true -> emit(Error.Validation)
            false -> emitAll(onAuthenticate(action))
        }
    }

    private fun onAuthenticate(action: Authenticate): Flow<OneTimePasswordInputState> = flow {
        emit(Loading)
        when (val twoFaFlow = flowManager.getCurrentActiveFlow(userId)) {
            is CurrentFlow.ChangingPassword -> {
                when (val submit = twoFaFlow.flow.submitTotp(action.code)) {
                    is PasswordFlowSubmitTotpResult.Error -> {
                        emit(Error.LoginFlow(submit.v1.getErrorMessage(context)))
                    }

                    is PasswordFlowSubmitTotpResult.Ok -> emit(LoggedIn)
                }
            }

            is CurrentFlow.LoggingIn -> {
                when (val submit = twoFaFlow.flow.submitTotp(action.code)) {
                    is LoginFlowSubmitTotpResult.Error -> emitAll(onSubmitTotpError(submit, twoFaFlow.flow))
                    is LoginFlowSubmitTotpResult.Ok -> emitAll(onSuccess(twoFaFlow.flow))
                }
            }
        }
    }

    private fun onSubmitTotpError(err: LoginFlowSubmitTotpResult.Error, loginFlow: LoginFlow) = flow {
        if (loginFlow.isAwaiting2fa()) {
            emitAll(onError(err.v1))
        } else if (err.v1 is LoginError.PostLoginValidationFailed) {
            // keep the account in account manager but signed out
            emitAll(onClose(message = err.v1.getErrorMessage(context), deleteAccount = false))
        } else {
            flowManager.clearCache(userId)
            emitAll(onClose(message = context.getString(R.string.auth_second_factor_incorrect_code)))
        }
    }

    private fun onError(error: LoginError): Flow<OneTimePasswordInputState> = flow {
        emit(Error.LoginFlow(error.getErrorMessage(context)))
    }

    private fun onClose(message: String? = null, deleteAccount: Boolean = true): Flow<OneTimePasswordInputState> =
        flow {
            if (deleteAccount) {
                sessionInterface.deleteAccount(userId.id)
            }
            emit(Closed(message = message))
        }

    private fun onSuccess(loginFlow: LoginFlow): Flow<OneTimePasswordInputState> = flow {
        when (loginFlow.isAwaitingMailboxPassword()) {
            true -> emit(Awaiting2Pass)
            false -> when (val result = sessionInterface.toUserSession(loginFlow)) {
                is MailSessionToUserSessionResult.Error -> emit(Error.LoginFlow(result.v1.getErrorMessage(context)))
                is MailSessionToUserSessionResult.Ok -> emit(LoggedIn)
            }
        }
    }
}
