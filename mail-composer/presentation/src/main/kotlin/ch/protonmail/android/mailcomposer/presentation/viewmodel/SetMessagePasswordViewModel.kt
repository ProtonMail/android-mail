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

package ch.protonmail.android.mailcomposer.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.protonmail.android.mailcommon.domain.usecase.ObservePrimaryUserId
import ch.protonmail.android.mailcomposer.domain.usecase.DeleteMessagePassword
import ch.protonmail.android.mailcomposer.domain.usecase.ObserveMessagePassword
import ch.protonmail.android.mailcomposer.domain.usecase.SaveMessagePassword
import ch.protonmail.android.mailcomposer.domain.usecase.SaveMessagePasswordAction
import ch.protonmail.android.mailcomposer.presentation.model.MessagePasswordOperation
import ch.protonmail.android.mailcomposer.presentation.model.SetMessagePasswordState
import ch.protonmail.android.mailcomposer.presentation.reducer.SetMessagePasswordReducer
import ch.protonmail.android.mailcomposer.presentation.ui.SetMessagePasswordScreen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import me.proton.core.util.kotlin.deserializeOrNull
import javax.inject.Inject

@HiltViewModel
class SetMessagePasswordViewModel @Inject constructor(
    private val deleteMessagePassword: DeleteMessagePassword,
    private val observeMessagePassword: ObserveMessagePassword,
    private val reducer: SetMessagePasswordReducer,
    private val saveMessagePassword: SaveMessagePassword,
    observePrimaryUserId: ObservePrimaryUserId,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val primaryUserId = observePrimaryUserId().filterNotNull()
    private val inputParams: SetMessagePasswordScreen.InputParams? = savedStateHandle.get<String>(
        SetMessagePasswordScreen.InputParamsKey
    )?.deserializeOrNull()

    private val mutableState = MutableStateFlow<SetMessagePasswordState>(SetMessagePasswordState.Loading)
    val state: StateFlow<SetMessagePasswordState> = mutableState.asStateFlow()

    init {
        initializeScreen()
    }

    fun submit(action: MessagePasswordOperation.Action) = when (action) {
        is MessagePasswordOperation.Action.ValidatePassword -> onValidatePassword(action.password)
        is MessagePasswordOperation.Action.ValidateRepeatedPassword ->
            onValidateRepeatedPassword(action.password, action.repeatedPassword)
        is MessagePasswordOperation.Action.ApplyPassword -> onApplyPassword(action.password, action.passwordHint)
        is MessagePasswordOperation.Action.UpdatePassword -> onUpdatePassword(action.password, action.passwordHint)
        is MessagePasswordOperation.Action.RemovePassword -> onRemovePassword()
    }

    private fun onValidatePassword(password: String) {
        viewModelScope.launch {
            val hasMessagePasswordError = password.length !in MIN_PASSWORD_LENGTH..MAX_PASSWORD_LENGTH
            emitNewStateFrom(MessagePasswordOperation.Event.PasswordValidated(hasMessagePasswordError))
        }
    }

    private fun onValidateRepeatedPassword(password: String, repeatedPassword: String) {
        viewModelScope.launch {
            val hasRepeatedMessagePasswordError = password != repeatedPassword
            emitNewStateFrom(MessagePasswordOperation.Event.RepeatedPasswordValidated(hasRepeatedMessagePasswordError))
        }
    }

    private fun onApplyPassword(password: String, passwordHint: String?) {
        viewModelScope.launch {
            inputParams?.let { inputParams ->
                saveMessagePassword(
                    primaryUserId.first(),
                    inputParams.messageId,
                    inputParams.senderEmail,
                    password,
                    passwordHint
                )
                emitNewStateFrom(MessagePasswordOperation.Event.ExitScreen)
            }
        }
    }

    private fun onUpdatePassword(password: String, passwordHint: String?) {
        viewModelScope.launch {
            inputParams?.let { inputParams ->
                saveMessagePassword(
                    primaryUserId.first(),
                    inputParams.messageId,
                    inputParams.senderEmail,
                    password,
                    passwordHint,
                    SaveMessagePasswordAction.Update
                )
                emitNewStateFrom(MessagePasswordOperation.Event.ExitScreen)
            }
        }
    }

    private fun onRemovePassword() {
        viewModelScope.launch {
            inputParams?.let {
                deleteMessagePassword(primaryUserId.first(), it.messageId)
                emitNewStateFrom(MessagePasswordOperation.Event.ExitScreen)
            }
        }
    }

    private fun initializeScreen() {
        viewModelScope.launch {
            inputParams?.let {
                val messagePassword = observeMessagePassword(primaryUserId.first(), inputParams.messageId).first()
                emitNewStateFrom(MessagePasswordOperation.Event.InitializeScreen(messagePassword))
            }
        }
    }

    private suspend fun emitNewStateFrom(event: MessagePasswordOperation.Event) {
        mutableState.emit(reducer.newStateFrom(mutableState.value, event))
    }

    companion object {
        const val MIN_PASSWORD_LENGTH = 4
        const val MAX_PASSWORD_LENGTH = 21
    }
}
