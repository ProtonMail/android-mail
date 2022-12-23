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
import arrow.core.Either
import arrow.core.NonEmptyList
import arrow.core.getOrElse
import arrow.core.getOrHandle
import ch.protonmail.android.mailcommon.domain.model.ConversationId
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcommon.domain.usecase.ObservePrimaryUserId
import ch.protonmail.android.mailcommon.presentation.model.BottomBarEvent
import ch.protonmail.android.mailcontact.domain.usecase.ObserveContacts
import ch.protonmail.android.mailconversation.domain.usecase.ObserveConversation
import ch.protonmail.android.maildetail.domain.model.MessageWithLabels
import ch.protonmail.android.maildetail.domain.usecase.MoveConversation
import ch.protonmail.android.maildetail.domain.usecase.ObserveConversationDetailActions
import ch.protonmail.android.maildetail.domain.usecase.ObserveConversationMessagesWithLabels
import ch.protonmail.android.maildetail.domain.usecase.StarConversation
import ch.protonmail.android.maildetail.domain.usecase.UnStarConversation
import ch.protonmail.android.maildetail.presentation.mapper.ActionUiModelMapper
import ch.protonmail.android.maildetail.presentation.mapper.ConversationDetailMessageUiModelMapper
import ch.protonmail.android.maildetail.presentation.mapper.ConversationDetailMetadataUiModelMapper
import ch.protonmail.android.maildetail.presentation.model.BottomSheetEvent
import ch.protonmail.android.maildetail.presentation.model.BottomSheetState
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailEvent
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailOperation
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailState
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailViewAction
import ch.protonmail.android.maildetail.presentation.reducer.ConversationDetailReducer
import ch.protonmail.android.maildetail.presentation.ui.ConversationDetailScreen
import ch.protonmail.android.maillabel.domain.model.MailLabelId
import ch.protonmail.android.maillabel.domain.model.SystemLabelId
import ch.protonmail.android.maillabel.domain.usecase.ObserveExclusiveDestinationMailLabels
import ch.protonmail.android.maillabel.presentation.toUiModels
import ch.protonmail.android.mailsettings.domain.usecase.ObserveFolderColorSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import me.proton.core.domain.entity.UserId
import me.proton.core.util.kotlin.exhaustive
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class ConversationDetailViewModel @Inject constructor(
    observePrimaryUserId: ObservePrimaryUserId,
    private val actionUiModelMapper: ActionUiModelMapper,
    private val conversationMessageMapper: ConversationDetailMessageUiModelMapper,
    private val conversationMetadataMapper: ConversationDetailMetadataUiModelMapper,
    private val moveConversation: MoveConversation,
    private val observeContacts: ObserveContacts,
    private val observeConversation: ObserveConversation,
    private val observeConversationMessages: ObserveConversationMessagesWithLabels,
    private val observeDetailActions: ObserveConversationDetailActions,
    private val observeDestinationMailLabels: ObserveExclusiveDestinationMailLabels,
    private val observeFolderColor: ObserveFolderColorSettings,
    private val reducer: ConversationDetailReducer,
    private val starConversation: StarConversation,
    private val unStarConversation: UnStarConversation,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val primaryUserId: Flow<UserId> = observePrimaryUserId().filterNotNull()
    private val mutableDetailState = MutableStateFlow(initialState)

    private val conversationId = requireConversationId()

    val state: StateFlow<ConversationDetailState> = mutableDetailState.asStateFlow()

    init {
        Timber.d("Open detail screen for conversation ID: $conversationId")
        observeConversationMetadata(conversationId)
        observeConversationMessages(conversationId)
        observeBottomBarActions(conversationId)
        observeMailFolders()
    }

    fun submit(action: ConversationDetailViewAction) {
        when (action) {
            is ConversationDetailViewAction.Star -> starConversation()
            is ConversationDetailViewAction.UnStar -> unStarConversation()
            is ConversationDetailViewAction.MarkUnread -> Timber.d("Mark Unread conversation clicked VM")
            is ConversationDetailViewAction.Trash -> moveConversationToTrash()
            is ConversationDetailViewAction.MoveToDestinationSelected -> moveToDestinationSelected(action.mailLabelId)
            is ConversationDetailViewAction.MoveToDestinationConfirmed ->
                onBottomSheetDestinationConfirmed(action.mailLabelText)
        }.exhaustive
    }

    private fun observeConversationMetadata(conversationId: ConversationId) {
        primaryUserId.flatMapLatest { userId ->
            observeConversation(userId, conversationId)
                .mapLatest { either ->
                    either.fold(
                        ifLeft = { ConversationDetailEvent.ErrorLoadingConversation },
                        ifRight = { ConversationDetailEvent.ConversationData(conversationMetadataMapper.toUiModel(it)) }
                    )
                }
        }.onEach { event ->
            emitNewStateFrom(event)
        }.launchIn(viewModelScope)
    }

    private fun observeConversationMessages(conversationId: ConversationId) {
        primaryUserId.flatMapLatest { userId ->
            combine(
                observeContacts(userId),
                observeConversationMessages(userId, conversationId).ignoreLocalErrors()
            ) { contactsEither, messagesEither ->
                val contacts = contactsEither.getOrElse {
                    Timber.i("Failed getting contacts for displaying initials. Fallback to display name")
                    emptyList()
                }
                val messages = messagesEither.getOrHandle {
                    return@combine ConversationDetailEvent.ErrorLoadingMessages
                }
                val messagesUiModels = messages.map { conversationMessageMapper.toUiModel(it, contacts) }
                ConversationDetailEvent.MessagesData(messagesUiModels)
            }
        }.onEach(::emitNewStateFrom)
            .launchIn(viewModelScope)
    }

    private fun observeBottomBarActions(conversationId: ConversationId) {
        primaryUserId.flatMapLatest { userId ->
            observeDetailActions(userId, conversationId).mapLatest { either ->
                either.fold(
                    ifLeft = { ConversationDetailEvent.ConversationBottomBarEvent(BottomBarEvent.ErrorLoadingActions) },
                    ifRight = { actions ->
                        val actionUiModels = actions.map { actionUiModelMapper.toUiModel(it) }
                        ConversationDetailEvent.ConversationBottomBarEvent(BottomBarEvent.ActionsData(actionUiModels))
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
                observeDestinationMailLabels(userId),
                observeFolderColor(userId)
            ) { folders, color ->
                ConversationDetailEvent.ConversationBottomSheetEvent(
                    BottomSheetEvent.Data(folders.toUiModels(color).let { it.folders + it.systems })
                )
            }
        }.onEach { event ->
            emitNewStateFrom(event)
        }.launchIn(viewModelScope)
    }

    private fun requireConversationId(): ConversationId {
        val conversationId = savedStateHandle.get<String>(ConversationDetailScreen.ConversationIdKey)
            ?: throw IllegalStateException("No Conversation id given")
        return ConversationId(conversationId)
    }

    private suspend fun emitNewStateFrom(event: ConversationDetailOperation) {
        mutableDetailState.emit(reducer.newStateFrom(state.value, event))
    }

    private fun moveConversationToTrash() {
        primaryUserId.mapLatest { userId ->
            moveConversation(userId, conversationId, SystemLabelId.Trash.labelId).fold(
                ifLeft = { ConversationDetailEvent.ErrorMovingToTrash },
                ifRight = { ConversationDetailViewAction.Trash }
            )
        }.onEach(::emitNewStateFrom)
            .launchIn(viewModelScope)
    }

    private fun starConversation() {
        primaryUserId.mapLatest { userId ->
            starConversation(userId, conversationId).fold(
                ifLeft = { ConversationDetailEvent.ErrorAddStar },
                ifRight = { ConversationDetailViewAction.Star }
            )
        }.onEach { event ->
            emitNewStateFrom(event)
        }.launchIn(viewModelScope)
    }

    private fun unStarConversation() {
        Timber.d("UnStar conversation clicked")
        primaryUserId.mapLatest { userId ->
            unStarConversation(userId, conversationId).fold(
                ifLeft = { ConversationDetailEvent.ErrorRemoveStar },
                ifRight = { ConversationDetailViewAction.UnStar }
            )
        }.onEach { event ->
            emitNewStateFrom(event)
        }.launchIn(viewModelScope)
    }

    private fun moveToDestinationSelected(mailLabelId: MailLabelId) {
        viewModelScope.launch {
            emitNewStateFrom(ConversationDetailViewAction.MoveToDestinationSelected(mailLabelId))
        }
    }

    private fun onBottomSheetDestinationConfirmed(mailLabelText: String) {
        primaryUserId.mapLatest { userId ->
            val bottomSheetState = state.value.bottomSheetState
            if (bottomSheetState is BottomSheetState.Data) {
                bottomSheetState.moveToDestinations.firstOrNull { it.isSelected }?.let { mailLabelUiModel ->
                    moveConversation(userId, conversationId, mailLabelUiModel.id.labelId).fold(
                        ifLeft = { ConversationDetailEvent.ErrorMovingConversation },
                        ifRight = { ConversationDetailViewAction.MoveToDestinationConfirmed(mailLabelText) }
                    )
                } ?: throw IllegalStateException("No destination selected")
            } else {
                ConversationDetailEvent.ErrorMovingConversation
            }
        }.onEach { event ->
            emitNewStateFrom(event)
        }.launchIn(viewModelScope)
    }

    companion object {

        val initialState = ConversationDetailState.Loading
    }
}

/**
 * Filters [DataError.Local] from messages flow, as we don't want to show them to the user, because the fetch is being
 *  done on the conversation flow.
 */
private fun Flow<Either<DataError, NonEmptyList<MessageWithLabels>>>.ignoreLocalErrors():
    Flow<Either<DataError, NonEmptyList<MessageWithLabels>>> =
    filter { either ->
        either.fold(
            ifLeft = { error -> error !is DataError.Local },
            ifRight = { true }
        )
    }
