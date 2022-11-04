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
import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailcommon.presentation.model.BottomBarEvent
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailcommon.presentation.reducer.BottomBarReducer
import ch.protonmail.android.maildetail.domain.usecase.MarkUnread
import ch.protonmail.android.maildetail.domain.usecase.ObserveMessageDetailActions
import ch.protonmail.android.maildetail.presentation.R
import ch.protonmail.android.maildetail.presentation.mapper.ActionUiModelMapper
import ch.protonmail.android.maildetail.presentation.mapper.MessageDetailUiModelMapper
import ch.protonmail.android.maildetail.presentation.model.MessageDetailEvent
import ch.protonmail.android.maildetail.presentation.model.MessageDetailOperation
import ch.protonmail.android.maildetail.presentation.model.MessageDetailState
import ch.protonmail.android.maildetail.presentation.model.MessageViewAction
import ch.protonmail.android.maildetail.presentation.reducer.MessageDetailMetadataReducer
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
    private val messageStateReducer: MessageDetailMetadataReducer,
    private val bottomBarReducer: BottomBarReducer,
    private val observeMessage: ObserveMessage,
    private val uiModelMapper: MessageDetailUiModelMapper,
    private val actionUiModelMapper: ActionUiModelMapper,
    private val observeDetailActions: ObserveMessageDetailActions,
    private val markUnread: MarkUnread,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val primaryUserId = observePrimaryUserId()
    private val mutableDetailState = MutableStateFlow(initialState)

    val state: StateFlow<MessageDetailState> = mutableDetailState.asStateFlow()

    init {
        val messageId = requireMessageId()
        Timber.d("Open detail screen for message ID: $messageId")
        observeMessageMetadata(messageId)
        observeBottomBarActions(messageId)
    }

    fun submit(action: MessageViewAction) {
        when (action) {
            is MessageViewAction.Star -> Timber.d("Star message clicked")
            is MessageViewAction.UnStar -> Timber.d("UnStar message clicked")
            is MessageViewAction.MarkUnread -> markMessageUnread()
        }
    }

    private fun markMessageUnread() {
        primaryUserId.flatMapLatest { userId ->
            if (userId == null) {
                return@flatMapLatest flowOf(MessageDetailEvent.NoPrimaryUser)
            }
            markUnread(userId, requireMessageId()).mapLatest { either ->
                either.fold(
                    ifLeft = { MessageDetailEvent.ErrorMarkingUnread },
                    ifRight = { MessageDetailEvent.MarkedUnread }
                )
            }
        }.onEach { event ->
            emitNewStateFrom(event)
        }.launchIn(viewModelScope)
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
            observeDetailActions(userId, messageId).mapLatest { either ->
                either.fold(
                    ifLeft = { MessageDetailEvent.MessageBottomBarEvent(BottomBarEvent.ErrorLoadingActions) },
                    ifRight = { actions ->
                        val actionUiModels = actions.map { actionUiModelMapper.toUiModel(it) }
                        MessageDetailEvent.MessageBottomBarEvent(BottomBarEvent.ActionsData(actionUiModels))
                    }
                )
            }
        }.onEach { event ->
            emitNewStateFrom(event)
        }.launchIn(viewModelScope)
    }

    private suspend fun emitNewStateFrom(operation: MessageDetailOperation) {
        val updatedDetailState = state.value.copy(
            messageState = updateMessageState(operation),
            bottomBarState = updateBottomBarState(operation),
            dismiss = if (operation is MessageDetailEvent.MarkedUnread) Effect.of(Unit) else Effect.empty(),
            error = if (operation is MessageDetailEvent.ErrorMarkingUnread)
                Effect.of(TextUiModel(R.string.error_mark_unread_failed))
            else
                Effect.empty()
        )
        mutableDetailState.emit(updatedDetailState)
    }

    private fun updateMessageState(operation: MessageDetailOperation) =
        if (operation is MessageDetailOperation.AffectingMessage) {
            messageStateReducer.newStateFrom(state.value.messageState, operation)
        } else {
            state.value.messageState
        }

    private fun updateBottomBarState(operation: MessageDetailOperation) =
        if (operation is MessageDetailEvent.MessageBottomBarEvent) {
            bottomBarReducer.newStateFrom(state.value.bottomBarState, operation.bottomBarEvent)
        } else {
            state.value.bottomBarState
        }

    private fun requireMessageId(): MessageId {
        val messageIdParam = savedStateHandle.get<String>(MessageDetailScreen.MESSAGE_ID_KEY)
            ?: throw IllegalStateException("No Message id given")

        return MessageId(messageIdParam)
    }

    companion object {

        val initialState = MessageDetailState.Loading
    }
}
