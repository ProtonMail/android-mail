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
import arrow.core.getOrElse
import ch.protonmail.android.mailcommon.domain.usecase.ObservePrimaryUserId
import ch.protonmail.android.mailcommon.presentation.model.BottomBarEvent
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailcommon.presentation.reducer.BottomBarReducer
import ch.protonmail.android.mailcontact.domain.usecase.GetContacts
import ch.protonmail.android.maildetail.domain.usecase.MarkUnread
import ch.protonmail.android.maildetail.domain.usecase.ObserveMessageDetailActions
import ch.protonmail.android.maildetail.domain.usecase.ObserveMessageWithLabels
import ch.protonmail.android.maildetail.domain.usecase.StarMessage
import ch.protonmail.android.maildetail.presentation.mapper.ActionUiModelMapper
import ch.protonmail.android.maildetail.presentation.mapper.MessageDetailUiModelMapper
import ch.protonmail.android.maildetail.presentation.model.MessageDetailEvent
import ch.protonmail.android.maildetail.presentation.model.MessageDetailOperation
import ch.protonmail.android.maildetail.presentation.model.MessageDetailState
import ch.protonmail.android.maildetail.presentation.model.MessageViewAction
import ch.protonmail.android.maildetail.presentation.reducer.MessageDetailReducer
import ch.protonmail.android.maildetail.presentation.ui.MessageDetailScreen
import ch.protonmail.android.mailmessage.domain.entity.MessageId
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class MessageDetailViewModel @Inject constructor(
    observePrimaryUserId: ObservePrimaryUserId,
    private val observeMessageWithLabels: ObserveMessageWithLabels,
    private val messageDetailReducer: MessageDetailReducer,
    private val uiModelMapper: MessageDetailUiModelMapper,
    private val actionUiModelMapper: ActionUiModelMapper,
    private val observeDetailActions: ObserveMessageDetailActions,
    private val markUnread: MarkUnread,
    private val getContacts: GetContacts,
    private val starMessage: StarMessage,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val messageId = requireMessageId()
    private val primaryUserId = observePrimaryUserId().filterNotNull()
    private val mutableDetailState = MutableStateFlow(initialState)

    val state: StateFlow<MessageDetailState> = mutableDetailState.asStateFlow()

    init {
        Timber.d("Open detail screen for message ID: $messageId")
        observeMessageWithLabels(messageId)
        observeBottomBarActions(messageId)
    }

    fun submit(action: MessageViewAction) {
        when (action) {
            is MessageViewAction.Star -> starMessage()
            is MessageViewAction.UnStar -> Timber.d("UnStar message clicked")
            is MessageViewAction.MarkUnread -> markMessageUnread()
        }
    }

    private fun starMessage() {
        primaryUserId.flatMapLatest { userId ->
            starMessage(userId, messageId).mapLatest { either ->
                either.fold(
                    ifLeft = { MessageDetailEvent.ErrorAddingStar },
                    ifRight = { MessageViewAction.Star }
                )
            }
        }.onEach { event ->
            emitNewStateFrom(event)
        }.launchIn(viewModelScope)
    }

    private fun markMessageUnread() {
        primaryUserId.flatMapLatest { userId ->
            markUnread(userId, messageId).mapLatest { either ->
                either.fold(
                    ifLeft = { MessageDetailEvent.ErrorMarkingUnread },
                    ifRight = { MessageViewAction.MarkUnread }
                )
            }
        }.onEach { event ->
            emitNewStateFrom(event)
        }.launchIn(viewModelScope)
    }

    private fun observeMessageWithLabels(messageId: MessageId) {
        primaryUserId.flatMapLatest { userId ->
            val contacts = getContacts(userId)
            return@flatMapLatest observeMessageWithLabels(userId, messageId).mapLatest { either ->
                either.fold(
                    ifLeft = { MessageDetailEvent.NoCachedMetadata },
                    ifRight = {
                        MessageDetailEvent.MessageMetadata(
                            uiModelMapper.toUiModel(
                                it,
                                contacts.getOrElse { emptyList() }
                            )
                        )
                    }
                )
            }
        }.onEach { event ->
            emitNewStateFrom(event)
        }.launchIn(viewModelScope)
    }

    private fun observeBottomBarActions(messageId: MessageId) {
        primaryUserId.flatMapLatest { userId ->
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
        val updatedState = messageDetailReducer.newStateFrom(state.value, operation)
        mutableDetailState.emit(updatedState)
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
