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
import ch.protonmail.android.mailcommon.domain.model.ConversationId
import ch.protonmail.android.mailcommon.domain.usecase.ObservePrimaryUserId
import ch.protonmail.android.mailcommon.presentation.model.BottomBarEvent
import ch.protonmail.android.mailcommon.presentation.reducer.BottomBarStateReducer
import ch.protonmail.android.mailconversation.domain.usecase.ObserveConversation
import ch.protonmail.android.maildetail.domain.usecase.ObserveConversationDetailActions
import ch.protonmail.android.maildetail.presentation.mapper.ActionUiModelMapper
import ch.protonmail.android.maildetail.presentation.mapper.ConversationDetailUiModelMapper
import ch.protonmail.android.maildetail.presentation.model.AffectingConversation
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailEvent
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailState
import ch.protonmail.android.maildetail.presentation.model.ConversationViewAction
import ch.protonmail.android.maildetail.presentation.reducer.ConversationStateReducer
import ch.protonmail.android.maildetail.presentation.ui.ConversationDetailScreen
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
class ConversationDetailViewModel @Inject constructor(
    observePrimaryUserId: ObservePrimaryUserId,
    private val conversationStateReducer: ConversationStateReducer,
    private val bottomBarStateReducer: BottomBarStateReducer,
    private val observeConversation: ObserveConversation,
    private val uiModelMapper: ConversationDetailUiModelMapper,
    private val actionUiModelMapper: ActionUiModelMapper,
    private val observeDetailActions: ObserveConversationDetailActions,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val primaryUserId = observePrimaryUserId()
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
        observeBottomBarActions(conversationId)
    }

    fun submit(action: ConversationViewAction) {
        when (action) {
            is ConversationViewAction.Star -> Timber.d("Star conversation clicked")
            is ConversationViewAction.UnStar -> Timber.d("UnStar conversation clicked")
        }
    }

    private fun observeConversationMetadata(conversationId: ConversationId) {
        primaryUserId.flatMapLatest { userId ->
            if (userId == null) {
                return@flatMapLatest flowOf(ConversationDetailEvent.NoPrimaryUser)
            }
            return@flatMapLatest observeConversation(userId, conversationId)
                .mapLatest { either ->
                    either.fold(
                        ifLeft = { ConversationDetailEvent.ErrorLoadingConversation },
                        ifRight = { ConversationDetailEvent.ConversationData(uiModelMapper.toUiModel(it)) }
                    )
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
        val updatedDetailState = state.value.copy(
            conversationState = updateConversationState(event),
            bottomBarState = updateBottomBarState(event)
        )
        mutableDetailState.emit(updatedDetailState)
    }

    private fun updateConversationState(event: ConversationDetailEvent) =
        if (event is AffectingConversation) {
            conversationStateReducer.reduce(state.value.conversationState, event)
        } else {
            state.value.conversationState
        }

    private fun updateBottomBarState(event: ConversationDetailEvent) =
        if (event is ConversationDetailEvent.ConversationBottomBarEvent) {
            bottomBarStateReducer.reduce(state.value.bottomBarState, event.bottomBarEvent)
        } else {
            state.value.bottomBarState
        }

    companion object {

        val initialState = ConversationDetailState.Loading
    }

}
