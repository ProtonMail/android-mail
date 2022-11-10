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

package ch.protonmail.android.mailsettings.presentation.settings

import android.content.res.Configuration.UI_MODE_NIGHT_NO
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Divider
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import ch.protonmail.android.mailcommon.domain.AppInformation
import ch.protonmail.android.mailsettings.domain.model.AppSettings
import ch.protonmail.android.mailsettings.presentation.R
import ch.protonmail.android.mailsettings.presentation.R.string
import ch.protonmail.android.mailsettings.presentation.settings.SettingsState.Data
import ch.protonmail.android.mailsettings.presentation.settings.SettingsState.Loading
import me.proton.core.compose.component.ProtonSettingsHeader
import me.proton.core.compose.component.ProtonSettingsItem
import me.proton.core.compose.component.ProtonSettingsList
import me.proton.core.compose.component.ProtonSettingsTopBar
import me.proton.core.compose.flow.rememberAsState
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.usersettings.presentation.compose.view.CrashReportSettingToggleItem
import me.proton.core.usersettings.presentation.compose.view.TelemetrySettingToggleItem

const val TEST_TAG_SETTINGS_SCREEN = "SettingsScreenTestTag"
const val TEST_TAG_SETTINGS_LIST = "SettingsListTestTag"
const val TEST_TAG_SETTINGS_SCREEN_ACCOUNT_ITEM = "AccountSettingsItemTestTag"

@Composable
fun MainSettingsScreen(
    modifier: Modifier = Modifier,
    actions: MainSettingsScreen.Actions,
    settingsViewModel: SettingsViewModel = hiltViewModel()
) {
    when (val settingsState = rememberAsState(flow = settingsViewModel.state, Loading).value) {
        is Data -> MainSettingsScreen(
            modifier = modifier,
            state = settingsState,
            actions = actions
        )
        is Loading -> Unit
    }
}


@Composable
fun MainSettingsScreen(
    state: Data,
    actions: MainSettingsScreen.Actions,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.testTag(TEST_TAG_SETTINGS_SCREEN),
        topBar = {
            ProtonSettingsTopBar(
                title = stringResource(id = string.mail_settings_settings),
                onBackClick = actions.onBackClick
            )
        }
    ) { contentPadding ->
        ProtonSettingsList(
            modifier = modifier
                .testTag(TEST_TAG_SETTINGS_LIST)
                .padding(contentPadding)
        ) {
            item { ProtonSettingsHeader(title = string.mail_settings_account_settings) }
            item {
                AccountSettingsItem(
                    modifier = Modifier.testTag(TEST_TAG_SETTINGS_SCREEN_ACCOUNT_ITEM),
                    accountInfo = state.account,
                    onAccountClicked = actions.onAccountClick
                )
            }
            item { ProtonSettingsHeader(title = string.mail_settings_app_settings) }
            item {
                ProtonSettingsItem(
                    name = stringResource(id = string.mail_settings_theme),
                    onClick = actions.onThemeClick
                )
                Divider()
            }
            item {
                ProtonSettingsItem(
                    name = stringResource(id = string.mail_settings_push_notifications),
                    onClick = actions.onPushNotificationsClick
                )
                Divider()
            }
            item {
                AutoLockSettingItem(
                    appSettings = state.appSettings,
                    onAutoLockClick = actions.onAutoLockClick
                )
            }
            item {
                AlternativeRoutingSettingItem(
                    appSettings = state.appSettings,
                    onAlternativeRoutingClick = actions.onAlternativeRoutingClick
                )
            }
            item {
                AppLanguageSettingItem(
                    appSettings = state.appSettings,
                    onAppLanguageClick = actions.onAppLanguageClick
                )
            }
            item {
                CombinedContactsSettingItem(
                    appSettings = state.appSettings,
                    onCombinedContactsClick = actions.onCombinedContactsClick
                )
            }
            item {
                ProtonSettingsItem(
                    name = stringResource(id = string.mail_settings_swipe_actions),
                    onClick = actions.onSwipeActionsClick
                )
                Divider()
            }
            item { TelemetrySettingToggleItem() }
            item { CrashReportSettingToggleItem() }
            item { ProtonSettingsHeader(title = string.mail_settings_app_information) }
            item {
                ProtonSettingsItem(
                    name = stringResource(id = string.mail_settings_app_version),
                    hint = state.appInformation.appVersionName,
                    isClickable = false
                )
            }
        }
    }
}

@Composable
private fun CombinedContactsSettingItem(
    modifier: Modifier = Modifier,
    appSettings: AppSettings,
    onCombinedContactsClick: () -> Unit
) {
    val hint = if (appSettings.hasCombinedContacts) {
        stringResource(id = string.mail_settings_enabled)
    } else {
        stringResource(id = string.mail_settings_disabled)
    }
    ProtonSettingsItem(
        modifier = modifier,
        name = stringResource(id = string.mail_settings_combined_contacts),
        hint = hint,
        onClick = onCombinedContactsClick
    )
    Divider()
}

@Composable
private fun AppLanguageSettingItem(
    modifier: Modifier = Modifier,
    appSettings: AppSettings,
    onAppLanguageClick: () -> Unit
) {
    val appLanguage = appSettings.customAppLanguage
        ?: stringResource(id = string.mail_settings_auto_detect)
    ProtonSettingsItem(
        modifier = modifier,
        name = stringResource(id = string.mail_settings_app_language),
        hint = appLanguage,
        onClick = onAppLanguageClick
    )
    Divider()
}

@Composable
private fun AlternativeRoutingSettingItem(
    modifier: Modifier = Modifier,
    appSettings: AppSettings,
    onAlternativeRoutingClick: () -> Unit
) {
    val hint = if (appSettings.hasAlternativeRouting) {
        stringResource(id = string.mail_settings_allowed)
    } else {
        stringResource(id = string.mail_settings_denied)
    }
    ProtonSettingsItem(
        modifier = modifier,
        name = stringResource(id = string.mail_settings_alternative_routing),
        hint = hint,
        onClick = onAlternativeRoutingClick
    )
    Divider()
}

@Composable
private fun AutoLockSettingItem(
    modifier: Modifier = Modifier,
    appSettings: AppSettings,
    onAutoLockClick: () -> Unit
) {
    val hint = if (appSettings.hasAutoLock) {
        stringResource(id = string.mail_settings_enabled)
    } else {
        stringResource(id = string.mail_settings_disabled)
    }
    ProtonSettingsItem(
        modifier = modifier,
        name = stringResource(id = string.mail_settings_auto_lock),
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
    val header = accountInfo?.name
        ?: stringResource(id = R.string.mail_settings_no_information_available)
    val hint = accountInfo?.email

    ProtonSettingsItem(
        modifier = modifier,
        name = header,
        hint = hint,
        onClick = onAccountClicked
    )
    Divider()
}

object MainSettingsScreen {

    data class Actions(
        val onAccountClick: () -> Unit,
        val onThemeClick: () -> Unit,
        val onPushNotificationsClick: () -> Unit,
        val onAutoLockClick: () -> Unit,
        val onAlternativeRoutingClick: () -> Unit,
        val onAppLanguageClick: () -> Unit,
        val onCombinedContactsClick: () -> Unit,
        val onSwipeActionsClick: () -> Unit,
        val onBackClick: () -> Unit
    )
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
fun PreviewMainSettingsScreen() {
    ProtonTheme {
        MainSettingsScreen(
            state = Data(
                AccountInfo("ProtonUser", "user@proton.ch"),
                AppSettings(
                    hasAutoLock = false,
                    hasAlternativeRouting = true,
                    customAppLanguage = null,
                    hasCombinedContacts = true
                ),
                AppInformation(appVersionName = "6.0.0-alpha")
            ),
            actions = MainSettingsScreen.Actions(
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
        )
    }
}
