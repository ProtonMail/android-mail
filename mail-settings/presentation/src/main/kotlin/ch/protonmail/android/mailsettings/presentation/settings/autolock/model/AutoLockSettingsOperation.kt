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

package ch.protonmail.android.mailsettings.presentation.settings.autolock.model

import ch.protonmail.android.mailsettings.domain.model.autolock.biometric.AutoLockBiometricsState
import ch.protonmail.android.mailsettings.domain.model.autolock.AutoLockInterval
import ch.protonmail.android.mailsettings.domain.model.autolock.AutoLockPreference

sealed interface AutoLockSettingsOperation

sealed interface AutoLockSettingsViewAction : AutoLockSettingsOperation {
    data class ToggleAutoLockBiometricsPreference(
        val autoLockBiometricsUiModel: AutoLockBiometricsUiModel
    ) : AutoLockSettingsViewAction

    data class ToggleAutoLockPreference(val newValue: Boolean) : AutoLockSettingsViewAction

    data class UpdateAutoLockInterval(val interval: AutoLockInterval) : AutoLockSettingsViewAction

    data class ToggleIntervalDropDownVisibility(val value: Boolean) : AutoLockSettingsViewAction
}

sealed interface AutoLockSettingsEvent : AutoLockSettingsOperation {

    sealed interface Data : AutoLockSettingsEvent {
        data class Loaded(
            val lockEnabled: AutoLockPreference,
            val selectedInterval: AutoLockInterval,
            val biometricsState: AutoLockBiometricsState,
            val dropDownMenuVisible: Boolean = false
        ) : Data
    }

    sealed interface Update : AutoLockSettingsEvent {
        data class AutoLockPreferenceEnabled(val newValue: Boolean) : Update
        data class AutoLockIntervalSet(val newValue: AutoLockInterval) : Update
        data class AutoLockIntervalsDropDownToggled(val newValue: Boolean) : Update
        data class AutoLockBiometricsToggled(val newValue: Boolean) : Update
    }

    object AutoLockBiometricsEnrollmentError : AutoLockSettingsEvent
    object AutoLockBiometricsHwError : AutoLockSettingsEvent

    object UpdateError : AutoLockSettingsEvent

    object ForcePinCreation : AutoLockSettingsEvent
    object ChangePinLockRequested : AutoLockSettingsEvent
}
