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

package ch.protonmail.android.mailsettings.presentation.settings.autolock.helpers

import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailsettings.domain.model.autolock.AutoLockPin
import ch.protonmail.android.mailsettings.presentation.R
import ch.protonmail.android.mailsettings.presentation.settings.autolock.model.pin.AutoLockPinState
import ch.protonmail.android.mailsettings.presentation.settings.autolock.model.pin.BiometricPinUiModel
import ch.protonmail.android.mailsettings.presentation.settings.autolock.model.pin.ConfirmButtonUiModel
import ch.protonmail.android.mailsettings.presentation.settings.autolock.model.pin.InsertedPin
import ch.protonmail.android.mailsettings.presentation.settings.autolock.model.pin.PinInsertionStep
import ch.protonmail.android.mailsettings.presentation.settings.autolock.model.pin.PinInsertionUiModel
import ch.protonmail.android.mailsettings.presentation.settings.autolock.model.pin.PinVerificationRemainingAttempts
import ch.protonmail.android.mailsettings.presentation.settings.autolock.model.pin.SignOutUiModel
import ch.protonmail.android.mailsettings.presentation.settings.autolock.model.pin.TopBarUiModel

internal object AutoLockTestData {

    val BaseTopBarUiModel = TopBarUiModel(true, R.string.mail_settings_pin_insertion_set_title)
    val BaseTopBarState = AutoLockPinState.TopBarState(BaseTopBarUiModel)

    val BasePinInsertionState = AutoLockPinState.PinInsertionState(
        startingStep = PinInsertionStep.PinInsertion,
        step = PinInsertionStep.PinInsertion,
        remainingAttempts = PinVerificationRemainingAttempts.Default,
        pinInsertionUiModel = PinInsertionUiModel(InsertedPin(emptyList()))
    )

    val BaseConfirmButtonUiModel =
        ConfirmButtonUiModel(isEnabled = false, R.string.mail_settings_pin_insertion_button_confirm)
    val BaseConfirmButtonState = AutoLockPinState.ConfirmButtonState(BaseConfirmButtonUiModel)

    val BaseSignOutUiModel = SignOutUiModel(isDisplayed = false, isRequested = false)
    val BaseSignOutState = AutoLockPinState.SignOutButtonState(BaseSignOutUiModel)

    val biometricPinState = BiometricPinUiModel(shouldDisplayButton = false)

    val BaseLoadedState = AutoLockPinState.DataLoaded(
        topBarState = BaseTopBarState,
        pinInsertionState = BasePinInsertionState,
        confirmButtonState = BaseConfirmButtonState,
        signOutButtonState = BaseSignOutState,
        biometricPinState = biometricPinState,
        showBiometricPromptEffect = Effect.of(Unit),
        Effect.empty(),
        Effect.empty(),
        Effect.empty()
    )

    val OneRemainingAttempt = PinVerificationRemainingAttempts(1)
    val NineRemainingAttempts = PinVerificationRemainingAttempts(9)

    val BaseAutoLockPin = AutoLockPin("1234")
    val BaseAutoLockUpdatedPin = AutoLockPin("1233")
    val BaseInvalidPinInserted = InsertedPin(listOf(1, 2, 3))
    val BaseValidPinInserted = InsertedPin(listOf(1, 2, 3, 4))

    val PlaceholderTextUiModel = TextUiModel("placeholder")
    val SignOutShownUiModel = SignOutUiModel(isDisplayed = true, isRequested = false)
    val SignOutRequestedUiModel = SignOutUiModel(isDisplayed = true, isRequested = true)
}
