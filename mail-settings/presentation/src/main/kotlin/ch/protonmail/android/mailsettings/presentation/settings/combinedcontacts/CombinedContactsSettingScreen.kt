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

package ch.protonmail.android.mailsettings.presentation.settings.combinedcontacts

import androidx.compose.foundation.layout.padding
import androidx.compose.material.Scaffold
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import ch.protonmail.android.mailcommon.presentation.ConsumableLaunchedEffect
import ch.protonmail.android.mailsettings.presentation.R
import ch.protonmail.android.uicomponents.settings.SettingsToggleItem
import ch.protonmail.android.uicomponents.snackbar.DismissableSnackbarHost
import me.proton.core.compose.component.ProtonSettingsTopBar
import me.proton.core.compose.component.ProtonSnackbarHostState
import me.proton.core.compose.component.ProtonSnackbarType
import me.proton.core.compose.flow.rememberAsState

const val TEST_TAG_COMBINED_CONTACTS_TOGGLE_ITEM = "CombinedContactsToggleItem"
const val TEST_TAG_COMBINED_CONTACTS_SNACKBAR = "CombinedContactsSnackbar"

@Composable
fun CombinedContactsSettingScreen(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit,
    viewModel: CombinedContactsSettingViewModel = hiltViewModel()
) {
    when (
        val state = rememberAsState(
            flow = viewModel.state,
            initial = CombinedContactsSettingState.Loading
        ).value
    ) {
        is CombinedContactsSettingState.Data -> {
            CombinedContactsSettingScreen(
                modifier = modifier,
                onBackClick = onBackClick,
                onToggle = viewModel::saveCombinedContactsPreference,
                state = state
            )
        }
        is CombinedContactsSettingState.Loading -> Unit
    }
}

@Composable
fun CombinedContactsSettingScreen(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit,
    onToggle: (Boolean) -> Unit,
    state: CombinedContactsSettingState.Data
) {
    val scaffoldState = rememberScaffoldState()
    val errorMessage = stringResource(id = R.string.mail_settings_generic_error_message)

    ConsumableLaunchedEffect(state.combinedContactsSettingErrorEffect) {
        scaffoldState.snackbarHostState.showSnackbar(errorMessage)
    }

    Scaffold(
        modifier = modifier,
        scaffoldState = scaffoldState,
        topBar = {
            ProtonSettingsTopBar(
                title = stringResource(id = R.string.mail_settings_combined_contacts),
                onBackClick = onBackClick
            )
        },
        content = { paddingValues ->
            SettingsToggleItem(
                modifier = Modifier
                    .padding(paddingValues)
                    .testTag(TEST_TAG_COMBINED_CONTACTS_TOGGLE_ITEM),
                name = stringResource(id = R.string.mail_settings_combined_contacts),
                hint = stringResource(id = R.string.mail_settings_combined_contacts_hint),
                value = state.isEnabled ?: false,
                onToggle = { state.isEnabled?.let { onToggle(!it) } }
            )
        },
        snackbarHost = { snackbarHostState ->
            DismissableSnackbarHost(
                modifier = Modifier.testTag(TEST_TAG_COMBINED_CONTACTS_SNACKBAR),
                protonSnackbarHostState = ProtonSnackbarHostState(snackbarHostState, ProtonSnackbarType.ERROR)
            )
        }
    )
}
