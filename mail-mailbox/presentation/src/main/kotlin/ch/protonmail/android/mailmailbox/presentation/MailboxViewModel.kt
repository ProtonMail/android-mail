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
import ch.protonmail.android.mailmailbox.domain.model.MailboxItem
import ch.protonmail.android.mailmailbox.domain.model.MailboxItemType
import ch.protonmail.android.mailmailbox.domain.usecase.MarkAsStaleMailboxItems
import ch.protonmail.android.mailmailbox.domain.usecase.ObserveMailboxItemType
import ch.protonmail.android.mailmailbox.presentation.model.MailboxTopAppBarState
import ch.protonmail.android.mailmailbox.presentation.paging.MailboxItemPagingSourceFactory
import ch.protonmail.android.mailpagination.domain.entity.PageKey
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import me.proton.core.accountmanager.domain.AccountManager
import me.proton.core.domain.entity.UserId
import javax.inject.Inject

@HiltViewModel
class MailboxViewModel @Inject constructor(
    private val accountManager: AccountManager,
    private val selectedSidebarLocation: SelectedSidebarLocation,
    private val markAsStaleMailboxItems: MarkAsStaleMailboxItems,
    private val observeMailboxItemType: ObserveMailboxItemType,
    private val pagingSourceFactory: MailboxItemPagingSourceFactory,
) : ViewModel() {

    private val mailboxItemType = observeMailboxItemType()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = MailboxItemType.Message
        )

    private val userIds = observeUserIds()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    val items: Flow<PagingData<MailboxItem>> = observePagingData()
        .cachedIn(viewModelScope)

    val state: MutableStateFlow<MailboxState> =
        MutableStateFlow(MailboxState.Loading)

    init {
        combine(
            userIds,
            selectedSidebarLocation.location
        ) { userIds, location ->
            when {
                userIds.isEmpty() -> MailboxState.Loading
                else -> {
                    val locationName = checkNotNull(location::class.simpleName) { "null location name" }
                    val newTopAppBarState = when (val topAppBarState = state.value.topAppBar) {
                        MailboxTopAppBarState.Loading ->
                            MailboxTopAppBarState.Data.DefaultMode(currentLabelName = locationName)
                        is MailboxTopAppBarState.Data.DefaultMode ->
                            topAppBarState.copy(currentLabelName = locationName)
                        is MailboxTopAppBarState.Data.SearchMode ->
                            topAppBarState.copy(currentLabelName = locationName)
                        is MailboxTopAppBarState.Data.SelectionMode ->
                            topAppBarState.copy(currentLabelName = locationName)
                    }
                    state.value.copy(topAppBar = newTopAppBarState, selectedLocation = location)
                }
            }
        }
            .onEach(state::emit)
            .launchIn(viewModelScope)
    }

    fun submit(action: Action) {
        viewModelScope.launch {
            when (action) {
                Action.OpenSelectionMode -> onOpenSelectionMode()
                Action.CloseSelectionMode -> onCloseSelectionMode()
                Action.Refresh -> onRefresh()
            }
        }
    }

    private suspend fun onCloseSelectionMode() {
        val newAppBarState = (state.value.topAppBar as? MailboxTopAppBarState.Data)
            ?.toDefaultMode()
            ?: return
        val newState = state.value.copy(topAppBar = newAppBarState)
        state.emit(newState)
    }

    private suspend fun onOpenSelectionMode() {
        val newAppBarState = (state.value.topAppBar as? MailboxTopAppBarState.Data)
            ?.toSelectionMode()
            ?: return
        val newState = state.value.copy(topAppBar = newAppBarState)
        state.emit(newState)
    }

    private suspend fun onRefresh() {
        markAsStaleMailboxItems(
            userIds = userIds.value,
            type = mailboxItemType.value,
            labelId = selectedSidebarLocation.location.value.labelId
        )
    }

    private fun observeUserIds(): Flow<List<UserId>> =
        // We only support 1 userId for now (primary).
        accountManager.getPrimaryUserId().map { listOfNotNull(it) }

    private fun observePagingData(): Flow<PagingData<MailboxItem>> = combine(
        userIds,
        mailboxItemType,
        selectedSidebarLocation.location,
    ) { userIds, mailboxItemType, location ->
        when {
            userIds.isEmpty() -> null
            else -> Pager(PagingConfig(pageSize = PageKey.defaultPageSize)) {
                pagingSourceFactory.create(
                    userIds = userIds,
                    location = location,
                    type = mailboxItemType,
                )
            }
        }
    }.flatMapLatest { pager ->
        pager?.flow ?: flowOf(PagingData.empty())
    }

    sealed interface Action {

        object CloseSelectionMode : Action
        object OpenSelectionMode : Action
        object Refresh : Action
    }
}
