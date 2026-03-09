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

package me.proton.android.core.auth.presentation.passmanagement

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import ch.protonmail.android.design.compose.viewmodel.UiEventFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import me.proton.android.core.account.domain.model.CoreUserId
import me.proton.android.core.auth.data.passvalidator.PasswordValidatorServiceHolder
import me.proton.android.core.auth.presentation.AuthOrchestrator
import me.proton.android.core.auth.presentation.flow.FlowManager
import me.proton.android.core.auth.presentation.passmanagement.PassManagementArg.getUserId
import me.proton.android.core.auth.presentation.passmanagement.PasswordManagementAction.Close
import me.proton.android.core.auth.presentation.passmanagement.PasswordManagementAction.ErrorShown
import me.proton.android.core.auth.presentation.passmanagement.PasswordManagementAction.Load
import me.proton.android.core.auth.presentation.passmanagement.PasswordManagementAction.UserInputAction
import me.proton.android.core.auth.presentation.passmanagement.PasswordManagementAction.UserInputAction.SelectTab
import me.proton.android.core.auth.presentation.passmanagement.PasswordManagementAction.UserInputAction.UpdateLoginPassword
import me.proton.android.core.auth.presentation.passmanagement.PasswordManagementAction.UserInputAction.UpdateMailboxPassword
import me.proton.android.core.auth.presentation.passmanagement.PasswordManagementState.UserInput
import me.proton.core.compose.viewmodel.BaseViewModel
import uniffi.mail_account_uniffi.ChangePasswordScreenId
import uniffi.mail_account_uniffi.recordChangePasswordScreenView
import javax.inject.Inject

@HiltViewModel
class PasswordManagementViewModel @Inject constructor(
    @ApplicationContext context: Context,
    private val savedStateHandle: SavedStateHandle,
    private val observePasswordConfig: ObservePasswordConfig,
    private val flowManager: FlowManager,
    private val authOrchestrator: AuthOrchestrator,
    private val passwordValidatorServiceHolder: PasswordValidatorServiceHolder
) : BaseViewModel<PasswordManagementAction, PasswordManagementState>(
    initialAction = Load,
    initialState = PasswordManagementState.Loading,
    sharingStarted = SharingStarted.Lazily
) {

    private val userId by lazy { CoreUserId(savedStateHandle.getUserId()) }
    private var userInput: UserInput by mutableStateOf(UserInput())

    private val passwordManagementFlowDeferred = viewModelScope.async {
        flowManager.getCurrentActiveFlow(userId, true)
    }
    val uiEvent: UiEventFlow<PasswordManagementEvent> = UiEventFlow()

    private val loginPasswordHandler = LoginPasswordHandler.create(
        context = context,
        getPasswordFlow = ::getCurrentFlow,
        getUserId = { userId },
        uiEventFlow = uiEvent
    )

    private val mailboxPasswordHandler = MailboxPasswordHandler.create(
        context = context,
        getFlow = ::getCurrentFlow,
        getUserId = { userId },
        uiEventFlow = uiEvent
    )

    override fun onAction(action: PasswordManagementAction): Flow<PasswordManagementState> {
        return when (action) {
            is Load -> onLoad()
            is ErrorShown -> handleErrorShown()
            is UserInputAction -> handleUserInputAction(action)
            is Close -> onClose()
        }
    }

    private fun handleErrorShown(): Flow<PasswordManagementState> = flow {
        getPasswordFlow()?.stepBack()
        emit(userInput)
    }

    private fun handleUserInputAction(action: UserInputAction): Flow<PasswordManagementState> {
        return flow {
            when (action) {
                is SelectTab -> {
                    userInput = userInput.copy(selectedTab = action.tab)
                    onScreenView(action.tab)
                    emit(userInput)
                }

                is UpdateLoginPassword ->
                    loginPasswordHandler.handleAction(action, userInput).collect { newState ->
                        handleStateUpdate(newState)
                    }

                is UpdateMailboxPassword ->
                    mailboxPasswordHandler.handleAction(action, userInput).collect { newState ->
                        handleStateUpdate(newState)
                    }
            }
        }
    }

    private suspend fun FlowCollector<PasswordManagementState>.handleStateUpdate(newState: PasswordManagementState) {
        when (newState) {
            is UserInput -> {
                userInput = newState
                emit(newState)
            }

            is PasswordManagementState.Error -> {
                userInput = newState.userInput
                emit(newState)
            }

            is PasswordManagementState.Awaiting2faForLogin -> {
                userInput = newState.userInput
                setup2faHandling(
                    userId = newState.userId.id,
                    onComplete = { perform(UpdateLoginPassword.TwoFaComplete(it, newState.token)) }
                )
                emit(newState)
            }

            is PasswordManagementState.Awaiting2faForMailbox -> {
                userInput = newState.userInput
                setup2faHandling(
                    userId = newState.userId.id,
                    onComplete = { perform(UpdateMailboxPassword.TwoFaComplete(it, newState.token)) }
                )
                emit(newState)
            }

            else -> emit(newState)
        }
    }

    private fun setup2faHandling(userId: String, onComplete: (Boolean) -> Unit) {
        authOrchestrator.setOnSecondFactorResult(onComplete)
        authOrchestrator.startSecondFactorWorkflow(userId)
        recordChangePasswordScreenView(screenId = ChangePasswordScreenId.CHANGE_PASSWORD2FA)
    }

    override suspend fun FlowCollector<PasswordManagementState>.onError(throwable: Throwable) {
        val errorState = when (val currentState = state.value) {
            is UserInput -> getErrorHandlerForCurrentTab(currentState).handleError(throwable, userInput)
            else -> PasswordManagementState.Error.General(throwable.message, userInput)
        }
        emit(errorState)
    }

    private fun getErrorHandlerForCurrentTab(currentState: UserInput): ErrorHandler {
        return if (currentState.selectedTab == PasswordManagementState.Tab.LOGIN) {
            loginPasswordHandler
        } else {
            mailboxPasswordHandler
        }
    }

    private suspend fun getCurrentFlow() = passwordManagementFlowDeferred.await()
    private suspend fun getPasswordFlow() = (getCurrentFlow() as? FlowManager.CurrentFlow.ChangingPassword)?.flow

    private fun onClose(): Flow<PasswordManagementState> = flow {
        flowManager.clearCache(userId)
        emit(PasswordManagementState.Closed)
    }

    private fun onLoad() = flow {
        emit(PasswordManagementState.Loading)
        observePasswordConfig(userId).collect { passwordConfig ->
            val flow = getCurrentFlow()
            val passwordValidator = when (flow) {
                is FlowManager.CurrentFlow.ChangingPassword -> flow.flow.passwordValidator()
                is FlowManager.CurrentFlow.LoggingIn -> flow.flow.passwordValidator()
            }
            passwordValidatorServiceHolder.bind {
                requireNotNull(passwordValidator) {
                    "Could not get password validator service."
                }
            }

            userInput = UserInput(
                loginPassword = LoginPasswordState(
                    currentPasswordNeeded = flow !is FlowManager.CurrentFlow.LoggingIn
                ),
                mailboxPassword = MailboxPasswordState(
                    isAvailable = passwordConfig.passwordMode == PasswordMode.TWO.value
                )
            )
            emit(userInput)
        }
    }

    fun register(context: AppCompatActivity) {
        authOrchestrator.register(context)
    }

    fun onScreenView(tab: PasswordManagementState.Tab) {
        recordChangePasswordScreenView(
            screenId = when (tab) {
                PasswordManagementState.Tab.LOGIN -> ChangePasswordScreenId.CHANGE_PASSWORD
                PasswordManagementState.Tab.MAILBOX -> ChangePasswordScreenId.CHANGE_MAILBOX_PASSWORD
            }
        )
    }

    override fun onCleared() {
        viewModelScope.launch {
            flowManager.clearCache(userId)
        }
        super.onCleared()
    }
}

interface ErrorHandler {

    fun handleError(throwable: Throwable, currentState: UserInput): PasswordManagementState
}
