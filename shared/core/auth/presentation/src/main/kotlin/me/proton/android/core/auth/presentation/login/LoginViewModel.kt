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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.protonmail.android.design.compose.viewmodel.UiEventFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.proton.android.core.auth.presentation.IODispatcher
import me.proton.android.core.auth.presentation.challenge.toUserBehavior
import me.proton.android.core.auth.presentation.flow.FlowCache
import me.proton.core.challenge.domain.entity.ChallengeFrameDetails
import uniffi.mail_account_uniffi.LoginError
import uniffi.mail_account_uniffi.LoginFlowLoginResult
import uniffi.mail_account_uniffi.LoginFlowUserIdResult
import uniffi.mail_account_uniffi.PostLoginValidationError
import uniffi.mail_uniffi.LoginScreenId
import uniffi.mail_uniffi.MailSession
import uniffi.mail_uniffi.MailSessionGetSessionResult
import uniffi.mail_uniffi.MailSessionNewLoginFlowResult
import uniffi.mail_uniffi.MailSessionToUserSessionResult
import uniffi.mail_uniffi.StoredSession
import uniffi.mail_uniffi.recordLoginScreenView
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject internal constructor(
    @ApplicationContext private val context: Context,
    private val sessionInterface: MailSession,
    private val flowCache: FlowCache,
    @IODispatcher private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {

    private val loginFlowDeferred: Deferred<MailSessionNewLoginFlowResult> =
        viewModelScope.async {
            flowCache.clear()
            sessionInterface.newLoginFlow()
        }
    private val mutableState: MutableStateFlow<LoginViewState> = MutableStateFlow(LoginViewState.Idle)

    val state: StateFlow<LoginViewState> = mutableState.asStateFlow()
    val uiEvent: UiEventFlow<LoginEvent> = UiEventFlow()

    fun submit(action: LoginAction) {
        viewModelScope.launch {
            when (action) {
                is LoginAction.Login -> onLogin(action)
                is LoginAction.Close -> onClose()
            }
        }
    }

    fun onScreenView() = viewModelScope.launch {
        recordLoginScreenView(LoginScreenId.SIGN_IN_WITH_USERNAME_PASSWORD)
    }

    private suspend fun onLogin(action: LoginAction.Login) {
        val loginFlowResult = loginFlowDeferred.await()
        when {
            action.username.isBlank() -> mutableState.emit(LoginViewState.Error.Validation)
            loginFlowResult is MailSessionNewLoginFlowResult.Error -> {
                emitLoginError(loginFlowResult.v1.getErrorMessage(context))
            }

            else -> performLogin(
                username = action.username,
                usernameFrameDetails = action.usernameFrameDetails,
                password = action.password
            )
        }
    }

    private suspend fun performLogin(
        username: String,
        usernameFrameDetails: ChallengeFrameDetails,
        password: String
    ) = withContext(ioDispatcher) {
        mutableState.emit(LoginViewState.LoggingIn)

        when (
            val result = getLoginFlow().login(
                username = username,
                password = password,
                userBehavior = usernameFrameDetails.toUserBehavior()
            )
        ) {
            is LoginFlowLoginResult.Ok -> onSuccess()
            is LoginFlowLoginResult.Error -> mutableState.emit(getErrorState(result.v1))
        }
    }

    private suspend fun onSuccess() {
        mutableState.emit(getLoginViewState())
    }

    private suspend fun getLoginViewState(): LoginViewState {
        val userId = when (val result = getLoginFlow().userId()) {
            is LoginFlowUserIdResult.Error -> return getErrorState(result.v1)
            is LoginFlowUserIdResult.Ok -> result.v1
        }

        return when {
            getLoginFlow().isAwaitingNewPassword() -> LoginViewState.AwaitingNewPass(userId)
            getLoginFlow().isAwaitingMailboxPassword() -> LoginViewState.Awaiting2Pass(userId)
            getLoginFlow().isAwaiting2fa() -> LoginViewState.Awaiting2fa(userId)
            getLoginFlow().isLoggedIn() -> onLoggedIn(userId)
            else -> LoginViewState.Idle
        }
    }

    private suspend fun onLoggedIn(userId: String): LoginViewState {
        return when (val result = sessionInterface.toUserSession(getLoginFlow())) {
            is MailSessionToUserSessionResult.Error -> {
                emitLoginError("${result.v1}")
                LoginViewState.Idle
            }

            is MailSessionToUserSessionResult.Ok -> LoginViewState.LoggedIn(userId)
        }
    }

    private suspend fun onClose() {
        getLoginFlow().destroy()
    }

    private suspend fun getErrorState(error: LoginError): LoginViewState {
        return when {
            error is LoginError.DuplicateSession -> handleDuplicateSessionError(error)
            (error as? LoginError.PostLoginValidationFailed)?.v1 is PostLoginValidationError.FreeAccountLimitExceeded
            -> handleLoginValidationError(error)

            else -> {
                emitLoginError(error.getErrorMessage(context))
                LoginViewState.Idle
            }
        }
    }

    private suspend fun handleLoginValidationError(error: LoginError.PostLoginValidationFailed): LoginViewState {
        emitLoginError(message = error.getErrorMessage(context), close = true)
        return LoginViewState.Idle
    }

    private suspend fun handleDuplicateSessionError(error: LoginError.DuplicateSession): LoginViewState {
        return sessionInterface.getSession(error.v1).getOrNull()
            ?.let { LoginViewState.Error.AlreadyLoggedIn(it.userId()) }
            ?: run {
                emitLoginError(error.getErrorMessage(context))
                LoginViewState.Idle
            }
    }

    private suspend fun emitLoginError(message: String, close: Boolean = false) {
        uiEvent.emit(LoginEvent.FailedToLogin(message, close))
    }

    private suspend fun getLoginFlow() = (loginFlowDeferred.await() as MailSessionNewLoginFlowResult.Ok).v1

    private fun MailSessionGetSessionResult.getOrNull(): StoredSession? = (this as? MailSessionGetSessionResult.Ok)?.v1
}
