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

import android.content.res.Configuration
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import ch.protonmail.android.R
import ch.protonmail.android.mailmailbox.presentation.MailboxState
import ch.protonmail.android.mailmessage.domain.model.MailLocation
import me.proton.core.accountmanager.presentation.compose.AccountPrimaryItem
import me.proton.core.compose.component.ProtonSidebarAppVersionItem
import me.proton.core.compose.component.ProtonSidebarItem
import me.proton.core.compose.component.ProtonSidebarLazy
import me.proton.core.compose.component.ProtonSidebarReportBugItem
import me.proton.core.compose.component.ProtonSidebarSettingsItem
import me.proton.core.compose.component.ProtonSidebarSignOutItem
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.domain.entity.UserId

@Composable
fun Sidebar(
    onRemove: (UserId?) -> Unit,
    onSignOut: (UserId) -> Unit,
    onSignIn: (UserId?) -> Unit,
    onSwitch: (UserId) -> Unit,
    onMailLocation: (MailLocation) -> Unit,
    onFolder: (String) -> Unit,
    onLabel: (String) -> Unit,
    onSettings: () -> Unit,
    onReportBug: () -> Unit,
    modifier: Modifier = Modifier,
    sidebarState: SidebarState = rememberSidebarState(),
) {
    ProtonSidebarLazy(
        modifier = modifier,
        drawerState = sidebarState.drawerState,
    ) {
        item {
            if (sidebarState.hasPrimaryAccount) {
                AccountPrimaryItem(
                    onRemove = {
                        sidebarState.close()
                        onRemove(it)
                    },
                    onSignIn = {
                        sidebarState.close()
                        onSignIn(it)
                    },
                    onSignOut = {
                        sidebarState.close()
                        onSignOut(it)
                    },
                    onSwitch = {
                        sidebarState.close()
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
                val isSelected = sidebarState.mailboxState.isLocationSelected(location)
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
                        MailLocation.Inbox -> sidebarState.mailboxState.inboxUnreadCount
                        MailLocation.Drafts -> sidebarState.mailboxState.draftsUnreadCount
                        MailLocation.Sent -> sidebarState.mailboxState.sentUnreadCount
                        MailLocation.Starred -> sidebarState.mailboxState.starredUnreadCount
                        MailLocation.Archive -> sidebarState.mailboxState.archiveUnreadCount
                        MailLocation.Spam -> sidebarState.mailboxState.spamUnreadCount
                        MailLocation.Trash -> sidebarState.mailboxState.trashUnreadCount
                        MailLocation.AllMail -> sidebarState.mailboxState.allMailUnreadCount
                    },
                    isSelected = isSelected,
                    iconTint = if (isSelected) {
                        ProtonTheme.colors.textNorm
                    } else {
                        ProtonTheme.colors.iconHint
                    }
                ) {
                    sidebarState.close()
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

        items(sidebarState.mailboxState.folders) {
            ProtonSidebarItem(
                icon = painterResource(id = R.drawable.ic_folder_filled),
                text = it.text,
                iconTint = it.color
            ) {
                sidebarState.close()
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

        items(sidebarState.mailboxState.labels) {
            ProtonSidebarItem(
                icon = painterResource(id = R.drawable.ic_label_filled),
                text = it.text,
                iconTint = it.color
            ) {
                sidebarState.close()
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
                sidebarState.close()
                onSettings()
            }
            ProtonSidebarReportBugItem {
                sidebarState.close()
                onReportBug()
            }
            ProtonSidebarSignOutItem {
                sidebarState.close()
                onRemove(null)
            }

            ProtonSidebarAppVersionItem(
                name = sidebarState.appName,
                version = sidebarState.appVersion
            )
        }
    }
}

@Preview(
    name = "Sidebar in light mode",
    uiMode = Configuration.UI_MODE_NIGHT_NO,
    showBackground = true,
)
@Preview(
    name = "Sidebar in dark mode",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true,
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
            onReportBug = {},
            sidebarState = SidebarState(
                mailboxState = MailboxState(
                    currentLocations = setOf(MailLocation.Inbox)
                ),
                hasPrimaryAccount = false,
            ),
        )
    }
}
