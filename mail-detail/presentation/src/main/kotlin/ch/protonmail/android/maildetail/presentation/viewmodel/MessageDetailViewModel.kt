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
import ch.protonmail.android.mailcontact.domain.usecase.GetContacts
import ch.protonmail.android.maildetail.domain.usecase.MarkUnread
import ch.protonmail.android.maildetail.domain.usecase.MoveMessageToTrash
import ch.protonmail.android.maildetail.domain.usecase.ObserveMessageDetailActions
import ch.protonmail.android.maildetail.domain.usecase.ObserveMessageWithLabels
import ch.protonmail.android.maildetail.domain.usecase.StarMessage
import ch.protonmail.android.maildetail.domain.usecase.UnStarMessage
import ch.protonmail.android.maildetail.presentation.mapper.ActionUiModelMapper
import ch.protonmail.android.maildetail.presentation.mapper.MessageDetailActionBarUiModelMapper
import ch.protonmail.android.maildetail.presentation.mapper.MessageDetailHeaderUiModelMapper
import ch.protonmail.android.maildetail.presentation.model.BottomSheetEvent
import ch.protonmail.android.maildetail.presentation.model.MessageDetailEvent
import ch.protonmail.android.maildetail.presentation.model.MessageDetailOperation
import ch.protonmail.android.maildetail.presentation.model.MessageDetailState
import ch.protonmail.android.maildetail.presentation.model.MessageViewAction
import ch.protonmail.android.maildetail.presentation.reducer.MessageDetailReducer
import ch.protonmail.android.maildetail.presentation.ui.MessageDetailScreen
import ch.protonmail.android.maillabel.domain.model.MailLabelId
import ch.protonmail.android.maillabel.domain.usecase.ObserveExclusiveMailFolders
import ch.protonmail.android.maillabel.presentation.toUiModels
import ch.protonmail.android.mailmessage.domain.entity.MessageId
import ch.protonmail.android.mailsettings.domain.ObserveFolderColorSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import me.proton.core.util.kotlin.exhaustive
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class MessageDetailViewModel @Inject constructor(
    observePrimaryUserId: ObservePrimaryUserId,
    private val observeMessageWithLabels: ObserveMessageWithLabels,
    private val messageDetailReducer: MessageDetailReducer,
    private val actionUiModelMapper: ActionUiModelMapper,
    private val observeDetailActions: ObserveMessageDetailActions,
    private val observeMailFolders: ObserveExclusiveMailFolders,
    private val observeFolderColor: ObserveFolderColorSettings,
    private val markUnread: MarkUnread,
    private val getContacts: GetContacts,
    private val starMessage: StarMessage,
    private val unStarMessage: UnStarMessage,
    private val savedStateHandle: SavedStateHandle,
    private val messageDetailHeaderUiModelMapper: MessageDetailHeaderUiModelMapper,
    private val messageDetailActionBarUiModelMapper: MessageDetailActionBarUiModelMapper,
    private val moveMessageToTrash: MoveMessageToTrash
) : ViewModel() {

    private val messageId = requireMessageId()
    private val primaryUserId = observePrimaryUserId().filterNotNull()
    private val mutableDetailState = MutableStateFlow(initialState)

    val state: StateFlow<MessageDetailState> = mutableDetailState.asStateFlow()

    init {
        Timber.d("Open detail screen for message ID: $messageId")
        observeMessageWithLabels(messageId)
        observeBottomBarActions(messageId)
        observeMailFolders()
    }

    fun submit(action: MessageViewAction) {
        when (action) {
            is MessageViewAction.Star -> starMessage()
            is MessageViewAction.UnStar -> unStarMessage()
            is MessageViewAction.MarkUnread -> markMessageUnread()
            is MessageViewAction.Trash -> trashMessage()
            is MessageViewAction.MoveToSelected -> moveToDestinationSelected(action.mailLabelId)
            is MessageViewAction.BottomSheetDismissed -> onBottomSheetDismissed()
        }.exhaustive
    }

    private fun starMessage() {
        primaryUserId.mapLatest { userId ->
            starMessage(userId, messageId).fold(
                ifLeft = { MessageDetailEvent.ErrorAddingStar },
                ifRight = { MessageViewAction.Star }
            )
        }.onEach { event ->
            emitNewStateFrom(event)
        }.launchIn(viewModelScope)
    }

    private fun unStarMessage() {
        primaryUserId.mapLatest { userId ->
            unStarMessage(userId, messageId).fold(
                ifLeft = { MessageDetailEvent.ErrorRemovingStar },
                ifRight = { MessageViewAction.UnStar }
            )
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

    private fun trashMessage() {
        primaryUserId.mapLatest { userId ->
            moveMessageToTrash(userId, messageId).fold(
                ifLeft = { MessageDetailEvent.ErrorMovingToTrash },
                ifRight = { MessageViewAction.Trash }
            )
        }.onEach { event ->
            emitNewStateFrom(event)
        }.launchIn(viewModelScope)
    }

    private fun moveToDestinationSelected(mailLabelId: MailLabelId) {
        viewModelScope.launch {
            emitNewStateFrom(MessageViewAction.MoveToSelected(mailLabelId))
        }
    }

    private fun onBottomSheetDismissed() {
        viewModelScope.launch {
            emitNewStateFrom(MessageViewAction.BottomSheetDismissed)
        }
    }

    private fun observeMessageWithLabels(messageId: MessageId) {
        primaryUserId.flatMapLatest { userId ->
            val contacts = getContacts(userId).getOrElse { emptyList() }
            return@flatMapLatest observeMessageWithLabels(userId, messageId).mapLatest { either ->
                either.fold(
                    ifLeft = { MessageDetailEvent.NoCachedMetadata },
                    ifRight = { messageWithLabels ->
                        MessageDetailEvent.MessageWithLabelsEvent(
                            messageDetailActionBarUiModelMapper.toUiModel(messageWithLabels.message),
                            messageDetailHeaderUiModelMapper.toUiModel(messageWithLabels, contacts)
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

    private fun observeMailFolders() {
        primaryUserId.flatMapLatest { userId ->
            combine(
                observeMailFolders(userId),
                observeFolderColor(userId)
            ) { folders, color ->
                MessageDetailEvent.MessageBottomSheetEvent(
                    BottomSheetEvent.Data(folders.toUiModels(color).let { it.folders + it.systems })
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
