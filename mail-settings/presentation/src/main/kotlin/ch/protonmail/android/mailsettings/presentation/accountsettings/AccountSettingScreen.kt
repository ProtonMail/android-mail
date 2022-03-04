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

import androidx.compose.material.Divider
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Modifier.Companion
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
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

@Composable
fun AccountSettingScreen(
    modifier: Modifier = Companion,
    onBackClick: () -> Unit,
    onSubscriptionClick: () -> Unit,
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
            onBackClick = onBackClick,
            onSubscriptionClick = onSubscriptionClick,
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
    modifier: Modifier = Companion,
    onBackClick: () -> Unit,
    onSubscriptionClick: () -> Unit,
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
                title = stringResource(id = R.string.account_settings),
                onBackClick = onBackClick
            )
        },
        content = {
            ProtonSettingsList {
                item { ProtonSettingsHeader(title = R.string.account) }
                item {
                    ProtonSettingsItem(
                        name = stringResource(id = R.string.subscription),
                        hint = state.currentPlan,
                        onClick = onSubscriptionClick
                    )
                    Divider()
                }
                item {
                    ProtonSettingsItem(
                        name = stringResource(id = R.string.password_management),
                        onClick = onPasswordManagementClick
                    )
                    Divider()
                }
                item {
                    ProtonSettingsItem(
                        name = stringResource(id = R.string.recovery_email),
                        hint = state.recoveryEmail ?: stringResource(id = R.string.not_set),
                        onClick = onRecoveryEmailClick
                    )
                    Divider()
                }
                item {
                    ProtonSettingsItem(
                        name = stringResource(id = R.string.mailbox_size),
                        hint = state.mailboxSize,
                        isClickable = false
                    )
                    Divider()
                }
                item {
                    ConversationModeSettingItem(
                        state = state,
                        onConversationModeClick = onConversationModeClick
                    )
                }

                item { ProtonSettingsHeader(title = R.string.addresses) }
                item {
                    ProtonSettingsItem(
                        name = stringResource(id = R.string.default_email_address),
                        hint = state.defaultEmail,
                        onClick = onDefaultEmailAddressClick
                    )
                    Divider()
                }
                item {
                    ProtonSettingsItem(
                        name = stringResource(id = R.string.display_name_and_signature),
                        onClick = onDisplayNameClick
                    )
                    Divider()
                }

                item { ProtonSettingsHeader(title = R.string.mailbox) }
                item {
                    ProtonSettingsItem(
                        name = stringResource(id = R.string.privacy),
                        onClick = onPrivacyClick
                    )
                    Divider()
                }
                item {
                    ProtonSettingsItem(
                        name = stringResource(id = R.string.search_message_content),
                        onClick = onSearchMessageContentClick
                    )
                    Divider()
                }
                item {
                    ProtonSettingsItem(
                        name = stringResource(id = R.string.labels_and_folders),
                        onClick = onLabelsFoldersClick
                    )
                    Divider()
                }
                item {
                    ProtonSettingsItem(
                        name = stringResource(id = R.string.local_storage),
                        onClick = onLocalStorageClick
                    )
                    Divider()
                }

                item { ProtonSettingsHeader(title = R.string.snooze) }
                item {
                    ProtonSettingsItem(
                        name = stringResource(id = R.string.snooze_notifications),
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
private fun ConversationModeSettingItem(
    modifier: Modifier = Modifier,
    state: Data,
    onConversationModeClick: () -> Unit
) {
    val hint = if (state.isConversationMode) {
        stringResource(id = R.string.enabled)
    } else {
        stringResource(id = R.string.disabled)
    }
    ProtonSettingsItem(
        modifier = modifier,
        name = stringResource(id = R.string.conversation_mode),
        hint = hint,
        onClick = onConversationModeClick
    )
    Divider()
}
