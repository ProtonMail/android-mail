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
import androidx.compose.material.ScaffoldState
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import ch.protonmail.android.R
import ch.protonmail.android.mailcommon.presentation.ConsumableLaunchedEffect
import ch.protonmail.android.mailmailbox.presentation.sidebar.Sidebar
import ch.protonmail.android.navigation.model.Destination.Dialog
import ch.protonmail.android.navigation.model.Destination.Screen
import ch.protonmail.android.navigation.model.HomeState
import ch.protonmail.android.navigation.route.addAccountSettings
import ch.protonmail.android.navigation.route.addAlternativeRoutingSetting
import ch.protonmail.android.navigation.route.addCombinedContactsSetting
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
import io.sentry.compose.withSentryObservableEffect
import kotlinx.coroutines.launch
import me.proton.core.compose.component.ProtonSnackbarHost
import me.proton.core.compose.component.ProtonSnackbarHostState
import me.proton.core.compose.component.ProtonSnackbarType
import me.proton.core.compose.flow.rememberAsState
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.network.domain.NetworkStatus

@Composable
fun Home(
    launcherActions: Launcher.Actions,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val navController = rememberNavController().withSentryObservableEffect()
    val scaffoldState = rememberScaffoldState()
    val scope = rememberCoroutineScope()
    val state = rememberAsState(flow = viewModel.state, initial = HomeState.Initial)

    val offlineSnackbarMessage = stringResource(id = R.string.you_are_offline)
    ConsumableLaunchedEffect(state.value.networkStatusEffect) {
        if (it == NetworkStatus.Disconnected) {
            showSnackbar(scaffoldState, offlineSnackbarMessage)
        }
    }

    Scaffold(
        scaffoldState = scaffoldState,
        drawerShape = RectangleShape,
        drawerScrimColor = ProtonTheme.colors.blenderNorm,
        drawerContent = {
            Sidebar(
                drawerState = scaffoldState.drawerState,
                navigationActions = buildSidebarActions(navController, launcherActions)
            )
        },
        snackbarHost = { snackbarHostState ->
            ProtonSnackbarHost(
                hostState = ProtonSnackbarHostState(snackbarHostState, ProtonSnackbarType.WARNING)
            )
        }
    ) { contentPadding ->
        Box(
            Modifier.padding(contentPadding)
        ) {
            NavHost(
                navController = navController,
                startDestination = Screen.Mailbox.route
            ) {
                // home
                addConversationDetail(navController)
                addMailbox(
                    navController,
                    openDrawerMenu = { scope.launch { scaffoldState.drawerState.open() } },
                    showOfflineSnackbar = { scope.launch { showSnackbar(scaffoldState, offlineSnackbarMessage) } }
                )
                addMessageDetail(navController)
                addRemoveAccountDialog(navController)
                addSettings(navController)
                // settings
                addAccountSettings(navController, launcherActions)
                addAlternativeRoutingSetting(navController)
                addCombinedContactsSetting(navController)
                addConversationModeSettings(navController)
                addEditSwipeActionsSettings(navController)
                addLanguageSettings(navController)
                addSwipeActionsSettings(navController)
                addThemeSettings(navController)
            }
        }
    }
}

private suspend fun showSnackbar(
    scaffoldState: ScaffoldState,
    message: String
) {
    scaffoldState.snackbarHostState.showSnackbar(message)
}

private fun buildSidebarActions(
    navController: NavHostController,
    launcherActions: Launcher.Actions
) = Sidebar.NavigationActions(
    onSignIn = launcherActions.onSignIn,
    onSignOut = launcherActions.onSignOut,
    onRemoveAccount = { navController.navigate(Dialog.RemoveAccount(it)) },
    onSwitchAccount = launcherActions.onSwitchAccount,
    onSettings = { navController.navigate(Screen.Settings.route) },
    onLabelsSettings = {},
    onSubscription = launcherActions.onSubscription,
    onReportBug = launcherActions.onReportBug
)
