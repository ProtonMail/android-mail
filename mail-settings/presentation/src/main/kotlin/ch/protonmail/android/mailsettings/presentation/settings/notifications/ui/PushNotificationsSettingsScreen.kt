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

package ch.protonmail.android.mailsettings.presentation.settings.notifications.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import ch.protonmail.android.mailcommon.presentation.ConsumableLaunchedEffect
import ch.protonmail.android.mailsettings.presentation.R
import ch.protonmail.android.mailsettings.presentation.settings.notifications.model.PushNotificationSettingsViewAction
import ch.protonmail.android.mailsettings.presentation.settings.notifications.model.PushNotificationsSettingsState
import ch.protonmail.android.mailsettings.presentation.settings.notifications.viewmodel.PushNotificationsSettingsViewModel
import ch.protonmail.android.uicomponents.snackbar.DismissableSnackbarHost
import me.proton.core.compose.component.ProtonCenteredProgress
import me.proton.core.compose.component.ProtonSettingsTopBar
import me.proton.core.compose.component.ProtonSnackbarHostState
import me.proton.core.compose.component.ProtonSnackbarType
import me.proton.core.compose.theme.ProtonTheme

@Composable
fun PushNotificationsSettingsScreen(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
    viewModel: PushNotificationsSettingsViewModel = hiltViewModel()
) {
    val state: PushNotificationsSettingsState by viewModel.state.collectAsState()

    val actions = PushNotificationsSettingsScreenList.Actions(
        onExtendedNotificationsTap = {
            viewModel.submit(PushNotificationSettingsViewAction.ToggleExtendedNotifications(it))
        }
    )

    PushNotificationsSettingsScreen(
        modifier = modifier,
        state = state,
        onBackClick = onBackClick,
        settingsActions = actions
    )
}

@Composable
fun PushNotificationsSettingsScreen(
    modifier: Modifier = Modifier,
    state: PushNotificationsSettingsState,
    onBackClick: () -> Unit,
    settingsActions: PushNotificationsSettingsScreenList.Actions
) {
    val snackbarHostState = ProtonSnackbarHostState()
    val updateErrorMessage = stringResource(id = R.string.mail_settings_notifications_update_error)

    Scaffold(
        modifier = modifier,
        topBar = {
            ProtonSettingsTopBar(
                title = stringResource(id = R.string.mail_settings_notifications_title),
                onBackClick = onBackClick
            )
        },
        snackbarHost = { DismissableSnackbarHost(protonSnackbarHostState = snackbarHostState) },
        content = { paddingValues ->
            when (state) {
                PushNotificationsSettingsState.Loading -> ProtonCenteredProgress()
                PushNotificationsSettingsState.LoadingError -> PushNotificationSettingsErrorScreen()
                is PushNotificationsSettingsState.DataLoaded -> {
                    PushNotificationsSettingsScreenList(
                        modifier = Modifier.padding(paddingValues),
                        state = state,
                        actions = settingsActions
                    )
                    ConsumableLaunchedEffect(state.updateErrorState.error) {
                        snackbarHostState.showSnackbar(ProtonSnackbarType.ERROR, message = updateErrorMessage)
                    }
                }
            }
        }
    )
}

@Preview
@Composable
private fun EditAddressIdentityScreenPreview() {
    ProtonTheme {
        PushNotificationsSettingsScreen(
            state = PushNotificationsSettingsScreenPreviewData.state,
            onBackClick = {},
            settingsActions = PushNotificationsSettingsScreenPreviewData.actions
        )
    }
}
