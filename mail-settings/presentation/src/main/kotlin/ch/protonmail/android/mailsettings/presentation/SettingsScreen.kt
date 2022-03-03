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
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import ch.protonmail.android.mailsettings.domain.model.AppInformation
import ch.protonmail.android.mailsettings.domain.model.AppSettings
import ch.protonmail.android.mailsettings.presentation.R.string
import ch.protonmail.android.mailsettings.presentation.State.Data
import ch.protonmail.android.mailsettings.presentation.State.Loading
import me.proton.core.compose.component.ProtonSettingsHeader
import me.proton.core.compose.component.ProtonSettingsItem
import me.proton.core.compose.component.ProtonSettingsList
import me.proton.core.compose.component.ProtonSettingsTopBar
import me.proton.core.compose.flow.rememberAsState

const val TEST_TAG_SETTINGS_SCREEN = "SettingsScreenTestTag"

@Composable
fun MainSettingsScreen(
    modifier: Modifier = Modifier,
    onAccountClicked: () -> Unit,
    onThemeClick: () -> Unit,
    onPushNotificationsClick: () -> Unit,
    onAutoLockClick: () -> Unit,
    onAlternativeRoutingClick: () -> Unit,
    onAppLanguageClick: () -> Unit,
    onCombinedContactsClick: () -> Unit,
    onSwipeActionsClick: () -> Unit,
    onBackClick: () -> Unit,
    settingsViewModel: SettingsViewModel = hiltViewModel()
) {
    when (val settingsState = rememberAsState(flow = settingsViewModel.state, Loading).value) {
        is Data -> MainSettingsScreen(
            modifier = modifier,
            state = settingsState,
            onAccountClick = onAccountClicked,
            onThemeClick = onThemeClick,
            onPushNotificationsClick = onPushNotificationsClick,
            onAutoLockClick = onAutoLockClick,
            onAlternativeRoutingClick = onAlternativeRoutingClick,
            onAppLanguageClick = onAppLanguageClick,
            onCombinedContactsClick = onCombinedContactsClick,
            onSwipeActionsClick = onSwipeActionsClick,
            onBackClick = onBackClick
        )
        is Loading -> Unit
    }
}


@Composable
fun MainSettingsScreen(
    modifier: Modifier = Modifier,
    state: Data,
    onAccountClick: () -> Unit,
    onThemeClick: () -> Unit,
    onPushNotificationsClick: () -> Unit,
    onAutoLockClick: () -> Unit,
    onAlternativeRoutingClick: () -> Unit,
    onAppLanguageClick: () -> Unit,
    onCombinedContactsClick: () -> Unit,
    onSwipeActionsClick: () -> Unit,
    onBackClick: () -> Unit
) {
    Scaffold(
        modifier = modifier.testTag(TEST_TAG_SETTINGS_SCREEN),
        topBar = {
            ProtonSettingsTopBar(
                title = stringResource(id = R.string.settings),
                onBackClick = onBackClick
            )
        },
        content = {
            ProtonSettingsList {
                item { ProtonSettingsHeader(title = R.string.account_settings) }
                item {
                    AccountSettingsItem(
                        accountInfo = state.account,
                        onAccountClicked = onAccountClick
                    )
                }
                item { ProtonSettingsHeader(title = R.string.app_settings) }
                item {
                    ProtonSettingsItem(
                        name = stringResource(id = R.string.theme),
                        onClick = onThemeClick
                    )
                    Divider()
                }
                item {
                    ProtonSettingsItem(
                        name = stringResource(id = R.string.push_notifications),
                        onClick = onPushNotificationsClick
                    )
                    Divider()
                }
                item {
                    AutoLockSettingItem(
                        state = state,
                        onAutoLockClick = onAutoLockClick
                    )
                }
                item {
                    AlternativeRoutingSettingItem(
                        state = state,
                        onAlternativeRoutingClick = onAlternativeRoutingClick
                    )
                }
                item {
                    AppLanguageSettingItem(
                        state = state,
                        onAppLanguageClick = onAppLanguageClick
                    )
                }
                item {
                    CombinedContactsSettingItem(
                        state = state,
                        onCombinedContactsClick = onCombinedContactsClick
                    )
                }
                item {
                    ProtonSettingsItem(
                        name = stringResource(id = R.string.swipe_actions),
                        onClick = onSwipeActionsClick
                    )
                    Divider()
                }
                item { ProtonSettingsHeader(title = R.string.app_information) }
                item {
                    ProtonSettingsItem(
                        name = stringResource(id = R.string.app_version),
                        hint = state.appInformation.version,
                        isClickable = false
                    )
                }
            }
        }
    )
}

@Composable
private fun CombinedContactsSettingItem(
    modifier: Modifier = Modifier,
    state: Data,
    onCombinedContactsClick: () -> Unit
) {
    val hint = if (state.appSettings.hasCombinedContacts) {
        stringResource(id = string.enabled)
    } else {
        stringResource(id = string.disabled)
    }
    ProtonSettingsItem(
        modifier = modifier,
        name = stringResource(id = string.combined_contacts),
        hint = hint,
        onClick = onCombinedContactsClick
    )
    Divider()
}

@Composable
private fun AppLanguageSettingItem(
    modifier: Modifier = Modifier,
    state: Data,
    onAppLanguageClick: () -> Unit
) {
    val appLanguage = state.appSettings.customAppLanguage
        ?: stringResource(id = string.auto_detect)
    ProtonSettingsItem(
        modifier = modifier,
        name = stringResource(id = string.app_language),
        hint = appLanguage,
        onClick = onAppLanguageClick
    )
    Divider()
}

@Composable
private fun AlternativeRoutingSettingItem(
    modifier: Modifier = Modifier,
    state: Data,
    onAlternativeRoutingClick: () -> Unit
) {
    val hint = if (state.appSettings.hasAlternativeRouting) {
        stringResource(id = string.allowed)
    } else {
        stringResource(id = string.denied)
    }
    ProtonSettingsItem(
        modifier = modifier,
        name = stringResource(id = string.alternative_routing),
        hint = hint,
        onClick = onAlternativeRoutingClick
    )
    Divider()
}

@Composable
private fun AutoLockSettingItem(
    modifier: Modifier = Modifier,
    state: Data,
    onAutoLockClick: () -> Unit
) {
    val hint = if (state.appSettings.hasAutoLock) {
        stringResource(id = string.enabled)
    } else {
        stringResource(id = string.disabled)
    }
    ProtonSettingsItem(
        modifier = modifier,
        name = stringResource(id = string.auto_lock),
        hint = hint,
        onClick = onAutoLockClick
    )
    Divider()
}

@Composable
fun AccountSettingsItem(
    modifier: Modifier = Modifier,
    accountInfo: AccountInfo?,
    onAccountClicked: () -> Unit
) {
    val header = accountInfo?.name ?: stringResource(id = R.string.no_information_available)
    val hint = accountInfo?.email

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
        state = Data(
            AccountInfo("Marino", "marino@proton.ch"),
            AppSettings(
                hasAutoLock = false,
                hasAlternativeRouting = true,
                customAppLanguage = null,
                hasCombinedContacts = true
            ),
            AppInformation("6.0.0-alpha")
        ),
        onAccountClick = { },
        onThemeClick = {},
        onPushNotificationsClick = {},
        onAutoLockClick = {},
        onAlternativeRoutingClick = {},
        onAppLanguageClick = {},
        onCombinedContactsClick = {},
        onSwipeActionsClick = {},
        onBackClick = {}
    )
}
