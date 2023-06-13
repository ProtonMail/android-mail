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
import androidx.paging.Pager
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.map
import arrow.core.getOrElse
import ch.protonmail.android.mailcommon.domain.MailFeatureId
import ch.protonmail.android.mailcommon.domain.usecase.ObserveMailFeature
import ch.protonmail.android.mailcommon.domain.usecase.ObservePrimaryUserId
import ch.protonmail.android.mailcontact.domain.usecase.GetContacts
import ch.protonmail.android.maillabel.domain.SelectedMailLabelId
import ch.protonmail.android.maillabel.domain.model.MailLabel
import ch.protonmail.android.maillabel.domain.model.MailLabelId
import ch.protonmail.android.maillabel.domain.model.MailLabels
import ch.protonmail.android.maillabel.domain.usecase.ObserveMailLabels
import ch.protonmail.android.mailmailbox.domain.model.MailboxItem
import ch.protonmail.android.mailmailbox.domain.model.MailboxPageKey
import ch.protonmail.android.mailmailbox.domain.model.UnreadCounter
import ch.protonmail.android.mailmailbox.domain.model.toMailboxItemType
import ch.protonmail.android.mailmailbox.domain.usecase.ObserveCurrentViewMode
import ch.protonmail.android.mailmailbox.domain.usecase.ObserveUnreadCounters
import ch.protonmail.android.mailmailbox.presentation.mailbox.mapper.MailboxItemUiModelMapper
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxEvent
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxItemUiModel
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxListState
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxOperation
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxState
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxTopAppBarState
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxViewAction
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.UnreadFilterState
import ch.protonmail.android.mailmailbox.presentation.mailbox.reducer.MailboxReducer
import ch.protonmail.android.mailmailbox.presentation.paging.MailboxPagerFactory
import ch.protonmail.android.mailsettings.domain.usecase.ObserveFolderColorSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.proton.core.contact.domain.entity.Contact
import me.proton.core.domain.entity.UserId
import me.proton.core.mailsettings.domain.entity.ViewMode
import me.proton.core.util.kotlin.DispatcherProvider
import me.proton.core.util.kotlin.exhaustive
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class MailboxViewModel @Inject constructor(
    private val mailboxPagerFactory: MailboxPagerFactory,
    private val observeCurrentViewMode: ObserveCurrentViewMode,
    observePrimaryUserId: ObservePrimaryUserId,
    private val observeMailLabels: ObserveMailLabels,
    private val selectedMailLabelId: SelectedMailLabelId,
    private val observeUnreadCounters: ObserveUnreadCounters,
    private val observeFolderColorSettings: ObserveFolderColorSettings,
    private val mailboxItemMapper: MailboxItemUiModelMapper,
    private val getContacts: GetContacts,
    private val mailboxReducer: MailboxReducer,
    private val observeMailFeature: ObserveMailFeature,
    private val dispatchersProvider: DispatcherProvider
) : ViewModel() {

    private val primaryUserId = observePrimaryUserId()
    private val mutableState = MutableStateFlow(initialState)

    val state: StateFlow<MailboxState> = mutableState.asStateFlow()
    val items: Flow<PagingData<MailboxItemUiModel>> = observePagingData().cachedIn(viewModelScope)

    init {
        observeCurrentMailLabel()
            .onEach { currentMailLabel ->
                emitNewStateFrom(MailboxEvent.SelectedLabelChanged(currentMailLabel))
            }
            .launchIn(viewModelScope)

        selectedMailLabelId.flow
            .mapToExistingLabel()
            .pairWithCurrentLabelCount()
            .onEach { (currentMailLabel, currentLabelCount) ->
                emitNewStateFrom(MailboxEvent.NewLabelSelected(currentMailLabel, currentLabelCount))
            }
            .launchIn(viewModelScope)

        observeUnreadCounters()
            .mapToCurrentLabelCount()
            .filterNotNull()
            .onEach { currentLabelCount ->
                emitNewStateFrom(MailboxEvent.SelectedLabelCountChanged(currentLabelCount))
            }
            .launchIn(viewModelScope)

        primaryUserId
            .filterNotNull()
            .flatMapLatest { userId -> observeMailFeature(userId, MailFeatureId.HideComposer) }
            .onEach { hideComposerFlag ->
                emitNewStateFrom(MailboxEvent.ComposerDisabledChanged(composerDisabled = hideComposerFlag.value))
            }
            .launchIn(viewModelScope)
    }

    internal fun submit(viewAction: MailboxViewAction) {
        viewModelScope.launch {
            when (viewAction) {
                is MailboxViewAction.EnterSelectionMode,
                is MailboxViewAction.ExitSelectionMode,
                is MailboxViewAction.DisableUnreadFilter,
                is MailboxViewAction.EnableUnreadFilter -> emitNewStateFrom(viewAction)
                is MailboxViewAction.Refresh -> onRefresh()
                is MailboxViewAction.OpenItemDetails -> onOpenItemDetails(viewAction.item)
            }.exhaustive
        }
    }

    private suspend fun onOpenItemDetails(item: MailboxItemUiModel) {
        emitNewStateFrom(MailboxEvent.ItemDetailsOpenedInViewMode(item, getPreferredViewMode()))
    }

    private fun onRefresh() {
        
    }

    private fun observePagingData(): Flow<PagingData<MailboxItemUiModel>> =
        primaryUserId.filterNotNull().flatMapLatest { userId ->
            combine(
                state.observeMailLabelChanges(),
                state.observeUnreadFilterState(),
                observeViewModeByLocation()
            ) { selectedMailLabel, unreadFilterEnabled, viewMode ->
                mailboxPagerFactory.create(
                    userIds = listOf(userId),
                    selectedMailLabelId = selectedMailLabel.id,
                    filterUnread = unreadFilterEnabled,
                    type = viewMode.toMailboxItemType()
                )
            }.flatMapLatest { mapPagingData(userId, it) }
        }

    private suspend fun mapPagingData(
        userId: UserId,
        pager: Pager<MailboxPageKey, MailboxItem>
    ): Flow<PagingData<MailboxItemUiModel>> {
        return withContext(dispatchersProvider.Comp) {
            val contacts = getContacts()
            combine(
                pager.flow.cachedIn(viewModelScope),
                observeFolderColorSettings(userId)
            ) { pagingData, folderColorSettings ->
                pagingData.map { mailboxItem ->
                    mailboxItemMapper.toUiModel(mailboxItem, contacts, folderColorSettings)
                }
            }
        }
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
            selectedMailLabelId.flow.flatMapLatest { observeCurrentViewMode(userId, it) }.distinctUntilChanged()
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

    private fun emitNewStateFrom(operation: MailboxOperation) {
        mutableState.value = mailboxReducer.newStateFrom(state.value, operation)
    }

    private fun Flow<MailboxState>.observeUnreadFilterState() =
        this.map { it.unreadFilterState as? UnreadFilterState.Data }
            .mapNotNull { it?.isFilterEnabled }
            .distinctUntilChanged()

    private fun Flow<MailboxState>.observeMailLabelChanges() =
        this.map { it.mailboxListState as? MailboxListState.Data }
            .mapNotNull { it?.currentMailLabel }
            .distinctUntilChanged()

    companion object {

        val initialState = MailboxState(
            mailboxListState = MailboxListState.Loading,
            topAppBarState = MailboxTopAppBarState.Loading(isComposerDisabled = false),
            unreadFilterState = UnreadFilterState.Loading
        )
    }
}
