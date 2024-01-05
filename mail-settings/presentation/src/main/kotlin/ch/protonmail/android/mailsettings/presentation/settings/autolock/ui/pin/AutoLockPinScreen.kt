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

import androidx.compose.foundation.layout.padding
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import ch.protonmail.android.mailcommon.presentation.ConsumableLaunchedEffect
import ch.protonmail.android.mailsettings.presentation.settings.autolock.model.pin.AutoLockPinState
import ch.protonmail.android.mailsettings.presentation.settings.autolock.model.pin.AutoLockPinViewAction
import ch.protonmail.android.mailsettings.presentation.settings.autolock.viewmodel.pin.AutoLockPinViewModel
import me.proton.core.compose.component.ProtonCenteredProgress

@Composable
fun AutoLockPinScreen(
    onBackClick: () -> Unit,
    onNavigateTo: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AutoLockPinViewModel = hiltViewModel()
) {
    val state: AutoLockPinState by viewModel.state.collectAsState()

    val actions = AutoLockPinDetailScreen.Actions(
        onBackspaceClick = { viewModel.submit(AutoLockPinViewAction.RemovePinDigit) },
        onDigitAdded = { viewModel.submit(AutoLockPinViewAction.AddPinDigit(it)) },
        onBackAction = { viewModel.submit(AutoLockPinViewAction.PerformBack) },
        onConfirmation = { viewModel.submit(AutoLockPinViewAction.PerformConfirm) },
        onBiometricsClick = {},
        onNavigateTo = onNavigateTo
    )

    AutoLockPinScreen(
        modifier = modifier,
        state = state,
        onBackClick = onBackClick,
        actions = actions
    )
}

@Composable
fun AutoLockPinScreen(
    state: AutoLockPinState,
    onBackClick: () -> Unit,
    actions: AutoLockPinDetailScreen.Actions,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            AutoLockPinInsertionTopBar(
                state = state,
                onBackClick = actions.onBackAction
            )
        },
        content = { paddingValues ->
            when (state) {
                AutoLockPinState.Loading -> ProtonCenteredProgress()
                is AutoLockPinState.DataLoaded -> {
                    AutoLockPinInsertionScreen(
                        modifier = Modifier.padding(paddingValues),
                        state = state,
                        actions = actions
                    )
                    ConsumableLaunchedEffect(state.closeScreenEffect) {
                        onBackClick()
                    }
                    ConsumableLaunchedEffect(state.navigateEffect) {
                        actions.onNavigateTo(it)
                    }
                }
            }
        }
    )
}

object AutoLockPinScreen {

    const val AutoLockPinModeKey = "auto_lock_pin_open_mode"
}
