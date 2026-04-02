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

package ch.protonmail.android.mailpadlocks.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailpadlocks.domain.usecase.GetPrivacyLockForMessage
import ch.protonmail.android.mailpadlocks.presentation.mapper.EncryptionInfoUiModelMapper
import ch.protonmail.android.mailpadlocks.presentation.model.EncryptionInfoState
import ch.protonmail.android.mailsession.domain.usecase.ObservePrimaryUserId
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn

@HiltViewModel(assistedFactory = EncryptionInfoViewModel.Factory::class)
class EncryptionInfoViewModel @AssistedInject constructor(
    observePrimaryUserId: ObservePrimaryUserId,
    private val getPrivacyLockForMessage: GetPrivacyLockForMessage,
    @Assisted private val messageId: MessageId
) : ViewModel() {

    val state: StateFlow<EncryptionInfoState> = observePrimaryUserId()
        .filterNotNull()
        .flatMapLatest { userId ->
            flow {
                emit(EncryptionInfoState.Loading)

                getPrivacyLockForMessage(userId, messageId).fold(
                    ifLeft = { emit(EncryptionInfoState.Disabled) },
                    ifRight = {
                        val uiModel = EncryptionInfoUiModelMapper.fromPrivacyLock(it)
                        emit(EncryptionInfoState.Enabled(uiModel))
                    }
                )
            }
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            EncryptionInfoState.Loading
        )

    @AssistedFactory
    interface Factory {

        fun create(messageId: MessageId): EncryptionInfoViewModel
    }
}

