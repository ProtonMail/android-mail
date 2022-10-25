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

package ch.protonmail.android.mailmailbox.presentation.mailbox

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.map
import arrow.core.getOrElse
import ch.protonmail.android.mailcommon.domain.usecase.ObservePrimaryUserId
import ch.protonmail.android.mailcontact.domain.usecase.GetContacts
import ch.protonmail.android.maillabel.domain.SelectedMailLabelId
import ch.protonmail.android.maillabel.domain.model.MailLabel
import ch.protonmail.android.maillabel.domain.model.MailLabelId
import ch.protonmail.android.maillabel.domain.model.MailLabels
import ch.protonmail.android.maillabel.domain.usecase.ObserveMailLabels
import ch.protonmail.android.mailmailbox.domain.extension.firstOrDefault
import ch.protonmail.android.mailmailbox.domain.model.UnreadCounter
import ch.protonmail.android.mailmailbox.domain.model.toMailboxItemType
import ch.protonmail.android.mailmailbox.domain.usecase.MarkAsStaleMailboxItems
import ch.protonmail.android.mailmailbox.domain.usecase.ObserveCurrentViewMode
import ch.protonmail.android.mailmailbox.domain.usecase.ObserveUnreadCounters
import ch.protonmail.android.mailmailbox.presentation.mailbox.mapper.MailboxItemUiModelMapper
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxEvent
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxItemUiModel
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxListState
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxState
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxTopAppBarState
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.UnreadFilterState
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxViewAction
import ch.protonmail.android.mailmailbox.presentation.mailbox.reducer.MailboxListReducer
import ch.protonmail.android.mailmailbox.presentation.mailbox.reducer.MailboxTopAppBarReducer
import ch.protonmail.android.mailmailbox.presentation.mailbox.reducer.MailboxUnreadFilterReducer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import me.proton.core.contact.domain.entity.Contact
import me.proton.core.mailsettings.domain.entity.ViewMode
import me.proton.core.util.kotlin.exhaustive
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class MailboxViewModel @Inject constructor(
    private val markAsStaleMailboxItems: MarkAsStaleMailboxItems,
    private val mailboxPagerFactory: MailboxPagerFactory,
    private val observeCurrentViewMode: ObserveCurrentViewMode,
    observePrimaryUserId: ObservePrimaryUserId,
    private val observeMailLabels: ObserveMailLabels,
    private val selectedMailLabelId: SelectedMailLabelId,
    private val observeUnreadCounters: ObserveUnreadCounters,
    private val mailboxItemMapper: MailboxItemUiModelMapper,
    private val getContacts: GetContacts,
    private val mailboxTopAppBarReducer: MailboxTopAppBarReducer,
    private val unreadFilterReducer: MailboxUnreadFilterReducer,
    private val mailboxListReducer: MailboxListReducer
) : ViewModel() {

    private val primaryUserId = observePrimaryUserId()
    private val mutableState = MutableStateFlow(initialState)

    val state: StateFlow<MailboxState> = mutableState.asStateFlow()
    val items: Flow<PagingData<MailboxItemUiModel>> = observePagingData().cachedIn(viewModelScope)

    init {
        observeCurrentMailLabel()
            .onEach { currentMailLabel ->
                updateStateForMailLabelChange(currentMailLabel)
            }
            .launchIn(viewModelScope)

        selectedMailLabelId.flow
            .mapToExistingLabel()
            .pairWithCurrentLabelCount()
            .onEach { (currentMailLabel, currentLabelCount) ->
                updateStateForSelectedMailLabelChange(currentMailLabel, currentLabelCount)
            }
            .launchIn(viewModelScope)

        observeUnreadCounters()
            .mapToCurrentLabelCount()
            .filterNotNull()
            .onEach { currentLabelCount ->
                updateStateForUnreadCountersChange(currentLabelCount)
            }
            .launchIn(viewModelScope)
    }

    internal fun submit(viewAction: MailboxViewAction) {
        viewModelScope.launch {
            when (viewAction) {
                is MailboxViewAction.EnterSelectionMode -> onOpenSelectionMode()
                is MailboxViewAction.ExitSelectionMode -> onCloseSelectionMode()
                is MailboxViewAction.OpenItemDetails -> onOpenItemDetails(viewAction.item)
                is MailboxViewAction.Refresh -> onRefresh()
                is MailboxViewAction.DisableUnreadFilter -> toggleUnreadFilter(isEnabled = false)
                is MailboxViewAction.EnableUnreadFilter -> toggleUnreadFilter(isEnabled = true)
            }.exhaustive
        }
    }

    private suspend fun toggleUnreadFilter(isEnabled: Boolean) {
        val unreadFilterState = unreadFilterReducer.newStateFrom(
            currentState = state.value.unreadFilterState,
            operation = if (isEnabled) MailboxViewAction.EnableUnreadFilter else MailboxViewAction.DisableUnreadFilter
        )
        mutableState.emit(
            state.value.copy(
                unreadFilterState = unreadFilterState
            )
        )
    }

    private suspend fun onCloseSelectionMode() {
        mutableState.emit(
            state.value.copy(
                topAppBarState = mailboxTopAppBarReducer.newStateFrom(
                    state.value.topAppBarState,
                    MailboxViewAction.ExitSelectionMode
                )
            )
        )
    }

    private suspend fun onOpenSelectionMode() {
        mutableState.emit(
            state.value.copy(
                topAppBarState = mailboxTopAppBarReducer.newStateFrom(
                    state.value.topAppBarState,
                    MailboxViewAction.EnterSelectionMode
                )
            )
        )
    }

    private suspend fun onOpenItemDetails(item: MailboxItemUiModel) {
        mutableState.emit(
            state.value.copy(
                mailboxListState = mailboxListReducer.newStateFrom(
                    currentState = state.value.mailboxListState,
                    operation = MailboxEvent.ItemDetailsOpenedInViewMode(item, getPreferredViewMode())
                )
            )
        )
    }

    private suspend fun onRefresh() {
        markAsStaleMailboxItems(
            userIds = listOfNotNull(primaryUserId.firstOrNull()),
            type = observeViewModeByLocation().firstOrDefault().toMailboxItemType(),
            labelId = selectedMailLabelId.flow.value.labelId
        )
    }

    private fun observePagingData(): Flow<PagingData<MailboxItemUiModel>> = combine(
        primaryUserId,
        state,
        observeViewModeByLocation()
    ) { userId, mailboxState, viewMode ->
        if (userId == null) {
            return@combine null
        }

        val unreadFilterEnabled = when (val filterState = mailboxState.unreadFilterState) {
            is UnreadFilterState.Data -> filterState.isFilterEnabled
            is UnreadFilterState.Loading -> false
        }
        val selectedMailLabelId = when (val listState = mailboxState.mailboxListState) {
            is MailboxListState.Data -> listState.currentMailLabel.id
            is MailboxListState.Loading -> MailLabelId.System.Inbox
        }

        mailboxPagerFactory.create(
            userIds = listOf(userId),
            selectedMailLabelId = selectedMailLabelId,
            filterUnread = unreadFilterEnabled,
            type = viewMode.toMailboxItemType()
        )
    }.flatMapLatest { pager ->
        val contacts = getContacts()
        pager?.flow?.map { pagingData ->
            pagingData.map { mailboxItem ->
                mailboxItemMapper.toUiModel(mailboxItem, contacts)
            }
        } ?: flowOf(PagingData.empty())
    }

    private fun observeCurrentMailLabel() = observeMailLabels()
        .map { mailLabels ->
            mailLabels.allById[selectedMailLabelId.flow.value]
        }
        .filterNotNull()

    private fun Flow<MailLabelId>.mapToExistingLabel() =
        map {
            observeMailLabels().firstOrNull()?.let { mailLabels ->
                mailLabels.allById[selectedMailLabelId.flow.value]
            }
        }.filterNotNull()

    private suspend fun updateStateForUnreadCountersChange(currentLabelCount: Int) {
        val unreadFilterState = unreadFilterReducer.newStateFrom(
            state.value.unreadFilterState,
            MailboxEvent.SelectedLabelCountChanged(currentLabelCount)
        )
        mutableState.emit(mutableState.value.copy(unreadFilterState = unreadFilterState))
    }

    private suspend fun updateStateForSelectedMailLabelChange(currentMailLabel: MailLabel, currentLabelCount: Int?) {
        val topAppBarState = mailboxTopAppBarReducer.newStateFrom(
            state.value.topAppBarState,
            MailboxEvent.NewLabelSelected(currentMailLabel, currentLabelCount)
        )
        val unreadFilterState = unreadFilterReducer.newStateFrom(
            state.value.unreadFilterState,
            MailboxEvent.NewLabelSelected(currentMailLabel, currentLabelCount)
        )
        val mailboxListState = mailboxListReducer.newStateFrom(
            state.value.mailboxListState,
            MailboxEvent.NewLabelSelected(currentMailLabel, currentLabelCount)
        )

        mutableState.emit(
            state.value.copy(
                mailboxListState = mailboxListState,
                topAppBarState = topAppBarState,
                unreadFilterState = unreadFilterState
            )
        )
    }

    private suspend fun updateStateForMailLabelChange(currentMailLabel: MailLabel) {
        val topAppBarState = mailboxTopAppBarReducer.newStateFrom(
            state.value.topAppBarState,
            MailboxEvent.SelectedLabelChanged(currentMailLabel)
        )
        val mailboxListState = mailboxListReducer.newStateFrom(
            state.value.mailboxListState,
            MailboxEvent.SelectedLabelChanged(currentMailLabel)
        )
        mutableState.emit(
            state.value.copy(
                mailboxListState = mailboxListState,
                topAppBarState = topAppBarState
            )
        )
    }

    private fun observeUnreadCounters(): Flow<List<UnreadCounter>> = primaryUserId.flatMapLatest { userId ->
        if (userId == null) {
            flowOf(emptyList())
        } else {
            observeUnreadCounters(userId)
        }
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

    private suspend fun getContacts(): List<Contact> {
        val userId = primaryUserId.firstOrNull() ?: return emptyList()

        return getContacts(userId).getOrElse {
            Timber.i("Failed getting user contacts for displaying mailbox. Fallback to using display name")
            emptyList()
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

    private fun Flow<MailLabel>.pairWithCurrentLabelCount() = map { currentLabel ->
        val currentLabelCount = observeUnreadCounters().firstOrNull()
            ?.find { it.labelId == currentLabel.id.labelId }
            ?.count
        Pair(currentLabel, currentLabelCount)
    }

    private fun Flow<List<UnreadCounter>>.mapToCurrentLabelCount() = map { unreadCounters ->
        val currentMailLabelId = selectedMailLabelId.flow.value
        unreadCounters.find { it.labelId == currentMailLabelId.labelId }?.count
    }

    companion object {

        val initialState = MailboxState(
            mailboxListState = MailboxListState.Loading,
            topAppBarState = MailboxTopAppBarState.Loading,
            unreadFilterState = UnreadFilterState.Loading
        )
    }
}
