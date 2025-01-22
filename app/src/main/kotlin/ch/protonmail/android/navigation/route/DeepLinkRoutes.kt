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

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.navDeepLink
import ch.protonmail.android.R
import ch.protonmail.android.mailnotifications.domain.NotificationInteraction
import ch.protonmail.android.mailnotifications.domain.NotificationsDeepLinkHelper
import ch.protonmail.android.mailnotifications.domain.resolveNotificationInteraction
import ch.protonmail.android.navigation.deeplinks.NotificationsDeepLinksViewModel
import ch.protonmail.android.navigation.model.Destination

@Suppress("ComplexMethod", "LongMethod")
internal fun NavGraphBuilder.addDeepLinkHandler(navController: NavHostController) {
    composable(
        route = Destination.Screen.DeepLinksHandler.route,
        deepLinks = listOf(
            navDeepLink { uriPattern = NotificationsDeepLinkHelper.DeepLinkMessageTemplate },
            navDeepLink { uriPattern = NotificationsDeepLinkHelper.DeepLinkMessageGroupTemplate }
        )
    ) {
        val context = LocalContext.current
        val viewModel: NotificationsDeepLinksViewModel = hiltViewModel()
        val state = viewModel.state.collectAsState().value

        LaunchedEffect(key1 = state) {
            when (state) {
                is NotificationsDeepLinksViewModel.State.Launched -> {
                    val interaction = resolveNotificationInteraction(
                        userId = it.arguments.userId,
                        messageId = it.arguments.messageId,
                        action = it.arguments.action
                    )

                    when (interaction) {
                        is NotificationInteraction.SingleTap -> {
                            viewModel.navigateToMessage(messageId = interaction.messageId, userId = interaction.userId)
                        }

                        is NotificationInteraction.GroupTap -> {
                            viewModel.navigateToInbox(interaction.userId)
                        }

                        NotificationInteraction.NoAction -> Unit
                    }
                }

                is NotificationsDeepLinksViewModel.State.NavigateToInbox.ActiveUser -> {
                    navController.navigate(Destination.Screen.Mailbox.route) {
                        popUpTo(navController.graph.id) { inclusive = false }
                    }
                }

                is NotificationsDeepLinksViewModel.State.NavigateToInbox.ActiveUserSwitched -> {
                    navController.navigate(Destination.Screen.Mailbox.route) {
                        popUpTo(navController.graph.id) { inclusive = true }
                    }
                    showUserSwitchedEmailIfRequired(context, state.email)
                }

                is NotificationsDeepLinksViewModel.State.NavigateToMessageDetails -> {
                    navController.navigate(Destination.Screen.Message(state.messageId)) {
                        popUpTo(Destination.Screen.Mailbox.route) { inclusive = false }
                    }
                    showUserSwitchedEmailIfRequired(context, state.userSwitchedEmail)
                }

                is NotificationsDeepLinksViewModel.State.NavigateToConversation -> {
                    navController.navigate(Destination.Screen.Conversation(conversationId = state.conversationId)) {
                        popUpTo(Destination.Screen.Mailbox.route) { inclusive = false }
                    }
                    showUserSwitchedEmailIfRequired(context, state.userSwitchedEmail)
                }
            }
        }
    }
}

private fun showUserSwitchedEmailIfRequired(context: Context, email: String?) {
    if (email.isNullOrBlank()) return

    Toast.makeText(
        context,
        context.getString(R.string.notification_switched_account, email),
        Toast.LENGTH_LONG
    ).show()
}

private val Bundle?.messageId: String?
    get() = this?.getString("messageId")

private val Bundle?.userId: String?
    get() = this?.getString("userId")

private val Bundle?.action: String?
    get() = this?.getString("action")
