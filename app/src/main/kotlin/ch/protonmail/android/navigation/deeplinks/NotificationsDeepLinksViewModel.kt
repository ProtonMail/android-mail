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
import ch.protonmail.android.mailcommon.domain.network.NetworkManager
import ch.protonmail.android.mailcommon.domain.network.NetworkStatus
import ch.protonmail.android.maildetail.domain.usecase.IsShowSingleMessageMode
import ch.protonmail.android.maillabel.domain.model.ExclusiveLocation
import ch.protonmail.android.maillabel.domain.model.LabelId
import ch.protonmail.android.maillabel.domain.model.SystemLabelId
import ch.protonmail.android.maillabel.domain.usecase.FindLocalSystemLabelId
import ch.protonmail.android.mailmessage.domain.model.Message
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailmessage.domain.model.RemoteMessageId
import ch.protonmail.android.mailmessage.domain.usecase.GetMessageByRemoteId
import ch.protonmail.android.mailsession.domain.model.AccountState
import ch.protonmail.android.mailsession.domain.repository.UserSessionRepository
import ch.protonmail.android.mailsession.domain.usecase.ObservePrimaryUserId
import ch.protonmail.android.mailsession.domain.usecase.SetPrimaryAccount
import ch.protonmail.android.navigation.deeplinks.NotificationsDeepLinksViewModel.State.NavigateToInbox
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import me.proton.core.domain.entity.UserId
import timber.log.Timber
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

@HiltViewModel
class NotificationsDeepLinksViewModel @Inject constructor(
    private val deepLinkHandler: NotificationsDeepLinkHandler,
    private val networkManager: NetworkManager,
    private val observePrimaryUserId: ObservePrimaryUserId,
    private val userSessionRepository: UserSessionRepository,
    private val setPrimaryAccount: SetPrimaryAccount,
    private val getMessage: GetMessageByRemoteId,
    private val findLocalSystemLabelId: FindLocalSystemLabelId,
    private val isShowSingleMessageMode: IsShowSingleMessageMode
) : ViewModel() {

    private val _state = MutableStateFlow<State>(State.Launched)
    val state: StateFlow<State> = _state

    private var navigateJob: Job? = null

    init {
        viewModelScope.launch {
            deepLinkHandler.pending.collect { data ->
                Timber.d("NotificationsDeepLinksViewModel: Received pending deep link $data")
                processPendingDeepLink(data)
            }
        }
    }

    private fun processPendingDeepLink(data: NotificationsDeepLinkData) {
        Timber.d("processPendingDeepLink: Processing deep link $data, current state: ${_state.value}")
        // Reset state to Launched before processing new deep link to ensure LaunchedEffect triggers
        // even when navigating to the same conversation/message
        _state.value = State.Launched

        when (data) {
            is NotificationsDeepLinkData.Message -> {
                navigateToDetails(data.messageId, data.userId)
            }

            is NotificationsDeepLinkData.Group -> {
                navigateToInbox(data.userId)
            }
        }
    }

    fun navigateToDetails(messageId: String, userId: String) {
        if (isOffline()) {
            navigateToInbox(userId)
        } else {
            resolveNavigation(RemoteMessageId(messageId), UserId(userId))
        }
    }

    fun navigateToInbox(userId: String) {
        viewModelScope.launch {
            val activeUserId = observePrimaryUserId().firstOrNull()
            if (activeUserId != null && activeUserId.id != userId) {
                switchUserAndNavigateToInbox(userId)
            } else {
                _state.update { NavigateToInbox.ActiveUser }
            }
        }
    }

    fun consume() {
        deepLinkHandler.consume()
        _state.value = State.Launched
    }

    private suspend fun switchUserAndNavigateToInbox(userId: String) {
        val switchAccountResult = switchActiveUserIfRequiredTo(userId)
        _state.update {
            when (switchAccountResult) {
                AccountSwitchResult.AccountSwitchError -> NavigateToInbox.ActiveUser
                is AccountSwitchResult.AccountSwitched ->
                    NavigateToInbox.ActiveUserSwitched(switchAccountResult.newEmail)

                AccountSwitchResult.NotRequired -> NavigateToInbox.ActiveUser
            }
        }
    }

    private fun resolveNavigation(messageId: RemoteMessageId, userId: UserId) {
        Timber.d("navigateToMessage: $messageId, $userId")
        navigateJob?.cancel()
        navigateJob = viewModelScope.launch {
            when (val switchAccountResult = switchActiveUserIfRequiredTo(userId.id)) {
                AccountSwitchResult.AccountSwitchError -> navigateToInbox(userId.id)
                is AccountSwitchResult.AccountSwitched -> resolveNavigation(
                    this.coroutineContext,
                    messageId,
                    switchAccountResult.newUserId,
                    switchAccountResult.newEmail
                )

                AccountSwitchResult.NotRequired -> resolveNavigation(
                    this.coroutineContext,
                    messageId,
                    userId
                )
            }
        }
    }

    private suspend fun resolveNavigation(
        coroutineContext: CoroutineContext,
        remoteMessageId: RemoteMessageId,
        userId: UserId,
        switchedAccountEmail: String? = null
    ) {
        getMessage(userId, remoteMessageId)
            .onLeft {
                Timber.e("Unable to fetch message - Navigating to inbox. - $it")
                navigateToInbox(userId.id)
            }
            .onRight { message ->
                navigateToDetails(message, userId, switchedAccountEmail)
                coroutineContext.cancel()
            }
    }

    private suspend fun switchActiveUserIfRequiredTo(rawUserId: String): AccountSwitchResult {
        val userId = UserId(rawUserId)
        val currentPrimaryUserId = observePrimaryUserId().firstOrNull()
        if (currentPrimaryUserId?.id == userId.id) return AccountSwitchResult.NotRequired

        val targetAccount = getAccountReadyForUserId(userId) ?: return AccountSwitchResult.AccountSwitchError

        setPrimaryAccount(targetAccount.userId)
        return AccountSwitchResult.AccountSwitched(targetAccount.userId, targetAccount.primaryAddress)
    }

    private suspend fun navigateToDetails(
        message: Message,
        userId: UserId,
        switchedAccountEmail: String?
    ) {
        val labelId = message.exclusiveLocation.getLabelId(userId)
        if (labelId == null) {
            Timber.e("Unable to resolve label for message - Navigating to inbox.")
            navigateToInbox(userId.id)
            return
        }

        _state.update {
            if (isShowSingleMessageMode(userId)) {
                State.NavigateToMessage(
                    conversationId = message.conversationId,
                    userSwitchedEmail = switchedAccountEmail,
                    contextLabelId = labelId,
                    messageId = message.messageId
                )
            } else {
                State.NavigateToConversation(
                    conversationId = message.conversationId,
                    userSwitchedEmail = switchedAccountEmail,
                    contextLabelId = labelId,
                    scrollToMessageId = message.messageId
                )
            }
        }
    }

    private suspend fun ExclusiveLocation.getLabelId(userId: UserId) = when (this) {
        is ExclusiveLocation.Folder -> this.labelId
        is ExclusiveLocation.NoLocation -> findLocalSystemLabelId(userId, SystemLabelId.AllMail)?.labelId
        is ExclusiveLocation.System -> this.labelId
    }

    private fun isOffline() = networkManager.networkStatus == NetworkStatus.Disconnected

    private suspend fun getAccountReadyForUserId(userId: UserId) = userSessionRepository
        .getAccount(userId)
        ?.takeIf { it.state == AccountState.Ready }

    sealed interface State {
        data object Launched : State

        sealed interface NavigateToInbox : State {
            data object ActiveUser : NavigateToInbox
            data class ActiveUserSwitched(val email: String) : NavigateToInbox
        }

        data class NavigateToMessage(
            val conversationId: ConversationId,
            val messageId: MessageId,
            val userSwitchedEmail: String? = null,
            val contextLabelId: LabelId
        ) : State

        data class NavigateToConversation(
            val conversationId: ConversationId,
            val scrollToMessageId: MessageId? = null,
            val userSwitchedEmail: String? = null,
            val contextLabelId: LabelId
        ) : State
    }

    private sealed interface AccountSwitchResult {
        data object NotRequired : AccountSwitchResult
        data class AccountSwitched(val newUserId: UserId, val newEmail: String) : AccountSwitchResult

        data object AccountSwitchError : AccountSwitchResult
    }
}
