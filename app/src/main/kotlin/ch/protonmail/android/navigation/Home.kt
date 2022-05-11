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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Scaffold
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.dialog
import androidx.navigation.compose.rememberNavController
import ch.protonmail.android.feature.account.RemoveAccountDialog
import ch.protonmail.android.mailconversation.domain.entity.ConversationId
import ch.protonmail.android.mailconversation.presentation.ConversationDetail
import ch.protonmail.android.mailmailbox.domain.model.MailboxItem
import ch.protonmail.android.mailmailbox.domain.model.MailboxItemType
import ch.protonmail.android.mailmailbox.presentation.MailboxScreen
import ch.protonmail.android.mailmailbox.presentation.Sidebar
import ch.protonmail.android.mailsettings.presentation.accountsettings.AccountSettingScreen
import ch.protonmail.android.mailsettings.presentation.addConversationModeSettings
import ch.protonmail.android.mailsettings.presentation.addLanguageSettings
import ch.protonmail.android.mailsettings.presentation.addSwipeActionsSettings
import ch.protonmail.android.mailsettings.presentation.addThemeSettings
import ch.protonmail.android.mailsettings.presentation.settings.MainSettingsScreen
import ch.protonmail.android.navigation.model.Destination
import ch.protonmail.android.navigation.model.Destination.Dialog
import ch.protonmail.android.navigation.model.Destination.Screen
import kotlinx.coroutines.launch
import me.proton.core.compose.navigation.require
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.domain.entity.UserId
import timber.log.Timber

@Composable
fun Home(
    onSignIn: (UserId?) -> Unit,
    onSignOut: (UserId) -> Unit,
    onSwitch: (UserId) -> Unit,
    onSubscription: () -> Unit,
    onReportBug: () -> Unit,
) {
    val navController = rememberNavController()
    val scaffoldState = rememberScaffoldState()
    val scope = rememberCoroutineScope()

    Scaffold(
        scaffoldState = scaffoldState,
        drawerShape = RectangleShape,
        drawerScrimColor = ProtonTheme.colors.blenderNorm,
        drawerContent = {
            Sidebar(
                onRemove = { navController.navigate(Dialog.RemoveAccount(it)) },
                onSignOut = onSignOut,
                onSignIn = onSignIn,
                onSwitch = onSwitch,
                onMailLocation = { /* stack screens? */ },
                onFolder = { /*navController.navigate(...)*/ },
                onLabel = { /*navController.navigate(...)*/ },
                onSettings = { navController.navigate(Screen.Settings.route) },
                onSubscription = onSubscription,
                onReportBug = onReportBug,
                drawerState = scaffoldState.drawerState
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
                addMailbox(
                    navController,
                    openDrawerMenu = { scope.launch { scaffoldState.drawerState.open() } }
                )
                addConversationDetail()
                addRemoveAccountDialog(navController)
                addSettings(navController)
                addAccountSettings(navController)
                addConversationModeSettings(navController, Screen.ConversationModeSettings.route)
                addThemeSettings(navController, Screen.ThemeSettings.route)
                addLanguageSettings(navController, Screen.LanguageSettings.route)
                addSwipeActionsSettings(navController, Screen.SwipeActionsSettings.route)
            }
        }
    }
}

private fun NavGraphBuilder.addMailbox(
    navController: NavHostController,
    openDrawerMenu: () -> Unit
) = composable(
    route = Screen.Mailbox.route
) {
    MailboxScreen(
        navigateToMailboxItem = { item: MailboxItem ->
            navController.navigate(
                when (item.type) {
                    MailboxItemType.Message -> Screen.Conversation(ConversationId(item.id))
                    MailboxItemType.Conversation -> Screen.Conversation(ConversationId(item.id))
                }
            )
        },
        openDrawerMenu = openDrawerMenu
    )
}

private fun NavGraphBuilder.addConversationDetail() = composable(
    route = Screen.Conversation.route,
) {
    ConversationDetail(Screen.Conversation.getConversationId(it.require(Destination.key)))
}

private fun NavGraphBuilder.addRemoveAccountDialog(navController: NavHostController) = dialog(
    route = Dialog.RemoveAccount.route,
) {
    RemoveAccountDialog(
        userId = Dialog.RemoveAccount.getUserId(it.require(Destination.key)),
        onRemoved = { navController.popBackStack() },
        onCancelled = { navController.popBackStack() }
    )
}

fun NavGraphBuilder.addSettings(navController: NavHostController) = composable(
    route = Screen.Settings.route
) {
    MainSettingsScreen(
        onAccountClicked = {
            Timber.d("Navigating to account settings")
            navController.navigate(Screen.AccountSettings.route)
        },
        onThemeClick = {
            Timber.d("Navigating to theme settings")
            navController.navigate(Screen.ThemeSettings.route)
        },
        onPushNotificationsClick = {
            Timber.i("Push Notifications setting clicked")
        },
        onAutoLockClick = {
            Timber.i("Auto Lock setting clicked")
        },
        onAlternativeRoutingClick = {
            Timber.i("Alternative routing setting clicked")
        },
        onAppLanguageClick = {
            Timber.d("Navigating to language settings")
            navController.navigate(Screen.LanguageSettings.route)
        },
        onCombinedContactsClick = {
            Timber.i("Combined contacts setting clicked")
        },
        onSwipeActionsClick = {
            Timber.d("Swipe actions setting clicked")
            navController.navigate(Screen.SwipeActionsSettings.route)
        },
        onBackClick = {
            navController.popBackStack()
        }
    )
}

fun NavGraphBuilder.addAccountSettings(navController: NavHostController) = composable(
    route = Screen.AccountSettings.route
) {
    AccountSettingScreen(
        onBackClick = { navController.popBackStack() },
        onPasswordManagementClick = {
            Timber.i("Password management setting clicked")
        },
        onRecoveryEmailClick = {
            Timber.i("Recovery email setting clicked")
        },
        onConversationModeClick = {
            Timber.d("Navigating to conversation mode settings")
            navController.navigate(Screen.ConversationModeSettings.route)
        },
        onDefaultEmailAddressClick = {
            Timber.i("Default email address setting clicked")
        },
        onDisplayNameClick = {
            Timber.i("Display name setting clicked")
        },
        onPrivacyClick = {
            Timber.i("Privacy setting clicked")
        },
        onSearchMessageContentClick = {
            Timber.i("Search message content setting clicked")
        },
        onLabelsFoldersClick = {
            Timber.i("Labels folders setting clicked")
        },
        onLocalStorageClick = {
            Timber.i("Local storage setting clicked")
        },
        onSnoozeNotificationsClick = {
            Timber.i("Snooze notification setting clicked")
        }
    )
}
