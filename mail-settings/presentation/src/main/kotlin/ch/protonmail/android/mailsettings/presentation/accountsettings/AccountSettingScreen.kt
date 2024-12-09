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
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Divider
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.Scaffold
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import ch.protonmail.android.mailcommon.presentation.ConsumableLaunchedEffect
import ch.protonmail.android.mailcommon.presentation.ConsumableTextEffect
import ch.protonmail.android.mailsettings.presentation.R.string
import ch.protonmail.android.mailsettings.presentation.accountsettings.identity.model.AccountSettingsViewAction
import ch.protonmail.android.mailsettings.presentation.accountsettings.identity.upselling.AutoDeleteUpsellingBottomSheet
import ch.protonmail.android.mailupselling.presentation.model.BottomSheetVisibilityEffect
import ch.protonmail.android.mailupselling.presentation.ui.UpsellingIcon
import ch.protonmail.android.mailupselling.presentation.ui.bottomsheet.UpsellingBottomSheet.DELAY_SHOWING
import ch.protonmail.android.mailupselling.presentation.ui.screen.UpsellingScreen
import ch.protonmail.android.uicomponents.bottomsheet.bottomSheetHeightConstrainedContent
import ch.protonmail.android.uicomponents.settings.SettingsItem
import ch.protonmail.android.uicomponents.snackbar.DismissableSnackbarHost
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.proton.core.accountmanager.presentation.compose.SecurityKeysSettingsItem
import me.proton.core.compose.component.ProtonCenteredProgress
import me.proton.core.compose.component.ProtonErrorMessage
import me.proton.core.compose.component.ProtonModalBottomSheetLayout
import me.proton.core.compose.component.ProtonSettingsHeader
import me.proton.core.compose.component.ProtonSettingsItem
import me.proton.core.compose.component.ProtonSettingsList
import me.proton.core.compose.component.ProtonSettingsTopBar
import me.proton.core.compose.component.ProtonSnackbarHostState
import me.proton.core.compose.component.ProtonSnackbarType
import me.proton.core.compose.flow.rememberAsState
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.presentation.utils.formatByteToHumanReadable
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

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun AccountSettingScreen(
    modifier: Modifier = Modifier,
    state: AccountSettingsState.Data,
    actions: AccountSettingScreen.Actions,
    accountSettingsViewModel: AccountSettingsViewModel = hiltViewModel()
) {

    val snackbarHostState = remember { ProtonSnackbarHostState() }

    val bottomSheetState = rememberModalBottomSheetState(
        initialValue = ModalBottomSheetValue.Hidden,
        skipHalfExpanded = true
    )

    var showBottomSheet by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    DisposableEffect(Unit) {
        onDispose {
            if (bottomSheetState.currentValue != ModalBottomSheetValue.Hidden) {
                accountSettingsViewModel.submit(AccountSettingsViewAction.DismissUpselling)
            }
        }
    }

    LaunchedEffect(bottomSheetState.isVisible) {
        if (!bottomSheetState.isVisible && showBottomSheet) {
            accountSettingsViewModel.submit(AccountSettingsViewAction.DismissUpselling)
        }
    }

    BackHandler(bottomSheetState.isVisible) {
        accountSettingsViewModel.submit(AccountSettingsViewAction.DismissUpselling)
    }

    ProtonModalBottomSheetLayout(
        sheetState = bottomSheetState,
        sheetContent = bottomSheetHeightConstrainedContent {
            if (showBottomSheet) {
                AutoDeleteUpsellingBottomSheet(
                    actions = UpsellingScreen.Actions.Empty.copy(
                        onDismiss = { accountSettingsViewModel.submit(AccountSettingsViewAction.DismissUpselling) },
                        onUpgrade = { message ->
                            scope.launch {
                                snackbarHostState.showSnackbar(ProtonSnackbarType.NORM, message = message)
                            }
                        },
                        onError = { message ->
                            scope.launch {
                                snackbarHostState.showSnackbar(ProtonSnackbarType.ERROR, message = message)
                            }
                        }
                    )
                )
            }
        }
    ) {
        Scaffold(
            modifier = modifier.testTag(TEST_TAG_ACCOUNT_SETTINGS_SCREEN),
            topBar = {
                ProtonSettingsTopBar(
                    title = stringResource(id = string.mail_settings_account_settings),
                    onBackClick = actions.onBackClick
                )
            },
            snackbarHost = { DismissableSnackbarHost(protonSnackbarHostState = snackbarHostState) },
            content = { paddingValues ->
                ProtonSettingsList(
                    modifier
                        .padding(paddingValues)
                        .testTag(TEST_TAG_ACCOUNT_SETTINGS_LIST)
                ) {
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
                    if (state.securityKeysVisible) {
                        item {
                            SecurityKeysSettingsItem(
                                onSecurityKeysClick = actions.onSecurityKeysClick,
                                registeredSecurityKeys = state.registeredSecurityKeys
                            )
                            Divider()
                        }
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
                    if (state.autoDeleteSettingsState.isSettingVisible) {
                        item {
                            AutoDeleteSettingItem(
                                modifier = modifier,
                                state = state,
                                onAutoDeleteClick = {
                                    if (state.autoDeleteSettingsState.isUpsellingVisible ||
                                        state.autoDeleteSettingsState.doesSettingNeedSubscription
                                    ) {
                                        accountSettingsViewModel.submit(AccountSettingsViewAction.SettingsItemClicked)
                                    } else {
                                        actions.onAutoDeleteClick()
                                    }
                                }
                            )
                        }
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
                            name = stringResource(id = string.mail_settings_labels),
                            onClick = actions.onLabelsClick
                        )
                        Divider()
                    }
                    item {
                        ProtonSettingsItem(
                            name = stringResource(id = string.mail_settings_folders),
                            onClick = actions.onFoldersClick
                        )
                    }
                }
                ConsumableLaunchedEffect(
                    effect = state.autoDeleteSettingsState.upsellingVisibility
                ) { bottomSheetEffect ->
                    when (bottomSheetEffect) {
                        BottomSheetVisibilityEffect.Hide -> scope.launch {
                            bottomSheetState.hide()
                            showBottomSheet = false
                        }

                        BottomSheetVisibilityEffect.Show -> scope.launch {
                            showBottomSheet = true
                            delay(DELAY_SHOWING)
                            bottomSheetState.show()
                        }
                    }
                }
                ConsumableTextEffect(effect = state.autoDeleteSettingsState.upsellingInProgress) { message ->
                    snackbarHostState.snackbarHostState.currentSnackbarData?.dismiss()
                    snackbarHostState.showSnackbar(ProtonSnackbarType.NORM, message)
                }
                ConsumableTextEffect(effect = state.autoDeleteSettingsState.subscriptionNeededError) { message ->
                    snackbarHostState.snackbarHostState.currentSnackbarData?.dismiss()
                    snackbarHostState.showSnackbar(ProtonSnackbarType.NORM, message)
                }
            }
        )
    }
}

@Composable
private fun MailboxSizeItem(state: AccountSettingsState.Data) {
    val formattedSize = if (state.mailboxUsedSpace != null && state.mailboxSize != null) {
        "${state.mailboxUsedSpace.formatByteToHumanReadable()} / ${state.mailboxSize.formatByteToHumanReadable()}"
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

@Composable
fun AutoDeleteSettingItem(
    modifier: Modifier = Modifier,
    state: AccountSettingsState.Data,
    onAutoDeleteClick: () -> Unit
) {

    val hint = when (state.autoDeleteSettingsState.autoDeleteInDays) {
        null, 0 -> stringResource(id = string.mail_settings_disabled)
        else -> stringResource(id = string.mail_settings_enabled)
    }

    SettingsItem(
        modifier = modifier,
        name = stringResource(id = string.mail_settings_auto_delete),
        hint = hint,
        onClick = onAutoDeleteClick,
        upsellingIcon = {
            if (state.autoDeleteSettingsState.isUpsellingVisible) {
                UpsellingIcon(modifier = Modifier.padding(horizontal = ProtonDimens.SmallSpacing))
            }
        }
    )
    Divider()
}

object AccountSettingScreen {

    data class Actions(
        val onBackClick: () -> Unit,
        val onPasswordManagementClick: () -> Unit,
        val onRecoveryEmailClick: () -> Unit,
        val onSecurityKeysClick: () -> Unit,
        val onConversationModeClick: () -> Unit,
        val onDefaultEmailAddressClick: () -> Unit,
        val onDisplayNameClick: () -> Unit,
        val onPrivacyClick: () -> Unit,
        val onLabelsClick: () -> Unit,
        val onFoldersClick: () -> Unit,
        val onAutoDeleteClick: () -> Unit
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
            isConversationMode = true,
            registeredSecurityKeys = emptyList(),
            securityKeysVisible = true,
            autoDeleteSettingsState = AutoDeleteSettingsState(
                isSettingVisible = true,
                isUpsellingVisible = true,
                autoDeleteInDays = 0
            )
        ),
        actions = AccountSettingScreen.Actions(
            onBackClick = {},
            onPasswordManagementClick = {},
            onRecoveryEmailClick = {},
            onSecurityKeysClick = {},
            onConversationModeClick = {},
            onDefaultEmailAddressClick = {},
            onDisplayNameClick = {},
            onPrivacyClick = {},
            onLabelsClick = {},
            onFoldersClick = {},
            onAutoDeleteClick = {}
        )
    )
}
