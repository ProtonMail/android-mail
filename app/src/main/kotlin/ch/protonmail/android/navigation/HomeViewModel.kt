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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.protonmail.android.mailcommon.domain.usecase.ObservePrimaryUser
import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailcomposer.domain.usecase.DeleteDraftState
import ch.protonmail.android.mailcomposer.domain.usecase.ObserveSendingDraftStates
import ch.protonmail.android.mailcomposer.domain.usecase.ResetDraftStateError
import ch.protonmail.android.mailcomposer.domain.model.DraftState
import ch.protonmail.android.mailcomposer.domain.model.DraftSyncState
import ch.protonmail.android.mailcomposer.presentation.model.MessageSendingStatus
import ch.protonmail.android.mailcomposer.presentation.model.MessageSendingStatus.MessageSent
import ch.protonmail.android.mailcomposer.presentation.model.MessageSendingStatus.SendMessageError
import ch.protonmail.android.navigation.model.HomeState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import me.proton.core.network.domain.NetworkManager
import me.proton.core.network.domain.NetworkStatus
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val networkManager: NetworkManager,
    private val observeSendingDraftStates: ObserveSendingDraftStates,
    private val resetDraftStateError: ResetDraftStateError,
    private val deleteDraftState: DeleteDraftState,
    observePrimaryUser: ObservePrimaryUser
) : ViewModel() {

    private val primaryUser = observePrimaryUser().filterNotNull()

    private val mutableState = MutableStateFlow(HomeState.Initial)

    val state: StateFlow<HomeState> = mutableState

    init {
        observeNetworkStatus().onEach { networkStatus ->
            if (networkStatus == NetworkStatus.Disconnected) {
                delay(NetworkStatusUpdateDelay)
                emitNewStateFor(networkManager.networkStatus)
            } else {
                emitNewStateFor(networkStatus)
            }
        }.launchIn(viewModelScope)

        primaryUser.flatMapLatest { user ->
            observeSendingDraftStates(user.userId)
        }.onEach {
            resetDraftStates(it)
            emitNewStateFor(it.toSendingStatus())
        }.launchIn(viewModelScope)
    }

    private fun emitNewStateFor(messageSendingStatus: MessageSendingStatus?) {
        if (messageSendingStatus == null) {
            // Do not override the error effect when empty. Given we update the draft states as part of this same
            // flow (resetDraftStateError / deleteDraftState) the null status would override the previous Effect
            // too fast causing it to never show. (This logic should go in a reducer)
            return
        }
        val currentState = state.value
        mutableState.value = currentState.copy(messageSendingStatusEffect = Effect.of(messageSendingStatus))
    }

    private fun emitNewStateFor(networkStatus: NetworkStatus) {
        val currentState = state.value
        mutableState.value = currentState.copy(networkStatusEffect = Effect.of(networkStatus))
    }

    private suspend fun resetDraftStates(states: List<DraftState>) = states.map {
        if (it.state == DraftSyncState.Sent) {
            deleteDraftState(it.userId, it.messageId)
        }
        if (it.state == DraftSyncState.ErrorSending) {
            resetDraftStateError(it.userId, it.messageId)
        }
    }

    private fun List<DraftState>.toSendingStatus(): MessageSendingStatus? {
        if (this.any { it.state == DraftSyncState.ErrorSending }) {
            return SendMessageError
        }

        if (this.any { it.state == DraftSyncState.Sent }) {
            return MessageSent
        }

        return null
    }

    private fun observeNetworkStatus() = networkManager.observe().distinctUntilChanged()

    companion object {
        const val NetworkStatusUpdateDelay = 5000L
    }
}
