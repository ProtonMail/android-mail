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

package ch.protonmail.android.mailsettings.presentation.settings.autolock.model.pin

import ch.protonmail.android.mailsettings.domain.model.autolock.biometric.AutoLockBiometricsState

sealed interface AutoLockPinOperation

sealed interface AutoLockPinViewAction : AutoLockPinOperation {
    data class AddPinDigit(val addition: Int) : AutoLockPinViewAction
    object RemovePinDigit : AutoLockPinViewAction
    object PerformConfirm : AutoLockPinViewAction
    object PerformBack : AutoLockPinViewAction
    object RequestSignOut : AutoLockPinViewAction
    object ConfirmSignOut : AutoLockPinViewAction
    object CancelSignOut : AutoLockPinViewAction
}

sealed interface AutoLockPinEvent : AutoLockPinOperation {

    sealed interface Data : AutoLockPinEvent {
        data class Loaded(
            val step: PinInsertionStep,
            val remainingAttempts: PinVerificationRemainingAttempts,
            val initialBiometricsState: AutoLockBiometricsState
        ) : Data
    }

    sealed interface Update : AutoLockPinEvent {
        data class BiometricStateChanged(val biometricState: AutoLockBiometricsState) : Update

        data class PinValueChanged(val newPin: InsertedPin) : Update
        data class MovedToStep(val step: PinInsertionStep) : Update

        object OperationAborted : Update
        object OperationCompleted : Update
        object VerificationCompleted : Update

        object SignOutRequested : Update
        object SignOutConfirmed : Update
        object SignOutCanceled : Update

        sealed interface Error : Update {
            data class WrongPinCode(val remainingAttempts: PinVerificationRemainingAttempts) : Error
            object NotMatchingPins : Error
            object UnknownError : Error
        }
    }
}
