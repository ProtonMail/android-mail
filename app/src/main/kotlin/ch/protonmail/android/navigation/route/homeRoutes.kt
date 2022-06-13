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

package ch.protonmail.android.navigation.route

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.compose.dialog
import ch.protonmail.android.feature.account.RemoveAccountDialog
import ch.protonmail.android.mailconversation.domain.entity.ConversationId
import ch.protonmail.android.mailconversation.presentation.ConversationDetailScreen
import ch.protonmail.android.mailmailbox.domain.model.MailboxItemType
import ch.protonmail.android.mailmailbox.presentation.MailboxScreen
import ch.protonmail.android.mailmessage.domain.entity.MessageId
import ch.protonmail.android.mailmessage.presentation.MessageDetailScreen
import ch.protonmail.android.mailsettings.presentation.settings.MainSettingsScreen
import ch.protonmail.android.navigation.model.Destination
import me.proton.core.compose.navigation.require
import timber.log.Timber

internal fun NavGraphBuilder.addConversationDetail() {
    composable(route = Destination.Screen.Conversation.route) {
        ConversationDetailScreen(Destination.Screen.Conversation.getConversationId(it.require(Destination.key)))
    }
}

internal fun NavGraphBuilder.addMailbox(
    navController: NavHostController,
    openDrawerMenu: () -> Unit
) {
    composable(route = Destination.Screen.Mailbox.route) {
        MailboxScreen(
            navigateToMailboxItem = { request ->
                navController.navigate(
                    when (request.itemType) {
                        MailboxItemType.Message -> Destination.Screen.Message(MessageId(request.itemId.value))
                        MailboxItemType.Conversation ->
                            Destination.Screen.Conversation(ConversationId(request.itemId.value))
                    }
                )
            },
            openDrawerMenu = openDrawerMenu
        )
    }
}

internal fun NavGraphBuilder.addMessageDetail() {
    composable(route = Destination.Screen.Message.route) {
        MessageDetailScreen(Destination.Screen.Message.getMessageId(it.require(Destination.key)))
    }
}

internal fun NavGraphBuilder.addRemoveAccountDialog(navController: NavHostController) {
    dialog(route = Destination.Dialog.RemoveAccount.route) {
        RemoveAccountDialog(
            userId = Destination.Dialog.RemoveAccount.getUserId(it.require(Destination.key)),
            onRemoved = { navController.popBackStack() },
            onCancelled = { navController.popBackStack() }
        )
    }
}

internal fun NavGraphBuilder.addSettings(navController: NavHostController) {
    composable(route = Destination.Screen.Settings.route) {
        MainSettingsScreen(
            actions = MainSettingsScreen.Actions(
                onAccountClick = {
                    navController.navigate(Destination.Screen.AccountSettings.route)
                },
                onThemeClick = {
                    navController.navigate(Destination.Screen.ThemeSettings.route)
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
                    navController.navigate(Destination.Screen.LanguageSettings.route)
                },
                onCombinedContactsClick = {
                    Timber.i("Combined contacts setting clicked")
                },
                onSwipeActionsClick = {
                    Timber.d("Swipe actions setting clicked")
                    navController.navigate(Destination.Screen.SwipeActionsSettings.route)
                },
                onBackClick = {
                    navController.popBackStack()
                }
            )
        )
    }
}

