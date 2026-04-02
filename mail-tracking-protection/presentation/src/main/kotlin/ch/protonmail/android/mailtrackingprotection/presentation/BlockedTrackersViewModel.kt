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

package ch.protonmail.android.mailtrackingprotection.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.protonmail.android.design.compose.viewmodel.stopTimeoutMillis
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailsession.domain.usecase.ObservePrimaryUserId
import ch.protonmail.android.mailtrackingprotection.domain.model.PrivacyItemsResult
import ch.protonmail.android.mailtrackingprotection.domain.repository.PrivacyInfoRepository
import ch.protonmail.android.mailtrackingprotection.presentation.mapper.BlockedElementsUiModelMapper
import ch.protonmail.android.mailtrackingprotection.presentation.model.BlockedElementsState
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn

@HiltViewModel(assistedFactory = BlockedTrackersViewModel.Factory::class)
internal class BlockedTrackersViewModel @AssistedInject constructor(
    observePrimaryUserId: ObservePrimaryUserId,
    private val privacyInfoRepository: PrivacyInfoRepository,
    @Assisted private val messageId: MessageId
) : ViewModel() {

    val state: StateFlow<BlockedElementsState> = observePrimaryUserId()
        .filterNotNull()
        .flatMapLatest { userId ->
            privacyInfoRepository.observePrivacyItemsForMessage(userId, messageId).mapLatest { either ->
                either.fold(
                    ifLeft = { BlockedElementsState.Disabled },
                    ifRight = { result -> result.toState() }
                )
            }
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(stopTimeoutMillis),
            BlockedElementsState.Loading
        )

    private fun PrivacyItemsResult.toState(): BlockedElementsState = when (this) {
        is PrivacyItemsResult.Pending -> BlockedElementsState.Loading
        is PrivacyItemsResult.Disabled -> BlockedElementsState.Disabled
        is PrivacyItemsResult.Detected -> {
            if (items.isEmpty) BlockedElementsState.NoBlockedElements
            else BlockedElementsState.BlockedElements(BlockedElementsUiModelMapper.toUiModel(items))
        }
    }

    @AssistedFactory
    interface Factory {

        fun create(messageId: MessageId): BlockedTrackersViewModel
    }
}
