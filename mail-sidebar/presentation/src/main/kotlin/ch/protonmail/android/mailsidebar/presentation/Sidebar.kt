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
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.LocalActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.DrawerState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ch.protonmail.android.design.compose.component.VerticalSpacer
import ch.protonmail.android.design.compose.theme.ProtonDimens
import ch.protonmail.android.design.compose.theme.ProtonTheme
import ch.protonmail.android.mailcommon.domain.AppInformation
import ch.protonmail.android.maillabel.domain.model.LabelType
import ch.protonmail.android.maillabel.presentation.MailLabelsUiModel
import ch.protonmail.android.mailsidebar.presentation.SidebarViewModel.State.Disabled
import ch.protonmail.android.mailsidebar.presentation.SidebarViewModel.State.Enabled
import ch.protonmail.android.mailsidebar.presentation.common.ProtonSidebarAppVersionItem
import ch.protonmail.android.mailsidebar.presentation.common.ProtonSidebarItem
import ch.protonmail.android.mailsidebar.presentation.common.ProtonSidebarLazy
import ch.protonmail.android.mailsidebar.presentation.common.ProtonSidebarReportBugItem
import ch.protonmail.android.mailsidebar.presentation.common.ProtonSidebarSettingsItem
import ch.protonmail.android.mailsidebar.presentation.common.ProtonSidebarSubscriptionItem
import ch.protonmail.android.mailsidebar.presentation.label.SidebarLabelAction
import ch.protonmail.android.mailsidebar.presentation.label.sidebarFolderItems
import ch.protonmail.android.mailsidebar.presentation.label.sidebarLabelItems
import ch.protonmail.android.mailsidebar.presentation.label.sidebarSystemLabelItems
import ch.protonmail.android.mailsidebar.presentation.upselling.SidebarUpsellRow
import ch.protonmail.android.mailupselling.domain.model.UpsellingEntryPoint
import ch.protonmail.android.mailupselling.presentation.model.UpsellingVisibility
import ch.protonmail.android.uicomponents.BottomNavigationBarSpacer
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.coroutines.launch

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
        viewState.drawerState.close()
    }

    when (val viewModelState = viewModel.state.collectAsStateWithLifecycle().value) {
        is Disabled -> Unit
        is Enabled -> {
            viewState.isSubscriptionVisible = viewModelState.canChangeSubscription
            viewState.mailLabels = viewModelState.mailLabels
            val actions = navigationActions.toSidebarActions(
                close = ::close,
                onLabelAction = { sidebarLabelAction ->
                    when (sidebarLabelAction) {
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
    val hazeState = rememberHazeState()
    Box(
        modifier
            .background(ProtonTheme.colors.sidebarBackground)
            .windowInsetsPadding(
                WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)
            )
    ) {
        SidebarHeader(modifier = Modifier.zIndex(1f), hazeState)

        ProtonSidebarLazy(
            modifier = modifier
                .testTag(SidebarMenuTestTags.Root)
                .hazeSource(state = hazeState),
            drawerState = viewState.drawerState
        ) {
            item {
                Spacer(Modifier.padding(top = ProtonDimens.sideBarHeaderHeight))
            }
            item {
                SidebarUpsellRow(onClick = { type ->
                    actions.onUpselling(UpsellingEntryPoint.Feature.Sidebar, type)
                })
            }
            item { SidebarDivider() }

            sidebarSystemLabelItems(viewState.mailLabels.systemLabels, actions.onLabelAction)
            item { SidebarDivider() }

            sidebarFolderItems(viewState.mailLabels.folders, actions.onLabelAction)
            item { SidebarDivider() }

            sidebarLabelItems(viewState.mailLabels.labels, actions.onLabelAction)
            item { SidebarDivider() }
            item { VerticalSpacer(height = ProtonDimens.Spacing.Standard) }

            item { ProtonSidebarSubscriptionItem { actions.onSubscription() } }

            item { ProtonSidebarSettingsItem(onClick = actions.onSettings) }
            item { SidebarContactsItem(onClick = actions.onContacts) }

            item { ProtonSidebarReportBugItem(onClick = actions.onReportBug) }

            item { VerticalSpacer(height = ProtonDimens.Spacing.ExtraLarge) }

            item { SidebarDivider() }
            item { VerticalSpacer(height = ProtonDimens.Spacing.ExtraLarge) }
            item { SidebarAppVersionItem(viewState.appInformation) }

            item { BottomNavigationBarSpacer() }
        }
    }

    val activity = LocalActivity.current as? ComponentActivity
    LaunchedEffect(viewState.drawerState.isOpen) {
        activity?.let {
            if (viewState.drawerState.isOpen) {
                it.enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.dark(scrim = android.graphics.Color.TRANSPARENT),
                    navigationBarStyle = SystemBarStyle.dark(scrim = android.graphics.Color.TRANSPARENT)
                )
            } else {
                it.enableEdgeToEdge()
            }
        }
    }
}

@Composable
private fun SidebarHeader(modifier: Modifier = Modifier, hazeState: HazeState) {
    Box(
        modifier = modifier
            .height(ProtonDimens.sideBarHeaderHeight)
            .background(ProtonTheme.colors.sidebarBackground)
            .hazeEffect(
                state = hazeState,
                style = HazeStyle(
                    tint = HazeTint(
                        color = Color.Transparent
                    ),
                    blurRadius = 20.dp,
                    noiseFactor = 0f
                )
            )
            .fillMaxWidth()
    ) {
        Image(
            modifier = Modifier.padding(
                start = ProtonDimens.Spacing.ExtraLarge,
                bottom = ProtonDimens.Spacing.Small,
                top = ProtonDimens.Spacing.Jumbo
            ),
            painter = painterResource(R.drawable.proton_mail_logo),
            contentDescription = null
        )
    }
}

@Composable
private fun SidebarDivider() {
    HorizontalDivider(
        color = ProtonTheme.colors.sidebarSeparator
    )
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
        version = "${appInformation.appVersionName} (${appInformation.appVersionCode})",
        sdkVersion = appInformation.rustSdkVersion
    )
}

object Sidebar {

    data class Actions(
        val onSettings: () -> Unit,
        val onLabelAction: (SidebarLabelAction) -> Unit,
        val onSubscription: () -> Unit,
        val onContacts: () -> Unit,
        val onReportBug: () -> Unit,
        val onUpselling: (entryPoint: UpsellingEntryPoint.Feature, type: UpsellingVisibility) -> Unit
    ) {

        companion object {

            val Empty = Actions(
                onSettings = {},
                onLabelAction = {},
                onSubscription = {},
                onContacts = {},
                onReportBug = {},
                onUpselling = { _, _ -> }
            )
        }
    }

    data class NavigationActions(
        val onSettings: () -> Unit,
        val onLabelAdd: () -> Unit,
        val onFolderAdd: () -> Unit,
        val onSubscription: () -> Unit,
        val onContacts: () -> Unit,
        val onReportBug: () -> Unit,
        val onUpselling: (entryPoint: UpsellingEntryPoint.Feature, type: UpsellingVisibility) -> Unit
    ) {

        fun toSidebarActions(close: () -> Unit, onLabelAction: (SidebarLabelAction) -> Unit) = Actions(
            onSettings = {
                onSettings()
                close()
            },
            onLabelAction = { action ->
                onLabelAction(action)
                if (action !is SidebarLabelAction.Collapse && action !is SidebarLabelAction.Expand) {
                    close()
                }
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
            },
            onUpselling = { entryPoint, type ->
                onUpselling(entryPoint, type)
                close()
            }
        )

        companion object {

            val Empty = NavigationActions(
                onSettings = {},
                onLabelAdd = {},
                onFolderAdd = {},
                onSubscription = {},
                onContacts = {},
                onReportBug = {},
                onUpselling = { _, _ -> }
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
                mailLabels = MailLabelsUiModel.PreviewForTesting,
                isSubscriptionVisible = true
            ),
            actions = Sidebar.Actions.Empty
        )
    }
}

object SidebarMenuTestTags {

    const val Root = "SidebarMenu"
}
