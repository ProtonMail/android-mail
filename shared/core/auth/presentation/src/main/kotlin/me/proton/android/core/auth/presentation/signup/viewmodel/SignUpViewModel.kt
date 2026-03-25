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

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import me.proton.android.core.auth.presentation.LogTag
import me.proton.android.core.auth.presentation.R
import me.proton.android.core.auth.presentation.flow.runWithExponentialBackoffResult
import me.proton.android.core.auth.presentation.signup.CreatePasswordAction
import me.proton.android.core.auth.presentation.signup.CreatePasswordState
import me.proton.android.core.auth.presentation.signup.CreateRecoveryAction
import me.proton.android.core.auth.presentation.signup.CreateRecoveryState
import me.proton.android.core.auth.presentation.signup.CreateUsernameAction
import me.proton.android.core.auth.presentation.signup.CreateUsernameState
import me.proton.android.core.auth.presentation.signup.SignUpAction
import me.proton.android.core.auth.presentation.signup.SignUpAction.CreateUser
import me.proton.android.core.auth.presentation.signup.SignUpAction.FinalizeSignup
import me.proton.android.core.auth.presentation.signup.SignUpState
import me.proton.android.core.auth.presentation.signup.SignUpState.LoginSuccess
import me.proton.android.core.auth.presentation.signup.SignUpState.SignUpError
import me.proton.android.core.auth.presentation.signup.SignUpState.SignUpSuccess
import me.proton.android.core.auth.presentation.signup.SignUpState.SigningUp
import me.proton.android.core.auth.presentation.signup.SignUpState.SignupFlowFailure
import me.proton.android.core.events.domain.AccountEvent
import me.proton.android.core.events.domain.AccountEventBroadcaster
import me.proton.core.account.domain.entity.AccountType
import me.proton.core.compose.viewmodel.BaseViewModel
import me.proton.core.presentation.savedstate.state
import me.proton.core.util.kotlin.CoreLogger
import me.proton.core.util.kotlin.CoroutineScopeProvider
import uniffi.mail_account_uniffi.PasswordValidatorService
import uniffi.mail_account_uniffi.PostLoginValidationError
import uniffi.mail_account_uniffi.SignupException
import uniffi.mail_account_uniffi.SignupFlow
import uniffi.mail_account_uniffi.SignupFlowCompleteResult
import uniffi.mail_account_uniffi.SignupFlowCreateResult
import uniffi.mail_uniffi.MailSession
import uniffi.mail_uniffi.MailSessionGetAccountResult
import uniffi.mail_uniffi.MailSessionGetAccountSessionsResult
import uniffi.mail_uniffi.MailSessionNewSignupFlowResult
import uniffi.mail_uniffi.ProtonError
import uniffi.mail_uniffi.SignupScreenId
import uniffi.mail_uniffi.StoredAccount
import uniffi.mail_uniffi.StoredSession
import uniffi.mail_uniffi.recordSignupScreenView
import javax.inject.Inject

@Suppress("TooGenericExceptionCaught")
@HiltViewModel
class SignUpViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val savedStateHandle: SavedStateHandle,
    private val requiredAccountType: AccountType,
    private val sessionInterface: MailSession,
    private val accountEventBroadcaster: AccountEventBroadcaster
) : BaseViewModel<SignUpAction, SignUpState>(
    initialAction = SignUpAction.InitSignUpFlow,
    initialState = CreateUsernameState.Idle(
        savedStateHandle.get<AccountType>("accountType") ?: requiredAccountType,
        isLoading = true
    ),
    sharingStarted = SharingStarted.Lazily
) {

    private var currentAccountType: AccountType by savedStateHandle.state(requiredAccountType)

    @Volatile
    private var cachedFlow: Result<SignupFlow>? = null
    private val flowMutex = Mutex()

    private val usernameHandler = UsernameHandler.create(
        getFlow = { getSignUpFlow() },
        getCurrentAccountType = { currentAccountType },
        getString = context::getString,
        getQuantityString = context.resources::getQuantityString,
        updateAccountType = { type -> currentAccountType = type }
    )

    private val passwordHandler = PasswordHandler.create(
        getFlow = { getSignUpFlow() },
        getString = context::getString,
        getQuantityString = context.resources::getQuantityString
    )

    private val recoveryHandler = RecoveryHandler.create(
        getFlow = { getSignUpFlow() },
        getString = context::getString,
        getQuantityString = context.resources::getQuantityString
    )

    @Inject
    internal lateinit var scopeProvider: CoroutineScopeProvider

    private suspend fun createSignUpFlowWithBackoff(): SignupFlow {
        return runWithExponentialBackoffResult(
            shouldRetry = { result ->
                result is MailSessionNewSignupFlowResult.Error &&
                    (result.v1 == ProtonError.Network || result.v1 is ProtonError.OtherReason)
            }
        ) {
            sessionInterface.newSignupFlow()
        }.let { result ->
            when (result) {
                is MailSessionNewSignupFlowResult.Ok -> {
                    result.v1
                }

                is MailSessionNewSignupFlowResult.Error -> {
                    throw IllegalStateException("Failed to create sign-up flow: $result")
                }
            }
        }
    }

    private suspend fun getSignUpFlow(): SignupFlow {
        cachedFlow?.let { return it.getOrThrow() }

        return flowMutex.withLock {
            cachedFlow?.let { return it.getOrThrow() }

            val result = runCatching { createSignUpFlowWithBackoff() }
            cachedFlow = result
            result.getOrThrow()
        }
    }

    override fun onAction(action: SignUpAction) = when (action) {
        is SignUpAction.CreatePlan -> emptyFlow()
        is SignUpAction.InitSignUpFlow -> onInitSignUpFlow()
        is CreateUsernameAction -> usernameHandler.handleAction(action)
        is CreatePasswordAction -> passwordHandler.handleAction(action)
        is CreateRecoveryAction -> recoveryHandler.handleAction(action)
        is CreateUser -> handleCreateUser()
        is FinalizeSignup -> finalizeSignUp()
    }

    override suspend fun FlowCollector<SignUpState>.onError(throwable: Throwable) {
        val currentState = state.value
        val errorState = when (currentState) {
            is CreateUsernameState -> usernameHandler.handleError(throwable)
            is CreatePasswordState -> passwordHandler.handleError(throwable)
            is CreateRecoveryState -> recoveryHandler.handleError(throwable)
            else -> {
                SignUpError(throwable.message)
            }
        }
        emit(errorState)
    }

    suspend fun getPasswordValidatorService(): PasswordValidatorService =
        requireNotNull(getSignUpFlow().passwordValidator()) {
            "Could not get password validator service."
        }

    fun onScreenView(screenId: SignupScreenId) {
        viewModelScope.launch {
            recordSignupScreenView(screenId = screenId)
        }
    }

    private fun onInitSignUpFlow() = flow<SignUpState> {
        runCatching {
            getSignUpFlow()
        }.onFailure {
            emit(SignupFlowFailure(it.message))
        }.onSuccess {
            val accountType = savedStateHandle.get<AccountType>("accountType") ?: requiredAccountType
            perform(CreateUsernameAction.LoadData(accountType))
        }
    }

    private fun handleCreateUser() = flow {
        emit(SigningUp)
        when (val result = getSignUpFlow().create()) {
            is SignupFlowCreateResult.Error -> emitAll(result.v1.onSignUpError())
            is SignupFlowCreateResult.Ok -> emit(SignUpSuccess)
        }
    }

    private fun finalizeSignUp() = flow {
        when (val result = getSignUpFlow().complete()) {
            is SignupFlowCompleteResult.Error -> {
                emitAll(result.v1.onSignUpError())
            }

            is SignupFlowCompleteResult.Ok -> {
                val userId = result.v1.userId
                getSession(getAccount(userId))?.firstOrNull()
                emit(LoginSuccess(userId))
                accountEventBroadcaster.emit(AccountEvent.SignupCompleted)
                clearUp()
            }
        }
    }

    private fun SignupException.onSignUpError() = flow {
        getSignUpFlow().stepBack()
        emit(
            SignUpError(
                message = getErrorMessage(
                    getString = context::getString,
                    getQuantityString = { id, quantity, args ->
                        context.resources.getQuantityString(id, quantity, args)
                    }
                )
            )
        )
    }

    private suspend fun getSession(account: StoredAccount?): List<StoredSession>? {
        if (account == null) {
            return null
        }

        return when (val result = sessionInterface.getAccountSessions(account)) {
            is MailSessionGetAccountSessionsResult.Error -> {
                CoreLogger.e(LogTag.SIGNUP, "Failed to get account sessions: ${result.v1}")
                null
            }

            is MailSessionGetAccountSessionsResult.Ok -> result.v1
        }
    }

    private suspend fun getAccount(userId: String): StoredAccount? =
        when (val result = sessionInterface.getAccount(userId)) {
            is MailSessionGetAccountResult.Error -> {
                CoreLogger.e(LogTag.SIGNUP, "Failed to get account: ${result.v1}")
                null
            }

            is MailSessionGetAccountResult.Ok -> result.v1
        }

    private suspend fun clearUp() {
        try {
            getSignUpFlow().destroy()
        } catch (e: Exception) {
            CoreLogger.e(LogTag.SIGNUP, "Error destroying signup flow: ${e.message}")
        }
    }
}

interface ErrorHandler {

    fun handleError(throwable: Throwable): SignUpState
}

@Suppress("MaxLineLength")
fun SignupException.getErrorMessage(
    getString: (resId: Int) -> String,
    getQuantityString: (resId: Int, param: Int, args: Int) -> String
): String = when (this) {
    is SignupException.PasswordEmpty -> getString(R.string.auth_signup_validation_password)
    is SignupException.PasswordValidationMismatch -> getString(R.string.auth_signup_createpassword_error_password_not_equal)
    is SignupException.PasswordsNotMatching -> getString(R.string.auth_signup_validation_passwords_do_not_match)
    is SignupException.RecoveryEmailInvalid -> getString(R.string.auth_signup_recovery_email_validation_error)
    is SignupException.RecoveryPhoneNumberInvalid -> getString(R.string.auth_signup_recovery_phone_validation_error)
    is SignupException.UsernameEmpty -> getString(R.string.auth_signup_validation_username)
    is SignupException.UsernameUnavailable -> this.v1 ?: getString(R.string.auth_signup_username_unavailable_error)
    is SignupException.AccountCreationFailed,
    is SignupException.AddressSetupFailed,
    is SignupException.Api,
    is SignupException.Crypto,
    is SignupException.Internal,
    is SignupException.KeySetupFailed,
    is SignupException.PasswordNotValidated,
    is SignupException.SignupBlockedByServer -> getString(R.string.common_error_something_went_wrong)

    is SignupException.PostLoginValidationException -> when (val loginError = this.v1) {
        is PostLoginValidationError.FreeAccountLimitExceeded -> getQuantityString(
            R.plurals.auth_user_check_max_free_error,
            loginError.v1.toInt(),
            loginError.v1.toInt()
        )
    }
}
