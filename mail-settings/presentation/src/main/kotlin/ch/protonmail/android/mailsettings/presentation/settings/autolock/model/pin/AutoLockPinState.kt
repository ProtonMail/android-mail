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

import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel

sealed interface AutoLockPinState {

    object Loading : AutoLockPinState

    data class DataLoaded(
        val topBarState: TopBarState,
        val pinInsertionState: PinInsertionState,
        val confirmButtonState: ConfirmButtonState,
        val signOutButtonState: SignOutButtonState,
        val biometricPinState: BiometricPinUiModel,
        val showBiometricPromptEffect: Effect<Unit>,
        val closeScreenEffect: Effect<Unit>,
        val pinInsertionErrorEffect: Effect<TextUiModel>,
        val snackbarSuccessEffect: Effect<TextUiModel>
    ) : AutoLockPinState

    data class SignOutButtonState(val signOutUiModel: SignOutUiModel)

    data class ConfirmButtonState(val confirmButtonUiModel: ConfirmButtonUiModel)

    data class TopBarState(val topBarStateUiModel: TopBarUiModel)

    data class PinInsertionState(
        val startingStep: PinInsertionStep,
        val step: PinInsertionStep,
        val remainingAttempts: PinVerificationRemainingAttempts,
        val pinInsertionUiModel: PinInsertionUiModel
    )
}
