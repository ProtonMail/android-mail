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

package ch.protonmail.android.navigation

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Divider
import androidx.compose.material.DrawerState
import androidx.compose.material.DrawerValue.Open
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import ch.protonmail.android.R
import me.proton.core.accountmanager.presentation.compose.AccountPrimaryItem
import me.proton.core.compose.component.ProtonSidebarAppVersionItem
import me.proton.core.compose.component.ProtonSidebarItem
import me.proton.core.compose.component.ProtonSidebarLazy
import me.proton.core.compose.component.ProtonSidebarReportBugItem
import me.proton.core.compose.component.ProtonSidebarSettingsItem
import me.proton.core.compose.component.ProtonSidebarSignOutItem
import me.proton.core.compose.flow.rememberAsState
import me.proton.core.compose.theme.ProtonColors
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.domain.entity.UserId

@Composable
fun Sidebar(
    drawerState: DrawerState,
    onRemove: (UserId?) -> Unit,
    onSignOut: (UserId) -> Unit,
    onSignIn: (UserId?) -> Unit,
    onSwitch: (UserId) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SidebarViewModel = hiltViewModel()
) {
    val viewState by rememberAsState(viewModel.state, SidebarViewModel.State())

    Sidebar(
        drawerState = drawerState,
        onRemove = onRemove,
        onSignOut = onSignOut,
        onSignIn = onSignIn,
        onSwitch = onSwitch,
        modifier = modifier,
        viewState = viewState
    )
}

@Composable
private fun Sidebar(
    drawerState: DrawerState,
    onRemove: (UserId?) -> Unit,
    onSignOut: (UserId) -> Unit,
    onSignIn: (UserId?) -> Unit,
    onSwitch: (UserId) -> Unit,
    modifier: Modifier = Modifier,
    viewState: SidebarViewModel.State = SidebarViewModel.State()
) {
    ProtonSidebarLazy(
        modifier = modifier,
        drawerState = drawerState,
    ) {
        item {
            if (viewState.isAccountVisible) {
                AccountPrimaryItem(
                    onRemove = { onRemove(it) },
                    onSignIn = { onSignIn(it) },
                    onSignOut = { onSignOut(it) },
                    onSwitch = { onSwitch(it) },
                    modifier = Modifier
                        .padding(all = ProtonDimens.SmallSpacing)
                        .fillMaxWidth(),
                )
            }

            ProtonSidebarItem(
                icon = R.drawable.ic_inbox,
                text = R.string.drawer_title_inbox,
                count = viewState.inboxCount
            )
            ProtonSidebarItem(
                icon = R.drawable.ic_drafts,
                text = R.string.drawer_title_drafts,
                count = viewState.draftsCount
            )
            ProtonSidebarItem(
                icon = R.drawable.ic_paper_plane,
                text = R.string.drawer_title_sent,
                count = viewState.sentCount
            )
            ProtonSidebarItem(
                icon = R.drawable.ic_star,
                text = R.string.drawer_title_starred,
                count = viewState.starredCount
            )
            ProtonSidebarItem(
                icon = R.drawable.ic_archive,
                text = R.string.drawer_title_archive,
                count = viewState.archiveCount
            )
            ProtonSidebarItem(
                icon = R.drawable.ic_fire,
                text = R.string.drawer_title_spam,
                count = viewState.spamCount
            )
            ProtonSidebarItem(
                icon = R.drawable.ic_trash,
                text = R.string.drawer_title_trash,
                count = viewState.trashCount
            )
            ProtonSidebarItem(
                icon = R.drawable.ic_envelope_all_emails,
                text = R.string.drawer_title_all_mail,
                count = viewState.allMailCount
            )

            Divider()

            ProtonSidebarItem(isClickable = false) {
                Text(
                    text = stringResource(R.string.drawer_title_folders),
                    color = ProtonTheme.colors.textHint
                )
            }
        }

        items(viewState.folders) {
            ProtonSidebarItem(
                icon = painterResource(id = R.drawable.ic_folder_filled),
                text = it.text,
                textColor = ProtonTheme.colors.textHint,
                iconTint = it.color
            )
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

        items(viewState.labels) {
            ProtonSidebarItem(
                icon = painterResource(id = R.drawable.ic_label_filled),
                text = it.text,
                textColor = ProtonTheme.colors.textHint,
                iconTint = it.color
            )
        }

        item {
            Divider()

            ProtonSidebarItem(isClickable = false) {
                Text(
                    text = stringResource(R.string.drawer_title_more),
                    color = ProtonTheme.colors.textHint
                )
            }
            ProtonSidebarSettingsItem()
            ProtonSidebarReportBugItem()
            ProtonSidebarSignOutItem { onRemove(null) }

            ProtonSidebarAppVersionItem(name = viewState.appName, version = viewState.appVersion)
        }
    }
}

@Preview
@Composable
fun PreviewSidebarLight() {
    ProtonTheme(colors = ProtonColors.Light) {
        Sidebar(
            drawerState = DrawerState(Open),
            onSignOut = {},
            onSignIn = {},
            onSwitch = {},
            onRemove = {},
            viewState = SidebarViewModel.State(),
        )
    }
}

@Preview
@Composable
fun PreviewSidebarDark() {
    ProtonTheme(colors = ProtonColors.Dark) {
        Sidebar(
            drawerState = DrawerState(Open),
            onSignOut = {},
            onSignIn = {},
            onSwitch = {},
            onRemove = {},
            viewState = SidebarViewModel.State(),
        )
    }
}
