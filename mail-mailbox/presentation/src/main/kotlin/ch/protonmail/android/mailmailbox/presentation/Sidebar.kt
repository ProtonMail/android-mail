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

package ch.protonmail.android.mailmailbox.presentation

import android.content.res.Configuration
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
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
import ch.protonmail.android.mailmailbox.domain.model.SidebarLocation
import ch.protonmail.android.mailmailbox.domain.model.SidebarLocation.AllMail
import ch.protonmail.android.mailmailbox.domain.model.SidebarLocation.Archive
import ch.protonmail.android.mailmailbox.domain.model.SidebarLocation.Drafts
import ch.protonmail.android.mailmailbox.domain.model.SidebarLocation.Inbox
import ch.protonmail.android.mailmailbox.domain.model.SidebarLocation.MailLocation
import ch.protonmail.android.mailmailbox.domain.model.SidebarLocation.Sent
import ch.protonmail.android.mailmailbox.domain.model.SidebarLocation.Spam
import ch.protonmail.android.mailmailbox.domain.model.SidebarLocation.Starred
import ch.protonmail.android.mailmailbox.domain.model.SidebarLocation.Trash
import ch.protonmail.android.mailmailbox.presentation.SidebarViewModel.State.Disabled
import ch.protonmail.android.mailmailbox.presentation.SidebarViewModel.State.Enabled
import kotlinx.coroutines.launch
import me.proton.core.accountmanager.presentation.compose.AccountPrimaryItem
import me.proton.core.compose.component.ProtonSidebarAppVersionItem
import me.proton.core.compose.component.ProtonSidebarItem
import me.proton.core.compose.component.ProtonSidebarLazy
import me.proton.core.compose.component.ProtonSidebarReportBugItem
import me.proton.core.compose.component.ProtonSidebarSettingsItem
import me.proton.core.compose.component.ProtonSidebarSignOutItem
import me.proton.core.compose.flow.rememberAsState
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.domain.entity.UserId
import me.proton.core.label.domain.entity.LabelId

const val TEST_TAG_SIDEBAR_MENU = "SidebarMenuTestTag"

@Composable
@Suppress("ComplexMethod")
fun Sidebar(
    onRemove: (UserId?) -> Unit,
    onSignOut: (UserId) -> Unit,
    onSignIn: (UserId?) -> Unit,
    onSwitch: (UserId) -> Unit,
    onMailLocation: (MailLocation) -> Unit,
    onFolder: (LabelId) -> Unit,
    onLabel: (LabelId) -> Unit,
    onSettings: () -> Unit,
    onSubscription: () -> Unit,
    onReportBug: () -> Unit,
    drawerState: DrawerState,
    modifier: Modifier = Modifier,
    viewModel: SidebarViewModel = hiltViewModel()
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
            viewState.selectedLocation = viewModelState.selectedLocation
            viewState.isSubscriptionVisible = viewModelState.canChangeSubscription
            Sidebar(
                onRemove = {
                    close()
                    onRemove(it)
                },
                onSignOut = {
                    close()
                    onSignOut(it)
                },
                onSignIn = {
                    close()
                    onSignIn(it)
                },
                onSwitch = {
                    close()
                    onSwitch(it)
                },
                onMailLocation = {
                    close()
                    viewModel.onSidebarItemSelected(it)
                    onMailLocation(it)
                },
                onFolder = {
                    close()
                    viewModel.onSidebarItemSelected(SidebarLocation.CustomFolder(it))
                    onFolder(it)
                },
                onLabel = {
                    close()
                    viewModel.onSidebarItemSelected(SidebarLocation.CustomLabel(it))
                    onLabel(it)
                },
                onSettings = {
                    close()
                    if (viewModelState.isSettingsEnabled) {
                        onSettings()
                    }
                },
                onSubscription = {
                    close()
                    onSubscription()
                },
                onReportBug = {
                    close()
                    onReportBug()
                },
                viewState = viewState,
                modifier = modifier
            )
        }
    }
}

@Composable
@Suppress("ComplexMethod")
fun Sidebar(
    onRemove: (UserId?) -> Unit,
    onSignOut: (UserId) -> Unit,
    onSignIn: (UserId?) -> Unit,
    onSwitch: (UserId) -> Unit,
    onMailLocation: (MailLocation) -> Unit,
    onFolder: (LabelId) -> Unit,
    onLabel: (LabelId) -> Unit,
    onSettings: () -> Unit,
    onSubscription: () -> Unit,
    onReportBug: () -> Unit,
    modifier: Modifier = Modifier,
    viewState: SidebarState
) {
    ProtonSidebarLazy(
        modifier = modifier.testTag(TEST_TAG_SIDEBAR_MENU),
        drawerState = viewState.drawerState,
    ) {
        item {
            if (viewState.hasPrimaryAccount) {
                AccountPrimaryItem(
                    onRemove = {
                        onRemove(it)
                    },
                    onSignIn = {
                        onSignIn(it)
                    },
                    onSignOut = {
                        onSignOut(it)
                    },
                    onSwitch = {
                        onSwitch(it)
                    },
                    modifier = Modifier
                        .padding(all = ProtonDimens.SmallSpacing)
                        .fillMaxWidth(),
                    viewState = viewState.accountPrimaryState
                )
            }

            @Composable
            fun ProtonSidebarMailLocationItem(location: MailLocation) {
                val isSelected = viewState.selectedLocation == location
                ProtonSidebarItem(
                    icon = when (location) {
                        Inbox -> R.drawable.ic_proton_inbox
                        Drafts -> R.drawable.ic_proton_file_lines
                        Sent -> R.drawable.ic_proton_paper_plane
                        Starred -> R.drawable.ic_proton_star
                        Archive -> R.drawable.ic_proton_archive_box
                        Spam -> R.drawable.ic_proton_fire
                        Trash -> R.drawable.ic_proton_trash
                        AllMail -> R.drawable.ic_proton_envelopes
                    },
                    text = when (location) {
                        Inbox -> R.string.drawer_title_inbox
                        Drafts -> R.string.drawer_title_drafts
                        Sent -> R.string.drawer_title_sent
                        Starred -> R.string.drawer_title_starred
                        Archive -> R.string.drawer_title_archive
                        Spam -> R.string.drawer_title_spam
                        Trash -> R.string.drawer_title_trash
                        AllMail -> R.string.drawer_title_all_mail
                    },
                    count = when (location) {
                        Inbox -> viewState.unreadCounters[Inbox.labelId]
                        Drafts -> viewState.unreadCounters[Drafts.labelId]
                        Sent -> viewState.unreadCounters[Sent.labelId]
                        Starred -> viewState.unreadCounters[Starred.labelId]
                        Archive -> viewState.unreadCounters[Archive.labelId]
                        Spam -> viewState.unreadCounters[Spam.labelId]
                        Trash -> viewState.unreadCounters[Trash.labelId]
                        AllMail -> viewState.unreadCounters[AllMail.labelId]
                    },
                    isSelected = isSelected,
                ) {
                    onMailLocation(location)
                }
            }

            ProtonSidebarMailLocationItem(Inbox)
            ProtonSidebarMailLocationItem(Drafts)
            ProtonSidebarMailLocationItem(Sent)
            ProtonSidebarMailLocationItem(Starred)
            ProtonSidebarMailLocationItem(Archive)
            ProtonSidebarMailLocationItem(Spam)
            ProtonSidebarMailLocationItem(Trash)
            ProtonSidebarMailLocationItem(AllMail)

            Divider()

            ProtonSidebarItem(isClickable = false) {
                Text(
                    text = stringResource(R.string.drawer_title_folders),
                    color = ProtonTheme.colors.textWeak
                )
            }
        }

        items(viewState.sidebarFolderUiModels) {
            val isSelected = viewState.selectedLocation == SidebarLocation.CustomFolder(it.id)
            ProtonSidebarItem(
                icon = painterResource(id = R.drawable.ic_proton_folder_filled),
                text = it.text,
                iconTint = it.color,
                isSelected = isSelected
            ) {
                onFolder(it.id)
            }
        }

        item {
            Divider()

            ProtonSidebarItem(isClickable = false) {
                Text(
                    text = stringResource(R.string.drawer_title_labels),
                    color = ProtonTheme.colors.textWeak,
                    modifier = Modifier.weight(1f, fill = true)
                )
            }
        }

        items(viewState.sidebarLabelUiModels) {
            val isSelected = viewState.selectedLocation == SidebarLocation.CustomLabel(it.id)
            ProtonSidebarItem(
                icon = painterResource(id = R.drawable.ic_proton_circle_filled),
                text = it.text,
                iconTint = it.color,
                isSelected = isSelected
            ) {
                onLabel(it.id)
            }
        }

        item {
            Divider()

            ProtonSidebarItem(isClickable = false) {
                Text(
                    text = stringResource(R.string.drawer_title_more),
                    color = ProtonTheme.colors.textWeak
                )
            }
            ProtonSidebarSettingsItem {
                onSettings()
            }
            if (viewState.isSubscriptionVisible) {
                ProtonSidebarSubscriptionItem {
                    onSubscription()
                }
            }
            ProtonSidebarReportBugItem {
                onReportBug()
            }
            ProtonSidebarSignOutItem {
                onRemove(null)
            }

            ProtonSidebarAppVersionItem(
                name = viewState.appInformation.appName,
                version = viewState.appInformation.appVersionName
            )
        }
    }
}

@Composable
fun ProtonSidebarSubscriptionItem(
    modifier: Modifier = Modifier,
    isClickable: Boolean = true,
    onClick: () -> Unit = {},
) {
    ProtonSidebarItem(
        text = R.string.drawer_title_subscription,
        icon = R.drawable.ic_proton_pencil,
        modifier = modifier,
        onClick = onClick,
        isClickable = isClickable,
        isSelected = false
    )
}

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
            onMailLocation = {},
            onFolder = {},
            onLabel = {},
            onSettings = {},
            onSubscription = {},
            onReportBug = {},
            viewState = SidebarState(
                selectedLocation = Inbox,
                hasPrimaryAccount = false,
                isSubscriptionVisible = true
            ),
        )
    }
}
