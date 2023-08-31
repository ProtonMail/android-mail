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
import ch.protonmail.android.mailcomposer.domain.model.DraftSyncState
import ch.protonmail.android.mailcomposer.domain.repository.DraftStateRepository
import ch.protonmail.android.mailcomposer.presentation.model.MessageSendingUiModel
import ch.protonmail.android.navigation.model.HomeState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import me.proton.core.domain.entity.UserId
import me.proton.core.network.domain.NetworkManager
import me.proton.core.network.domain.NetworkStatus
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val networkManager: NetworkManager,
    private val draftStateRepository: DraftStateRepository,
    observePrimaryUser: ObservePrimaryUser
) : ViewModel() {

    private val primaryUser = observePrimaryUser().stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = null
    )

    val state: Flow<HomeState> = primaryUser.flatMapLatest { user ->
        if (user == null) {
            return@flatMapLatest flowOf(
                HomeState(
                    networkStatusEffect = Effect.empty(),
                    messageSendingStatusEffect = Effect.empty()
                )
            )
        }

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
                messageSendingStatusEffect = Effect.of(
                    draftStates.map { draftState ->
                        MessageSendingUiModel(
                            draftState.userId,
                            draftState.apiMessageId ?: draftState.messageId,
                            draftState.state
                        )
                    }.toList()
                )
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Lazily,
        initialValue = HomeState.Initial
    )

    private fun observeNetworkStatus() = networkManager.observe().distinctUntilChanged()
    private fun observeSendingDraftStates(userId: UserId) = draftStateRepository.observeAll(userId).map { draftStates ->
        draftStates.filter { it.state == DraftSyncState.Sent || it.state == DraftSyncState.ErrorSending }
    }.distinctUntilChanged()
}
