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

package ch.protonmail.android.mailsettings.presentation.accountsettings

import android.content.res.Configuration.UI_MODE_NIGHT_NO
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import android.text.format.Formatter.formatShortFileSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Divider
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import ch.protonmail.android.mailsettings.presentation.R.string
import me.proton.core.compose.component.ProtonCenteredProgress
import me.proton.core.compose.component.ProtonErrorMessage
import me.proton.core.compose.component.ProtonSettingsHeader
import me.proton.core.compose.component.ProtonSettingsItem
import me.proton.core.compose.component.ProtonSettingsList
import me.proton.core.compose.component.ProtonSettingsTopBar
import me.proton.core.compose.flow.rememberAsState
import me.proton.core.util.kotlin.exhaustive
import ch.protonmail.android.mailcommon.presentation.R.string as commonString

const val TEST_TAG_ACCOUNT_SETTINGS_SCREEN = "AccountSettingsScreenTestTag"
const val TEST_TAG_ACCOUNT_SETTINGS_LIST = "AccountSettingsListTestTag"


@Composable
fun AccountSettingScreen(
    actions: AccountSettingScreen.Actions,
    modifier: Modifier = Modifier,
    accountSettingsViewModel: AccountSettingsViewModel = hiltViewModel()
) {
    when (
        val settingsState = rememberAsState(
            flow = accountSettingsViewModel.state,
            AccountSettingsState.Loading
        ).value
    ) {
        is AccountSettingsState.Data -> AccountSettingScreen(
            modifier = modifier,
            state = settingsState,
            actions = actions
        )
        AccountSettingsState.Loading -> ProtonCenteredProgress()
        AccountSettingsState.NotLoggedIn ->
            ProtonErrorMessage(errorMessage = stringResource(id = commonString.x_error_not_logged_in))
    }.exhaustive
}

@Composable
fun AccountSettingScreen(
    modifier: Modifier = Modifier,
    state: AccountSettingsState.Data,
    actions: AccountSettingScreen.Actions
) {
    Scaffold(
        modifier = modifier.testTag(TEST_TAG_ACCOUNT_SETTINGS_SCREEN),
        topBar = {
            ProtonSettingsTopBar(
                title = stringResource(id = string.mail_settings_account_settings),
                onBackClick = actions.onBackClick
            )
        },
        content = { paddingValues ->
            ProtonSettingsList(
                modifier
                    .padding(paddingValues)
                    .testTag(TEST_TAG_ACCOUNT_SETTINGS_LIST)) {
                item { ProtonSettingsHeader(title = string.mail_settings_account) }
                item {
                    ProtonSettingsItem(
                        name = stringResource(id = string.mail_settings_password_management),
                        onClick = actions.onPasswordManagementClick
                    )
                    Divider()
                }
                item {
                    ProtonSettingsItem(
                        name = stringResource(id = string.mail_settings_recovery_email),
                        hint = state.recoveryEmail
                            ?: stringResource(id = string.mail_settings_not_set),
                        onClick = actions.onRecoveryEmailClick
                    )
                    Divider()
                }
                item {
                    MailboxSizeItem(state)
                }
                item {
                    ConversationModeSettingItem(
                        state = state,
                        onConversationModeClick = actions.onConversationModeClick
                    )
                }

                item { ProtonSettingsHeader(title = string.mail_settings_addresses) }
                item {
                    ProtonSettingsItem(
                        name = stringResource(id = string.mail_settings_default_email_address),
                        hint = state.defaultEmail
                            ?: stringResource(id = string.mail_settings_no_information_available),
                        onClick = actions.onDefaultEmailAddressClick
                    )
                    Divider()
                }
                item {
                    ProtonSettingsItem(
                        name = stringResource(id = string.mail_settings_display_name_and_signature),
                        onClick = actions.onDisplayNameClick
                    )
                    Divider()
                }

                item { ProtonSettingsHeader(title = string.mail_settings_mailbox) }
                item {
                    ProtonSettingsItem(
                        name = stringResource(id = string.mail_settings_privacy),
                        onClick = actions.onPrivacyClick
                    )
                    Divider()
                }
                item {
                    ProtonSettingsItem(
                        name = stringResource(id = string.mail_settings_search_message_content),
                        onClick = actions.onSearchMessageContentClick
                    )
                    Divider()
                }
                item {
                    ProtonSettingsItem(
                        name = stringResource(id = string.mail_settings_labels_and_folders),
                        onClick = actions.onLabelsFoldersClick
                    )
                    Divider()
                }
                item {
                    ProtonSettingsItem(
                        name = stringResource(id = string.mail_settings_local_storage),
                        onClick = actions.onLocalStorageClick
                    )
                    Divider()
                }

                item { ProtonSettingsHeader(title = string.mail_settings_snooze) }
                item {
                    ProtonSettingsItem(
                        name = stringResource(id = string.mail_settings_snooze_notifications),
                        hint = "(Off)",
                        onClick = actions.onSnoozeNotificationsClick
                    )
                    Divider()
                }
            }
        }
    )
}

@Composable
private fun MailboxSizeItem(state: AccountSettingsState.Data) {
    val formattedSize = if (state.mailboxUsedSpace != null && state.mailboxSize != null) {
        "${formatFileSize(state.mailboxUsedSpace)} / ${formatFileSize(state.mailboxSize)}"
    } else {
        stringResource(id = string.mail_settings_no_information_available)
    }
    ProtonSettingsItem(
        name = stringResource(id = string.mail_settings_mailbox_size),
        hint = formattedSize,
        isClickable = false
    )
    Divider()
}

@Composable
private fun ConversationModeSettingItem(
    modifier: Modifier = Modifier,
    state: AccountSettingsState.Data,
    onConversationModeClick: () -> Unit
) {
    val hint = state.isConversationMode?.let { isConversationEnabled ->
        if (isConversationEnabled) {
            stringResource(id = string.mail_settings_enabled)
        } else {
            stringResource(id = string.mail_settings_disabled)
        }
    } ?: stringResource(id = string.mail_settings_no_information_available)
    ProtonSettingsItem(
        modifier = modifier,
        name = stringResource(id = string.mail_settings_conversation_mode),
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

object AccountSettingScreen {

    data class Actions(
        val onBackClick: () -> Unit,
        val onPasswordManagementClick: () -> Unit,
        val onRecoveryEmailClick: () -> Unit,
        val onConversationModeClick: () -> Unit,
        val onDefaultEmailAddressClick: () -> Unit,
        val onDisplayNameClick: () -> Unit,
        val onPrivacyClick: () -> Unit,
        val onSearchMessageContentClick: () -> Unit,
        val onLabelsFoldersClick: () -> Unit,
        val onLocalStorageClick: () -> Unit,
        val onSnoozeNotificationsClick: () -> Unit
    )
}

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
fun AccountSettingsScreenPreview() {
    AccountSettingScreen(
        state = AccountSettingsState.Data(
            recoveryEmail = "recovery@protonmail.com",
            mailboxSize = 20_000,
            mailboxUsedSpace = 4000,
            defaultEmail = "hello@protonmail.ch",
            isConversationMode = true
        ),
        actions = AccountSettingScreen.Actions(
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
            onSnoozeNotificationsClick = {}
        )
    )
}
