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

package ch.protonmail.android.mailsettings.presentation.settings.autolock.ui

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import ch.protonmail.android.mailcommon.presentation.ConsumableLaunchedEffect
import ch.protonmail.android.mailcommon.presentation.ConsumableTextEffect
import ch.protonmail.android.mailsettings.domain.model.autolock.AutoLockInsertionMode
import ch.protonmail.android.mailsettings.domain.model.autolock.AutoLockInterval
import ch.protonmail.android.mailsettings.presentation.settings.autolock.model.AutoLockBiometricsUiModel
import ch.protonmail.android.mailsettings.presentation.settings.autolock.model.AutoLockSettingsState
import me.proton.core.compose.component.ProtonSnackbarHostState
import me.proton.core.compose.component.ProtonSnackbarType

@Composable
fun AutoLockSettingsScreenList(
    modifier: Modifier = Modifier,
    state: AutoLockSettingsState.DataLoaded,
    actions: AutoLockSettingsScreenList.Actions,
    snackbarHostState: ProtonSnackbarHostState
) {
    LazyColumn(modifier = modifier) {
        AutoLockEnabledItem(state.autoLockEnabledState.autoLockEnabledUiModel, actions.onToggleAutoLockEnabled)

        if (state.autoLockEnabledState.autoLockEnabledUiModel.enabled) {
            AutoLockIntervalsSection(
                state.autoLockIntervalsState,
                actions.onIntervalSelected,
                actions.onPinScreenNavigation,
                actions.onTimerItemClick
            )

            AutoLockBiometricsItem(state.autoLockBiometricsState, actions.onToggleBiometricsEnabled)
        }
    }

    ConsumableLaunchedEffect(state.updateError) {
        actions.onUpdateError()
    }

    ConsumableLaunchedEffect(state.forceOpenPinCreation) {
        actions.onPinScreenNavigation(AutoLockInsertionMode.CreatePin)
    }

    ConsumableLaunchedEffect(state.pinLockChangeRequested) {
        actions.onPinScreenNavigation(AutoLockInsertionMode.ChangePin)
    }

    ConsumableTextEffect(state.autoLockBiometricsState.biometricsHwError) { message ->
        snackbarHostState.showSnackbar(
            message = message,
            type = ProtonSnackbarType.WARNING
        )
    }

    ConsumableTextEffect(state.autoLockBiometricsState.biometricsEnrollmentError) { message ->
        snackbarHostState.showSnackbar(
            message = message,
            type = ProtonSnackbarType.WARNING
        )
    }
}

object AutoLockSettingsScreenList {

    data class Actions(
        val onToggleAutoLockEnabled: (Boolean) -> Unit,
        val onPinScreenNavigation: (AutoLockInsertionMode) -> Unit,
        val onIntervalSelected: (AutoLockInterval) -> Unit,
        val onToggleBiometricsEnabled: (AutoLockBiometricsUiModel) -> Unit,
        val onTimerItemClick: (show: Boolean) -> Unit,
        val onUpdateError: suspend () -> Unit
    )
}
