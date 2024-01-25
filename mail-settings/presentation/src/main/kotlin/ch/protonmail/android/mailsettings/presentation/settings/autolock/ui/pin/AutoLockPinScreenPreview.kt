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

package ch.protonmail.android.mailsettings.presentation.settings.autolock.ui.pin

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
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
import me.proton.core.compose.theme.ProtonTheme

@Preview
@Composable
private fun AutoLockPinScreenPreview() {
    val remainingAttempts = 10
    val insertedPin = InsertedPin(listOf(1, 2, 3, 4))
    ProtonTheme {
        AutoLockPinScreen(
            state = AutoLockPinState.DataLoaded(
                AutoLockPinState.TopBarState(
                    TopBarUiModel(true, R.string.mail_settings_pin_insertion_confirm_title)
                ),
                AutoLockPinState.PinInsertionState(
                    startingStep = PinInsertionStep.PinInsertion,
                    step = PinInsertionStep.PinInsertion,
                    remainingAttempts = PinVerificationRemainingAttempts(remainingAttempts),
                    PinInsertionUiModel(insertedPin)
                ),
                AutoLockPinState.ConfirmButtonState(
                    ConfirmButtonUiModel(
                        isEnabled = true,
                        R.string.mail_settings_pin_insertion_button_create
                    )
                ),
                AutoLockPinState.SignOutButtonState(
                    SignOutUiModel(isDisplayed = true, isRequested = false)
                ),
                BiometricPinUiModel(shouldDisplayButton = false),
                Effect.empty(),
                Effect.empty(),
                Effect.empty(),
                Effect.of(TextUiModel.Text("PIN error placeholder"))
            ),
            actions = AutoLockPinDetailScreen.Actions(
                onConfirmation = {},
                onBiometricsClick = {},
                onDigitAdded = {},
                onBackspaceClick = {},
                onBack = {},
                onShowSuccessSnackbar = {}
            ),
            signOutActions = AutoLockPinDetailScreen.SignOutActions(
                onSignOut = {},
                onSignOutConfirmed = {},
                onSignOutCanceled = {}
            ),
            onBack = {}
        )
    }
}
