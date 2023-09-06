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
import ch.protonmail.android.mailcomposer.presentation.model.MessageSendingUiModel
import ch.protonmail.android.mailcomposer.presentation.model.MessageSendingUiModel.MessageSent
import ch.protonmail.android.mailcomposer.presentation.model.MessageSendingUiModel.SendMessageError
import ch.protonmail.android.navigation.model.HomeAction
import ch.protonmail.android.navigation.model.HomeState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
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

    val state: Flow<HomeState> = primaryUser.flatMapLatest { user ->
        combine(
            observeNetworkStatus(),
            observeSendingDraftStates(user.userId)
        ) { networkStatus, draftStates ->
            HomeState(
                networkStatusEffect = if (networkStatus == NetworkStatus.Disconnected) {
                    delay(5000)
                    Effect.of(networkManager.networkStatus)
                } else {
                    Effect.of(networkStatus)
                },
                messageSendingStatusEffect = draftStates.toSendingStatusEffect()
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Lazily,
        initialValue = HomeState.Initial
    )

    internal suspend fun submit(action: HomeAction) {
        when (action) {
            is HomeAction.MessageSendingErrorShown -> action.messageError.messageIds.forEach {
                resetDraftStateError(action.messageError.userId, it)
            }
            is HomeAction.MessageSentShown -> action.messageSent.messageIds.forEach {
                deleteDraftState(action.messageSent.userId, it)
            }
        }
    }

    private suspend fun List<DraftState>.toSendingStatusEffect(): Effect<MessageSendingUiModel> {
        val sentMessages = this.filter { it.state == DraftSyncState.Sent }
        val erroredMessages = this.filter { it.state == DraftSyncState.ErrorSending }

        return when {
            erroredMessages.isNotEmpty() -> Effect.of(
                SendMessageError(primaryUser.first().userId, erroredMessages.map { it.messageId })
            )
            sentMessages.isNotEmpty() -> Effect.of(
                MessageSent(primaryUser.first().userId, sentMessages.map { it.messageId })
            )
            else -> Effect.empty()
        }
    }

    private fun observeNetworkStatus() = networkManager.observe().distinctUntilChanged()
}
