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

package ch.protonmail.android.maildetail.presentation.message

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.protonmail.android.mailcommon.domain.usecase.ObservePrimaryUserId
import ch.protonmail.android.maildetail.presentation.message.model.MessageDetailAction
import ch.protonmail.android.maildetail.presentation.message.model.MessageDetailEvent
import ch.protonmail.android.maildetail.presentation.message.model.MessageDetailState
import ch.protonmail.android.mailmessage.domain.entity.MessageId
import ch.protonmail.android.mailmessage.domain.repository.MessageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class MessageDetailViewModel @Inject constructor(
    private val observePrimaryUserId: ObservePrimaryUserId,
    private val messageDetailReducer: MessageDetailReducer,
    private val messageRepository: MessageRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val mutableDetailState = MutableStateFlow<MessageDetailState>(MessageDetailState.Loading)
    val state: StateFlow<MessageDetailState> = mutableDetailState.asStateFlow()

    init {
        val messageIdParam = savedStateHandle.get<String>(MessageDetailScreen.MESSAGE_ID_KEY)
        Timber.d("Open detail screen for message ID: $messageIdParam")

        if (messageIdParam == null) {
            viewModelScope.launch { emitNewStateFrom(MessageDetailEvent.NoMessageIdProvided) }
        } else {
            observeMessageMetadata(MessageId(messageIdParam))
        }
    }

    fun submit(action: MessageDetailAction) {
        TODO("Implement when adding first action")
    }

    private fun observeMessageMetadata(messageId: MessageId) {
        observePrimaryUserId().flatMapLatest { userId ->
            if (userId == null) {
                return@flatMapLatest flowOf(MessageDetailEvent.NoPrimaryUser)
            }
            return@flatMapLatest messageRepository.observeCachedMessage(userId, messageId).mapLatest { either ->
                either.fold(
                    ifLeft = { MessageDetailEvent.NoCachedMetadata },
                    ifRight = { MessageDetailEvent.MessageMetadata(it) }
                )
            }
        }.onEach { event ->
            emitNewStateFrom(event)
        }.launchIn(viewModelScope)
    }

    private suspend fun emitNewStateFrom(event: MessageDetailEvent) {
        mutableDetailState.emit(messageDetailReducer.reduce(state.value, event))
    }

    private fun MessageDetailAction.toEvent(): MessageDetailEvent = TODO("Implement when adding first action")

}
