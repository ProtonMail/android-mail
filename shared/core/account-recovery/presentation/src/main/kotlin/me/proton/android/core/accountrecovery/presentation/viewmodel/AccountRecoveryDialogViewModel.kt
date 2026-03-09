/*
 * Copyright (c) 2023 Proton AG
 * This file is part of Proton AG and ProtonCore.
 *
 * ProtonCore is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ProtonCore is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with ProtonCore.  If not, see <https://www.gnu.org/licenses/>.
 */

package me.proton.android.core.accountrecovery.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import me.proton.android.core.account.domain.model.CoreUserId
import me.proton.android.core.accountrecovery.presentation.LogTag
import me.proton.android.core.accountrecovery.presentation.R
import me.proton.android.core.accountrecovery.presentation.entity.UserRecovery
import me.proton.android.core.accountrecovery.presentation.entity.UserRecovery.State.Cancelled
import me.proton.android.core.accountrecovery.presentation.entity.UserRecovery.State.Expired
import me.proton.android.core.accountrecovery.presentation.entity.UserRecovery.State.Grace
import me.proton.android.core.accountrecovery.presentation.entity.UserRecovery.State.Insecure
import me.proton.android.core.accountrecovery.presentation.entity.UserRecovery.State.None
import me.proton.android.core.accountrecovery.presentation.ui.AccountRecoveryDialogAction.CancelPasswordRequest
import me.proton.android.core.accountrecovery.presentation.ui.AccountRecoveryDialogAction.HideCancellationForm
import me.proton.android.core.accountrecovery.presentation.ui.AccountRecoveryDialogAction.Init
import me.proton.android.core.accountrecovery.presentation.ui.AccountRecoveryDialogAction.ShowCancellationForm
import me.proton.android.core.accountrecovery.presentation.ui.AccountRecoveryDialogAction.ShowPasswordChangeForm
import me.proton.android.core.accountrecovery.presentation.ui.AccountRecoveryDialogAction.UserAcknowledged
import me.proton.android.core.accountrecovery.presentation.ui.AccountRecoveryDialogOperation
import me.proton.android.core.accountrecovery.presentation.ui.AccountRecoveryNavigationAction.Back
import me.proton.android.core.accountrecovery.presentation.ui.AccountRecoveryNavigationAction.StartPasswordManager
import me.proton.android.core.accountrecovery.presentation.ui.AccountRecoveryViewState
import me.proton.android.core.accountrecovery.presentation.ui.AccountRecoveryViewState.Closed
import me.proton.android.core.accountrecovery.presentation.ui.AccountRecoveryViewState.Error
import me.proton.android.core.accountrecovery.presentation.ui.AccountRecoveryViewState.Loading
import me.proton.android.core.accountrecovery.presentation.ui.AccountRecoveryViewState.Opened
import me.proton.android.core.accountrecovery.presentation.ui.Arg
import me.proton.android.core.accountrecovery.presentation.usecase.CancelRecovery
import me.proton.android.core.accountrecovery.presentation.usecase.ObserveUserRecovery
import me.proton.core.compose.viewmodel.BaseViewModel
import me.proton.core.compose.viewmodel.stopTimeoutMillis
import me.proton.core.presentation.utils.StringBox
import me.proton.core.util.kotlin.CoreLogger
import uniffi.mail_uniffi.AccountRecoveryScreenId
import uniffi.mail_uniffi.recordAccountRecoveryScreenView
import javax.inject.Inject

@SuppressWarnings("TooGenericExceptionCaught")
@HiltViewModel
class AccountRecoveryDialogViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val observeUserRecovery: ObserveUserRecovery,
    private val cancelRecovery: CancelRecovery
) : BaseViewModel<AccountRecoveryDialogOperation, AccountRecoveryViewState>(
    initialAction = Init,
    initialState = Loading
) {

    private val userId = CoreUserId(requireNotNull(savedStateHandle.get<String>(Arg.UserId)))

    val screenId: StateFlow<AccountRecoveryScreenId?> =
        state.map(AccountRecoveryViewState::toScreenId).stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis),
            initialValue = null
        )

    override suspend fun FlowCollector<AccountRecoveryViewState>.onError(throwable: Throwable) {
        CoreLogger.e(LogTag.ERROR_OBSERVING_STATE, throwable)
        emit(Error(throwable.message))
    }

    override fun onAction(action: AccountRecoveryDialogOperation): Flow<AccountRecoveryViewState> {
        return when (action) {
            is Init -> {
                observeState()
            }

            is UserAcknowledged -> {
                flowOf(Closed())
            }

            is CancelPasswordRequest -> {
                handleCancelPasswordRequest(action.password)
            }

            is ShowCancellationForm -> {
                observeState(showCancellationForm = true)
            }

            is HideCancellationForm -> {
                observeState(showCancellationForm = false)
            }

            is ShowPasswordChangeForm -> {
                observeState(showRecoveryReset = true)
            }

            is Back -> {
                observeState(showCancellationForm = false)
            }

            is StartPasswordManager -> {
                flowOf(AccountRecoveryViewState.StartPasswordManager(userId))
            }
        }
    }

    private fun handleCancelPasswordRequest(password: String): Flow<AccountRecoveryViewState> = flow {
        val processingState = observeUserRecovery(userId).first()?.let { userRecovery ->
            when (userRecovery.state.enum) {
                UserRecovery.State.Grace ->
                    Opened.Cancellation.Processing

                UserRecovery.State.Insecure ->
                    Opened.Cancellation.Processing

                else -> Loading
            }
        } ?: Loading

        emit(processingState)

        if (password.isEmpty()) {
            emit(
                Opened.Cancellation.Error(
                    passwordError = StringBox(R.string.presentation_field_required)
                )
            )
            return@flow
        }

        try {
            cancelRecovery(password, userId)
            emit(Opened.Cancellation.Success)
        } catch (error: Throwable) {
            emit(Opened.Cancellation.Error(error = error.message))
        }
    }

    private fun observeState(
        showCancellationForm: Boolean = false,
        showRecoveryReset: Boolean = false
    ): Flow<AccountRecoveryViewState> = observeUserRecovery(userId)
        .map { userRecovery ->
            if (showRecoveryReset && userRecovery?.isAccountRecoveryResetEnabled == true) {
                return@map AccountRecoveryViewState.StartPasswordManager(userId)
            }

            when (userRecovery?.state?.enum) {
                null, None -> Closed()
                Grace -> handleGracePeriod(userRecovery, showCancellationForm)
                Cancelled -> Opened.CancellationHappened
                Insecure -> handleInsecurePeriod(userRecovery, showCancellationForm)
                Expired -> Opened.RecoveryEnded(userRecovery.email)
            }
        }

    private fun handleGracePeriod(userRecovery: UserRecovery, showCancellationForm: Boolean): AccountRecoveryViewState =
        when (showCancellationForm) {
            true -> Opened.Cancellation.Init
            false -> Opened.GracePeriodStarted(
                email = userRecovery.email,
                remainingHours = userRecovery.remainingHours
            )
        }

    private fun handleInsecurePeriod(
        userRecovery: UserRecovery,
        showCancellationForm: Boolean
    ): AccountRecoveryViewState = when (showCancellationForm) {
        true -> Opened.Cancellation.Init
        false -> {
            if (userRecovery.selfInitiated && userRecovery.isAccountRecoveryResetEnabled) {
                Opened.PasswordChangePeriodStarted.SelfInitiated(
                    endDate = userRecovery.endDateFormatted
                )
            } else {
                Opened.PasswordChangePeriodStarted.OtherDeviceInitiated(
                    endDate = userRecovery.endDateFormatted
                )
            }
        }
    }

    fun onScreenView(screenId: AccountRecoveryScreenId) = viewModelScope.launch {
        recordAccountRecoveryScreenView(screenId)
    }
}
