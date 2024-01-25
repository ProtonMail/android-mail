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

package ch.protonmail.android.mailsettings.presentation.settings.autolock.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import arrow.core.getOrElse
import ch.protonmail.android.mailsettings.domain.model.autolock.AutoLockInterval
import ch.protonmail.android.mailsettings.domain.model.autolock.AutoLockPreference
import ch.protonmail.android.mailsettings.domain.usecase.autolock.biometric.ObserveAutoLockBiometricsState
import ch.protonmail.android.mailsettings.domain.usecase.autolock.ObserveAutoLockEnabled
import ch.protonmail.android.mailsettings.domain.usecase.autolock.ObserveAutoLockPinValue
import ch.protonmail.android.mailsettings.domain.usecase.autolock.ObserveSelectedAutoLockInterval
import ch.protonmail.android.mailsettings.domain.usecase.autolock.ToggleAutoLockBiometricsPreference
import ch.protonmail.android.mailsettings.domain.usecase.autolock.ToggleAutoLockEnabled
import ch.protonmail.android.mailsettings.domain.usecase.autolock.UpdateAutoLockInterval
import ch.protonmail.android.mailsettings.presentation.settings.autolock.model.AutoLockSettingsEvent
import ch.protonmail.android.mailsettings.presentation.settings.autolock.model.AutoLockSettingsState
import ch.protonmail.android.mailsettings.presentation.settings.autolock.model.AutoLockSettingsViewAction
import ch.protonmail.android.mailsettings.presentation.settings.autolock.reducer.AutoLockSettingsReducer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AutoLockSettingsViewModel @Inject constructor(
    observeAutoLockEnabled: ObserveAutoLockEnabled,
    observeSelectedAutoLockInterval: ObserveSelectedAutoLockInterval,
    observeAutoLockBiometricsState: ObserveAutoLockBiometricsState,
    private val observeAutoLockPinValue: ObserveAutoLockPinValue,
    private val toggleAutoLockEnabled: ToggleAutoLockEnabled,
    private val toggleAutoLockBiometricsPreference: ToggleAutoLockBiometricsPreference,
    private val updateAutoLockInterval: UpdateAutoLockInterval,
    private val reducer: AutoLockSettingsReducer
) : ViewModel() {

    private val mutableState = MutableStateFlow<AutoLockSettingsState>(AutoLockSettingsState.Loading)
    val state = mutableState.asStateFlow()

    init {
        combine(
            observeAutoLockEnabled(),
            observeSelectedAutoLockInterval(),
            observeAutoLockBiometricsState()
        ) { enabled, interval, biometrics ->
            val isEnabled = enabled.getOrElse { AutoLockPreference(false) }
            val lockInterval = interval.getOrElse { AutoLockInterval.Immediately }

            emitNewStateFrom(AutoLockSettingsEvent.Data.Loaded(isEnabled, lockInterval, biometrics))
        }.launchIn(viewModelScope)
    }

    internal fun submit(action: AutoLockSettingsViewAction) {
        viewModelScope.launch {
            when (action) {
                is AutoLockSettingsViewAction.ToggleAutoLockPreference -> updateAutoLockEnabledValue(action.newValue)
                is AutoLockSettingsViewAction.ToggleIntervalDropDownVisibility ->
                    updateAutoLockDropDownVisible(action.value)

                is AutoLockSettingsViewAction.UpdateAutoLockInterval -> updateAutoLockIntervalValue(action.interval)

                is AutoLockSettingsViewAction.ToggleAutoLockBiometricsPreference ->
                    handleToggleAutoLockBiometricsPreference(action)
            }
        }
    }

    private fun updateAutoLockDropDownVisible(newValue: Boolean) {
        emitNewStateFrom(AutoLockSettingsEvent.Update.AutoLockIntervalsDropDownToggled(newValue))
    }

    private suspend fun updateAutoLockEnabledValue(newValue: Boolean) {
        if (newValue) {
            observeAutoLockPinValue().firstOrNull()?.getOrNull()?.value?.takeIf { it.isNotEmpty() }
                ?: return emitNewStateFrom(AutoLockSettingsEvent.ForcePinCreation)
        }

        toggleAutoLockEnabled(newValue)
            .onRight { emitNewStateFrom(AutoLockSettingsEvent.Update.AutoLockPreferenceEnabled(newValue)) }
            .onLeft { emitNewStateFrom(AutoLockSettingsEvent.UpdateError) }
    }

    private suspend fun handleToggleAutoLockBiometricsPreference(
        action: AutoLockSettingsViewAction.ToggleAutoLockBiometricsPreference
    ) {
        val biometricsUiModel = action.autoLockBiometricsUiModel
        return if (!biometricsUiModel.biometricsHwAvailable) {
            emitNewStateFrom(AutoLockSettingsEvent.AutoLockBiometricsHwError)
        } else if (!biometricsUiModel.biometricsEnrolled) {
            emitNewStateFrom(AutoLockSettingsEvent.AutoLockBiometricsEnrollmentError)
        } else {
            updateAutoLockBiometricsPreference(!biometricsUiModel.enabled)
        }
    }

    private suspend fun updateAutoLockBiometricsPreference(enabled: Boolean) {

        toggleAutoLockBiometricsPreference(enabled)
            .onRight { emitNewStateFrom(AutoLockSettingsEvent.Update.AutoLockBiometricsToggled(enabled)) }
            .onLeft { emitNewStateFrom(AutoLockSettingsEvent.UpdateError) }
    }

    private suspend fun updateAutoLockIntervalValue(interval: AutoLockInterval) = updateAutoLockInterval(interval)
        .onRight { emitNewStateFrom(AutoLockSettingsEvent.Update.AutoLockIntervalSet(interval)) }
        .onLeft { emitNewStateFrom(AutoLockSettingsEvent.UpdateError) }


    private fun emitNewStateFrom(event: AutoLockSettingsEvent) = mutableState.update {
        reducer.newStateFrom(it, event)
    }
}
