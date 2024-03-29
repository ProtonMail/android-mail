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

import androidx.compose.foundation.layout.padding
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import ch.protonmail.android.mailsettings.domain.model.autolock.AutoLockInsertionMode
import ch.protonmail.android.mailsettings.presentation.R
import ch.protonmail.android.mailsettings.presentation.settings.autolock.model.AutoLockSettingsState
import ch.protonmail.android.mailsettings.presentation.settings.autolock.model.AutoLockSettingsViewAction
import ch.protonmail.android.mailsettings.presentation.settings.autolock.viewmodel.AutoLockSettingsViewModel
import ch.protonmail.android.uicomponents.snackbar.DismissableSnackbarHost
import me.proton.core.compose.component.ProtonCenteredProgress
import me.proton.core.compose.component.ProtonSettingsTopBar
import me.proton.core.compose.component.ProtonSnackbarHostState
import me.proton.core.compose.component.ProtonSnackbarType

@Composable
fun AutoLockSettingsScreen(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
    onPinScreenNavigation: (AutoLockInsertionMode) -> Unit = {},
    viewModel: AutoLockSettingsViewModel = hiltViewModel()
) {
    val state: AutoLockSettingsState by viewModel.state.collectAsState()

    val actions = AutoLockSettingsScreenList.Actions(
        onPinScreenNavigation = onPinScreenNavigation,
        onToggleAutoLockEnabled = { viewModel.submit(AutoLockSettingsViewAction.ToggleAutoLockPreference(it)) },
        onIntervalSelected = { viewModel.submit(AutoLockSettingsViewAction.UpdateAutoLockInterval(it)) },
        onTimerItemClick = { viewModel.submit(AutoLockSettingsViewAction.ToggleIntervalDropDownVisibility(it)) },
        onUpdateError = {},
        onToggleBiometricsEnabled = {
            viewModel.submit(AutoLockSettingsViewAction.ToggleAutoLockBiometricsPreference(it))
        }
    )

    AutoLockSettingsScreen(
        modifier = modifier,
        state = state,
        onBackClick = onBackClick,
        actions = actions
    )
}

@Composable
private fun AutoLockSettingsScreen(
    modifier: Modifier = Modifier,
    state: AutoLockSettingsState,
    actions: AutoLockSettingsScreenList.Actions,
    onBackClick: () -> Unit
) {
    val snackbarHostState = ProtonSnackbarHostState()
    val updateErrorMessage = stringResource(id = R.string.mail_settings_auto_lock_update_error)

    val updatedActions = actions.copy(
        onUpdateError = { snackbarHostState.showSnackbar(ProtonSnackbarType.ERROR, updateErrorMessage) }
    )

    Scaffold(
        modifier = modifier,
        topBar = {
            ProtonSettingsTopBar(
                title = stringResource(id = R.string.mail_settings_auto_lock_title),
                onBackClick = onBackClick
            )
        },
        snackbarHost = { DismissableSnackbarHost(protonSnackbarHostState = snackbarHostState) },
        content = { paddingValues ->
            when (state) {
                AutoLockSettingsState.Loading -> ProtonCenteredProgress()
                is AutoLockSettingsState.DataLoaded -> {
                    AutoLockSettingsScreenList(
                        modifier = Modifier.padding(paddingValues),
                        state = state,
                        actions = updatedActions,
                        snackbarHostState = snackbarHostState
                    )
                }
            }
        }
    )
}
