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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import ch.protonmail.android.mailsettings.presentation.State.Data
import ch.protonmail.android.mailsettings.presentation.State.Loading
import me.proton.core.compose.component.ProtonSettingsHeader
import me.proton.core.compose.component.ProtonSettingsItem
import me.proton.core.compose.component.ProtonSettingsList
import me.proton.core.compose.flow.rememberAsState
import timber.log.Timber

const val TEST_TAG_SETTINGS_SCREEN = "SettingsScreenTestTag"

@Composable
fun MainSettingsScreen(
    modifier: Modifier = Modifier,
    onAccountClicked: () -> Unit,
    onPushNotificationsClick: () -> Unit,
    onAutoLockClick: () -> Unit,
    onAlternativeRoutingClick: () -> Unit,
    onAppLanguageClick: () -> Unit,
    onCombinedContactsClick: () -> Unit,
    settingsViewModel: MainSettingsViewModel = hiltViewModel()
) {
    when (val settingsState = rememberAsState(flow = settingsViewModel.state, Loading).value) {
        is Data -> MainSettingsScreen(
            modifier = modifier,
            state = settingsState,
            onAccountClick = onAccountClicked,
            onPushNotificationsClick = onPushNotificationsClick,
            onAutoLockClick = onAutoLockClick,
            onAlternativeRoutingClick = onAlternativeRoutingClick,
            onAppLanguageClick = onAppLanguageClick,
            onCombinedContactsClick = onCombinedContactsClick

        )
        is Loading -> Unit
    }
}


@Composable
fun MainSettingsScreen(
    modifier: Modifier = Modifier,
    state: Data,
    onAccountClick: () -> Unit,
    onPushNotificationsClick: () -> Unit,
    onAutoLockClick: () -> Unit,
    onAlternativeRoutingClick: () -> Unit,
    onAppLanguageClick: () -> Unit,
    onCombinedContactsClick: () -> Unit
) {
    Timber.d("Showing settings screen with $state")
    ProtonSettingsList(modifier.testTag(TEST_TAG_SETTINGS_SCREEN)) {
        item { ProtonSettingsHeader(title = R.string.account_settings) }
        item {
            AccountSettingsItem(
                accountData = state.account,
                onAccountClicked = onAccountClick
            )
        }
        item { ProtonSettingsHeader(title = R.string.app_settings) }
        item {
            ProtonSettingsItem(
                name = stringResource(id = R.string.push_notifications),
                onClick = onPushNotificationsClick
            )
            Divider()
        }
        item {
            ProtonSettingsItem(
                name = stringResource(id = R.string.auto_lock),
                hint = stringResource(id = R.string.enabled),
                onClick = onAutoLockClick
            )
            Divider()
        }
        item {
            ProtonSettingsItem(
                name = stringResource(id = R.string.alternative_routing),
                hint = stringResource(id = R.string.allowed),
                onClick = onAlternativeRoutingClick
            )
            Divider()
        }
        item {
            ProtonSettingsItem(
                name = stringResource(id = R.string.app_language),
                hint = stringResource(id = R.string.auto_detect),
                onClick = onAppLanguageClick
            )
            Divider()
        }
        item {
            ProtonSettingsItem(
                name = stringResource(id = R.string.combined_contacts),
                hint = stringResource(id = R.string.disabled),
                onClick = onCombinedContactsClick
            )
            Divider()
        }
    }
}

@Composable
fun AccountSettingsItem(
    modifier: Modifier = Modifier,
    accountData: AccountData?,
    onAccountClicked: () -> Unit
) {
    val header = accountData?.name ?: stringResource(id = R.string.no_information_available)
    val hint = accountData?.email

    ProtonSettingsItem(
        modifier = modifier,
        name = header,
        hint = hint,
        onClick = onAccountClicked
    )
    Divider()
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
fun previewMainSettingsScreen() {
    MainSettingsScreen(
        modifier = Modifier,
        state = Data(AccountData("Marino", "marino@proton.ch")),
        onAccountClick = { },
        onPushNotificationsClick = {},
        onAutoLockClick = {},
        onAlternativeRoutingClick = {},
        onAppLanguageClick = {},
        onCombinedContactsClick = {}
    )
}
