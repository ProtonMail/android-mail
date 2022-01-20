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

package ch.protonmail.android.sidebar

import android.content.res.Configuration
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Divider
import androidx.compose.material.DrawerState
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import ch.protonmail.android.R
import ch.protonmail.android.mailmessage.domain.model.MailLocation
import ch.protonmail.android.sidebar.SidebarViewModel.State.Disabled
import ch.protonmail.android.sidebar.SidebarViewModel.State.Enabled
import ch.protonmail.android.sidebar.model.SidebarState
import ch.protonmail.android.sidebar.model.rememberSidebarState
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

@Composable
@SuppressWarnings("LongParameterList", "ComplexMethod", "LongMethod")
fun Sidebar(
    onRemove: (UserId?) -> Unit,
    onSignOut: (UserId) -> Unit,
    onSignIn: (UserId?) -> Unit,
    onSwitch: (UserId) -> Unit,
    onMailLocation: (MailLocation) -> Unit,
    onFolder: (String) -> Unit,
    onLabel: (String) -> Unit,
    onSettings: () -> Unit,
    onSubscription: () -> Unit,
    onReportBug: () -> Unit,
    drawerState: DrawerState,
    modifier: Modifier = Modifier,
    viewModel: SidebarViewModel = hiltViewModel()
) {
    val scope = rememberCoroutineScope()
    val sidebarState = rememberSidebarState(
        drawerState = drawerState
    )
    val state by rememberAsState(viewModel.state, Enabled(MailLocation.Inbox))

    fun close() = scope.launch {
        sidebarState.accountPrimaryState.dismissDialog()
        sidebarState.drawerState.close()
    }

    when (state) {
        is Disabled -> Unit
        is Enabled ->
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
                    viewModel.onLocationSelected(it)
                    onMailLocation(it)
                },
                onFolder = {
                    close()
                    onFolder(it)
                },
                onLabel = {
                    close()
                    onLabel(it)
                },
                onSettings = {
                    close()
                    onSettings()
                },
                onSubscription = {
                    close()
                    onSubscription()
                },
                onReportBug = {
                    close()
                    onReportBug()
                },
                sidebarState = sidebarState.copy(
                    selectedLocation = (state as Enabled).selectedLocation
                ),
                modifier = modifier
            )

    }
}

@Composable
@SuppressWarnings("LongParameterList", "ComplexMethod", "LongMethod")
fun Sidebar(
    onRemove: (UserId?) -> Unit,
    onSignOut: (UserId) -> Unit,
    onSignIn: (UserId?) -> Unit,
    onSwitch: (UserId) -> Unit,
    onMailLocation: (MailLocation) -> Unit,
    onFolder: (String) -> Unit,
    onLabel: (String) -> Unit,
    onSettings: () -> Unit,
    onSubscription: () -> Unit,
    onReportBug: () -> Unit,
    modifier: Modifier = Modifier,
    sidebarState: SidebarState
) {
    ProtonSidebarLazy(
        modifier = modifier,
        drawerState = sidebarState.drawerState,
    ) {
        item {
            if (sidebarState.hasPrimaryAccount) {
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
                    viewState = sidebarState.accountPrimaryState
                )
            }

            @Composable
            fun ProtonSidebarMailLocationItem(location: MailLocation) {
                val isSelected = sidebarState.selectedLocation == location
                ProtonSidebarItem(
                    icon = when (location) {
                        MailLocation.Inbox -> R.drawable.ic_inbox
                        MailLocation.Drafts -> R.drawable.ic_drafts
                        MailLocation.Sent -> R.drawable.ic_paper_plane
                        MailLocation.Starred -> R.drawable.ic_star
                        MailLocation.Archive -> R.drawable.ic_archive
                        MailLocation.Spam -> R.drawable.ic_fire
                        MailLocation.Trash -> R.drawable.ic_trash
                        MailLocation.AllMail -> R.drawable.ic_envelope_all_emails
                    },
                    text = when (location) {
                        MailLocation.Inbox -> R.string.drawer_title_inbox
                        MailLocation.Drafts -> R.string.drawer_title_drafts
                        MailLocation.Sent -> R.string.drawer_title_sent
                        MailLocation.Starred -> R.string.drawer_title_starred
                        MailLocation.Archive -> R.string.drawer_title_archive
                        MailLocation.Spam -> R.string.drawer_title_spam
                        MailLocation.Trash -> R.string.drawer_title_trash
                        MailLocation.AllMail -> R.string.drawer_title_all_mail
                    },
                    count = when (location) {
                        MailLocation.Inbox -> sidebarState.counters.inbox
                        MailLocation.Drafts -> sidebarState.counters.drafts
                        MailLocation.Sent -> sidebarState.counters.sent
                        MailLocation.Starred -> sidebarState.counters.starred
                        MailLocation.Archive -> sidebarState.counters.archive
                        MailLocation.Spam -> sidebarState.counters.spam
                        MailLocation.Trash -> sidebarState.counters.trash
                        MailLocation.AllMail -> sidebarState.counters.allMail
                    },
                    isSelected = isSelected,
                    iconTint = if (isSelected) {
                        ProtonTheme.colors.textNorm
                    } else {
                        ProtonTheme.colors.iconHint
                    }
                ) {
                    onMailLocation(location)
                }
            }

            ProtonSidebarMailLocationItem(MailLocation.Inbox)
            ProtonSidebarMailLocationItem(MailLocation.Drafts)
            ProtonSidebarMailLocationItem(MailLocation.Sent)
            ProtonSidebarMailLocationItem(MailLocation.Starred)
            ProtonSidebarMailLocationItem(MailLocation.Archive)
            ProtonSidebarMailLocationItem(MailLocation.Spam)
            ProtonSidebarMailLocationItem(MailLocation.Trash)
            ProtonSidebarMailLocationItem(MailLocation.AllMail)

            Divider()

            ProtonSidebarItem(isClickable = false) {
                Text(
                    text = stringResource(R.string.drawer_title_folders),
                    color = ProtonTheme.colors.textHint
                )
            }
        }

        items(sidebarState.folderUiModels) {
            ProtonSidebarItem(
                icon = painterResource(id = R.drawable.ic_folder_filled),
                text = it.text,
                iconTint = it.color
            ) {
                onFolder(it.id)
            }
        }

        item {
            Divider()

            ProtonSidebarItem(isClickable = false) {
                Text(
                    text = stringResource(R.string.drawer_title_labels),
                    color = ProtonTheme.colors.textHint,
                    modifier = Modifier.weight(1f, fill = true)
                )
            }
        }

        items(sidebarState.labelUiModels) {
            ProtonSidebarItem(
                icon = painterResource(id = R.drawable.ic_label_filled),
                text = it.text,
                iconTint = it.color
            ) {
                onLabel(it.id)
            }
        }

        item {
            Divider()

            ProtonSidebarItem(isClickable = false) {
                Text(
                    text = stringResource(R.string.drawer_title_more),
                    color = ProtonTheme.colors.textHint
                )
            }
            ProtonSidebarSettingsItem {
                onSettings()
            }
            ProtonSidebarSubscriptionItem {
                onSubscription()
            }
            ProtonSidebarReportBugItem {
                onReportBug()
            }
            ProtonSidebarSignOutItem {
                onRemove(null)
            }

            ProtonSidebarAppVersionItem(
                name = sidebarState.appName,
                version = sidebarState.appVersion
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
        icon = R.drawable.ic_pencil,
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
            sidebarState = SidebarState(
                selectedLocation = MailLocation.Inbox,
                hasPrimaryAccount = false
            ),
        )
    }
}
