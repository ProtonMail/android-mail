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

import androidx.compose.material.Divider
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Modifier.Companion
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import me.proton.core.compose.component.ProtonSettingsHeader
import me.proton.core.compose.component.ProtonSettingsItem
import me.proton.core.compose.component.ProtonSettingsList
import me.proton.core.compose.component.ProtonSettingsTopBar

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
    onSnoozeNotificationsClick: () -> Unit
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
                        hint = "ProtonMail Visionary",
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
                        hint = "recovery@proton.ch",
                        onClick = onRecoveryEmailClick
                    )
                    Divider()
                }
                item {
                    ProtonSettingsItem(
                        name = stringResource(id = R.string.mailbox_size),
                        hint = "32 MB / 20 GB",
                        isClickable = false
                    )
                    Divider()
                }
                item {
                    ProtonSettingsItem(
                        name = stringResource(id = R.string.conversation_mode),
                        hint = "Enabled",
                        onClick = onConversationModeClick
                    )
                    Divider()
                }

                item { ProtonSettingsHeader(title = R.string.addresses) }
                item {
                    ProtonSettingsItem(
                        name = stringResource(id = R.string.default_email_address),
                        hint = "default@proton.ch",
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
