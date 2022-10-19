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

package ch.protonmail.android.maildetail.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.protonmail.android.mailcommon.domain.usecase.ObservePrimaryUserId
import ch.protonmail.android.maildetail.domain.ObserveDetailActions
import ch.protonmail.android.maildetail.presentation.mapper.ActionUiModelMapper
import ch.protonmail.android.maildetail.presentation.mapper.MessageDetailUiModelMapper
import ch.protonmail.android.maildetail.presentation.model.MessageDetailAction
import ch.protonmail.android.maildetail.presentation.model.MessageDetailEvent
import ch.protonmail.android.maildetail.presentation.model.MessageDetailState
import ch.protonmail.android.maildetail.presentation.reducer.BottomBarStateReducer
import ch.protonmail.android.maildetail.presentation.reducer.MessageStateReducer
import ch.protonmail.android.maildetail.presentation.ui.MessageDetailScreen
import ch.protonmail.android.mailmessage.domain.entity.MessageId
import ch.protonmail.android.mailmessage.domain.usecase.ObserveMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class MessageDetailViewModel @Inject constructor(
    observePrimaryUserId: ObservePrimaryUserId,
    private val messageStateReducer: MessageStateReducer,
    private val bottomBarStateReducer: BottomBarStateReducer,
    private val observeMessage: ObserveMessage,
    private val uiModelMapper: MessageDetailUiModelMapper,
    private val actionUiModelMapper: ActionUiModelMapper,
    private val observeDetailActions: ObserveDetailActions,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val primaryUserId = observePrimaryUserId()
    private val mutableDetailState = MutableStateFlow(initialState)

    val state: StateFlow<MessageDetailState> = mutableDetailState.asStateFlow()

    init {
        val messageIdParam = savedStateHandle.get<String>(MessageDetailScreen.MESSAGE_ID_KEY)
        Timber.d("Open detail screen for message ID: $messageIdParam")

        if (messageIdParam == null) {
            throw IllegalStateException("No Message id given")
        }

        val messageId = MessageId(messageIdParam)
        observeMessageMetadata(messageId)
        observeBottomBarActions(messageId)
    }

    @SuppressWarnings("UnusedPrivateMember", "NotImplementedDeclaration")
    fun submit(action: MessageDetailAction) = when (action) {
        is MessageDetailAction.Star -> Timber.d("Star message clicked")
        is MessageDetailAction.UnStar -> Timber.d("UnStar message clicked")
    }

    private fun observeMessageMetadata(messageId: MessageId) {
        primaryUserId.flatMapLatest { userId ->
            if (userId == null) {
                return@flatMapLatest flowOf(MessageDetailEvent.NoPrimaryUser)
            }
            return@flatMapLatest observeMessage(userId, messageId).mapLatest { either ->
                either.fold(
                    ifLeft = { MessageDetailEvent.NoCachedMetadata },
                    ifRight = { MessageDetailEvent.MessageMetadata(uiModelMapper.toUiModel(it)) }
                )
            }
        }.onEach { event ->
            emitNewStateFrom(event)
        }.launchIn(viewModelScope)
    }

    private fun observeBottomBarActions(messageId: MessageId) {
        primaryUserId.flatMapLatest { userId ->
            if (userId == null) {
                return@flatMapLatest flowOf(MessageDetailEvent.NoPrimaryUser)
            }
            return@flatMapLatest observeDetailActions(userId, messageId).mapLatest { actions ->
                val actionUiModels = actions.map { actionUiModelMapper.toUiModel(it) }
                return@mapLatest if (actionUiModels.isEmpty()) {
                    MessageDetailEvent.ErrorLoadingActions
                } else {
                    MessageDetailEvent.MessageActionsData(actionUiModels)
                }
            }
        }.onEach { event ->
            emitNewStateFrom(event)
        }.launchIn(viewModelScope)
    }

    private suspend fun emitNewStateFrom(event: MessageDetailEvent) {
        val updatedDetailState = state.value.copy(
            messageState = messageStateReducer.reduce(state.value.messageState, event),
            bottomBarState = bottomBarStateReducer.reduce(state.value.bottomBarState, event)
        )
        mutableDetailState.emit(updatedDetailState)
    }

    @SuppressWarnings("UnusedPrivateMember", "NotImplementedDeclaration")
    private fun MessageDetailAction.toEvent(): MessageDetailEvent = TODO("Implement when adding first action")

    companion object {

        val initialState = MessageDetailState.Loading
    }
}
