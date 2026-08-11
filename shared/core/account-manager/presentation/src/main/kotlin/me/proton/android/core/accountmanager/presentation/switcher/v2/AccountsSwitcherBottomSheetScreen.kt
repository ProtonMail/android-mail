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

package me.proton.android.core.accountmanager.presentation.switcher.v2

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ch.protonmail.android.design.compose.component.ProtonHorizontallyCenteredProgress
import ch.protonmail.android.design.compose.theme.LocalColors
import ch.protonmail.android.design.compose.theme.LocalTypography
import ch.protonmail.android.design.compose.theme.ProtonDimens
import ch.protonmail.android.design.compose.theme.ProtonTheme
import ch.protonmail.android.design.compose.theme.bodyMediumWeak
import ch.protonmail.android.design.compose.theme.bodySmallWeak
import me.proton.android.core.account.domain.model.CoreUserId
import me.proton.android.core.accountmanager.presentation.AccountDimens
import me.proton.android.core.accountmanager.presentation.ButtonWithIconAndText
import me.proton.android.core.accountmanager.presentation.ButtonWithIconAndTextAndCustom
import me.proton.android.core.accountmanager.presentation.R
import me.proton.android.core.accountmanager.presentation.manager.AccountsManagerState
import me.proton.android.core.accountmanager.presentation.manager.AccountsManagerViewModel
import me.proton.android.core.accountmanager.presentation.switcher.v1.AccountItem
import me.proton.android.core.accountmanager.presentation.switcher.v1.AccountListItem
import me.proton.android.core.accountmanager.presentation.switcher.v1.AccountSwitchEvent
import me.proton.core.util.kotlin.takeIfNotBlank

@Composable
fun AccountsSwitcherBottomSheetScreen(
    modifier: Modifier = Modifier,
    onEvent: (AccountSwitchEvent) -> Unit,
    viewModel: AccountsManagerViewModel = hiltViewModel(),
    upsellItem: @Composable () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    AccountsSwitcherBottomSheetScreen(
        state = state,
        modifier = modifier,
        onEvent = onEvent,
        upsellItem = upsellItem
    )
}

@Composable
fun AccountsSwitcherBottomSheetScreen(
    state: AccountsManagerState,
    modifier: Modifier = Modifier,
    onEvent: (AccountSwitchEvent) -> Unit = {},
    upsellItem: @Composable () -> Unit = {}
) {
    Box {
        when (state) {
            is AccountsManagerState.Loading -> ProtonHorizontallyCenteredProgress()
            is AccountsManagerState.Idle -> {
                Column(
                    modifier = modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CurrentAccountSection(
                        currentAccount = state.signedInAccounts.filterIsInstance<AccountListItem.Ready.Primary>()
                            .first(),
                        onEvent = onEvent
                    )

                    Spacer(Modifier.size(ProtonDimens.Spacing.Large))

                    Box(modifier = Modifier.padding(horizontal = ProtonDimens.Spacing.Medium)) {
                        upsellItem()
                    }

                    OtherAccountsSection(
                        signedInAccounts = state.signedInAccounts.filterNot { it is AccountListItem.Ready.Primary },
                        signedOutAccounts = state.disabledAccounts,
                        onEvent = onEvent
                    )

                    Spacer(Modifier.size(ProtonDimens.Spacing.Large))

                    Options(
                        currentAccount = state.signedInAccounts.filterIsInstance<AccountListItem.Ready.Primary>()
                            .first(),
                        onEvent = onEvent
                    )

                    Spacer(
                        modifier = Modifier.height(
                            WindowInsets
                                .navigationBars
                                .only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal)
                                .asPaddingValues()
                                .calculateBottomPadding()
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun CurrentAccountSection(
    modifier: Modifier = Modifier,
    currentAccount: AccountListItem,
    onEvent: (AccountSwitchEvent) -> Unit
) {
    val talkbackTitle = stringResource(
        R.string.account_switcher_talkback_title
    )
    Column(
        modifier = modifier
            .semantics(mergeDescendants = true) {
                contentDescription = talkbackTitle
            }
            .padding(horizontal = ProtonDimens.Spacing.Standard),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CurrentAccountAvatar(
            modifier = modifier,
            currentAccount = currentAccount,
            onEvent = onEvent
        )

        Text(
            text = currentAccount.accountItem.name,
            color = ProtonTheme.colors.textNorm,
            textAlign = TextAlign.Center,
            style = LocalTypography.current.titleLarge
        )

        currentAccount.accountItem.email?.takeIfNotBlank()?.let {
            Spacer(modifier = Modifier.height(ProtonDimens.Spacing.Compact))
            Text(
                text = it,
                color = ProtonTheme.colors.textWeak,
                textAlign = TextAlign.Center,
                style = LocalTypography.current.bodyMediumWeak
            )
        }
    }
}

@Composable
private fun CurrentAccountAvatar(
    modifier: Modifier = Modifier,
    currentAccount: AccountListItem,
    onEvent: (AccountSwitchEvent) -> Unit
) {
    val cameraVisible by remember { mutableStateOf(false) } // remove when avatar image implemented
    Box(
        modifier = modifier
            .padding(ProtonDimens.Spacing.MediumLight)
    ) {
        PrimaryAccountAvatar(
            modifier = modifier,
            initials = currentAccount.accountItem.initials ?: stringResource(R.string.account_switcher_no_initials),
            backgroundColor = currentAccount.accountItem.color
        )

        if (cameraVisible) {
            Box(
                modifier = Modifier.align(Alignment.BottomEnd)
            ) {
                Image(
                    modifier = modifier
                        .sizeIn(
                            minWidth = AccountDimens.PrimaryAccountAvatarCameraButtonSize,
                            minHeight = AccountDimens.PrimaryAccountAvatarCameraButtonSize
                        )
                        .clip(shape = CircleShape)
                        .background(
                            color = ProtonTheme.colors.backgroundSecondary,
                            shape = CircleShape
                        )
                        .border(
                            color = ProtonTheme.colors.borderStrong,
                            width = ProtonDimens.Spacing.Tiny,
                            shape = CircleShape
                        )
                        .padding(ProtonDimens.Spacing.Standard)
                        .clickable(
                            enabled = true,
                            onClick = { onEvent(AccountSwitchEvent.OnSetPrimaryAccountAvatar) }
                        ),
                    painter = painterResource(id = R.drawable.ic_proton_camera),
                    contentDescription = stringResource(id = R.string.primary_account_avatar_camera_content_description)
                )
            }
        }
    }
}

@Composable
private fun OtherAccountsSection(
    modifier: Modifier = Modifier,
    signedInAccounts: List<AccountListItem>,
    signedOutAccounts: List<AccountListItem>,
    onEvent: (AccountSwitchEvent) -> Unit
) {
    if (signedInAccounts.isNotEmpty() || signedOutAccounts.isNotEmpty()) {
        Text(
            color = LocalColors.current.textNorm,
            style = LocalTypography.current.titleMedium,
            textAlign = TextAlign.Start,
            text = stringResource(R.string.manage_accounts_switch_to),
            modifier = Modifier
                .padding(
                    bottom = ProtonDimens.Spacing.Medium,
                    top = ProtonDimens.Spacing.Standard,
                    start = ProtonDimens.Spacing.Medium
                )
                .fillMaxWidth()
        )
    }
    Card(
        shape = RoundedCornerShape(AccountDimens.AccountCardRadius),
        modifier = modifier
            .padding(horizontal = ProtonDimens.Spacing.Medium)
            .fillMaxWidth(),
        colors = CardDefaults.cardColors().copy(
            containerColor = ProtonTheme.colors.backgroundInvertedSecondary
        )
    ) {
        Column(modifier = modifier) {
            signedInAccounts.forEach { account ->
                SignedInAccountRow(
                    accountListItem = account,
                    onEvent = onEvent
                )
            }
            signedOutAccounts.forEach { account ->
                SignedOutAccountRow(
                    accountListItem = account,
                    onEvent = onEvent
                )
            }
        }
    }
}

@Composable
private fun SignedInAccountRow(
    accountListItem: AccountListItem,
    modifier: Modifier = Modifier,
    onEvent: (AccountSwitchEvent) -> Unit = {}
) {
    RowForSignedInAccountWithCounter(
        accountListItem = accountListItem,
        modifier = modifier,
        onEvent = onEvent
    )
    HorizontalDivider(thickness = 1.dp, color = ProtonTheme.colors.separatorNorm)
}

@Composable
private fun SignedOutAccountRow(
    accountListItem: AccountListItem,
    modifier: Modifier = Modifier,
    onEvent: (AccountSwitchEvent) -> Unit = {}
) {
    RowForSignedOutAccount(
        modifier = modifier,
        accountListItem = accountListItem,
        onEvent = onEvent
    )
    HorizontalDivider(thickness = 1.dp, color = ProtonTheme.colors.separatorNorm)
}

@Composable
private fun Options(
    modifier: Modifier = Modifier,
    currentAccount: AccountListItem,
    onEvent: (AccountSwitchEvent) -> Unit
) {
    Text(
        color = LocalColors.current.textNorm,
        style = LocalTypography.current.titleMedium,
        textAlign = TextAlign.Start,
        text = stringResource(R.string.manage_accounts_options),
        modifier = Modifier
            .padding(
                bottom = ProtonDimens.Spacing.Medium,
                top = ProtonDimens.Spacing.Standard,
                start = ProtonDimens.Spacing.Medium
            )
            .fillMaxWidth()
    )
    Card(
        shape = RoundedCornerShape(AccountDimens.AccountCardRadius),
        modifier = modifier
            .padding(horizontal = ProtonDimens.Spacing.Medium)
            .fillMaxWidth(),
        colors = CardDefaults.cardColors().copy(
            containerColor = ProtonTheme.colors.backgroundInvertedSecondary
        )
    ) {
        Column(modifier = modifier) {
            AddAnotherAccountButton(onEvent = onEvent)

            HorizontalDivider(thickness = 1.dp, color = ProtonTheme.colors.separatorNorm)

            ManageAccountsOnDeviceButton(onEvent = onEvent)

            HorizontalDivider(thickness = 1.dp, color = ProtonTheme.colors.separatorNorm)
        }
    }

    Spacer(Modifier.padding(top = ProtonDimens.Spacing.ModeratelyLarge))

    Card(
        shape = RoundedCornerShape(AccountDimens.AccountCardRadius),
        modifier = modifier
            .padding(horizontal = ProtonDimens.Spacing.Medium)
            .fillMaxWidth(),
        colors = CardDefaults.cardColors().copy(
            containerColor = ProtonTheme.colors.backgroundInvertedSecondary
        )
    ) {
        SignOutButton(currentAccount = currentAccount, onEvent = onEvent)
    }
}

@Composable
private fun SignOutButton(
    modifier: Modifier = Modifier,
    currentAccount: AccountListItem,
    onEvent: (AccountSwitchEvent) -> Unit
) {
    ButtonWithIconAndTextAndCustom(
        modifier = modifier,
        text = R.string.account_switcher_sign_out,
        icon = R.drawable.ic_proton_arrow_out_from_rectangle,
        onClick = { onEvent(AccountSwitchEvent.OnSignOut(currentAccount.accountItem.userId)) }
    ) {
        currentAccount.accountItem.email?.let {
            Text(
                style = ProtonTheme.typography.bodySmallWeak,
                text = it
            )
        }
    }
}

@Composable
private fun ManageAccountsOnDeviceButton(modifier: Modifier = Modifier, onEvent: (AccountSwitchEvent) -> Unit) {
    ButtonWithIconAndText(
        modifier = modifier,
        text = R.string.manage_accounts_on_device,
        icon = R.drawable.ic_proton_cog_wheel,
        onClick = { onEvent(AccountSwitchEvent.OnManageAccounts) }
    )
}

@Composable
private fun AddAnotherAccountButton(modifier: Modifier = Modifier, onEvent: (AccountSwitchEvent) -> Unit) {
    ButtonWithIconAndText(
        modifier = modifier,
        text = R.string.add_another_account,
        icon = R.drawable.ic_proton_user_plus,
        onClick = { onEvent(AccountSwitchEvent.OnAddAccount) }
    )
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
@Composable
private fun CurrentAccountPreview() {
    CurrentAccountSection(
        currentAccount = AccountListItem.Ready.Primary(
            AccountItem(CoreUserId("user-1"), "User One", "user.one@example.test", "U1")
        ),
        onEvent = {}
    )
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
@Composable
private fun CurrentAccountAvatarPreview() {
    CurrentAccountAvatar(
        currentAccount = AccountListItem.Ready.Primary(
            AccountItem(CoreUserId("user-1"), "User One", "user.one@example.test", "U1")
        ),
        onEvent = {}
    )
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
@Composable
private fun AccountsSwitcherScreenPreview() {
    AccountsSwitcherBottomSheetScreen(
        modifier = Modifier,
        state = AccountsManagerState.Idle(
            signedInAccounts = listOf(
                AccountListItem.Ready.Primary(
                    AccountItem(CoreUserId("user-1"), "User One", "user.one@example.test", "U1")
                ),
                AccountListItem.Ready(
                    AccountItem(CoreUserId("user-2"), "User Two", "user.two@example.test", "U2")
                ),
                AccountListItem.Ready(
                    AccountItem(CoreUserId("user-3"), "User Three", "user.three@example.test", "U3")
                )
            ),
            disabledAccounts = listOf(
                AccountListItem.Disabled(
                    AccountItem(CoreUserId("user-4"), "User Four", "user.four@example.test", "U4")
                )
            )
        ),
        onEvent = {}
    )
}


@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
@Composable
private fun SignedInAccountPreview() {
    OtherAccountsSection(
        modifier = Modifier,
        signedInAccounts = listOf(
            AccountListItem.Ready.Primary(
                AccountItem(CoreUserId("user-1"), "User One", "user.one@example.test", "U1")
            ),
            AccountListItem.Ready(
                AccountItem(CoreUserId("user-2"), "User Two", "user.two@example.test", "U1")
            )
        ),
        signedOutAccounts = listOf(
            AccountListItem.Disabled(
                AccountItem(CoreUserId("user-3"), "User Three", "user.three@example.test", "U3")
            )
        ),
        onEvent = {}
    )
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
@Composable
private fun NoOtherAccountsPreview() {
    AccountsSwitcherBottomSheetScreen(
        modifier = Modifier,
        state = AccountsManagerState.Idle(
            signedInAccounts = listOf(
                AccountListItem.Ready.Primary(
                    AccountItem(CoreUserId("user-1"), "User One", "user.one@example.test", "U1")
                )
            ),
            disabledAccounts = emptyList()
        ),
        onEvent = {}
    )
}
