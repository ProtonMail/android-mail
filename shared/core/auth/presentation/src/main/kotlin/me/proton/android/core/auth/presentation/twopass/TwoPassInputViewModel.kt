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

package me.proton.android.core.auth.presentation.twopass

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted.Companion.WhileSubscribed
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import me.proton.android.core.auth.presentation.LogTag
import me.proton.android.core.auth.presentation.login.getErrorMessage
import me.proton.android.core.auth.presentation.twopass.TwoPassArg.getUserId
import me.proton.android.core.auth.presentation.twopass.TwoPassInputState.Error
import me.proton.core.compose.viewmodel.stopTimeoutMillis
import me.proton.core.presentation.utils.InputValidationResult
import me.proton.core.presentation.utils.ValidationType
import me.proton.core.util.kotlin.CoreLogger
import uniffi.mail_account_uniffi.LoginError
import uniffi.mail_account_uniffi.LoginFlow
import uniffi.mail_account_uniffi.LoginFlowSubmitMailboxPasswordResult
import uniffi.mail_uniffi.LoginScreenId
import uniffi.mail_uniffi.MailSession
import uniffi.mail_uniffi.MailSessionGetAccountResult
import uniffi.mail_uniffi.MailSessionGetAccountSessionsResult
import uniffi.mail_uniffi.MailSessionResumeLoginFlowResult
import uniffi.mail_uniffi.MailSessionToUserSessionResult
import uniffi.mail_uniffi.ProtonError
import uniffi.mail_uniffi.StoredAccount
import uniffi.mail_uniffi.StoredSession
import uniffi.mail_uniffi.recordLoginScreenView
import javax.inject.Inject

@HiltViewModel
class TwoPassInputViewModel @Inject constructor(
    @ApplicationContext
    private val context: Context,
    private val savedStateHandle: SavedStateHandle,
    private val sessionInterface: MailSession
) : ViewModel() {

    private val userId by lazy { savedStateHandle.getUserId() }

    private val mutableAction = MutableStateFlow<TwoPassInputAction>(TwoPassInputAction.Load())

    val state: StateFlow<TwoPassInputState> = mutableAction.flatMapLatest { action ->
        when (action) {
            is TwoPassInputAction.Load -> onLoad()
            is TwoPassInputAction.Close -> onClose()
            is TwoPassInputAction.Unlock -> onValidateAndUnlock(action)
        }
    }.stateIn(viewModelScope, WhileSubscribed(stopTimeoutMillis), TwoPassInputState.Idle)

    fun onScreenView() = viewModelScope.launch {
        recordLoginScreenView(LoginScreenId.MAILBOX_PASSWORD)
    }

    fun submit(action: TwoPassInputAction) = viewModelScope.launch {
        mutableAction.emit(action)
    }

    private fun onLoad() = flow {
        emit(TwoPassInputState.Idle)
    }

    private fun onClose(): Flow<TwoPassInputState> = flow {
        sessionInterface.deleteAccount(userId)
        emit(TwoPassInputState.Closed)
    }

    private fun onValidateAndUnlock(action: TwoPassInputAction.Unlock) = flow {
        emit(TwoPassInputState.Loading)
        when (InputValidationResult(action.mailboxPassword, ValidationType.Password).isValid) {
            false -> emit(Error.PasswordIsEmpty)
            true -> emitAll(onUnlock(action))
        }
    }

    private fun onUnlock(action: TwoPassInputAction.Unlock) = flow {
        emit(TwoPassInputState.Loading)
        val session = getSession(getAccount(userId))?.firstOrNull()
        val loginFlow =
            session?.let { sessionInterface.resumeLoginFlow(userId, session.sessionId()) }
        when (loginFlow) {
            null -> emitAll(onClose())
            is MailSessionResumeLoginFlowResult.Error -> emitAll(onError(loginFlow.v1))
            is MailSessionResumeLoginFlowResult.Ok -> {
                when (val submit = loginFlow.v1.submitMailboxPassword(action.mailboxPassword)) {
                    is LoginFlowSubmitMailboxPasswordResult.Error -> emitAll(onError(submit.v1))
                    is LoginFlowSubmitMailboxPasswordResult.Ok -> {
                        emitAll(onSuccess(loginFlow.v1))
                    }
                }
            }
        }
    }

    private fun onError(error: ProtonError): Flow<TwoPassInputState> = flow {
        emit(Error.LoginFlow(error.getErrorMessage(context)))
    }

    private fun onError(error: LoginError): Flow<TwoPassInputState> = flow {
        emit(Error.LoginFlow(error.getErrorMessage(context)))
    }

    private fun onSuccess(loginFlow: LoginFlow): Flow<TwoPassInputState> = flow {
        when (val result = sessionInterface.toUserSession(loginFlow)) {
            is MailSessionToUserSessionResult.Error -> emit(Error.LoginFlow(result.v1.getErrorMessage(context)))
            is MailSessionToUserSessionResult.Ok -> {
                emit(TwoPassInputState.Success)
            }
        }
    }

    private suspend fun getSession(account: StoredAccount?): List<StoredSession>? {
        if (account == null) {
            return null
        }

        return when (val result = sessionInterface.getAccountSessions(account)) {
            is MailSessionGetAccountSessionsResult.Error -> {
                CoreLogger.e(LogTag.LOGIN, result.v1.toString())
                null
            }

            is MailSessionGetAccountSessionsResult.Ok -> result.v1
        }
    }

    private suspend fun getAccount(userId: String) = when (val result = sessionInterface.getAccount(userId)) {
        is MailSessionGetAccountResult.Error -> {
            CoreLogger.e(LogTag.LOGIN, result.v1.toString())
            null
        }

        is MailSessionGetAccountResult.Ok -> result.v1
    }
}
