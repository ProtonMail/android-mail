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

package ch.protonmail.android.mailmailbox.presentation.sidebar

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import ch.protonmail.android.mailcommon.domain.AppInformation
import ch.protonmail.android.maillabel.presentation.MailLabelsUiModel
import ch.protonmail.android.maillabel.presentation.sidebar.SidebarLabelAction
import ch.protonmail.android.maillabel.presentation.sidebar.sidebarFolderItems
import ch.protonmail.android.maillabel.presentation.sidebar.sidebarLabelItems
import ch.protonmail.android.maillabel.presentation.sidebar.sidebarSystemLabelItems
import ch.protonmail.android.mailmailbox.presentation.R
import ch.protonmail.android.mailmailbox.presentation.sidebar.SidebarViewModel.State.Disabled
import ch.protonmail.android.mailmailbox.presentation.sidebar.SidebarViewModel.State.Enabled
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

const val TEST_TAG_SIDEBAR_MENU = "SidebarMenuTestTag"

@Composable
@Suppress("ComplexMethod")
fun Sidebar(
    drawerState: DrawerState,
    actions: Sidebar.Actions,
    modifier: Modifier = Modifier,
    viewModel: SidebarViewModel = hiltViewModel(),
) {
    val scope = rememberCoroutineScope()
    val viewState = rememberSidebarState(
        drawerState = drawerState,
        appInformation = viewModel.appInformation,
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
            Sidebar(
                onRemove = {
                    close()
                    actions.onRemoveAccount(it)
                },
                onSignOut = {
                    close()
                    actions.onSignOut(it)
                },
                onSignIn = {
                    close()
                    actions.onSignIn(it)
                },
                onSwitch = {
                    close()
                    actions.onSwitchAccount(it)
                },
                onLabelAction = {
                    when (it) {
                        is SidebarLabelAction.Add -> {
                            close()
                            actions.onLabelsSettings()
                        }
                        is SidebarLabelAction.Select -> {
                            close()
                            viewModel.submit(SidebarViewModel.Action.LabelAction(it))
                        }
                        is SidebarLabelAction.Collapse,
                        is SidebarLabelAction.Expand -> viewModel.submit(SidebarViewModel.Action.LabelAction(it))
                    }
                },
                onSettings = {
                    close()
                    if (viewModelState.isSettingsEnabled) {
                        actions.onSettings()
                    }
                },
                onSubscription = {
                    close()
                    actions.onSubscription()
                },
                onReportBug = {
                    close()
                    actions.onReportBug()
                },
                viewState = viewState,
                modifier = modifier
            )
        }
    }
}

@Composable
fun Sidebar(
    onRemove: (UserId?) -> Unit,
    onSignOut: (UserId) -> Unit,
    onSignIn: (UserId?) -> Unit,
    onSwitch: (UserId) -> Unit,
    onLabelAction: (SidebarLabelAction) -> Unit,
    onSettings: () -> Unit,
    onSubscription: () -> Unit,
    onReportBug: () -> Unit,
    modifier: Modifier = Modifier,
    viewState: SidebarState,
) {
    val sidebarColors = requireNotNull(ProtonTheme.colors.sidebarColors)

    if (viewState.hasPrimaryAccount) {
        AccountPrimaryItem(
            modifier = Modifier
                .background(sidebarColors.backgroundNorm)
                .padding(all = ProtonDimens.SmallSpacing)
                .fillMaxWidth(),
            onRemove = { onRemove(it) },
            onSignIn = { onSignIn(it) },
            onSignOut = { onSignOut(it) },
            onSwitch = { onSwitch(it) },
            viewState = viewState.accountPrimaryState
        )
    }

    ProtonSidebarLazy(
        modifier = modifier.testTag(TEST_TAG_SIDEBAR_MENU),
        drawerState = viewState.drawerState,
    ) {
        sidebarSystemLabelItems(viewState.mailLabels.systems, onLabelAction)
        item { Divider() }
        sidebarFolderItems(viewState.mailLabels.folders, onLabelAction)
        item { Divider() }
        sidebarLabelItems(viewState.mailLabels.labels, onLabelAction)
        item { Divider() }
        item { SidebarMoreTitleItem() }
        item { ProtonSidebarSettingsItem { onSettings() } }
        item { SidebarSubscriptionItem(viewState.isSubscriptionVisible) { onSubscription() } }
        item { ProtonSidebarReportBugItem { onReportBug() } }
        item { ProtonSidebarSignOutItem { onRemove(null) } }
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
private fun SidebarSubscriptionItem(
    isVisible: Boolean,
    onSubscription: () -> Unit,
) {
    if (isVisible) {
        ProtonSidebarSubscriptionItem { onSubscription() }
    }
}

@Composable
private fun SidebarAppVersionItem(
    appInformation: AppInformation,
) {
    ProtonSidebarAppVersionItem(
        name = appInformation.appName,
        version = appInformation.appVersionName
    )
}

object Sidebar {

    data class Actions(
        val onSignIn: (UserId?) -> Unit,
        val onSignOut: (UserId) -> Unit,
        val onRemoveAccount: (UserId?) -> Unit,
        val onSwitchAccount: (UserId) -> Unit,
        val onSettings: () -> Unit,
        val onLabelsSettings: () -> Unit,
        val onSubscription: () -> Unit,
        val onReportBug: () -> Unit
    )
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
            onSignOut = {},
            onSignIn = {},
            onSwitch = {},
            onRemove = {},
            onLabelAction = {},
            onSettings = {},
            onSubscription = {},
            onReportBug = {},
            viewState = SidebarState(
                hasPrimaryAccount = false,
                isSubscriptionVisible = true,
                mailLabels = MailLabelsUiModel.PreviewForTesting
            ),
        )
    }
}
