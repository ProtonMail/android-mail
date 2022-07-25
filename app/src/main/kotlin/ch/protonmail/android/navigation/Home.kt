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

package ch.protonmail.android.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Scaffold
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import ch.protonmail.android.mailmailbox.presentation.sidebar.Sidebar
import ch.protonmail.android.navigation.model.Destination.Dialog
import ch.protonmail.android.navigation.model.Destination.Screen
import ch.protonmail.android.navigation.route.addAccountSettings
import ch.protonmail.android.navigation.route.addConversationDetail
import ch.protonmail.android.navigation.route.addConversationModeSettings
import ch.protonmail.android.navigation.route.addEditSwipeActionsSettings
import ch.protonmail.android.navigation.route.addLanguageSettings
import ch.protonmail.android.navigation.route.addMailbox
import ch.protonmail.android.navigation.route.addMessageDetail
import ch.protonmail.android.navigation.route.addRemoveAccountDialog
import ch.protonmail.android.navigation.route.addSettings
import ch.protonmail.android.navigation.route.addSwipeActionsSettings
import ch.protonmail.android.navigation.route.addThemeSettings
import kotlinx.coroutines.launch
import me.proton.core.compose.theme.ProtonTheme

@Composable
fun Home(launcherActions: Launcher.Actions) {
    val navController = rememberNavController()
    val scaffoldState = rememberScaffoldState()
    val scope = rememberCoroutineScope()

    Scaffold(
        scaffoldState = scaffoldState,
        drawerShape = RectangleShape,
        drawerScrimColor = ProtonTheme.colors.blenderNorm,
        drawerContent = {
            Sidebar(
                drawerState = scaffoldState.drawerState,
                actions = buildSidebarActions(navController, launcherActions)
            )
        }
    ) { contentPadding ->
        Box(
            Modifier.padding(contentPadding)
        ) {
            NavHost(
                navController = navController,
                startDestination = Screen.Mailbox.route,
            ) {
                // home
                addConversationDetail()
                addMailbox(
                    navController,
                    openDrawerMenu = { scope.launch { scaffoldState.drawerState.open() } }
                )
                addMessageDetail()
                addRemoveAccountDialog(navController)
                addSettings(navController)
                // settings
                addAccountSettings(navController, launcherActions)
                addConversationModeSettings(navController)
                addEditSwipeActionsSettings(navController)
                addLanguageSettings(navController)
                addSwipeActionsSettings(navController)
                addThemeSettings(navController)
            }
        }
    }
}


private fun buildSidebarActions(
    navController: NavHostController,
    launcherActions: Launcher.Actions
) = Sidebar.Actions(
    onSignIn = launcherActions.onSignIn,
    onSignOut = launcherActions.onSignOut,
    onRemoveAccount = { navController.navigate(Dialog.RemoveAccount(it)) },
    onSwitchAccount = launcherActions.onSwitchAccount,
    onSettings = { navController.navigate(Screen.Settings.route) },
    onLabelsSettings = { /*navController.navigate(...)*/ },
    onSubscription = launcherActions.onSubscription,
    onReportBug = launcherActions.onReportBug
)
