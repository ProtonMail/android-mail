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
import ch.protonmail.android.maillabel.domain.model.MailLabelId
import ch.protonmail.android.maillabel.domain.model.MailLabels
import ch.protonmail.android.maillabel.domain.usecase.ObserveMailLabels
import ch.protonmail.android.mailmailbox.domain.extension.firstOrDefault
import ch.protonmail.android.mailmailbox.domain.model.MailboxItem
import ch.protonmail.android.mailmailbox.domain.model.MailboxItemId
import ch.protonmail.android.mailmailbox.domain.model.MailboxItemType
import ch.protonmail.android.mailmailbox.domain.model.MailboxPageKey
import ch.protonmail.android.mailmailbox.domain.model.OpenMailboxItemRequest
import ch.protonmail.android.mailmailbox.domain.model.UnreadCounter
import ch.protonmail.android.mailmailbox.domain.model.toMailboxItemType
import ch.protonmail.android.mailmailbox.domain.usecase.MarkAsStaleMailboxItems
import ch.protonmail.android.mailmailbox.domain.usecase.ObserveCurrentViewMode
import ch.protonmail.android.mailmailbox.domain.usecase.ObserveUnreadCounters
import ch.protonmail.android.mailmailbox.presentation.model.MailboxListState
import ch.protonmail.android.mailmailbox.presentation.model.MailboxTopAppBarState
import ch.protonmail.android.mailmailbox.presentation.model.UnreadFilterState
import ch.protonmail.android.mailmailbox.presentation.paging.MailboxItemPagingSourceFactory
import ch.protonmail.android.mailpagination.domain.entity.PageFilter
import ch.protonmail.android.mailpagination.domain.entity.PageKey
import ch.protonmail.android.mailpagination.domain.entity.ReadStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import me.proton.core.domain.entity.UserId
import me.proton.core.mailsettings.domain.entity.ViewMode
import me.proton.core.util.kotlin.exhaustive
import javax.inject.Inject

@HiltViewModel
class MailboxViewModel @Inject constructor(
    private val markAsStaleMailboxItems: MarkAsStaleMailboxItems,
    private val pagingSourceFactory: MailboxItemPagingSourceFactory,
    private val observeCurrentViewMode: ObserveCurrentViewMode,
    observePrimaryUserId: ObservePrimaryUserId,
    private val observeMailLabels: ObserveMailLabels,
    private val selectedMailLabelId: SelectedMailLabelId,
    private val observeUnreadCounters: ObserveUnreadCounters
) : ViewModel() {

    private val primaryUserId = observePrimaryUserId()

    private val isUnreadFilterEnabled: MutableStateFlow<Boolean> = MutableStateFlow(false)

    val items: Flow<PagingData<MailboxItem>> = observePagingData()
        .cachedIn(viewModelScope)

    val initialState = MailboxState(
        mailboxListState = MailboxListState.Loading,
        topAppBarState = MailboxTopAppBarState.Loading,
        unreadFilterState = UnreadFilterState.Loading
    )

    private val _mutableState = MutableStateFlow(initialState)
    val state: StateFlow<MailboxState> = _mutableState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                primaryUserId,
                selectedMailLabelId.flow,
                observeUnreadCounters(),
                observeMailLabels()
            ) { userId, currentMailLabelId, unreadCounters, mailLabels ->
                val currentMailLabel = mailLabels.allById[currentMailLabelId]

                if (userId == null) {
                    return@combine // TODO no user logged in?! handle error
                }
                if (currentMailLabel == null) {
                    return@combine // TODO no currently selected label?
                }

                val count = unreadCounters.find { it.labelId == currentMailLabelId.labelId }?.count ?: 0

                val unreadFilterState = when (val currentState = state.value.unreadFilterState) {
                    is UnreadFilterState.Loading -> UnreadFilterState.Data(count, false)
                    is UnreadFilterState.Data -> currentState.copy(count)
                }
                val topAppBarState = when (val currentState = state.value.topAppBarState) {
                    MailboxTopAppBarState.Loading -> MailboxTopAppBarState.Data.DefaultMode(currentMailLabel)
                    is MailboxTopAppBarState.Data -> currentState.with(currentMailLabel)
                }

                val mailboxListState = when (val currentState = state.value.mailboxListState) {
                    is MailboxListState.Loading -> MailboxListState.Data(currentMailLabel, Effect.empty())
                    is MailboxListState.Data -> currentState.copy(currentMailLabel = currentMailLabel)
                }

                _mutableState.emit(MailboxState(mailboxListState, topAppBarState, unreadFilterState))

            }.collect()
        }
    }

    fun submit(action: Action) {
        viewModelScope.launch {
            when (action) {
                Action.EnterSelectionMode -> onOpenSelectionMode()
                Action.ExitSelectionMode -> onCloseSelectionMode()
                is Action.OpenItemDetails -> onOpenItemDetails(action.item)
                Action.Refresh -> onRefresh()
                Action.DisableUnreadFilter -> toggleUnreadFilter(false)
                Action.EnableUnreadFilter -> toggleUnreadFilter(true)
            }.exhaustive
        }
    }

    private suspend fun toggleUnreadFilter(enabled: Boolean) {
        isUnreadFilterEnabled.emit(enabled)

        when (val currentState = state.value.unreadFilterState) {
            is UnreadFilterState.Loading -> return
            is UnreadFilterState.Data -> _mutableState.emit(
                state.value.copy(unreadFilterState = currentState.copy(isFilterEnabled = enabled))
            )
        }

    }

    private suspend fun onCloseSelectionMode() {
        when (val currentState = state.value.topAppBarState) {
            is MailboxTopAppBarState.Loading -> return // TODO is this illegal?
            is MailboxTopAppBarState.Data -> _mutableState.emit(
                state.value.copy(topAppBarState = currentState.toDefaultMode())
            )
        }
    }

    private suspend fun onOpenSelectionMode() {
        when (val currentState = state.value.topAppBarState) {
            is MailboxTopAppBarState.Loading -> return
            is MailboxTopAppBarState.Data -> _mutableState.emit(
                state.value.copy(topAppBarState = currentState.toSelectionMode())
            )
        }
    }

    private suspend fun onOpenItemDetails(item: MailboxItem) {
        val request = when (getPreferredViewMode()) {
            ViewMode.ConversationGrouping -> {
                OpenMailboxItemRequest(MailboxItemId(item.conversationId.id), MailboxItemType.Conversation)
            }
            ViewMode.NoConversationGrouping -> {
                OpenMailboxItemRequest(MailboxItemId(item.id), item.type)
            }
        }
        when (val currentListState = state.value.mailboxListState) {
            is MailboxListState.Loading -> throw IllegalStateException("Can't open item while list is loading") // TODO any better way around?
            is MailboxListState.Data -> _mutableState.emit(
                state.value.copy(
                    mailboxListState = currentListState.copy(openItemEffect = Effect.of(request))
                )
            )
        }
    }

    private suspend fun onRefresh() {
        markAsStaleMailboxItems(
            userIds = listOfNotNull(primaryUserId.firstOrNull()),
            type = observeViewModeByLocation().firstOrDefault().toMailboxItemType(),
            labelId = selectedMailLabelId.flow.value.labelId
        )
    }

    private fun mailboxPageKey(
        hasUnreadFilter: Boolean,
        selectedMailLabelId: MailLabelId,
        userId: UserId
    ): MailboxPageKey {
        val readStatus = if (hasUnreadFilter) ReadStatus.Unread else ReadStatus.All
        val pageFilter = PageFilter(
            labelId = selectedMailLabelId.labelId,
            read = readStatus
        )
        return MailboxPageKey(userIds = listOf(userId), pageKey = PageKey(pageFilter))
    }

    // TODO can this be moved to observing the state only?
    private fun observePagingData(): Flow<PagingData<MailboxItem>> = combine(
        selectedMailLabelId.flow,
        primaryUserId,
        observeViewModeByLocation(),
        isUnreadFilterEnabled
    ) { selectedMailLabelId, userId, viewMode, hasUnreadFilter ->
        if (userId == null) {
            return@combine null
        }

        Pager(
            config = PagingConfig(PageKey.defaultPageSize),
            initialKey = mailboxPageKey(hasUnreadFilter, selectedMailLabelId, userId)
        ) {
            pagingSourceFactory.create(
                userIds = listOf(userId),
                selectedMailLabelId = selectedMailLabelId,
                type = viewMode.toMailboxItemType()
            )
        }
    }.flatMapLatest { pager ->
        pager?.flow ?: flowOf(PagingData.empty())
    }


    private fun observeUnreadCounters(): Flow<List<UnreadCounter>> = primaryUserId.flatMapLatest { userId ->
        if (userId == null) flowOf(emptyList())
        else observeUnreadCounters(userId)
    }

    private fun observeViewModeByLocation(): Flow<ViewMode> = primaryUserId.flatMapLatest { userId ->
        if (userId == null) {
            flowOf(ObserveCurrentViewMode.DefaultViewMode)
        } else {
            selectedMailLabelId.flow.flatMapLatest { observeCurrentViewMode(userId, it) }
        }
    }

    private fun observeMailLabels() = primaryUserId.flatMapLatest { userId ->
        if (userId == null) {
            flowOf(MailLabels.Initial)
        } else {
            observeMailLabels(userId)
        }
    }

    private suspend fun getPreferredViewMode(): ViewMode {
        val userId = primaryUserId.firstOrNull()

        return if (userId == null) {
            ObserveCurrentViewMode.DefaultViewMode
        } else {
            observeCurrentViewMode(userId).first()
        }
    }

    sealed interface Action {

        object EnterSelectionMode : Action
        object ExitSelectionMode : Action
        data class OpenItemDetails(val item: MailboxItem) : Action
        object Refresh : Action
        object EnableUnreadFilter : Action
        object DisableUnreadFilter : Action
    }
}
