/*
 * Copyright (c) 2025 Proton Technologies AG
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

package ch.protonmail.android.navigation.deeplinks

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import ch.protonmail.android.mailconversation.domain.entity.ConversationDetailEntryPoint
import ch.protonmail.android.navigation.model.Destination
import timber.log.Timber

@Composable
internal fun DeepLinkNavigationEffect(navController: NavHostController, onUserSwitched: (email: String) -> Unit) {
    val viewModel: NotificationsDeepLinksViewModel = hiltViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state) {
        Timber.d("DeepLinkNavigationEffect: State changed to $state")
        when (val current = state) {
            is NotificationsDeepLinksViewModel.State.Launched -> {}

            is NotificationsDeepLinksViewModel.State.NavigateToInbox.ActiveUser -> {
                viewModel.consume()
            }

            is NotificationsDeepLinksViewModel.State.NavigateToInbox.ActiveUserSwitched -> {
                onUserSwitched(current.email)
                viewModel.consume()
            }

            is NotificationsDeepLinksViewModel.State.NavigateToConversation -> {
                Timber.d("Navigating to conversation ${current.conversationId}, scrollTo=${current.scrollToMessageId}")
                navController.navigate(
                    Destination.Screen.Conversation(
                        conversationId = current.conversationId,
                        scrollToMessageId = current.scrollToMessageId,
                        openedFromLocation = current.contextLabelId,
                        isSingleMessageMode = false,
                        entryPoint = ConversationDetailEntryPoint.PushNotification,
                        locationViewModeIsConversation = true
                    )
                ) {
                    popUpTo(Destination.Screen.Mailbox.route) { inclusive = false }
                }
                current.userSwitchedEmail?.let { onUserSwitched(it) }
                viewModel.consume()
            }

            is NotificationsDeepLinksViewModel.State.NavigateToMessage -> {
                Timber.d("Navigating to message ${current.messageId} in convo ${current.conversationId}")
                navController.navigate(
                    Destination.Screen.Conversation(
                        conversationId = current.conversationId,
                        scrollToMessageId = current.messageId,
                        openedFromLocation = current.contextLabelId,
                        isSingleMessageMode = true,
                        entryPoint = ConversationDetailEntryPoint.PushNotification,
                        locationViewModeIsConversation = false
                    )
                ) {
                    popUpTo(Destination.Screen.Mailbox.route) { inclusive = false }
                }
                current.userSwitchedEmail?.let { onUserSwitched(it) }
                viewModel.consume()
            }
        }
    }
}
