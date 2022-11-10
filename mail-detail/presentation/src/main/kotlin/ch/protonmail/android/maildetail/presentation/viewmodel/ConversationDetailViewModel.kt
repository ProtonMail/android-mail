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
import ch.protonmail.android.mailcommon.domain.model.ConversationId
import ch.protonmail.android.mailcommon.domain.usecase.ObservePrimaryUserId
import ch.protonmail.android.mailcommon.presentation.model.BottomBarEvent
import ch.protonmail.android.mailcontact.domain.usecase.ObserveContacts
import ch.protonmail.android.mailconversation.domain.usecase.ObserveConversation
import ch.protonmail.android.mailconversation.domain.usecase.ObserveConversationMessages
import ch.protonmail.android.maildetail.domain.usecase.ObserveConversationDetailActions
import ch.protonmail.android.maildetail.presentation.mapper.ActionUiModelMapper
import ch.protonmail.android.maildetail.presentation.mapper.ConversationDetailMessageUiModelMapper
import ch.protonmail.android.maildetail.presentation.mapper.ConversationDetailMetadataUiModelMapper
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailEvent
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailState
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailViewAction
import ch.protonmail.android.maildetail.presentation.reducer.ConversationDetailReducer
import ch.protonmail.android.maildetail.presentation.ui.ConversationDetailScreen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import me.proton.core.domain.entity.UserId
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class ConversationDetailViewModel @Inject constructor(
    observePrimaryUserId: ObservePrimaryUserId,
    private val actionUiModelMapper: ActionUiModelMapper,
    private val conversationMessageMapper: ConversationDetailMessageUiModelMapper,
    private val conversationMetadataMapper: ConversationDetailMetadataUiModelMapper,
    private val observeContacts: ObserveContacts,
    private val observeConversation: ObserveConversation,
    private val observeConversationMessages: ObserveConversationMessages,
    private val observeDetailActions: ObserveConversationDetailActions,
    private val reducer: ConversationDetailReducer,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val primaryUserId: Flow<UserId?> = observePrimaryUserId()
    private val mutableDetailState = MutableStateFlow(initialState)

    val state: StateFlow<ConversationDetailState> = mutableDetailState.asStateFlow()

    init {
        val conversationIdParam = savedStateHandle.get<String>(ConversationDetailScreen.CONVERSATION_ID_KEY)
        Timber.d("Open detail screen for conversation ID: $conversationIdParam")

        if (conversationIdParam == null) {
            throw IllegalStateException("No Conversation id given")
        }
        val conversationId = ConversationId(conversationIdParam)
        observeConversationMetadata(conversationId)
        observeConversationMessages(conversationId)
        observeBottomBarActions(conversationId)
    }

    fun submit(action: ConversationDetailViewAction) {
        when (action) {
            is ConversationDetailViewAction.Star -> Timber.d("Star conversation clicked")
            is ConversationDetailViewAction.UnStar -> Timber.d("UnStar conversation clicked")
            is ConversationDetailViewAction.MarkUnread -> Timber.d("Mark Unread conversation clicked VM")
        }
    }

    private fun observeConversationMetadata(conversationId: ConversationId) {
        primaryUserId.flatMapLatest { userId ->
            if (userId == null) {
                return@flatMapLatest flowOf(ConversationDetailEvent.NoPrimaryUser)
            }
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
            if (userId == null) {
                return@flatMapLatest flowOf(ConversationDetailEvent.NoPrimaryUser)
            }
            combine(
                observeContacts(userId),
                observeConversationMessages(userId, conversationId)
            ) { contactsEither, messages ->
                val contacts = contactsEither.getOrElse {
                    Timber.i("Failed getting contacts for displaying initials. Fallback to display name")
                    emptyList()
                }
                val messagesUiModels = messages.map { conversationMessageMapper.toUiModel(it, contacts) }
                ConversationDetailEvent.MessagesData(messagesUiModels)
            }
        }.onEach { event ->
            emitNewStateFrom(event)
        }.launchIn(viewModelScope)
    }

    private fun observeBottomBarActions(conversationId: ConversationId) {
        primaryUserId.flatMapLatest { userId ->
            if (userId == null) {
                return@flatMapLatest flowOf(ConversationDetailEvent.NoPrimaryUser)
            }
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

    private suspend fun emitNewStateFrom(event: ConversationDetailEvent) {
        mutableDetailState.emit(reducer.newStateFrom(state.value, event))
    }

    companion object {

        val initialState = ConversationDetailState.Loading
    }

}
