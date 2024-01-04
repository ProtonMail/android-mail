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

package ch.protonmail.android.mailsettings.presentation.settings.autolock.mapper.pin

import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailsettings.presentation.R
import ch.protonmail.android.mailsettings.presentation.settings.autolock.model.pin.AutoLockPinEvent
import ch.protonmail.android.mailsettings.presentation.settings.autolock.model.pin.PinVerificationRemainingAttempts
import javax.inject.Inject

class AutoLockPinErrorUiMapper @Inject constructor() {

    fun toUiModel(error: AutoLockPinEvent.Update.Error): TextUiModel {
        return when (error) {
            AutoLockPinEvent.Update.Error.NotMatchingPins ->
                TextUiModel(R.string.mail_settings_pin_insertion_error_no_match)

            AutoLockPinEvent.Update.Error.UnknownError ->
                TextUiModel(R.string.mail_settings_pin_insertion_error_unknown)

            is AutoLockPinEvent.Update.Error.WrongPinCode -> toRemainingAttemptsUiModel(error.remainingAttempts)
        }
    }

    fun toUiModel(remainingAttempts: PinVerificationRemainingAttempts): TextUiModel? {
        return remainingAttempts.takeIf { it != PinVerificationRemainingAttempts.Default }?.let {
            toRemainingAttemptsUiModel(it)
        }
    }

    private fun toRemainingAttemptsUiModel(remainingAttempts: PinVerificationRemainingAttempts) =
        if (remainingAttempts.value <= AttemptsThresholdWarningLimit) {
            TextUiModel.PluralisedText(
                R.plurals.mail_settings_pin_insertion_error_wrong_code_ultimatum,
                remainingAttempts.value
            )
        } else {
            TextUiModel.PluralisedText(
                R.plurals.mail_settings_pin_insertion_error_wrong_code,
                remainingAttempts.value
            )
        }

    private companion object {

        const val AttemptsThresholdWarningLimit = 3
    }
}
