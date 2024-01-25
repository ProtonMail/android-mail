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

package ch.protonmail.android.mailsettings.presentation.settings.autolock.viewmodel.pin

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.protonmail.android.mailsettings.domain.model.autolock.AutoLockInsertionMode
import ch.protonmail.android.mailsettings.domain.model.autolock.AutoLockPin
import ch.protonmail.android.mailsettings.domain.usecase.autolock.GetRemainingAutoLockAttempts
import ch.protonmail.android.mailsettings.domain.usecase.autolock.biometric.ObserveAutoLockBiometricsState
import ch.protonmail.android.mailsettings.domain.usecase.autolock.ObserveAutoLockPinValue
import ch.protonmail.android.mailsettings.domain.usecase.autolock.SaveAutoLockPin
import ch.protonmail.android.mailsettings.domain.usecase.autolock.ToggleAutoLockAttemptPendingStatus
import ch.protonmail.android.mailsettings.domain.usecase.autolock.ToggleAutoLockEnabled
import ch.protonmail.android.mailsettings.domain.usecase.autolock.UpdateLastForegroundMillis
import ch.protonmail.android.mailsettings.domain.usecase.autolock.UpdateRemainingAutoLockAttempts
import ch.protonmail.android.mailsettings.domain.usecase.autolock.biometric.GetCurrentAutoLockBiometricState
import ch.protonmail.android.mailsettings.presentation.settings.autolock.model.pin.AutoLockPinEvent
import ch.protonmail.android.mailsettings.presentation.settings.autolock.model.pin.AutoLockPinState
import ch.protonmail.android.mailsettings.presentation.settings.autolock.model.pin.AutoLockPinViewAction
import ch.protonmail.android.mailsettings.presentation.settings.autolock.model.pin.InsertedPin
import ch.protonmail.android.mailsettings.presentation.settings.autolock.model.pin.PinInsertionStep
import ch.protonmail.android.mailsettings.presentation.settings.autolock.model.pin.PinVerificationRemainingAttempts
import ch.protonmail.android.mailsettings.presentation.settings.autolock.reducer.pin.AutoLockPinReducer
import ch.protonmail.android.mailsettings.presentation.settings.autolock.ui.pin.AutoLockPinScreen
import ch.protonmail.android.mailsettings.presentation.settings.autolock.usecase.ClearPinDataAndForceLogout
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import me.proton.core.util.kotlin.deserialize
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class AutoLockPinViewModel @Inject constructor(
    private val observeAutoLockPin: ObserveAutoLockPinValue,
    private val toggleAutoLockEnabled: ToggleAutoLockEnabled,
    private val observeAutoLockBiometricsState: ObserveAutoLockBiometricsState,
    private val getCurrentAutoLockBiometricState: GetCurrentAutoLockBiometricState,
    private val getRemainingAutoLockAttempts: GetRemainingAutoLockAttempts,
    private val updateRemainingAutoLockAttempts: UpdateRemainingAutoLockAttempts,
    private val saveAutoLockPin: SaveAutoLockPin,
    private val clearPinDataAndForceLogout: ClearPinDataAndForceLogout,
    private val toggleAutoLockAttemptStatus: ToggleAutoLockAttemptPendingStatus,
    private val updateAutoLockLastForegroundMillis: UpdateLastForegroundMillis,
    private val reducer: AutoLockPinReducer,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val mutableState = MutableStateFlow<AutoLockPinState>(AutoLockPinState.Loading)
    val state = mutableState.asStateFlow()

    private var temporaryInsertedPin: InsertedPin? = null

    init {
        val openMode = savedStateHandle.get<String>(AutoLockPinScreen.AutoLockPinModeKey)
            ?.runCatching { deserialize<AutoLockInsertionMode>() }
            ?.getOrNull()

        val step = when (openMode) {
            AutoLockInsertionMode.CreatePin -> PinInsertionStep.PinInsertion
            AutoLockInsertionMode.ChangePin -> PinInsertionStep.PinChange
            is AutoLockInsertionMode.VerifyPin -> PinInsertionStep.PinVerification
            else -> PinInsertionStep.PinInsertion
        }

        viewModelScope.launch {
            val remainingAttempts = getRemainingAutoLockAttempts().getOrNull()?.value?.let {
                PinVerificationRemainingAttempts(it)
            } ?: PinVerificationRemainingAttempts.Default

            toggleAutoLockAttemptStatus(value = true)

            val currentBiometricsState = getCurrentAutoLockBiometricState()
            emitNewStateFrom(AutoLockPinEvent.Data.Loaded(step, remainingAttempts, currentBiometricsState))

            observeAutoLockBiometricsState()
                .onEach {
                    emitNewStateFrom(AutoLockPinEvent.Update.BiometricStateChanged(it))
                }
        }
    }

    fun submit(action: AutoLockPinViewAction) {
        viewModelScope.launch {
            when (action) {
                AutoLockPinViewAction.PerformBack -> onBackPerformed()
                AutoLockPinViewAction.PerformConfirm -> onPerformConfirm()
                AutoLockPinViewAction.RemovePinDigit -> onPinDigitRemoved()
                is AutoLockPinViewAction.AddPinDigit -> onPinDigitAdded(action)
                AutoLockPinViewAction.RequestSignOut -> onSignOutRequested()
                AutoLockPinViewAction.ConfirmSignOut -> onSignOutConfirmed()
                AutoLockPinViewAction.CancelSignOut -> onSignOutCanceled()
                AutoLockPinViewAction.BiometricAuthenticationSucceeded -> onBiometricAuthenticationSucceeded()
            }
        }
    }

    private fun onBackPerformed() {
        val state = state.value as? AutoLockPinState.DataLoaded ?: return emitNewStateFrom(
            AutoLockPinEvent.Update.OperationAborted
        )

        when (state.pinInsertionState.step) {
            PinInsertionStep.PinChange,
            PinInsertionStep.PinInsertion -> emitNewStateFrom(AutoLockPinEvent.Update.OperationAborted)

            PinInsertionStep.PinConfirmation -> emitNewStateFrom(
                AutoLockPinEvent.Update.MovedToStep(PinInsertionStep.PinInsertion)
            )

            PinInsertionStep.PinVerification -> Unit
        }
    }

    private fun handlePinInsertion(insertedPin: InsertedPin) {
        temporaryInsertedPin = insertedPin
        emitNewStateFrom(AutoLockPinEvent.Update.MovedToStep(PinInsertionStep.PinConfirmation))
    }

    private suspend fun handlePinConfirmed(insertedPin: InsertedPin) {
        if (temporaryInsertedPin != insertedPin) {
            return emitNewStateFrom(AutoLockPinEvent.Update.Error.NotMatchingPins)
        }

        val autoLockPin = AutoLockPin(insertedPin.toString())

        saveAutoLockPin(autoLockPin).mapLeft {
            Timber.e("Unable to save auto pin lock value. - $it")
            return emitNewStateFrom(AutoLockPinEvent.Update.Error.UnknownError)
        }

        toggleAutoLockEnabled(newValue = true).mapLeft {
            Timber.e("Unable to enable pin lock. - $it")
            return emitNewStateFrom(AutoLockPinEvent.Update.Error.UnknownError)
        }

        emitNewStateFrom(AutoLockPinEvent.Update.OperationCompleted)
    }

    private suspend fun handlePinChangeConfirmation(
        insertedPin: InsertedPin,
        remainingAttempts: PinVerificationRemainingAttempts
    ) = matchExistingPin(insertedPin, remainingAttempts) {
        emitNewStateFrom(AutoLockPinEvent.Update.MovedToStep(PinInsertionStep.PinInsertion))
    }

    private suspend fun handlePinVerification(
        insertedPin: InsertedPin,
        remainingAttempts: PinVerificationRemainingAttempts
    ) = matchExistingPin(insertedPin, remainingAttempts) {
        emitNewStateFrom(AutoLockPinEvent.Update.VerificationCompleted)
    }

    private suspend inline fun matchExistingPin(
        insertedPin: InsertedPin,
        remainingAttempts: PinVerificationRemainingAttempts,
        continuation: () -> Unit
    ) {
        val storedPin = observeAutoLockPin().firstOrNull()?.getOrNull()
            ?: return emitNewStateFrom(AutoLockPinEvent.Update.Error.UnknownError)

        if (!storedPin.matches(insertedPin)) {
            if (remainingAttempts.value <= 1) {
                clearPinDataAndForceLogout().await()
                emitNewStateFrom(AutoLockPinEvent.Update.OperationAborted)
                return
            }

            val decrementedRemainingAttempts = remainingAttempts.decrement()

            updateRemainingAutoLockAttempts(decrementedRemainingAttempts.value).onLeft {
                Timber.e("Unable to update remaining auto lock attempts. - $it")
            }

            return emitNewStateFrom(AutoLockPinEvent.Update.Error.WrongPinCode(decrementedRemainingAttempts))
        }

        updateRemainingAutoLockAttempts(PinVerificationRemainingAttempts.MaxAttempts).onLeft {
            Timber.e("Unable to reset remaining auto lock attempts. - $it")
        }

        toggleAutoLockAttemptStatus(value = false).onLeft {
            Timber.e("Unable to reset pending lock attempt. - $it")
        }

        updateAutoLockLastForegroundMillis(Long.MAX_VALUE).onLeft {
            Timber.e("Unable to update last foreground millis - $it")
        }

        continuation()
    }

    private suspend fun onBiometricAuthenticationSucceeded() {
        updateRemainingAutoLockAttempts(PinVerificationRemainingAttempts.MaxAttempts).onLeft {
            Timber.e("Unable to reset remaining auto lock attempts. - $it")
        }

        toggleAutoLockAttemptStatus(value = false).onLeft {
            Timber.e("Unable to reset pending lock attempt. - $it")
        }

        updateAutoLockLastForegroundMillis(Long.MAX_VALUE).onLeft {
            Timber.e("Unable to update last foreground millis - $it")
        }
        emitNewStateFrom(AutoLockPinEvent.Update.VerificationCompleted)
    }

    private suspend fun onPerformConfirm() {
        val state = state.value as? AutoLockPinState.DataLoaded ?: return
        val currentStep = state.pinInsertionState.step
        val remainingAttempts = state.pinInsertionState.remainingAttempts
        val insertedPin = state.pinInsertionState.pinInsertionUiModel.currentPin

        when (currentStep) {
            PinInsertionStep.PinChange -> handlePinChangeConfirmation(insertedPin, remainingAttempts)
            PinInsertionStep.PinInsertion -> handlePinInsertion(insertedPin)
            PinInsertionStep.PinConfirmation -> handlePinConfirmed(insertedPin)
            PinInsertionStep.PinVerification -> handlePinVerification(insertedPin, remainingAttempts)
        }
    }

    private fun onPinDigitRemoved() {
        val state = state.value as? AutoLockPinState.DataLoaded ?: return
        val currentPin =
            state.pinInsertionState.pinInsertionUiModel.currentPin.takeIf { it.isNotEmpty() } ?: return

        emitNewStateFrom(AutoLockPinEvent.Update.PinValueChanged(currentPin.deleteLastDigit()))
    }

    private fun onPinDigitAdded(action: AutoLockPinViewAction.AddPinDigit) {
        val state = state.value as? AutoLockPinState.DataLoaded ?: return
        val currentPin = state.pinInsertionState.pinInsertionUiModel.currentPin
        if (currentPin.isMaxLength()) return

        emitNewStateFrom(AutoLockPinEvent.Update.PinValueChanged(currentPin.appendDigit(action.addition)))
    }

    private fun onSignOutRequested() {
        emitNewStateFrom(AutoLockPinEvent.Update.SignOutRequested)
    }

    private suspend fun onSignOutConfirmed() {
        clearPinDataAndForceLogout().await()
        emitNewStateFrom(AutoLockPinEvent.Update.SignOutConfirmed)
    }

    private fun onSignOutCanceled() {
        emitNewStateFrom(AutoLockPinEvent.Update.SignOutCanceled)
    }

    private fun emitNewStateFrom(event: AutoLockPinEvent) = mutableState.update {
        reducer.newStateFrom(it, event)
    }

    private fun AutoLockPin.matches(insertedPin: InsertedPin) = value == insertedPin.toString()
}
