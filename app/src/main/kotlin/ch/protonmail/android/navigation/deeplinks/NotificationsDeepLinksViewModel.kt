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
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailconversation.domain.repository.ConversationRepository
import ch.protonmail.android.mailmessage.domain.entity.Message
import ch.protonmail.android.mailmessage.domain.entity.MessageId
import ch.protonmail.android.mailmessage.domain.repository.MessageRepository
import ch.protonmail.android.mailnotifications.domain.NotificationsDeepLinkHelper
import ch.protonmail.android.navigation.deeplinks.NotificationsDeepLinksViewModel.State.NavigateToInbox
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import me.proton.core.account.domain.entity.isReady
import me.proton.core.accountmanager.domain.AccountManager
import me.proton.core.domain.entity.UserId
import me.proton.core.mailsettings.domain.entity.ViewMode
import me.proton.core.mailsettings.domain.repository.MailSettingsRepository
import me.proton.core.network.domain.NetworkManager
import me.proton.core.network.domain.NetworkStatus
import timber.log.Timber
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

@HiltViewModel
class NotificationsDeepLinksViewModel @Inject constructor(
    private val networkManager: NetworkManager,
    private val accountManager: AccountManager,
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
            navigateToInbox(notificationId, userId)
        } else {
            navigateToMessageOrConversation(notificationId, messageId, UserId(userId))
        }
    }

    fun navigateToInbox(notificationId: Int, userId: String) {
        notificationsDeepLinkHelper.cancelNotification(notificationId)
        viewModelScope.launch {
            val activeUserId = accountManager.getPrimaryUserId().firstOrNull()
            if (activeUserId != null && activeUserId.id != userId) {
                switchUserAndNavigateToInbox(userId)
            } else {
                _state.value = NavigateToInbox.ActiveUser
            }
        }
    }

    private suspend fun switchUserAndNavigateToInbox(userId: String) {
        val switchAccountResult = switchActiveUserIfRequiredTo(userId)
        _state.value = when (switchAccountResult) {
            AccountSwitchResult.AccountSwitchError -> NavigateToInbox.ActiveUser
            is AccountSwitchResult.AccountSwitched -> NavigateToInbox.ActiveUserSwitched(switchAccountResult.newEmail)
            AccountSwitchResult.NotRequired -> NavigateToInbox.ActiveUser
        }
    }

    private fun navigateToMessageOrConversation(
        notificationId: Int,
        messageId: String,
        userId: UserId
    ) {
        Timber.d("navigateToMessage: $messageId, $userId")
        navigateToMessageJob?.cancel()
        navigateToMessageJob = viewModelScope.launch {
            when (val switchAccountResult = switchActiveUserIfRequiredTo(userId.id)) {
                AccountSwitchResult.AccountSwitchError -> navigateToInbox(notificationId, userId.id)
                is AccountSwitchResult.AccountSwitched -> navigateToMessageOrConversation(
                    this.coroutineContext,
                    notificationId,
                    messageId,
                    switchAccountResult.newUserId,
                    switchAccountResult.newEmail
                )

                AccountSwitchResult.NotRequired -> navigateToMessageOrConversation(
                    this.coroutineContext,
                    notificationId,
                    messageId,
                    userId
                )
            }
        }
    }

    private suspend fun navigateToMessageOrConversation(
        coroutineContext: CoroutineContext,
        notificationId: Int,
        messageId: String,
        userId: UserId,
        switchedAccountEmail: String? = null
    ) {
        messageRepository.observeCachedMessage(userId, MessageId(messageId))
            .distinctUntilChanged()
            .collectLatest { messageResult ->
                notificationsDeepLinkHelper.cancelNotification(notificationId)
                messageResult
                    .onLeft {
                        if (it != DataError.Local.NoDataCached) navigateToInbox(notificationId, userId.id)
                    }
                    .onRight { message ->
                        if (isConversationModeEnabled(userId)) {
                            navigateToConversation(message, userId, notificationId, switchedAccountEmail)
                        } else {
                            _state.value = State.NavigateToMessageDetails(message.messageId, switchedAccountEmail)
                        }
                        coroutineContext.cancel()
                    }
            }
    }

    private suspend fun switchActiveUserIfRequiredTo(userId: String): AccountSwitchResult {
        return if (accountManager.getPrimaryUserId().firstOrNull()?.id == userId) {
            AccountSwitchResult.NotRequired
        } else {
            val targetAccount = accountManager.getAccount(UserId(userId))
                .filter { account -> account?.isReady() == true }
                .firstOrNull()
            if (targetAccount != null) {
                accountManager.setAsPrimary(UserId(userId))
                AccountSwitchResult.AccountSwitched(targetAccount.userId, targetAccount.email ?: "")
            } else {
                AccountSwitchResult.AccountSwitchError
            }
        }
    }

    private suspend fun isConversationModeEnabled(userId: UserId): Boolean =
        mailSettingsRepository.getMailSettings(userId)
            .viewMode
            ?.value == ViewMode.ConversationGrouping.value

    private suspend fun navigateToConversation(
        message: Message,
        userId: UserId,
        notificationId: Int,
        switchedAccountEmail: String?
    ) {
        conversationRepository.observeConversation(
            userId,
            message.conversationId,
            true
        ).collectLatest { conversationResult ->
            conversationResult
                .onLeft {
                    Timber.d("Conversation not found: $it")
                    if (it != DataError.Local.NoDataCached) navigateToInbox(notificationId, userId.id)
                }
                .onRight { conversation ->
                    _state.value =
                        State.NavigateToConversation(conversation.conversationId, switchedAccountEmail)
                }
        }
    }

    private fun isOffline() = networkManager.networkStatus == NetworkStatus.Disconnected

    sealed interface State {
        object None : State
        sealed interface NavigateToInbox : State {
            object ActiveUser : NavigateToInbox
            data class ActiveUserSwitched(val email: String) : NavigateToInbox
        }

        data class NavigateToMessageDetails(
            val messageId: MessageId,
            val userSwitchedEmail: String? = null
        ) : State

        data class NavigateToConversation(
            val conversationId: ConversationId,
            val userSwitchedEmail: String? = null
        ) : State
    }

    private sealed interface AccountSwitchResult {
        object NotRequired : AccountSwitchResult
        data class AccountSwitched(val newUserId: UserId, val newEmail: String) : AccountSwitchResult

        object AccountSwitchError : AccountSwitchResult
    }
}
