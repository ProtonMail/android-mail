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

package ch.protonmail.android.mailsidebar.presentation

import android.annotation.SuppressLint
import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Divider
import androidx.compose.material.DrawerState
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import ch.protonmail.android.mailcommon.domain.AppInformation
import ch.protonmail.android.maillabel.presentation.MailLabelsUiModel
import ch.protonmail.android.maillabel.presentation.sidebar.SidebarLabelAction
import ch.protonmail.android.maillabel.presentation.sidebar.SidebarUpsellItem
import ch.protonmail.android.maillabel.presentation.sidebar.sidebarFolderItems
import ch.protonmail.android.maillabel.presentation.sidebar.sidebarLabelItems
import ch.protonmail.android.maillabel.presentation.sidebar.sidebarSystemLabelItems
import ch.protonmail.android.mailsidebar.presentation.SidebarViewModel.State.Disabled
import ch.protonmail.android.mailsidebar.presentation.SidebarViewModel.State.Enabled
import kotlinx.coroutines.launch
import me.proton.core.accountmanager.presentation.compose.AccountPrimaryItem
import me.proton.core.compose.component.ProtonSidebarAppVersionItem
import me.proton.core.compose.component.ProtonSidebarItem
import me.proton.core.compose.component.ProtonSidebarLazy
import me.proton.core.compose.component.ProtonSidebarReportBugItem
import me.proton.core.compose.component.ProtonSidebarSettingsItem
import me.proton.core.compose.component.ProtonSidebarSignOutItem
import me.proton.core.compose.component.ProtonSidebarSubscriptionItem
import me.proton.core.compose.flow.rememberAsState
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.domain.entity.UserId
import me.proton.core.label.domain.entity.LabelType
import me.proton.core.plan.presentation.compose.component.UpgradeStorageInfo

@Composable
@Suppress("ComplexMethod")
fun Sidebar(
    drawerState: DrawerState,
    navigationActions: Sidebar.NavigationActions,
    modifier: Modifier = Modifier,
    viewModel: SidebarViewModel = hiltViewModel()
) {
    val scope = rememberCoroutineScope()
    val viewState = rememberSidebarState(
        drawerState = drawerState,
        appInformation = viewModel.appInformation
    )

    fun close() = scope.launch {
        viewState.accountPrimaryState.dismissDialog()
        viewState.drawerState.close()
    }

    when (val viewModelState = rememberAsState(viewModel.state, viewModel.initialState).value) {
        is Disabled -> Unit
        is Enabled -> {
            viewState.isSubscriptionVisible = viewModelState.canChangeSubscription
            viewState.mailLabels = viewModelState.mailLabels
            viewState.showUpsellButton = viewModelState.showUpsell
            val actions = navigationActions.toSidebarActions(
                close = ::close,
                onUpsellAction = {
                    viewModel.submit(SidebarViewModel.Action.UpsellClicked)
                    navigationActions.onUpsell()
                },
                onLabelAction = { sidebarLabelAction ->
                    when (sidebarLabelAction) {
                        is SidebarLabelAction.ViewList -> {
                            close()
                            if (sidebarLabelAction.type == LabelType.MessageLabel) {
                                navigationActions.onLabelList()
                            } else if (sidebarLabelAction.type == LabelType.MessageFolder) {
                                navigationActions.onFolderList()
                            }
                        }

                        is SidebarLabelAction.Add -> {
                            close()
                            if (sidebarLabelAction.type == LabelType.MessageLabel) {
                                navigationActions.onLabelAdd()
                            } else if (sidebarLabelAction.type == LabelType.MessageFolder) {
                                navigationActions.onFolderAdd()
                            }
                        }

                        is SidebarLabelAction.Select -> {
                            close()
                            viewModel.submit(SidebarViewModel.Action.LabelAction(sidebarLabelAction))
                        }

                        is SidebarLabelAction.Collapse,
                        is SidebarLabelAction.Expand -> {
                            viewModel.submit(SidebarViewModel.Action.LabelAction(sidebarLabelAction))
                        }
                    }
                }
            )
            Sidebar(
                modifier = modifier,
                viewState = viewState,
                actions = actions
            )
        }
    }
}

@Composable
fun Sidebar(
    modifier: Modifier = Modifier,
    viewState: SidebarState,
    actions: Sidebar.Actions
) {
    val sidebarColors = requireNotNull(ProtonTheme.colors.sidebarColors)

    if (viewState.hasPrimaryAccount) {
        AccountPrimaryItem(
            modifier = Modifier
                .background(sidebarColors.backgroundNorm)
                .padding(all = ProtonDimens.SmallSpacing)
                .fillMaxWidth(),
            onRemove = actions.onRemoveAccount,
            onSignIn = actions.onSignIn,
            onSignOut = actions.onSignOut,
            onSwitch = actions.onSwitchAccount,
            viewState = viewState.accountPrimaryState
        )
    }

    ProtonTheme(
        colors = ProtonTheme.colors.sidebarColors ?: ProtonTheme.colors
    ) {
        UpgradeStorageInfo(
            modifier = Modifier
                .background(sidebarColors.backgroundNorm),
            onUpgradeClicked = { actions.onSubscription() },
            withTopDivider = true,
            withBottomDivider = true
        )
        SidebarUpsellItem(
            show = viewState.showUpsellButton,
            onClick = {
                actions.onUpsell()
            },
            modifier = Modifier
                .background(sidebarColors.backgroundNorm)
        )
    }

    ProtonSidebarLazy(
        modifier = modifier.testTag(SidebarMenuTestTags.Root),
        drawerState = viewState.drawerState
    ) {
        sidebarSystemLabelItems(viewState.mailLabels.systems, actions.onLabelAction)
        item { Divider() }
        sidebarFolderItems(viewState.mailLabels.folders, actions.onLabelAction)
        item { Divider() }
        sidebarLabelItems(viewState.mailLabels.labels, actions.onLabelAction)
        item { Divider() }
        item { SidebarMoreTitleItem() }
        item { ProtonSidebarSettingsItem(onClick = actions.onSettings) }
        item { SidebarSubscriptionItem(viewState.isSubscriptionVisible, onSubscription = actions.onSubscription) }
        if (viewState.showContacts) item { SidebarContactsItem(onClick = actions.onContacts) }
        item { ProtonSidebarReportBugItem(onClick = actions.onReportBug) }
        item { ProtonSidebarSignOutItem(onClick = { actions.onSignOut(null) }) }
        item { SidebarAppVersionItem(viewState.appInformation) }
    }
}

@Composable
private fun SidebarMoreTitleItem() {
    ProtonSidebarItem(isClickable = false) {
        Text(
            text = stringResource(R.string.drawer_title_more),
            color = ProtonTheme.colors.textWeak
        )
    }
}

@Composable
private fun SidebarSubscriptionItem(isVisible: Boolean, onSubscription: () -> Unit) {
    if (isVisible) {
        ProtonSidebarSubscriptionItem { onSubscription() }
    }
}

@Composable
private fun SidebarContactsItem(onClick: () -> Unit) {
    ProtonSidebarItem(
        icon = painterResource(R.drawable.ic_proton_users),
        text = stringResource(R.string.drawer_title_contacts),
        isSelected = false,
        onClick = onClick
    )
}

@Composable
private fun SidebarAppVersionItem(appInformation: AppInformation) {
    ProtonSidebarAppVersionItem(
        name = appInformation.appName,
        version = "${appInformation.appVersionName} (${appInformation.appVersionCode})"
    )
}

object Sidebar {

    data class Actions(
        val onSignIn: (UserId?) -> Unit,
        val onSignOut: (UserId?) -> Unit,
        val onUpsell: () -> Unit,
        val onRemoveAccount: (UserId?) -> Unit,
        val onSwitchAccount: (UserId) -> Unit,
        val onSettings: () -> Unit,
        val onLabelAction: (SidebarLabelAction) -> Unit,
        val onSubscription: () -> Unit,
        val onContacts: () -> Unit,
        val onReportBug: () -> Unit
    ) {

        companion object {

            val Empty = Actions(
                onSignIn = {},
                onSignOut = {},
                onUpsell = {},
                onRemoveAccount = {},
                onSwitchAccount = {},
                onSettings = {},
                onLabelAction = {},
                onSubscription = {},
                onContacts = {},
                onReportBug = {}
            )
        }
    }

    data class NavigationActions(
        val onSignIn: (UserId?) -> Unit,
        val onSignOut: (UserId?) -> Unit,
        val onUpsell: () -> Unit,
        val onRemoveAccount: (UserId?) -> Unit,
        val onSwitchAccount: (UserId) -> Unit,
        val onSettings: () -> Unit,
        val onLabelList: () -> Unit,
        val onFolderList: () -> Unit,
        val onLabelAdd: () -> Unit,
        val onFolderAdd: () -> Unit,
        val onSubscription: () -> Unit,
        val onContacts: () -> Unit,
        val onReportBug: () -> Unit
    ) {

        fun toSidebarActions(
            close: () -> Unit,
            onLabelAction: (SidebarLabelAction) -> Unit,
            onUpsellAction: () -> Unit
        ) = Actions(
            onSignIn = {
                onSignIn(it)
                close()
            },
            onSignOut = {
                onSignOut(it)
                close()
            },
            onUpsell = {
                onUpsellAction()
                close()
            },
            onRemoveAccount = {
                onRemoveAccount(it)
                close()
            },
            onSwitchAccount = {
                onSwitchAccount(it)
                close()
            },
            onSettings = {
                onSettings()
                close()
            },
            onLabelAction = { action ->
                onLabelAction(action)
                close()
            },
            onSubscription = {
                onSubscription()
                close()
            },
            onContacts = {
                onContacts()
                close()
            },
            onReportBug = {
                onReportBug()
                close()
            }
        )

        companion object {

            val Empty = NavigationActions(
                onSignIn = {},
                onSignOut = {},
                onUpsell = {},
                onRemoveAccount = {},
                onSwitchAccount = {},
                onSettings = {},
                onLabelList = {},
                onFolderList = {},
                onLabelAdd = {},
                onFolderAdd = {},
                onSubscription = {},
                onContacts = {},
                onReportBug = {}
            )
        }
    }
}

@SuppressLint("VisibleForTests")
@Preview(
    name = "Sidebar in light mode",
    uiMode = Configuration.UI_MODE_NIGHT_NO,
    showBackground = true
)
@Preview(
    name = "Sidebar in dark mode",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true
)
@Composable
fun PreviewSidebar() {
    ProtonTheme {
        Sidebar(
            viewState = SidebarState(
                hasPrimaryAccount = false,
                isSubscriptionVisible = true,
                mailLabels = MailLabelsUiModel.PreviewForTesting,
                showUpsell = true
            ),
            actions = Sidebar.Actions.Empty
        )
    }
}

object SidebarMenuTestTags {

    const val Root = "SidebarMenu"
}
