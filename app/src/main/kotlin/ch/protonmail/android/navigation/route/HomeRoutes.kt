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

import android.net.Uri
import android.widget.Toast
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.compose.dialog
import androidx.navigation.navDeepLink
import ch.protonmail.android.R
import ch.protonmail.android.feature.account.RemoveAccountDialog
import ch.protonmail.android.feature.account.RemoveAccountDialog.USER_ID_KEY
import ch.protonmail.android.mailcommon.domain.model.ConversationId
import ch.protonmail.android.mailcomposer.presentation.ui.ComposerScreen
import ch.protonmail.android.maildetail.domain.model.OpenAttachmentIntentValues
import ch.protonmail.android.maildetail.presentation.ui.ConversationDetail
import ch.protonmail.android.maildetail.presentation.ui.ConversationDetailScreen
import ch.protonmail.android.maildetail.presentation.ui.MessageDetail
import ch.protonmail.android.maildetail.presentation.ui.MessageDetailScreen
import ch.protonmail.android.mailmailbox.domain.model.MailboxItemType
import ch.protonmail.android.mailmailbox.presentation.mailbox.MailboxScreen
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailnotifications.domain.NotificationsDeepLinkHelper.Companion.DEEP_LINK_MESSAGE_GROUP_TEMPLATE
import ch.protonmail.android.mailnotifications.domain.NotificationsDeepLinkHelper.Companion.DEEP_LINK_MESSAGE_TEMPLATE
import ch.protonmail.android.mailsettings.presentation.settings.MainSettingsScreen
import ch.protonmail.android.navigation.deeplinks.NotificationsDeepLinksViewModel
import ch.protonmail.android.navigation.model.Destination
import me.proton.core.compose.navigation.get
import me.proton.core.domain.entity.UserId
import me.proton.core.util.kotlin.takeIfNotBlank
import timber.log.Timber

internal fun NavGraphBuilder.addConversationDetail(
    navController: NavHostController,
    showSnackbar: (message: String) -> Unit,
    openMessageBodyLink: (uri: Uri) -> Unit,
    openAttachment: (values: OpenAttachmentIntentValues) -> Unit,
    showFeatureMissingSnackbar: () -> Unit
) {
    composable(route = Destination.Screen.Conversation.route) {
        ConversationDetailScreen(
            actions = ConversationDetail.Actions(
                onExit = { notifyUserMessage ->
                    navController.popBackStack()
                    notifyUserMessage?.let(showSnackbar)
                },
                openMessageBodyLink = { url -> openMessageBodyLink(Uri.parse(url)) },
                openAttachment = openAttachment,
                showFeatureMissingSnackbar = showFeatureMissingSnackbar
            )
        )
    }
}

internal fun NavGraphBuilder.addMailbox(
    navController: NavHostController,
    openDrawerMenu: () -> Unit,
    showOfflineSnackbar: () -> Unit,
    showFeatureMissingSnackbar: () -> Unit,
    showRefreshErrorSnackbar: () -> Unit
) {
    composable(route = Destination.Screen.Mailbox.route) {
        MailboxScreen(
            actions = MailboxScreen.Actions.Empty.copy(
                navigateToMailboxItem = { request ->
                    val destination = when (request.shouldOpenInComposer) {
                        true -> Destination.Screen.PrefilledComposer(MessageId(request.itemId.value))
                        false -> when (request.itemType) {
                            MailboxItemType.Message -> Destination.Screen.Message(MessageId(request.itemId.value))
                            MailboxItemType.Conversation ->
                                Destination.Screen.Conversation(ConversationId(request.itemId.value))
                        }
                    }
                    navController.navigate(destination)
                },
                navigateToComposer = { navController.navigate(Destination.Screen.Composer.route) },
                openDrawerMenu = openDrawerMenu,
                showOfflineSnackbar = showOfflineSnackbar,
                showRefreshErrorSnackbar = showRefreshErrorSnackbar,
                showFeatureMissingSnackbar = showFeatureMissingSnackbar
            )
        )
    }
}

internal fun NavGraphBuilder.addMessageDetail(
    navController: NavHostController,
    showSnackbar: (notifyUserMessage: String) -> Unit,
    openMessageBodyLink: (uri: Uri) -> Unit,
    openAttachment: (values: OpenAttachmentIntentValues) -> Unit,
    showFeatureMissingSnackbar: () -> Unit
) {
    composable(route = Destination.Screen.Message.route) {
        MessageDetailScreen(
            actions = MessageDetail.Actions(
                onExit = { notifyUserMessage ->
                    navController.popBackStack()
                    notifyUserMessage?.let(showSnackbar)
                },
                openMessageBodyLink = openMessageBodyLink,
                openAttachment = openAttachment,
                showFeatureMissingSnackbar = showFeatureMissingSnackbar
            )
        )
    }
}

internal fun NavGraphBuilder.addComposer(navController: NavHostController, showDraftSavedSnackbar: () -> Unit) {
    composable(route = Destination.Screen.Composer.route) {
        ComposerScreen(
            onCloseComposerClick = navController::popBackStack,
            showDraftSavedSnackbar = showDraftSavedSnackbar
        )
    }
    composable(route = Destination.Screen.PrefilledComposer.route) {
        ComposerScreen(
            onCloseComposerClick = navController::popBackStack,
            showDraftSavedSnackbar = showDraftSavedSnackbar
        )
    }
}

internal fun NavGraphBuilder.addRemoveAccountDialog(navController: NavHostController) {
    dialog(route = Destination.Dialog.RemoveAccount.route) {
        RemoveAccountDialog(
            userId = it.get<String>(USER_ID_KEY)?.takeIfNotBlank()?.let(::UserId),
            onRemoved = { navController.popBackStack() },
            onCancelled = { navController.popBackStack() }
        )
    }
}

internal fun NavGraphBuilder.addSettings(navController: NavHostController, showFeatureMissingSnackbar: () -> Unit) {
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
                    showFeatureMissingSnackbar()
                },
                onAutoLockClick = {
                    showFeatureMissingSnackbar()
                },
                onAlternativeRoutingClick = {
                    navController.navigate(Destination.Screen.AlternativeRoutingSettings.route)
                },
                onAppLanguageClick = {
                    navController.navigate(Destination.Screen.LanguageSettings.route)
                },
                onCombinedContactsClick = {
                    navController.navigate(Destination.Screen.CombinedContactsSettings.route)
                },
                onSwipeActionsClick = {
                    navController.navigate(Destination.Screen.SwipeActionsSettings.route)
                },
                onBackClick = {
                    navController.popBackStack()
                }
            )
        )
    }
}

@Suppress("ComplexMethod")
internal fun NavGraphBuilder.addDeepLinkHandler(navController: NavHostController) {
    composable(
        route = Destination.Screen.DeepLinksHandler.route,
        deepLinks = listOf(
            navDeepLink { uriPattern = DEEP_LINK_MESSAGE_TEMPLATE },
            navDeepLink { uriPattern = DEEP_LINK_MESSAGE_GROUP_TEMPLATE }
        )
    ) {
        val context = LocalContext.current
        val viewModel: NotificationsDeepLinksViewModel = hiltViewModel()
        val state = viewModel.state.collectAsState().value

        fun onGroupNotification(
            messageId: String?,
            userId: String?,
            action: (userId: String) -> Unit
        ) = when {
            userId != null && messageId == null -> action(userId)
            else -> Unit
        }

        fun onMessageNotification(
            messageId: String?,
            userId: String?,
            action: (messageId: String, userId: String) -> Unit
        ) = when {
            messageId != null && userId != null -> action(messageId, userId)
            else -> Unit
        }

        fun showUserSwitchedEmailIfRequired(email: String?) {
            if (email.isNullOrBlank()) return

            Toast.makeText(
                context,
                context.getString(R.string.notification_switched_account, email),
                Toast.LENGTH_LONG
            ).show()
        }

        LaunchedEffect(key1 = state) {
            when (state) {
                is NotificationsDeepLinksViewModel.State.None -> {
                    Timber.d("Deep link state is None")
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
                    showUserSwitchedEmailIfRequired(state.email)
                }

                is NotificationsDeepLinksViewModel.State.NavigateToMessageDetails -> {
                    navController.navigate(Destination.Screen.Message(state.messageId)) {
                        popUpTo(Destination.Screen.Mailbox.route) { inclusive = false }
                    }
                    showUserSwitchedEmailIfRequired(state.userSwitchedEmail)
                }

                is NotificationsDeepLinksViewModel.State.NavigateToConversation -> {
                    navController.navigate(Destination.Screen.Conversation(state.conversationId)) {
                        popUpTo(Destination.Screen.Mailbox.route) { inclusive = false }
                    }
                    showUserSwitchedEmailIfRequired(state.userSwitchedEmail)
                }
            }
            val messageIdArg = it.arguments?.getString("messageId")
            val userIdArg = it.arguments?.getString("userId")
            onGroupNotification(messageIdArg, userIdArg) { userId ->
                viewModel.navigateToInbox(userId = userId)
            }
            onMessageNotification(messageIdArg, userIdArg) { messageId, userId ->
                viewModel.navigateToMessage(messageId = messageId, userId = userId)
            }
        }
    }
}
