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

package ch.protonmail.android.mailsettings.presentation.accountsettings

import android.content.res.Configuration.UI_MODE_NIGHT_NO
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import android.text.format.Formatter.formatShortFileSize
import androidx.compose.material.Divider
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import ch.protonmail.android.mailsettings.presentation.R
import ch.protonmail.android.mailsettings.presentation.accountsettings.AccountSettingsState.Data
import ch.protonmail.android.mailsettings.presentation.accountsettings.AccountSettingsState.Loading
import me.proton.core.compose.component.ProtonSettingsHeader
import me.proton.core.compose.component.ProtonSettingsItem
import me.proton.core.compose.component.ProtonSettingsList
import me.proton.core.compose.component.ProtonSettingsTopBar
import me.proton.core.compose.flow.rememberAsState

const val TEST_TAG_ACCOUNT_SETTINGS_SCREEN = "AccountSettingsScreenTestTag"
const val TEST_TAG_ACCOUNT_SETTINGS_LIST = "AccountSettingsListTestTag"

@Composable
fun AccountSettingScreen(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit,
    onPasswordManagementClick: () -> Unit,
    onRecoveryEmailClick: () -> Unit,
    onConversationModeClick: () -> Unit,
    onDefaultEmailAddressClick: () -> Unit,
    onDisplayNameClick: () -> Unit,
    onPrivacyClick: () -> Unit,
    onSearchMessageContentClick: () -> Unit,
    onLabelsFoldersClick: () -> Unit,
    onLocalStorageClick: () -> Unit,
    onSnoozeNotificationsClick: () -> Unit,
    accountSettingsViewModel: AccountSettingsViewModel = hiltViewModel()
) {
    when (
        val settingsState = rememberAsState(
            flow = accountSettingsViewModel.state,
            Loading
        ).value
    ) {
        is Data -> AccountSettingScreen(
            modifier = modifier,
            onBackClick = onBackClick,
            onPasswordManagementClick = onPasswordManagementClick,
            onRecoveryEmailClick = onRecoveryEmailClick,
            onConversationModeClick = onConversationModeClick,
            onDefaultEmailAddressClick = onDefaultEmailAddressClick,
            onDisplayNameClick = onDisplayNameClick,
            onPrivacyClick = onPrivacyClick,
            onSearchMessageContentClick = onSearchMessageContentClick,
            onLabelsFoldersClick = onLabelsFoldersClick,
            onLocalStorageClick = onLocalStorageClick,
            onSnoozeNotificationsClick = onSnoozeNotificationsClick,
            state = settingsState
        )
        is Loading -> Unit
    }
}

@Composable
fun AccountSettingScreen(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit,
    onPasswordManagementClick: () -> Unit,
    onRecoveryEmailClick: () -> Unit,
    onConversationModeClick: () -> Unit,
    onDefaultEmailAddressClick: () -> Unit,
    onDisplayNameClick: () -> Unit,
    onPrivacyClick: () -> Unit,
    onSearchMessageContentClick: () -> Unit,
    onLabelsFoldersClick: () -> Unit,
    onLocalStorageClick: () -> Unit,
    onSnoozeNotificationsClick: () -> Unit,
    state: Data
) {
    Scaffold(
        modifier = modifier.testTag(TEST_TAG_ACCOUNT_SETTINGS_SCREEN),
        topBar = {
            ProtonSettingsTopBar(
                title = stringResource(id = R.string.mail_settings_account_settings),
                onBackClick = onBackClick
            )
        },
        content = {
            ProtonSettingsList(modifier.testTag(TEST_TAG_ACCOUNT_SETTINGS_LIST)) {
                item { ProtonSettingsHeader(title = R.string.mail_settings_account) }
                item {
                    ProtonSettingsItem(
                        name = stringResource(id = R.string.mail_settings_password_management),
                        onClick = onPasswordManagementClick
                    )
                    Divider()
                }
                item {
                    ProtonSettingsItem(
                        name = stringResource(id = R.string.mail_settings_recovery_email),
                        hint = state.recoveryEmail
                            ?: stringResource(id = R.string.mail_settings_not_set),
                        onClick = onRecoveryEmailClick
                    )
                    Divider()
                }
                item {
                    MailboxSizeItem(state)
                }
                item {
                    ConversationModeSettingItem(
                        state = state,
                        onConversationModeClick = onConversationModeClick
                    )
                }

                item { ProtonSettingsHeader(title = R.string.mail_settings_addresses) }
                item {
                    ProtonSettingsItem(
                        name = stringResource(id = R.string.mail_settings_default_email_address),
                        hint = state.defaultEmail
                            ?: stringResource(id = R.string.mail_settings_no_information_available),
                        onClick = onDefaultEmailAddressClick
                    )
                    Divider()
                }
                item {
                    ProtonSettingsItem(
                        name = stringResource(id = R.string.mail_settings_display_name_and_signature),
                        onClick = onDisplayNameClick
                    )
                    Divider()
                }

                item { ProtonSettingsHeader(title = R.string.mail_settings_mailbox) }
                item {
                    ProtonSettingsItem(
                        name = stringResource(id = R.string.mail_settings_privacy),
                        onClick = onPrivacyClick
                    )
                    Divider()
                }
                item {
                    ProtonSettingsItem(
                        name = stringResource(id = R.string.mail_settings_search_message_content),
                        onClick = onSearchMessageContentClick
                    )
                    Divider()
                }
                item {
                    ProtonSettingsItem(
                        name = stringResource(id = R.string.mail_settings_labels_and_folders),
                        onClick = onLabelsFoldersClick
                    )
                    Divider()
                }
                item {
                    ProtonSettingsItem(
                        name = stringResource(id = R.string.mail_settings_local_storage),
                        onClick = onLocalStorageClick
                    )
                    Divider()
                }

                item { ProtonSettingsHeader(title = R.string.mail_settings_snooze) }
                item {
                    ProtonSettingsItem(
                        name = stringResource(id = R.string.mail_settings_snooze_notifications),
                        hint = "(Off)",
                        onClick = onSnoozeNotificationsClick
                    )
                    Divider()
                }
            }
        }
    )
}

@Composable
private fun MailboxSizeItem(state: Data) {
    val formattedSize = if (state.mailboxUsedSpace != null && state.mailboxSize != null) {
        "${formatFileSize(state.mailboxUsedSpace)} / ${formatFileSize(state.mailboxSize)}"
    } else {
        stringResource(id = R.string.mail_settings_no_information_available)
    }
    ProtonSettingsItem(
        name = stringResource(id = R.string.mail_settings_mailbox_size),
        hint = formattedSize,
        isClickable = false
    )
    Divider()
}

@Composable
private fun ConversationModeSettingItem(
    modifier: Modifier = Modifier,
    state: Data,
    onConversationModeClick: () -> Unit
) {
    val hint = state.isConversationMode?.let { isConversationEnabled ->
        if (isConversationEnabled) {
            stringResource(id = R.string.mail_settings_enabled)
        } else {
            stringResource(id = R.string.mail_settings_disabled)
        }
    } ?: stringResource(id = R.string.mail_settings_no_information_available)
    ProtonSettingsItem(
        modifier = modifier,
        name = stringResource(id = R.string.mail_settings_conversation_mode),
        hint = hint,
        onClick = onConversationModeClick
    )
    Divider()
}

/**
 * Formats the given parameter to a suitable file size measurement.
 * Please note that this will yield different values depending on the
 * android version it is invoked on, since on android N and earlier
 * the calculation is done considering `1kB = 1024B`
 * while on newer versions it's done using `1kB = 1000B`
 *
 * See [formatShortFileSize] docs for more details
 */
@Composable
private fun formatFileSize(mailboxUsedSpace: Long) = formatShortFileSize(
    LocalContext.current,
    mailboxUsedSpace
)

@Preview(
    name = "Account settings screen light mode",
    showBackground = true,
    uiMode = UI_MODE_NIGHT_NO
)
@Preview(
    name = "Account settings screen dark mode",
    showBackground = true,
    uiMode = UI_MODE_NIGHT_YES
)
@Composable
fun previewAccountSettingsScreen() {
    AccountSettingScreen(
        onBackClick = {},
        onPasswordManagementClick = {},
        onRecoveryEmailClick = {},
        onConversationModeClick = {},
        onDefaultEmailAddressClick = {},
        onDisplayNameClick = {},
        onPrivacyClick = {},
        onSearchMessageContentClick = {},
        onLabelsFoldersClick = {},
        onLocalStorageClick = {},
        onSnoozeNotificationsClick = {},
        state = Data(
            recoveryEmail = "recovery@protonmail.com",
            mailboxSize = 20_000,
            mailboxUsedSpace = 4000,
            defaultEmail = "hello@protonmail.ch",
            isConversationMode = true
        )
    )
}
