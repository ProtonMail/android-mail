/*
 * Copyright (c) 2021 Proton Technologies AG
 * This file is part of Proton Technologies AG and ProtonMail.
 *
 * ProtonMail is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ProtonMail is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with ProtonMail.  If not, see <https://www.gnu.org/licenses/>.
 */

package ch.protonmail.android.mailsettings.presentation

import android.content.res.Configuration.UI_MODE_NIGHT_NO
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.material.Divider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import ch.protonmail.android.mailsettings.presentation.State.Data
import ch.protonmail.android.mailsettings.presentation.State.Loading
import me.proton.core.compose.component.ProtonSettingsHeader
import me.proton.core.compose.component.ProtonSettingsItem
import me.proton.core.compose.component.ProtonSettingsScreen
import me.proton.core.compose.flow.rememberAsState

const val TEST_TAG_SETTINGS_SCREEN = "SettingsScreenTestTag"

@Composable
fun MainSettingsScreen(
    modifier: Modifier = Modifier,
    onAccountClicked: () -> Unit,
    settingsViewModel: MainSettingsViewModel = hiltViewModel()
) {
    when (val settingsState = rememberAsState(flow = settingsViewModel.state, Loading).value) {
        is Data -> MainSettingsScreen(
            modifier = modifier,
            onAccountClicked = onAccountClicked,
            state = settingsState
        )
        is Loading -> Unit
    }
}


@Composable
fun MainSettingsScreen(
    modifier: Modifier = Modifier,
    onAccountClicked: () -> Unit,
    state: Data
) {
    ProtonSettingsScreen(modifier.testTag(TEST_TAG_SETTINGS_SCREEN)) {
        item { ProtonSettingsHeader(title = R.string.account_settings) }
        item {
            ProtonSettingsItem(
                name = state.name,
                hint = state.email,
                onClick = onAccountClicked
            )
            Divider()
        }
    }
}

@Preview(
    name = "Main settings screen light mode",
    showBackground = true,
    uiMode = UI_MODE_NIGHT_NO
)
@Preview(
    name = "Main settings screen dark mode",
    showBackground = true,
    uiMode = UI_MODE_NIGHT_YES
)
@Composable
fun previewSettings() {
    MainSettingsScreen(onAccountClicked = {})
}
