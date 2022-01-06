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

import androidx.compose.material.DrawerState
import androidx.compose.material.DrawerValue
import androidx.compose.material.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import ch.protonmail.android.BuildConfig
import ch.protonmail.android.mailmailbox.presentation.MailboxState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import me.proton.core.accountmanager.presentation.compose.AccountPrimaryState
import me.proton.core.accountmanager.presentation.compose.rememberAccountPrimaryState

@Stable
class SidebarState(
    val drawerState: DrawerState = DrawerState(DrawerValue.Closed),
    val mailboxState: MailboxState = MailboxState(),
    val accountPrimaryState: AccountPrimaryState = AccountPrimaryState(),
    val hasPrimaryAccount: Boolean = true,
    val appName: String = "ProtonMail",
    val appVersion: String = BuildConfig.VERSION_NAME,
    val scope: CoroutineScope? = null,
) {
    fun close() = scope?.launch {
        accountPrimaryState.dismissDialog()
        drawerState.close()
    }
}

@Composable
fun rememberSidebarState(
    drawerState: DrawerState = rememberDrawerState(DrawerValue.Closed),
    accountPrimaryState: AccountPrimaryState = rememberAccountPrimaryState(),
    mailboxState: MailboxState = remember { MailboxState() },
    scope: CoroutineScope = rememberCoroutineScope(),
): SidebarState = remember(mailboxState) {
    SidebarState(
        drawerState = drawerState,
        mailboxState = mailboxState,
        accountPrimaryState = accountPrimaryState,
        scope = scope
    )
}
