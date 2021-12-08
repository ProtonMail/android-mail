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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Scaffold
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NamedNavArgument
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.dialog
import androidx.navigation.compose.navArgument
import androidx.navigation.compose.rememberNavController
import ch.protonmail.android.mailconversation.domain.ConversationId
import ch.protonmail.android.mailconversation.presentation.ConversationDetail
import ch.protonmail.android.mailmailbox.presentation.MailboxScreen
import ch.protonmail.android.navigation.common.require
import ch.protonmail.android.navigation.drawer.NavigationDrawer
import ch.protonmail.android.navigation.drawer.SignOutConfirmationDialog
import ch.protonmail.android.navigation.launcher.LauncherScreen
import com.google.accompanist.insets.statusBarsPadding
import com.google.accompanist.insets.systemBarsPadding
import me.proton.core.accountmanager.presentation.view.AccountPrimaryView
import timber.log.Timber

@Composable
fun AppNavGraph(
    onAccountViewAdded: (AccountPrimaryView) -> Unit,
    navigateToLogin: () -> Unit
) {
    val navController = rememberNavController()
    val scaffoldState = rememberScaffoldState()

    Scaffold(
        scaffoldState = scaffoldState,
        drawerContent = {
            NavigationDrawer(
                scaffoldState.drawerState,
                navigateToSigningOut = {
                    navController.navigate(Destination.Dialog.SignOut.route)
                },
                onAccountViewAdded
            )
        }
    ) { contentPadding ->
        Box(
            Modifier
                .padding(contentPadding)
                .run {
                    if (isLandscape()) {
                        systemBarsPadding()
                    } else {
                        statusBarsPadding()
                    }
                }
        ) {
            NavHost(
                navController = navController,
                startDestination = Destination.Launcher.route,
            ) {
                addLauncher(navController, navigateToLogin)
                addMailbox(navController)
                addConversationDetail()
                addSignOutConfirmationDialog(navController)
            }
        }
    }
}

private fun NavGraphBuilder.addLauncher(
    navController: NavHostController,
    navigateToLogin: () -> Unit
) = composable(
    route = Destination.Launcher.route,
) {
    LauncherScreen(
        navigateToMailbox = {
            navController.navigate(Destination.Mailbox.route) {
                popUpTo(Destination.Launcher.route) { inclusive = true }
            }
        },
        navigateToLogin = navigateToLogin
    )
}

private fun NavGraphBuilder.addMailbox(navController: NavHostController) = composable(
    route = Destination.Mailbox.route,
) {
    MailboxScreen({ conversationId: ConversationId ->
        navController.navigate(Destination.ConversationDetail(conversationId))
    })
}

private fun NavGraphBuilder.addConversationDetail(
    arguments: List<NamedNavArgument> = listOf(
        navArgument(Destination.ConversationDetail.CONVERSATION_ID_KEY) {
            type = NavType.StringType
        }
    )
) = composable(
    route = Destination.ConversationDetail.route,
    arguments = arguments
) { navBackStackEntry ->
    val rawConversationId: String = navBackStackEntry.require(
        Destination.ConversationDetail.CONVERSATION_ID_KEY
    )
    ConversationDetail(ConversationId(rawConversationId))
}

private fun NavGraphBuilder.addSignOutConfirmationDialog(navController: NavHostController) = dialog(
    route = Destination.Dialog.SignOut.route
) {
    SignOutConfirmationDialog(
        onRemove = {
            Timber.i("Sign out request confirmed")
            navController.popBackStack()
        },
        onDismiss = { navController.popBackStack() }
    )
}

@Composable
private fun isLandscape() =
    LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
