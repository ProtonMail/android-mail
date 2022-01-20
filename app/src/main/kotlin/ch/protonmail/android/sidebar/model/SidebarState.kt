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

package ch.protonmail.android.sidebar.model

import androidx.compose.material.DrawerState
import androidx.compose.material.DrawerValue
import androidx.compose.material.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import ch.protonmail.android.BuildConfig
import ch.protonmail.android.mailmessage.domain.model.MailLocation
import ch.protonmail.android.mailmessage.domain.model.MailLocation.Inbox
import me.proton.core.accountmanager.presentation.compose.AccountPrimaryState
import me.proton.core.accountmanager.presentation.compose.rememberAccountPrimaryState

@Stable
data class SidebarState(
    val selectedLocation: MailLocation = Inbox,
    val drawerState: DrawerState = DrawerState(DrawerValue.Closed),
    val accountPrimaryState: AccountPrimaryState = AccountPrimaryState(),
    val hasPrimaryAccount: Boolean = true,
    val appName: String = "ProtonMail",
    val appVersion: String = BuildConfig.VERSION_NAME,
    val folderUiModels: List<FolderUiModel> = FAKE_FOLDERS,
    val labelUiModels: List<LabelUiModel> = FAKE_LABELS,
    val counters: UnreadCounters = UnreadCounters()
)

private val FAKE_FOLDERS = listOf(
    FolderUiModel("1", "Folder 1", Color.Red)
)

private val FAKE_LABELS = listOf(
    LabelUiModel("1", "Label 1", Color.Cyan),
    LabelUiModel("2", "Label 2", Color.Yellow)
)

@Composable
fun rememberSidebarState(
    drawerState: DrawerState = rememberDrawerState(DrawerValue.Closed),
    accountPrimaryState: AccountPrimaryState = rememberAccountPrimaryState(),
): SidebarState = remember {
    SidebarState(
        drawerState = drawerState,
        accountPrimaryState = accountPrimaryState
    )
}
