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
import ch.protonmail.android.mailcommon.domain.model.ConversationId
import ch.protonmail.android.mailcommon.domain.usecase.ObserveMailFeature
import ch.protonmail.android.mailcommon.domain.usecase.ObservePrimaryUserId
import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailcommon.presentation.mapper.ActionUiModelMapper
import ch.protonmail.android.mailcommon.presentation.model.ActionUiModel
import ch.protonmail.android.mailcommon.presentation.model.BottomBarEvent
import ch.protonmail.android.mailcommon.presentation.model.BottomBarState
import ch.protonmail.android.mailcontact.domain.usecase.GetContacts
import ch.protonmail.android.mailconversation.domain.usecase.DeleteConversations
import ch.protonmail.android.maillabel.domain.SelectedMailLabelId
import ch.protonmail.android.maillabel.domain.model.MailLabel
import ch.protonmail.android.maillabel.domain.model.MailLabelId
import ch.protonmail.android.maillabel.domain.model.MailLabels
import ch.protonmail.android.maillabel.domain.model.SystemLabelId
import ch.protonmail.android.maillabel.domain.usecase.ObserveMailLabels
import ch.protonmail.android.mailmailbox.domain.model.MailboxItem
import ch.protonmail.android.mailmailbox.domain.model.MailboxPageKey
import ch.protonmail.android.mailmailbox.domain.model.UnreadCounter
import ch.protonmail.android.mailmailbox.domain.model.toMailboxItemType
import ch.protonmail.android.mailmailbox.domain.usecase.GetMailboxActions
import ch.protonmail.android.mailmailbox.domain.usecase.MarkConversationsAsRead
import ch.protonmail.android.mailmailbox.domain.usecase.MarkConversationsAsUnread
import ch.protonmail.android.mailmailbox.domain.usecase.MarkMessagesAsRead
import ch.protonmail.android.mailmailbox.domain.usecase.MarkMessagesAsUnread
import ch.protonmail.android.mailmailbox.domain.usecase.MoveConversations
import ch.protonmail.android.mailmailbox.domain.usecase.MoveMessages
import ch.protonmail.android.mailmailbox.domain.usecase.ObserveCurrentViewMode
import ch.protonmail.android.mailmailbox.domain.usecase.ObserveOnboarding
import ch.protonmail.android.mailmailbox.domain.usecase.ObserveUnreadCounters
import ch.protonmail.android.mailmailbox.domain.usecase.SaveOnboarding
import ch.protonmail.android.mailmailbox.presentation.mailbox.mapper.MailboxItemUiModelMapper
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.DeleteDialogState
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxEvent
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxItemUiModel
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxListState
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxOperation
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxState
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxTopAppBarState
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxViewAction
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.OnboardingState
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.UnreadFilterState
import ch.protonmail.android.mailmailbox.presentation.mailbox.reducer.MailboxReducer
import ch.protonmail.android.mailmailbox.presentation.paging.MailboxPagerFactory
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailmessage.domain.usecase.DeleteMessages
import ch.protonmail.android.mailsettings.domain.usecase.ObserveFolderColorSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.toImmutableList
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
@SuppressWarnings("LongParameterList", "TooManyFunctions")
class MailboxViewModel @Inject constructor(
    private val mailboxPagerFactory: MailboxPagerFactory,
    private val observeCurrentViewMode: ObserveCurrentViewMode,
    observePrimaryUserId: ObservePrimaryUserId,
    private val observeMailLabels: ObserveMailLabels,
    private val selectedMailLabelId: SelectedMailLabelId,
    private val observeUnreadCounters: ObserveUnreadCounters,
    private val observeFolderColorSettings: ObserveFolderColorSettings,
    private val getMailboxActions: GetMailboxActions,
    private val actionUiModelMapper: ActionUiModelMapper,
    private val mailboxItemMapper: MailboxItemUiModelMapper,
    private val getContacts: GetContacts,
    private val markConversationsAsRead: MarkConversationsAsRead,
    private val markConversationsAsUnread: MarkConversationsAsUnread,
    private val markMessagesAsRead: MarkMessagesAsRead,
    private val markMessagesAsUnread: MarkMessagesAsUnread,
    private val moveConversations: MoveConversations,
    private val moveMessages: MoveMessages,
    private val deleteConversations: DeleteConversations,
    private val deleteMessages: DeleteMessages,
    private val mailboxReducer: MailboxReducer,
    private val observeMailFeature: ObserveMailFeature,
    private val dispatchersProvider: DispatcherProvider,
    private val observeOnboarding: ObserveOnboarding,
    private val saveOnboarding: SaveOnboarding
) : ViewModel() {

    private val primaryUserId = observePrimaryUserId()
    private val mutableState = MutableStateFlow(initialState)

    val state: StateFlow<MailboxState> = mutableState.asStateFlow()
    val items: Flow<PagingData<MailboxItemUiModel>> = observePagingData().cachedIn(viewModelScope)

    init {
        viewModelScope.launch {
            if (shouldDisplayOnboarding()) {
                emitNewStateFrom(MailboxEvent.ShowOnboarding)
            }
        }

        observeCurrentMailLabel()
            .onEach { currentMailLabel -> emitNewStateFrom(MailboxEvent.SelectedLabelChanged(currentMailLabel)) }
            .launchIn(viewModelScope)

        selectedMailLabelId.flow
            .mapToExistingLabel()
            .pairWithCurrentLabelCount()
            .onEach { (currentMailLabel, currentLabelCount) ->
                emitNewStateFrom(MailboxEvent.NewLabelSelected(currentMailLabel, currentLabelCount))
            }
            .launchIn(viewModelScope)

        selectedMailLabelId.flow.mapToExistingLabel()
            .combine(state.observeSelectedMailboxItems()) { selectedMailLabelId, selectedMailboxItems ->
                getMailboxActions(selectedMailLabelId, selectedMailboxItems.none { it.isRead }).fold(
                    ifLeft = { MailboxEvent.MessageBottomBarEvent(BottomBarEvent.ErrorLoadingActions) },
                    ifRight = { actions ->
                        MailboxEvent.MessageBottomBarEvent(
                            BottomBarEvent.ActionsData(
                                actions.map { action -> actionUiModelMapper.toUiModel(action) }
                                    .toImmutableList()
                            )
                        )
                    }
                )
            }
            .distinctUntilChanged()
            .onEach { emitNewStateFrom(it) }
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
            .flatMapLatest { userId -> observeMailFeature(userId, MailFeatureId.SelectionMode) }
            .onEach {
                emitNewStateFrom(MailboxEvent.SelectionModeEnabledChanged(selectionModeEnabled = it.value))
            }
            .launchIn(viewModelScope)
    }

    @SuppressWarnings("ComplexMethod")
    internal fun submit(viewAction: MailboxViewAction) {
        viewModelScope.launch {
            when (viewAction) {
                is MailboxViewAction.ExitSelectionMode,
                is MailboxViewAction.DisableUnreadFilter,
                is MailboxViewAction.EnableUnreadFilter -> emitNewStateFrom(viewAction)

                is MailboxViewAction.OnItemAvatarClicked -> handleOnAvatarClicked(viewAction.item)
                is MailboxViewAction.OnItemLongClicked -> handleItemLongClick(viewAction.item)
                is MailboxViewAction.Refresh -> emitNewStateFrom(viewAction)
                is MailboxViewAction.RefreshCompleted -> emitNewStateFrom(viewAction)
                is MailboxViewAction.ItemClicked -> handleItemClick(viewAction.item)
                is MailboxViewAction.OnOfflineWithData -> emitNewStateFrom(viewAction)
                is MailboxViewAction.OnErrorWithData -> emitNewStateFrom(viewAction)
                is MailboxViewAction.MarkAsRead -> handleMarkAsReadAction(viewAction)
                is MailboxViewAction.MarkAsUnread -> handleMarkAsUnreadAction(viewAction)
                is MailboxViewAction.CloseOnboarding -> handleCloseOnboarding()
                is MailboxViewAction.Trash -> handleTrashAction()
                is MailboxViewAction.Delete -> handleDeleteAction()
                is MailboxViewAction.DeleteConfirmed -> handleDeleteConfirmedAction()
                is MailboxViewAction.DeleteDialogDismissed -> handleDeleteDialogDismissed()
            }.exhaustive
        }
    }

    private suspend fun handleItemClick(item: MailboxItemUiModel) {
        when (state.value.mailboxListState) {
            is MailboxListState.Data.SelectionMode -> handleItemClickInSelectionMode(item)
            is MailboxListState.Data.ViewMode -> handleItemClickInViewMode(item)
            is MailboxListState.Loading -> {
                Timber.d("Loading state can't handle item clicks")
            }
        }
    }

    private suspend fun handleItemClickInViewMode(item: MailboxItemUiModel) {
        if (item.shouldOpenInComposer) {
            emitNewStateFrom(MailboxEvent.ItemClicked.OpenComposer(item))
        } else {
            emitNewStateFrom(MailboxEvent.ItemClicked.ItemDetailsOpenedInViewMode(item, getPreferredViewMode()))
        }
    }

    private fun handleItemLongClick(item: MailboxItemUiModel) {
        when (val state = state.value.mailboxListState) {
            is MailboxListState.Data.ViewMode -> enterSelectionMode(item)
            else -> {
                Timber.d("Long click not supported in state: $state")
            }
        }
    }

    private fun handleOnAvatarClicked(item: MailboxItemUiModel) {
        when (val state = state.value.mailboxListState) {
            is MailboxListState.Data.ViewMode -> enterSelectionMode(item)
            is MailboxListState.Data.SelectionMode -> handleItemClickInSelectionMode(item)
            else -> {
                Timber.d("Avatar clicked not supported in state: $state")
            }
        }
    }

    private fun handleItemClickInSelectionMode(item: MailboxItemUiModel) {
        val selectionMode = state.value.mailboxListState as? MailboxListState.Data.SelectionMode
        if (selectionMode == null) {
            Timber.d("MailboxListState is not in SelectionMode")
            return
        }

        val event = if (selectionMode.selectedMailboxItems.any { it.id == item.id }) {
            if (selectionMode.selectedMailboxItems.size == 1) {
                MailboxViewAction.ExitSelectionMode
            } else {
                MailboxEvent.ItemClicked.ItemRemovedFromSelection(item)
            }
        } else {
            MailboxEvent.ItemClicked.ItemAddedToSelection(item)
        }

        emitNewStateFrom(event)
    }

    private fun enterSelectionMode(item: MailboxItemUiModel) {
        when (val state = state.value.mailboxListState) {
            is MailboxListState.Data.ViewMode -> emitNewStateFrom(MailboxEvent.EnterSelectionMode(item))
            else -> Timber.d("Cannot enter selection mode from state: $state")
        }
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
                pagingData.map {
                    withContext(dispatchersProvider.Comp) {
                        mailboxItemMapper.toUiModel(it, contacts, folderColorSettings)
                    }
                }
            }
        }
    }

    private suspend fun handleMarkAsReadAction(markAsReadOperation: MailboxViewAction.MarkAsRead) {
        val selectionModeDataState = state.value.mailboxListState as? MailboxListState.Data.SelectionMode
        if (selectionModeDataState == null) {
            Timber.d("MailboxListState is not in SelectionMode")
            return
        }

        val selectedItemsByUserId = selectionModeDataState.selectedMailboxItems.groupBy { it.userId }

        selectedItemsByUserId.keys.forEach { userId ->
            when (getPreferredViewMode()) {
                ViewMode.ConversationGrouping -> markConversationsAsRead(
                    userId = userId,
                    conversationIds = selectedItemsByUserId[userId]?.map { ConversationId(it.id) } ?: emptyList()
                )

                ViewMode.NoConversationGrouping -> markMessagesAsRead(
                    userId = userId,
                    messageIds = selectedItemsByUserId[userId]?.map { MessageId(it.id) } ?: emptyList()
                )
            }
        }
        emitNewStateFrom(markAsReadOperation)
    }

    private suspend fun handleMarkAsUnreadAction(markAsReadOperation: MailboxViewAction.MarkAsUnread) {
        val selectionModeDataState = state.value.mailboxListState as? MailboxListState.Data.SelectionMode
        if (selectionModeDataState == null) {
            Timber.d("MailboxListState is not in SelectionMode")
            return
        }
        val selectedItemsByUserId = selectionModeDataState.selectedMailboxItems.groupBy { it.userId }

        selectedItemsByUserId.keys.forEach { userId ->
            when (getPreferredViewMode()) {
                ViewMode.ConversationGrouping -> markConversationsAsUnread(
                    userId = userId,
                    conversationIds = selectedItemsByUserId[userId]?.map { ConversationId(it.id) } ?: emptyList()
                )

                ViewMode.NoConversationGrouping -> markMessagesAsUnread(
                    userId = userId,
                    messageIds = selectedItemsByUserId[userId]?.map { MessageId(it.id) } ?: emptyList()
                )
            }
        }
        emitNewStateFrom(markAsReadOperation)
    }

    private suspend fun handleCloseOnboarding() {
        saveOnboarding(display = false)
        emitNewStateFrom(MailboxViewAction.CloseOnboarding)
    }

    private suspend fun shouldDisplayOnboarding(): Boolean {
        val showOnboardingEither = observeOnboarding().first()
        return showOnboardingEither.fold(
            ifLeft = { false },
            ifRight = { onboardingPreference -> onboardingPreference.display }
        )
    }

    private suspend fun handleTrashAction() {
        val selectionModeDataState = state.value.mailboxListState as? MailboxListState.Data.SelectionMode
        if (selectionModeDataState == null) {
            Timber.d("MailboxListState is not in SelectionMode")
            return
        }
        val selectedItemsByUserId = selectionModeDataState.selectedMailboxItems.groupBy { it.userId }
        selectedItemsByUserId.keys.forEach { userId ->
            when (getPreferredViewMode()) {
                ViewMode.ConversationGrouping -> moveConversations(
                    userId = userId,
                    conversationIds = selectedItemsByUserId[userId]?.map { ConversationId(it.id) } ?: emptyList(),
                    labelId = SystemLabelId.Trash.labelId
                )

                ViewMode.NoConversationGrouping -> moveMessages(
                    userId = userId,
                    messageIds = selectedItemsByUserId[userId]?.map { MessageId(it.id) } ?: emptyList(),
                    labelId = SystemLabelId.Trash.labelId
                )
            }
        }
        emitNewStateFrom(MailboxEvent.Trash(selectionModeDataState.selectedMailboxItems.size))
    }

    private suspend fun handleDeleteAction() {
        val selectionModeDataState = state.value.mailboxListState as? MailboxListState.Data.SelectionMode
        if (selectionModeDataState == null) {
            Timber.d("MailboxListState is not in SelectionMode")
            return
        }
        val event = MailboxEvent.Delete(
            viewMode = getPreferredViewMode(),
            numAffectedMessages = selectionModeDataState.selectedMailboxItems.size
        )
        emitNewStateFrom(event)
    }

    private suspend fun handleDeleteConfirmedAction() {
        val selectionModeDataState = state.value.mailboxListState as? MailboxListState.Data.SelectionMode
        if (selectionModeDataState == null) {
            Timber.d("MailboxListState is not in SelectionMode")
            return
        }

        val selectedItemsByUserId = selectionModeDataState.selectedMailboxItems.groupBy { it.userId }
        val viewMode = getPreferredViewMode()
        selectedItemsByUserId.keys.forEach { userId ->
            when (viewMode) {
                ViewMode.ConversationGrouping -> deleteConversations(
                    userId = userId,
                    conversationIds = selectedItemsByUserId[userId]?.map { ConversationId(it.id) } ?: emptyList(),
                    currentLabelId = selectionModeDataState.currentMailLabel.id.labelId
                )

                ViewMode.NoConversationGrouping -> deleteMessages(
                    userId = userId,
                    messageIds = selectedItemsByUserId[userId]?.map { MessageId(it.id) } ?: emptyList(),
                    currentLabelId = selectionModeDataState.currentMailLabel.id.labelId
                )
            }
        }
        emitNewStateFrom(MailboxEvent.DeleteConfirmed(viewMode, selectionModeDataState.selectedMailboxItems.size))
    }

    private fun handleDeleteDialogDismissed() {
        emitNewStateFrom(MailboxViewAction.DeleteDialogDismissed)
    }

    private fun observeCurrentMailLabel() = observeMailLabels()
        .map { mailLabels ->
            mailLabels.allById[selectedMailLabelId.flow.value]
        }
        .filterNotNull()

    private fun Flow<MailLabelId>.mapToExistingLabel() = map {
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

    private fun observeViewModeByLocation(): Flow<ViewMode> = primaryUserId.filterNotNull().flatMapLatest { userId ->
        selectedMailLabelId.flow.flatMapLatest { observeCurrentViewMode(userId, it) }.distinctUntilChanged()
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
        this.map { it.mailboxListState as? MailboxListState.Data.ViewMode }
            .mapNotNull { it?.currentMailLabel }
            .distinctUntilChanged()

    private fun Flow<MailboxState>.observeSelectedMailboxItems() =
        this.map { it.mailboxListState as? MailboxListState.Data.SelectionMode }
            .mapNotNull { it?.selectedMailboxItems }
            .distinctUntilChanged()

    companion object {

        val initialState = MailboxState(
            mailboxListState = MailboxListState.Loading(selectionModeEnabled = false),
            topAppBarState = MailboxTopAppBarState.Loading,
            unreadFilterState = UnreadFilterState.Loading,
            bottomAppBarState = BottomBarState.Data.Hidden(emptyList<ActionUiModel>().toImmutableList()),
            onboardingState = OnboardingState.Hidden,
            actionMessage = Effect.empty(),
            deleteDialogState = DeleteDialogState.Hidden
        )
    }
}
