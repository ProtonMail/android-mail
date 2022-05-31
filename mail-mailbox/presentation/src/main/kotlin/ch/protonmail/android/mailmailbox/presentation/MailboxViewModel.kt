/*
 * Copyright (c) 2021 Proton Technologies AG
 * This file is part of Proton Technologies AG and ProtonMail.
 *
 * ProtonMail is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ProtonMail is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with ProtonMail.  If not, see <https://www.gnu.org/licenses/>.
 */

package ch.protonmail.android.mailmailbox.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import ch.protonmail.android.mailcommon.presentation.Effect
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
import me.proton.core.accountmanager.domain.AccountManager
import me.proton.core.compose.viewmodel.stopTimeoutMillis
import me.proton.core.domain.entity.UserId
import me.proton.core.mailsettings.domain.entity.ViewMode
import me.proton.core.util.kotlin.EMPTY_STRING
import me.proton.core.util.kotlin.exhaustive
import javax.inject.Inject

@HiltViewModel
class MailboxViewModel @Inject constructor(
    private val accountManager: AccountManager,
    private val selectedSidebarLocation: SelectedSidebarLocation,
    private val markAsStaleMailboxItems: MarkAsStaleMailboxItems,
    observeCurrentViewMode: ObserveCurrentViewMode,
    private val pagingSourceFactory: MailboxItemPagingSourceFactory,
) : ViewModel() {

    private val viewModeBySettings: StateFlow<ViewMode> = observeCurrentViewMode()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = ObserveCurrentViewMode.DefaultViewMode
        )

    private val viewModeByLocation: StateFlow<ViewMode> = selectedSidebarLocation.location
        .flatMapLatest { sidebarLocation -> observeCurrentViewMode(sidebarLocation) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = ObserveCurrentViewMode.DefaultViewMode
        )

    private val userIds = observeUserIds()
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
            labelId = selectedSidebarLocation.location.value.labelId
        )
    }

    private fun observeUserIds(): Flow<List<UserId>> =
        // We only support 1 userId for now (primary).
        accountManager.getPrimaryUserId().map { listOfNotNull(it) }

    private fun observePagingData(): Flow<PagingData<MailboxItem>> = combine(
        userIds,
        viewModeByLocation,
        selectedSidebarLocation.location,
    ) { userIds, viewMode, location ->
        when {
            userIds.isEmpty() -> null
            else -> Pager(PagingConfig(pageSize = PageKey.defaultPageSize)) {
                pagingSourceFactory.create(
                    userIds = userIds,
                    location = location,
                    type = viewMode.toMailboxItemType(),
                )
            }
        }
    }.flatMapLatest { pager ->
        pager?.flow ?: flowOf(PagingData.empty())
    }

    private fun observeState() = combine(
        openItemDetailEffect,
        selectedSidebarLocation.location,
        topAppBarState,
        userIds,
    ) { openItemDetailEffect, location, topAppBarState, userIds ->
        when {
            userIds.isEmpty() -> MailboxState.Loading
            else -> {
                val currentLabelName = location::class.simpleName ?: EMPTY_STRING
                val newTopAppBarState = topAppBarState.withCurrentLabelName(currentLabelName)
                MailboxState(
                    topAppBar = newTopAppBarState,
                    selectedLocation = location,
                    unread = 0,
                    openItemEffect = openItemDetailEffect
                )
            }
        }
    }

    sealed interface Action {

        object EnterSelectionMode : Action
        object ExitSelectionMode : Action
        data class OpenItemDetails(val item: MailboxItem): Action
        object Refresh : Action
    }
}
