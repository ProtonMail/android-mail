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

import androidx.compose.material.DrawerState
import androidx.compose.material.DrawerValue
import androidx.compose.material.DrawerValue.Closed
import androidx.compose.material.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import ch.protonmail.android.mailcommon.domain.AppInformation
import ch.protonmail.android.mailmailbox.domain.model.SidebarLocation
import ch.protonmail.android.mailmailbox.domain.model.SidebarLocation.Inbox
import me.proton.core.accountmanager.presentation.compose.AccountPrimaryState
import me.proton.core.accountmanager.presentation.compose.rememberAccountPrimaryState
import me.proton.core.label.domain.entity.LabelId

@Stable
class SidebarState(
    selectedLocation: SidebarLocation = Inbox,
    val appInformation: AppInformation = AppInformation(),
    val drawerState: DrawerState = DrawerState(Closed),
    val accountPrimaryState: AccountPrimaryState = AccountPrimaryState(),
    val hasPrimaryAccount: Boolean = true,
    sidebarFolderUiModels: List<SidebarFolderUiModel> = FAKE_FOLDERS,
    sidebarLabelUiModels: List<SidebarLabelUiModel> = FAKE_LABELS,
    unreadCounters: Map<LabelId, Int?> = FAKE_UNREAD_COUNTERS,
    isSubscriptionVisible: Boolean = true
) {
    var selectedLocation by mutableStateOf(selectedLocation)
    var sidebarFolderUiModels by mutableStateOf(sidebarFolderUiModels)
    var sidebarLabelUiModels by mutableStateOf(sidebarLabelUiModels)
    var unreadCounters by mutableStateOf(unreadCounters)
    var isSubscriptionVisible by mutableStateOf(isSubscriptionVisible)
}

val FAKE_UNREAD_COUNTERS: Map<LabelId, Int?> = mapOf(
    Pair(LabelId("0"), 1),
    Pair(LabelId("3"), null),
    Pair(LabelId("4"), null),
    Pair(LabelId("5"), 4),
    Pair(LabelId("6"), null),
    Pair(LabelId("7"), null),
    Pair(LabelId("8"), null),
    Pair(LabelId("10"), 1),
    Pair(LabelId("f1"), 2)
)

private val FAKE_FOLDERS = listOf(
    SidebarFolderUiModel(LabelId("f1"), "Folder 1", Color.Red)
)

private val FAKE_LABELS = listOf(
    SidebarLabelUiModel(LabelId("l1"), "Label 1", Color.Cyan),
    SidebarLabelUiModel(LabelId("l2"), "Label 2", Color.Yellow)
)

@Composable
fun rememberSidebarState(
    appInformation: AppInformation,
    drawerState: DrawerState = rememberDrawerState(DrawerValue.Closed),
    accountPrimaryState: AccountPrimaryState = rememberAccountPrimaryState(),
): SidebarState = remember {
    SidebarState(
        appInformation = appInformation,
        drawerState = drawerState,
        accountPrimaryState = accountPrimaryState
    )
}
