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

package ch.protonmail.android.mailmailbox.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import ch.protonmail.android.mailcommon.domain.usecase.ObservePrimaryUserId
import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.maillabel.domain.SelectedMailLabelId
import ch.protonmail.android.maillabel.domain.model.MailLabels
import ch.protonmail.android.maillabel.domain.usecase.ObserveMailLabels
import ch.protonmail.android.mailmailbox.domain.model.MailboxItem
import ch.protonmail.android.mailmailbox.domain.model.MailboxItemId
import ch.protonmail.android.mailmailbox.domain.model.MailboxItemType
import ch.protonmail.android.mailmailbox.domain.model.OpenMailboxItemRequest
import ch.protonmail.android.mailmailbox.domain.model.toMailboxItemType
import ch.protonmail.android.mailmailbox.domain.usecase.MarkAsStaleMailboxItems
import ch.protonmail.android.mailmailbox.domain.usecase.ObserveCurrentViewMode
import ch.protonmail.android.mailmailbox.presentation.model.MailboxTopAppBarState
import ch.protonmail.android.mailmailbox.presentation.paging.MailboxItemPagingSourceFactory
import ch.protonmail.android.mailpagination.domain.entity.PageKey
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import me.proton.core.compose.viewmodel.stopTimeoutMillis
import me.proton.core.domain.entity.UserId
import me.proton.core.mailsettings.domain.entity.ViewMode
import me.proton.core.util.kotlin.exhaustive
import javax.inject.Inject

@HiltViewModel
class MailboxViewModel @Inject constructor(
    private val selectedSidebarLocation: SelectedSidebarLocation,
    private val markAsStaleMailboxItems: MarkAsStaleMailboxItems,
    private val pagingSourceFactory: MailboxItemPagingSourceFactory,
    private val observeCurrentViewMode: ObserveCurrentViewMode,
    private val observePrimaryUserId: ObservePrimaryUserId,
    private val observeMailLabels: ObserveMailLabels,
    private val selectedMailLabelId: SelectedMailLabelId,
) : ViewModel() {

    private val primaryUserMailLabels = observePrimaryUserId().flatMapLatest { userId ->
        when (userId) {
            null -> flowOf(MailLabels.Initial)
            else -> observeMailLabels(userId)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis),
        initialValue = MailLabels.Initial
    )

    private val currentMailLabel = combine(
        selectedMailLabelId.flow,
        primaryUserMailLabels
    ) { selectedMailLabelId, primaryMailLabels ->
        primaryMailLabels.allById[selectedMailLabelId]
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis),
        initialValue = null
    )

    private val viewModeBySettings: StateFlow<ViewMode> = observePrimaryUserId().flatMapLatest { _userId ->
        val userId = _userId
            ?: return@flatMapLatest flowOf(ObserveCurrentViewMode.DefaultViewMode)

        observeCurrentViewMode(userId)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis),
        initialValue = ObserveCurrentViewMode.DefaultViewMode
    )

    private val viewModeByLocation: StateFlow<ViewMode> = combine(
        observePrimaryUserId(),
        selectedMailLabelId.flow
    ) { userId, mailLabelId -> userId to mailLabelId }
        .flatMapLatest { (_userId, mailLabelId) ->
            val userId = _userId
                ?: return@flatMapLatest flowOf(ObserveCurrentViewMode.DefaultViewMode)

            observeCurrentViewMode(userId, mailLabelId)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = ObserveCurrentViewMode.DefaultViewMode
        )

    private val userIds: StateFlow<List<UserId>> = observeUserIds()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    private val openItemDetailEffect: MutableStateFlow<Effect<OpenMailboxItemRequest>> =
        MutableStateFlow(Effect.empty())

    private val topAppBarState: MutableStateFlow<MailboxTopAppBarState> =
        MutableStateFlow(MailboxTopAppBarState.Loading)

    val items: Flow<PagingData<MailboxItem>> = observePagingData()
        .cachedIn(viewModelScope)

    val state: StateFlow<MailboxState> = observeState()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(stopTimeoutMillis), MailboxState.Loading)

    fun submit(action: Action) {
        viewModelScope.launch {
            when (action) {
                Action.EnterSelectionMode -> onOpenSelectionMode()
                Action.ExitSelectionMode -> onCloseSelectionMode()
                is Action.OpenItemDetails -> onOpenItemDetails(action.item)
                Action.Refresh -> onRefresh()
            }.exhaustive
        }
    }

    private suspend fun onCloseSelectionMode() {
        when (val currentState = state.value.topAppBar) {
            MailboxTopAppBarState.Loading -> return
            is MailboxTopAppBarState.Data -> topAppBarState.emit(currentState.toDefaultMode())
        }
    }

    private suspend fun onOpenSelectionMode() {
        when (val currentState = state.value.topAppBar) {
            MailboxTopAppBarState.Loading -> return
            is MailboxTopAppBarState.Data -> topAppBarState.emit(currentState.toSelectionMode())
        }
    }

    private suspend fun onOpenItemDetails(item: MailboxItem) {
        val request = when (viewModeBySettings.value) {
            ViewMode.ConversationGrouping -> {
                OpenMailboxItemRequest(MailboxItemId(item.conversationId.id), MailboxItemType.Conversation)
            }
            ViewMode.NoConversationGrouping -> {
                OpenMailboxItemRequest(MailboxItemId(item.id), item.type)
            }
        }
        openItemDetailEffect.emit(Effect.of(request))
    }

    private suspend fun onRefresh() {
        markAsStaleMailboxItems(
            userIds = userIds.value,
            type = viewModeByLocation.value.toMailboxItemType(),
            labelId = selectedMailLabelId.flow.value.labelId
        )
    }

    private fun observeUserIds(): Flow<List<UserId>> =
        // We only support 1 userId for now (primary).
        observePrimaryUserId().map { listOfNotNull(it) }

    private fun observePagingData(): Flow<PagingData<MailboxItem>> = combine(
        selectedMailLabelId.flow,
        userIds,
        viewModeByLocation
    ) { selectedMailLabelId, userIds, viewMode ->
        when {
            userIds.isEmpty() -> null
            else -> Pager(PagingConfig(pageSize = PageKey.defaultPageSize)) {
                pagingSourceFactory.create(
                    userIds = userIds,
                    selectedMailLabelId = selectedMailLabelId,
                    type = viewMode.toMailboxItemType(),
                )
            }
        }
    }.flatMapLatest { pager ->
        pager?.flow ?: flowOf(PagingData.empty())
    }

    private fun observeState() = combine(
        currentMailLabel,
        openItemDetailEffect,
        topAppBarState,
        userIds,
    ) { currentMailLabel, openItemDetailEffect, topAppBarState, userIds ->
        if (userIds.isEmpty() || currentMailLabel == null)
            return@combine MailboxState.Loading

        val newTopAppBarState = topAppBarState.withCurrentMailLabel(currentMailLabel)
        MailboxState.Data(
            topAppBar = newTopAppBarState,
            currentMailLabel = currentMailLabel,
            openItemEffect = openItemDetailEffect
        )
    }

    sealed interface Action {

        object EnterSelectionMode : Action
        object ExitSelectionMode : Action
        data class OpenItemDetails(val item: MailboxItem): Action
        object Refresh : Action
    }
}
