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

package ch.protonmail.android.mailsettings.presentation.accountsettings.autodelete

import androidx.compose.foundation.layout.padding
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ch.protonmail.android.mailcommon.presentation.ui.delete.AutoDeleteDialog
import ch.protonmail.android.mailsettings.presentation.R.string
import ch.protonmail.android.mailsettings.presentation.accountsettings.autodelete.AutoDeleteSettingState.Data
import ch.protonmail.android.mailsettings.presentation.accountsettings.autodelete.AutoDeleteSettingState.Loading
import ch.protonmail.android.uicomponents.settings.SettingsToggleItem
import me.proton.core.compose.component.ProtonCenteredProgress
import me.proton.core.compose.component.ProtonSettingsTopBar
import me.proton.core.util.kotlin.exhaustive

const val TEST_TAG_AUTO_DELETE_SETTINGS_SCREEN = "AccountAutoDeleteTestTag"

@Composable
fun AutoDeleteSettingScreen(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit,
    viewModel: AutoDeleteSettingViewModel = hiltViewModel()
) {
    when (
        val state = viewModel.state.collectAsStateWithLifecycle().value
    ) {
        is Data -> {
            AutoDeleteSettingScreen(
                modifier = modifier,
                onBackClick = onBackClick,
                onAutoDeleteToggled = {
                    if (state.isEnabled) {
                        viewModel.submit(AutoDeleteViewAction.DeactivationRequested)
                    } else {
                        viewModel.submit(AutoDeleteViewAction.ActivationRequested)
                    }
                },
                state = state
            )

            AutoDeleteDialog(
                state = state.enablingDialogState,
                confirm = { viewModel.submit(AutoDeleteViewAction.ActivationConfirmed) },
                dismiss = { viewModel.submit(AutoDeleteViewAction.DialogDismissed) }
            )

            AutoDeleteDialog(
                state = state.disablingDialogState,
                confirm = { viewModel.submit(AutoDeleteViewAction.DeactivationConfirmed) },
                dismiss = { viewModel.submit(AutoDeleteViewAction.DialogDismissed) }
            )

        }

        Loading -> ProtonCenteredProgress()
    }.exhaustive
}

@Composable
private fun AutoDeleteSettingScreen(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit,
    onAutoDeleteToggled: (Boolean) -> Unit,
    state: Data
) {
    Scaffold(
        modifier = modifier.testTag(TEST_TAG_AUTO_DELETE_SETTINGS_SCREEN),
        topBar = {
            ProtonSettingsTopBar(
                title = stringResource(id = string.mail_settings_auto_delete),
                onBackClick = onBackClick
            )
        },
        content = { paddingValues ->
            SettingsToggleItem(
                modifier = Modifier.padding(paddingValues),
                name = stringResource(id = string.mail_settings_auto_delete_full_text),
                hint = stringResource(id = string.mail_settings_auto_delete_hint),
                value = state.isEnabled,
                onToggle = onAutoDeleteToggled
            )
        }
    )
}

@Preview(name = "Auto-delete settings screen")
@Composable
private fun AutoDeleteSettingsScreenPreview() {
    AutoDeleteSettingScreen(
        onBackClick = {}
    )
}
