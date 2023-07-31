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

package ch.protonmail.android.navigation.deeplinks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.protonmail.android.mailcommon.domain.model.ConversationId
import ch.protonmail.android.mailconversation.domain.repository.ConversationRepository
import ch.protonmail.android.mailmessage.domain.entity.MessageId
import ch.protonmail.android.mailmessage.domain.repository.MessageRepository
import ch.protonmail.android.mailnotifications.domain.NotificationsDeepLinkHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import me.proton.core.domain.entity.UserId
import me.proton.core.mailsettings.domain.entity.ViewMode
import me.proton.core.mailsettings.domain.repository.MailSettingsRepository
import me.proton.core.network.domain.NetworkManager
import me.proton.core.network.domain.NetworkStatus
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class NotificationsDeepLinksViewModel @Inject constructor(
    private val networkManager: NetworkManager,
    private val messageRepository: MessageRepository,
    private val conversationRepository: ConversationRepository,
    private val mailSettingsRepository: MailSettingsRepository,
    private val notificationsDeepLinkHelper: NotificationsDeepLinkHelper
) : ViewModel() {

    private val _state = MutableStateFlow<State>(State.None)
    val state: StateFlow<State> = _state

    private var navigateToMessageJob: Job? = null

    fun navigateToMessage(
        notificationId: Int,
        messageId: String,
        userId: String
    ) {
        if (isOffline()) {
            _state.value = State.NavigateToInbox
            notificationsDeepLinkHelper.cancelNotification(notificationId)
        } else {
            Timber.d("navigateToMessage: $messageId, $userId")
            navigateToMessageJob?.cancel()
            navigateToMessageJob = viewModelScope.launch {
                val conversationModeEnabled = mailSettingsRepository.getMailSettings(UserId(userId))
                    .viewMode
                    ?.value == ViewMode.ConversationGrouping.value
                messageRepository.observeCachedMessage(UserId(userId), MessageId(messageId))
                    .distinctUntilChanged()
                    .collectLatest { messageResult ->
                        notificationsDeepLinkHelper.cancelNotification(notificationId)
                        messageResult
                            .onLeft {
                                _state.value = State.NavigateToInbox
                            }
                            .onRight { message ->
                                if (conversationModeEnabled) {
                                    conversationRepository.observeConversation(
                                        UserId(userId),
                                        message.conversationId,
                                        true
                                    ).collectLatest { conversationResult ->
                                        conversationResult
                                            .onLeft { Timber.d("Conversation not found: $it") }
                                            .onRight { conversation ->
                                                _state.value =
                                                    State.NavigateToConversation(conversation.conversationId, false)
                                            }
                                    }
                                } else {
                                    _state.value = State.NavigateToMessageDetails(message.messageId, false)
                                }
                                navigateToMessageJob?.cancel()
                            }
                    }
            }
        }
    }

    fun navigateToInbox(notificationId: Int) {
        notificationsDeepLinkHelper.cancelNotification(notificationId)
        _state.value = State.NavigateToInbox
    }

    private fun isOffline() = networkManager.networkStatus == NetworkStatus.Disconnected

    sealed interface State {
        object None : State
        object NavigateToInbox : State
        data class NavigateToMessageDetails(val messageId: MessageId, val userSwitched: Boolean) : State
        data class NavigateToConversation(val conversationId: ConversationId, val userSwitched: Boolean) : State
    }
}
