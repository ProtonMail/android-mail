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

import java.util.Collections
import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.map
import arrow.core.getOrElse
import ch.protonmail.android.mailcommon.domain.model.ConversationId
import ch.protonmail.android.mailcommon.domain.usecase.ObservePrimaryUserId
import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailcommon.presentation.mapper.ActionUiModelMapper
import ch.protonmail.android.mailcommon.presentation.model.ActionUiModel
import ch.protonmail.android.mailcommon.presentation.model.BottomBarEvent
import ch.protonmail.android.mailcommon.presentation.model.BottomBarState
import ch.protonmail.android.mailcommon.presentation.ui.delete.DeleteDialogState
import ch.protonmail.android.mailcontact.domain.usecase.GetContacts
import ch.protonmail.android.mailconversation.domain.usecase.DeleteConversations
import ch.protonmail.android.mailconversation.domain.usecase.GetConversationsWithLabels
import ch.protonmail.android.mailconversation.domain.usecase.MarkConversationsAsRead
import ch.protonmail.android.mailconversation.domain.usecase.MarkConversationsAsUnread
import ch.protonmail.android.mailconversation.domain.usecase.MoveConversations
import ch.protonmail.android.mailconversation.domain.usecase.ObserveClearConversationOperation
import ch.protonmail.android.mailconversation.domain.usecase.StarConversations
import ch.protonmail.android.mailconversation.domain.usecase.UnStarConversations
import ch.protonmail.android.maillabel.domain.SelectedMailLabelId
import ch.protonmail.android.maillabel.domain.model.MailLabel
import ch.protonmail.android.maillabel.domain.model.MailLabelId
import ch.protonmail.android.maillabel.domain.model.MailLabels
import ch.protonmail.android.maillabel.domain.model.SystemLabelId
import ch.protonmail.android.maillabel.domain.usecase.ObserveCustomMailLabels
import ch.protonmail.android.maillabel.domain.usecase.ObserveExclusiveDestinationMailLabels
import ch.protonmail.android.maillabel.domain.usecase.ObserveMailLabels
import ch.protonmail.android.maillabel.presentation.model.LabelSelectedState
import ch.protonmail.android.maillabel.presentation.toCustomUiModel
import ch.protonmail.android.maillabel.presentation.toUiModels
import ch.protonmail.android.mailmailbox.domain.model.MailboxItem
import ch.protonmail.android.mailmailbox.domain.model.MailboxItemType
import ch.protonmail.android.mailmailbox.domain.model.MailboxPageKey
import ch.protonmail.android.mailmailbox.domain.model.toMailboxItemType
import ch.protonmail.android.mailmailbox.domain.usecase.GetMailboxActions
import ch.protonmail.android.mailmailbox.domain.usecase.GetMailboxBottomSheetActions
import ch.protonmail.android.mailmailbox.domain.usecase.ObserveCurrentViewMode
import ch.protonmail.android.mailmailbox.domain.usecase.ObservePrimaryUserAccountStorageStatus
import ch.protonmail.android.mailmailbox.domain.usecase.ObserveUnreadCounters
import ch.protonmail.android.mailmailbox.domain.usecase.RecordRatingBoosterTriggered
import ch.protonmail.android.mailmailbox.domain.usecase.RelabelConversations
import ch.protonmail.android.mailmailbox.domain.usecase.RelabelMessages
import ch.protonmail.android.mailmailbox.domain.usecase.ShouldShowRatingBooster
import ch.protonmail.android.mailmailbox.presentation.mailbox.mapper.MailboxItemUiModelMapper
import ch.protonmail.android.mailmailbox.presentation.mailbox.mapper.SwipeActionsMapper
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxEvent
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxItemUiModel
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxListState
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxOperation
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxState
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxTopAppBarState
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxViewAction
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.StorageLimitState
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.UnreadFilterState
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.UpgradeStorageState
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.UsedLabels
import ch.protonmail.android.mailmailbox.presentation.mailbox.reducer.MailboxReducer
import ch.protonmail.android.mailmailbox.presentation.mailbox.usecase.ShowRatingBooster
import ch.protonmail.android.mailmailbox.presentation.paging.MailboxPagerFactory
import ch.protonmail.android.mailmessage.domain.model.LabelSelectionList
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailmessage.domain.model.UnreadCounter
import ch.protonmail.android.mailmessage.domain.usecase.DeleteMessages
import ch.protonmail.android.mailmessage.domain.usecase.DeleteSearchResults
import ch.protonmail.android.mailmessage.domain.usecase.GetMessagesWithLabels
import ch.protonmail.android.mailmessage.domain.usecase.MarkMessagesAsRead
import ch.protonmail.android.mailmessage.domain.usecase.MarkMessagesAsUnread
import ch.protonmail.android.mailmessage.domain.usecase.MoveMessages
import ch.protonmail.android.mailmessage.domain.usecase.ObserveClearMessageOperation
import ch.protonmail.android.mailmessage.domain.usecase.StarMessages
import ch.protonmail.android.mailmessage.domain.usecase.UnStarMessages
import ch.protonmail.android.mailmessage.presentation.model.bottomsheet.LabelAsBottomSheetEntryPoint
import ch.protonmail.android.mailmessage.presentation.model.bottomsheet.LabelAsBottomSheetState
import ch.protonmail.android.mailmessage.presentation.model.bottomsheet.MailboxMoreActionsBottomSheetState
import ch.protonmail.android.mailmessage.presentation.model.bottomsheet.MoveToBottomSheetEntryPoint
import ch.protonmail.android.mailmessage.presentation.model.bottomsheet.MoveToBottomSheetState
import ch.protonmail.android.mailmessage.presentation.model.bottomsheet.UpsellingBottomSheetState
import ch.protonmail.android.mailpagination.presentation.paging.EmptyLabelId
import ch.protonmail.android.mailpagination.presentation.paging.EmptyLabelInProgressSignal
import ch.protonmail.android.mailsettings.data.usecase.UpdateAutoDeleteSpamAndTrashDays
import ch.protonmail.android.mailsettings.domain.annotations.AutodeleteFeatureEnabled
import ch.protonmail.android.mailsettings.domain.model.AutoDeleteSetting
import ch.protonmail.android.mailsettings.domain.usecase.ObserveAlmostAllMailSettings
import ch.protonmail.android.mailsettings.domain.usecase.ObserveAutoDeleteSetting
import ch.protonmail.android.mailsettings.domain.usecase.ObserveFolderColorSettings
import ch.protonmail.android.mailsettings.domain.usecase.ObserveSwipeActionsPreference
import ch.protonmail.android.mailsettings.presentation.accountsettings.autodelete.AutoDeleteSettingState
import ch.protonmail.android.mailupselling.presentation.usecase.ObserveNPSEligibility
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.proton.core.contact.domain.entity.Contact
import me.proton.core.domain.entity.UserId
import me.proton.core.label.domain.entity.LabelId
import me.proton.core.mailsettings.domain.entity.ViewMode
import me.proton.core.plan.presentation.compose.usecase.ShouldUpgradeStorage
import me.proton.core.util.kotlin.DispatcherProvider
import me.proton.core.util.kotlin.exhaustive
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
@SuppressWarnings("LongParameterList", "TooManyFunctions", "LargeClass")
class MailboxViewModel @Inject constructor(
    private val mailboxPagerFactory: MailboxPagerFactory,
    private val observeCurrentViewMode: ObserveCurrentViewMode,
    observePrimaryUserId: ObservePrimaryUserId,
    private val observeMailLabels: ObserveMailLabels,
    private val observeCustomMailLabels: ObserveCustomMailLabels,
    private val observeDestinationMailLabels: ObserveExclusiveDestinationMailLabels,
    private val observeSwipeActionsPreference: ObserveSwipeActionsPreference,
    private val observeClearMessageOperation: ObserveClearMessageOperation,
    private val observeClearConversationOperation: ObserveClearConversationOperation,
    private val selectedMailLabelId: SelectedMailLabelId,
    private val observeUnreadCounters: ObserveUnreadCounters,
    private val observeFolderColorSettings: ObserveFolderColorSettings,
    private val observeAlmostAllMailSettings: ObserveAlmostAllMailSettings,
    observeAutoDeleteSetting: ObserveAutoDeleteSetting,
    private val updateAutoDeleteSpamAndTrashDays: UpdateAutoDeleteSpamAndTrashDays,
    @AutodeleteFeatureEnabled private val isAutodeleteFeatureEnabled: Boolean,
    private val getMessagesWithLabels: GetMessagesWithLabels,
    private val getConversationsWithLabels: GetConversationsWithLabels,
    private val getMailboxActions: GetMailboxActions,
    private val getMailboxBottomSheetActions: GetMailboxBottomSheetActions,
    private val actionUiModelMapper: ActionUiModelMapper,
    private val mailboxItemMapper: MailboxItemUiModelMapper,
    private val swipeActionsMapper: SwipeActionsMapper,
    private val getContacts: GetContacts,
    private val markConversationsAsRead: MarkConversationsAsRead,
    private val markConversationsAsUnread: MarkConversationsAsUnread,
    private val markMessagesAsRead: MarkMessagesAsRead,
    private val markMessagesAsUnread: MarkMessagesAsUnread,
    private val relabelMessages: RelabelMessages,
    private val relabelConversations: RelabelConversations,
    private val moveConversations: MoveConversations,
    private val moveMessages: MoveMessages,
    private val deleteConversations: DeleteConversations,
    private val deleteMessages: DeleteMessages,
    private val starMessages: StarMessages,
    private val starConversations: StarConversations,
    private val unStarMessages: UnStarMessages,
    private val unStarConversations: UnStarConversations,
    private val mailboxReducer: MailboxReducer,
    private val dispatchersProvider: DispatcherProvider,
    private val observePrimaryUserAccountStorageStatus: ObservePrimaryUserAccountStorageStatus,
    private val shouldUpgradeStorage: ShouldUpgradeStorage,
    private val shouldShowRatingBooster: ShouldShowRatingBooster,
    private val showRatingBooster: ShowRatingBooster,
    private val recordRatingBoosterTriggered: RecordRatingBoosterTriggered,
    private val emptyLabelInProgressSignal: EmptyLabelInProgressSignal,
    private val deleteSearchResults: DeleteSearchResults,
    private val observeNPSEligibility: ObserveNPSEligibility
) : ViewModel() {

    private val primaryUserId = observePrimaryUserId()
    private val mutableState = MutableStateFlow(initialState)
    private val itemIds = Collections.synchronizedList(mutableListOf<String>())

    val state: StateFlow<MailboxState> = mutableState.asStateFlow()
    val items: Flow<PagingData<MailboxItemUiModel>> = observePagingData().cachedIn(viewModelScope)

    init {
        observeCurrentMailLabel()
            .onEach { currentMailLabel ->
                currentMailLabel?.let {
                    emitNewStateFrom(MailboxEvent.SelectedLabelChanged(currentMailLabel))
                } ?: run {
                    emitNewStateFrom(MailboxEvent.SelectedLabelChanged(MailLabelId.System.Inbox.toMailLabel()))
                }
            }
            .filterNotNull()
            .launchIn(viewModelScope)

        selectedMailLabelId.flow
            .mapToExistingLabel()
            .pairWithCurrentLabelCount()
            .onEach { (currentMailLabel, currentLabelCount) ->
                itemIds.clear()
                emitNewStateFrom(MailboxEvent.NewLabelSelected(currentMailLabel, currentLabelCount))
            }
            .combine(primaryUserId.filterNotNull()) { currentMailLabel, userId -> userId to currentMailLabel }
            .flatMapLatest {
                merge(
                    handleSwipeActionPreferences(it.first, it.second.first),
                    observeClearAllOperation(it.first, it.second.first.id)
                )
            }
            .onEach {
                emitNewStateFrom(it)
            }
            .launchIn(viewModelScope)

        selectedMailLabelId.flow.mapToExistingLabel().combine(
            observeAutoDeleteSetting()
        ) { currentMailLabel, autoDeleteSetting ->
            handleLabelSelectedForAutoDelete(currentMailLabel, autoDeleteSetting, isAutodeleteFeatureEnabled)
        }.launchIn(viewModelScope)

        observeNPSEligibility.invoke().filter { it }.onEach {
            emitNewStateFrom(MailboxEvent.ShowNPSFeedback)
        }.launchIn(viewModelScope)

        combine(
            selectedMailLabelId.flow.mapToExistingLabel(),
            state.observeSelectedMailboxItems(),
            primaryUserId.filterNotNull()
        ) { selectedMailLabelId, selectedMailboxItems, userId ->
            Triple(selectedMailLabelId, selectedMailboxItems, userId)
        }.flatMapLatest { (selectedMailLabelId, selectedMailboxItems, userId) ->
            getMailboxActions(
                selectedMailLabelId,
                areAllItemsUnread = selectedMailboxItems.none { it.isRead },
                areAllItemsStarred = selectedMailboxItems.all { it.isStarred },
                userId = userId
            ).map {
                it.fold(
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

        observePrimaryUserAccountStorageStatus().map {
            MailboxEvent.StorageLimitStatusChanged(userAccountStorageStatus = it)
        }
            .filterNotNull()
            .distinctUntilChanged()
            .onEach { emitNewStateFrom(it) }
            .launchIn(viewModelScope)

        shouldUpgradeStorage()
            .onEach {
                emitNewStateFrom(
                    MailboxEvent.UpgradeStorageStatusChanged(
                        notificationDotVisible = it != ShouldUpgradeStorage.Result.NoUpgrade
                    )
                )
            }
            .launchIn(viewModelScope)

        primaryUserId.filterNotNull().flatMapLatest { userId ->
            shouldShowRatingBooster(userId)
        }.onEach { shouldShowRatingBooster ->
            if (shouldShowRatingBooster) {
                recordRatingBoosterTriggered(primaryUserId.filterNotNull().first())
                emitNewStateFrom(MailboxEvent.ShowRatingBooster)
            }
        }.launchIn(viewModelScope)
    }

    private fun handleLabelSelectedForAutoDelete(
        currentMailLabel: MailLabel,
        autoDeleteSetting: AutoDeleteSetting,
        featureEnabled: Boolean
    ) {
        val operation = MailboxEvent.AutoDeleteStateChanged(
            featureEnabled,
            currentMailLabel.id,
            autoDeleteSetting
        )
        emitNewStateFrom(operation)
    }

    private fun handleSwipeActionPreferences(userId: UserId, currentMailLabel: MailLabel): Flow<MailboxEvent> {
        return observeSwipeActionsPreference(userId)
            .map { swipeActionsPreference ->
                val swipeActions = swipeActionsMapper(currentMailLabel.id.labelId, swipeActionsPreference)
                MailboxEvent.SwipeActionsChanged(swipeActions)
            }
            .distinctUntilChanged()
    }

    private fun observeClearAllOperation(userId: UserId, currentMailLabel: MailLabelId): Flow<MailboxEvent> {
        return combine(
            observeClearMessageOperation(userId, currentMailLabel.labelId),
            observeClearConversationOperation(userId, currentMailLabel.labelId)
        ) { messageOperation, conversationOperation ->
            val clearAllOperation = messageOperation || conversationOperation
            MailboxEvent.ClearAllOperationStatus(clearAllOperation)
        }.distinctUntilChanged()
    }

    @SuppressWarnings("ComplexMethod", "LongMethod")
    internal fun submit(viewAction: MailboxViewAction) {
        viewModelScope.launch {
            when (viewAction) {
                is MailboxViewAction.StorageLimitConfirmed -> emitNewStateFrom(viewAction)

                is MailboxViewAction.ExitSelectionMode,
                is MailboxViewAction.DisableUnreadFilter,
                is MailboxViewAction.EnableUnreadFilter -> emitNewStateFrom(viewAction)

                is MailboxViewAction.MailboxItemsChanged -> handleMailboxItemChanged(viewAction.itemIds)
                is MailboxViewAction.OnItemAvatarClicked -> handleOnAvatarClicked(viewAction.item)
                is MailboxViewAction.OnItemLongClicked -> handleItemLongClick(viewAction.item)
                is MailboxViewAction.Refresh -> emitNewStateFrom(viewAction)
                is MailboxViewAction.RefreshCompleted -> emitNewStateFrom(viewAction)
                is MailboxViewAction.ItemClicked -> handleItemClick(viewAction.item)
                is MailboxViewAction.OnOfflineWithData -> emitNewStateFrom(viewAction)
                is MailboxViewAction.OnErrorWithData -> emitNewStateFrom(viewAction)
                is MailboxViewAction.MarkAsRead -> handleMarkAsReadAction(viewAction)
                is MailboxViewAction.MarkAsUnread -> handleMarkAsUnreadAction(viewAction)
                is MailboxViewAction.SwipeReadAction -> handleSwipeReadAction(viewAction)
                is MailboxViewAction.SwipeArchiveAction -> handleSwipeArchiveAction(viewAction)
                is MailboxViewAction.SwipeSpamAction -> handleSwipeSpamAction(viewAction)
                is MailboxViewAction.SwipeTrashAction -> handleSwipeTrashAction(viewAction)
                is MailboxViewAction.SwipeStarAction -> handleSwipeStarAction(viewAction)
                is MailboxViewAction.SwipeLabelAsAction -> showLabelAsBottomSheetAndLoadData(viewAction)
                is MailboxViewAction.SwipeMoveToAction -> showMoveToBottomSheetAndLoadData(viewAction)
                is MailboxViewAction.Trash -> handleTrashAction()
                is MailboxViewAction.Delete -> handleDeleteAction()
                is MailboxViewAction.DeleteConfirmed -> handleDeleteConfirmedAction()
                is MailboxViewAction.DeleteDialogDismissed -> handleDeleteDialogDismissed()
                is MailboxViewAction.RequestLabelAsBottomSheet -> showLabelAsBottomSheetAndLoadData(viewAction)
                is MailboxViewAction.LabelAsToggleAction -> emitNewStateFrom(viewAction)
                is MailboxViewAction.LabelAsConfirmed ->
                    onLabelAsConfirmed(viewAction.archiveSelected, viewAction.entryPoint)
                is MailboxViewAction.RequestMoveToBottomSheet -> showMoveToBottomSheetAndLoadData(viewAction)
                is MailboxViewAction.MoveToDestinationSelected -> emitNewStateFrom(viewAction)
                is MailboxViewAction.MoveToConfirmed -> onMoveToConfirmed(viewAction.entryPoint)
                is MailboxViewAction.RequestMoreActionsBottomSheet -> showMoreBottomSheet(viewAction)
                is MailboxViewAction.DismissBottomSheet -> emitNewStateFrom(viewAction)
                is MailboxViewAction.Star -> handleStarAction(viewAction)
                is MailboxViewAction.UnStar -> handleUnStarAction(viewAction)
                is MailboxViewAction.MoveToArchive,
                is MailboxViewAction.MoveToSpam -> handleMoveToAction(viewAction)

                is MailboxViewAction.EnterSearchMode -> emitNewStateFrom(viewAction)
                is MailboxViewAction.ExitSearchMode -> handleExitSearchMode(viewAction)
                is MailboxViewAction.SearchQuery -> emitNewStateFrom(viewAction)
                is MailboxViewAction.SearchResult -> handleSearchResult(viewAction)
                is MailboxViewAction.DeleteAll -> handleClearAllAction()
                is MailboxViewAction.DeleteAllConfirmed -> handleClearAllConfirmedAction()
                is MailboxViewAction.DeleteAllDialogDismissed -> handleClearAllDialogDismissed(viewAction)
                is MailboxViewAction.RequestUpsellingBottomSheet -> showUpsellingBottomSheet(viewAction)
                is MailboxViewAction.NavigateToInboxLabel -> selectedMailLabelId.set(MailLabelId.System.Inbox)
                is MailboxViewAction.ShowRatingBooster -> showRatingBooster(viewAction)
                is MailboxViewAction.AutoDeleteDialogActionSubmitted -> {
                    handleAutoDeleteDialogActionSubmitted(viewAction)
                }

                is MailboxViewAction.DismissAutoDelete -> handleDismissAutoDelete(viewAction)
                is MailboxViewAction.ShowAutoDeleteDialog -> emitNewStateFrom(viewAction)
                MailboxViewAction.IncludeAllClicked -> emitNewStateFrom(viewAction)
                is MailboxViewAction.SearchResultsReady -> emitNewStateFrom(viewAction)
            }.exhaustive
        }
    }

    private fun handleAutoDeleteDialogActionSubmitted(viewAction: MailboxViewAction.AutoDeleteDialogActionSubmitted) {
        viewModelScope.launch {
            if (viewAction.enable) {
                primaryUserId.filterNotNull().firstOrNull()?.let {
                    updateAutoDeleteSpamAndTrashDays(it, true)
                }
            }
        }
        emitNewStateFrom(viewAction)
    }

    private fun handleDismissAutoDelete(viewAction: MailboxViewAction.DismissAutoDelete) {
        viewModelScope.launch {
            primaryUserId.filterNotNull().firstOrNull()?.let {
                updateAutoDeleteSpamAndTrashDays(it, false)
            }
        }
        emitNewStateFrom(viewAction)
    }

    private suspend fun handleSearchResult(viewAction: MailboxViewAction.SearchResult) {
        emitNewStateFrom(viewAction)
        val almostAllSetting = observeAlmostAllMailSettings(primaryUserId.filterNotNull().first())
            .first()
        emitNewStateFrom(MailboxViewAction.SearchResultsReady(almostAllSetting))
    }

    private suspend fun handleExitSearchMode(viewAction: MailboxViewAction) {
        val searchedAllMail = (state.value.mailboxListState as? MailboxListState.Data)
            ?.searchState
            ?.isSearchingAllMail
            ?: false
        if (searchedAllMail) {
            val user = primaryUserId.filterNotNull().first()
            deleteSearchResults(user)
        }

        emitNewStateFrom(viewAction)
    }

    private suspend fun handleMailboxItemChanged(updatedItemIds: List<String>) {
        withContext(dispatchersProvider.Comp) {
            val removedItems = itemIds.filterNot { updatedItemIds.contains(it) }
            itemIds.clear()
            itemIds.addAll(updatedItemIds)
            Timber.d("Removed items: $removedItems")
            if (removedItems.isNotEmpty()) {
                when (val currentState = state.value.mailboxListState) {
                    is MailboxListState.Data.SelectionMode -> {
                        currentState.selectedMailboxItems
                            .map { it.id }
                            .filter { currentSelectedItem -> removedItems.contains(currentSelectedItem) }
                            .takeIf { it.isNotEmpty() }
                            ?.let { emitNewStateFrom(MailboxEvent.ItemsRemovedFromSelection(it)) }
                    }

                    else -> {}
                }
            }
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
                observeViewModeByLocation(),
                state.observeSearchState(),
                observeAlmostAllMailSettings.invoke(userId)
            ) { selectedMailLabel, unreadFilterEnabled, viewMode, searchState, almostAllMailSetting ->
                val query = searchState.searchQuery
                mailboxPagerFactory.create(
                    userIds = listOf(userId),
                    selectedMailLabelId = when {
                        query.isEmpty() -> selectedMailLabel.id
                        searchState.isSearchingAllMail.not() && almostAllMailSetting -> MailLabelId.System.AlmostAllMail
                        else -> MailLabelId.System.AllMail
                    },
                    filterUnread = unreadFilterEnabled,
                    type = if (query.isEmpty()) viewMode.toMailboxItemType() else MailboxItemType.Message,
                    searchQuery = query,
                    emptyLabelInProgressSignal = emptyLabelInProgressSignal
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
                        mailboxItemMapper.toUiModel(it, contacts, folderColorSettings, state.value.isInSearchMode())
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

        val user = primaryUserId.filterNotNull().first()
        val viewMode = getViewModeForCurrentLocation(selectedMailLabelId.flow.value)
        when (viewMode) {
            ViewMode.ConversationGrouping -> markConversationsAsRead(
                userId = user,
                conversationIds = selectionModeDataState.selectedMailboxItems.map { ConversationId(it.id) }
            )

            ViewMode.NoConversationGrouping -> markMessagesAsRead(
                userId = user,
                messageIds = selectionModeDataState.selectedMailboxItems.map { MessageId(it.id) }
            )
        }
        emitNewStateFrom(markAsReadOperation)
    }

    private suspend fun handleMarkAsUnreadAction(markAsReadOperation: MailboxViewAction.MarkAsUnread) {
        val selectionModeDataState = state.value.mailboxListState as? MailboxListState.Data.SelectionMode
        if (selectionModeDataState == null) {
            Timber.d("MailboxListState is not in SelectionMode")
            return
        }
        val userId = primaryUserId.filterNotNull().first()
        val viewMode = getViewModeForCurrentLocation(selectedMailLabelId.flow.value)
        when (viewMode) {
            ViewMode.ConversationGrouping -> markConversationsAsUnread(
                userId = userId,
                conversationIds = selectionModeDataState.selectedMailboxItems.map { ConversationId(it.id) }
            )

            ViewMode.NoConversationGrouping -> markMessagesAsUnread(
                userId = userId,
                messageIds = selectionModeDataState.selectedMailboxItems.map { MessageId(it.id) }
            )
        }
        emitNewStateFrom(markAsReadOperation)
    }

    private suspend fun handleSwipeReadAction(swipeReadAction: MailboxViewAction.SwipeReadAction) {
        if (swipeReadAction.isRead) {
            when (getViewModeForCurrentLocation(selectedMailLabelId.flow.value)) {
                ViewMode.ConversationGrouping -> markConversationsAsUnread(
                    userId = swipeReadAction.userId,
                    conversationIds = listOf(ConversationId(swipeReadAction.itemId))
                )

                ViewMode.NoConversationGrouping -> markMessagesAsUnread(
                    userId = swipeReadAction.userId,
                    messageIds = listOf(MessageId(swipeReadAction.itemId))
                )
            }
        } else {
            when (getViewModeForCurrentLocation(selectedMailLabelId.flow.value)) {
                ViewMode.ConversationGrouping -> markConversationsAsRead(
                    userId = swipeReadAction.userId,
                    conversationIds = listOf(ConversationId(swipeReadAction.itemId))
                )

                ViewMode.NoConversationGrouping -> markMessagesAsRead(
                    userId = swipeReadAction.userId,
                    messageIds = listOf(MessageId(swipeReadAction.itemId))
                )
            }
        }
        emitNewStateFrom(swipeReadAction)
    }

    private suspend fun handleSwipeStarAction(swipeStarAction: MailboxViewAction.SwipeStarAction) {
        if (swipeStarAction.isStarred) {
            when (getViewModeForCurrentLocation(selectedMailLabelId.flow.value)) {
                ViewMode.ConversationGrouping -> unStarConversations(
                    userId = swipeStarAction.userId,
                    conversationIds = listOf(ConversationId(swipeStarAction.itemId))
                )

                ViewMode.NoConversationGrouping -> unStarMessages(
                    userId = swipeStarAction.userId,
                    messageIds = listOf(MessageId(swipeStarAction.itemId))
                )
            }
        } else {
            when (getViewModeForCurrentLocation(selectedMailLabelId.flow.value)) {
                ViewMode.ConversationGrouping -> starConversations(
                    userId = swipeStarAction.userId,
                    conversationIds = listOf(ConversationId(swipeStarAction.itemId))
                )

                ViewMode.NoConversationGrouping -> starMessages(
                    userId = swipeStarAction.userId,
                    messageIds = listOf(MessageId(swipeStarAction.itemId))
                )
            }
        }
        emitNewStateFrom(swipeStarAction)
    }

    private suspend fun handleSwipeArchiveAction(swipeArchiveAction: MailboxViewAction.SwipeArchiveAction) {
        if (isActionAllowedForCurrentLabel(SystemLabelId.Archive.labelId)) {
            val viewMode = getViewModeForCurrentLocation(selectedMailLabelId.flow.value)
            swipeArchiveAction.let {
                moveSingleItemToDestination(it.userId, it.itemId, SystemLabelId.Archive.labelId, viewMode)
            }
            emitNewStateFrom(MailboxEvent.SwipeActionMoveCompleted.Archive(viewMode))
        }
    }

    private suspend fun handleSwipeSpamAction(swipeSpamAction: MailboxViewAction.SwipeSpamAction) {
        if (isActionAllowedForCurrentLabel(SystemLabelId.Spam.labelId)) {
            val viewMode = getViewModeForCurrentLocation(selectedMailLabelId.flow.value)
            swipeSpamAction.let {
                moveSingleItemToDestination(it.userId, it.itemId, SystemLabelId.Spam.labelId, viewMode)
            }
            emitNewStateFrom(MailboxEvent.SwipeActionMoveCompleted.Spam(viewMode))
        }
    }

    private suspend fun handleSwipeTrashAction(swipeTrashAction: MailboxViewAction.SwipeTrashAction) {
        if (isActionAllowedForCurrentLabel(SystemLabelId.Trash.labelId)) {
            val viewMode = getViewModeForCurrentLocation(selectedMailLabelId.flow.value)
            swipeTrashAction.let {
                moveSingleItemToDestination(it.userId, it.itemId, SystemLabelId.Trash.labelId, viewMode)
            }
            emitNewStateFrom(MailboxEvent.SwipeActionMoveCompleted.Trash(viewMode))
        }
    }

    private suspend fun moveSingleItemToDestination(
        userId: UserId,
        itemId: String,
        labelId: LabelId,
        viewMode: ViewMode
    ) {
        when (viewMode) {
            ViewMode.ConversationGrouping -> moveConversations(
                userId = userId,
                conversationIds = listOf(ConversationId(itemId)),
                labelId = labelId
            )

            ViewMode.NoConversationGrouping -> moveMessages(
                userId = userId,
                messageIds = listOf(MessageId(itemId)),
                labelId = labelId
            )
        }
    }

    private fun showLabelAsBottomSheetAndLoadData(operation: MailboxViewAction) {
        val items: Set<String> = when (operation) {
            is MailboxViewAction.RequestLabelAsBottomSheet -> {
                val selectionMode = state.value.mailboxListState as? MailboxListState.Data.SelectionMode
                if (selectionMode == null) {
                    Timber.d("MailboxListState is not in SelectionMode")
                    return
                }
                selectionMode.selectedMailboxItems.map { it.id }.toSet()
            }

            is MailboxViewAction.SwipeLabelAsAction -> {
                setOf(operation.itemId)
            }

            else -> return
        }

        val entryPoint = when (operation) {
            is MailboxViewAction.RequestLabelAsBottomSheet -> LabelAsBottomSheetEntryPoint.SelectionMode
            is MailboxViewAction.SwipeLabelAsAction -> LabelAsBottomSheetEntryPoint.LabelAsSwipeAction(operation.itemId)
            else -> {
                Timber.e("Unsupported operation: $operation")
                return
            }
        }

        viewModelScope.launch {
            emitNewStateFrom(operation)

            val userId = primaryUserId.filterNotNull().first()
            val labels = observeCustomMailLabels(userId).firstOrNull()
            val color = observeFolderColorSettings(userId).firstOrNull()

            if (labels == null) {
                emitNewStateFrom(MailboxEvent.ErrorRetrievingCustomMailLabels)
                return@launch
            }

            if (color == null) {
                emitNewStateFrom(MailboxEvent.ErrorRetrievingFolderColorSettings)
                return@launch
            }

            val mappedLabels = labels.onLeft {
                Timber.e("Error while observing custom labels")
            }.getOrElse { emptyList() }

            val usedLabels = getUsedLabels(userId, items)

            val (selectedLabels, partiallySelectedLabels) = mappedLabels.getLabelSelectionState(usedLabels)
            val event = MailboxEvent.MailboxBottomSheetEvent(
                LabelAsBottomSheetState.LabelAsBottomSheetEvent.ActionData(
                    customLabelList = mappedLabels.map { it.toCustomUiModel(color, emptyMap(), null) }
                        .toImmutableList(),
                    selectedLabels = selectedLabels.toImmutableList(),
                    partiallySelectedLabels = partiallySelectedLabels.toImmutableList(),
                    entryPoint = entryPoint
                )
            )
            emitNewStateFrom(event)
        }
    }

    private fun onLabelAsConfirmed(archiveSelected: Boolean, entryPoint: LabelAsBottomSheetEntryPoint) {
        val items: Set<String> = when (entryPoint) {
            is LabelAsBottomSheetEntryPoint.SelectionMode -> {
                val selectionMode = state.value.mailboxListState as? MailboxListState.Data.SelectionMode
                if (selectionMode == null) {
                    Timber.d("MailboxListState is not in SelectionMode")
                    return
                }
                selectionMode.selectedMailboxItems.map { it.id }.toSet()
            }
            is LabelAsBottomSheetEntryPoint.LabelAsSwipeAction -> {
                setOf(entryPoint.itemId)
            }
            else -> return
        }
        viewModelScope.launch {
            val userId = primaryUserId.filterNotNull().first()
            val labels = observeCustomMailLabels(userId).firstOrNull()?.onLeft {
                Timber.e("Error while observing custom labels when relabeling got confirmed: $it")
            }?.getOrElse { emptyList() }
            if (labels == null) {
                emitNewStateFrom(MailboxEvent.ErrorRetrievingCustomMailLabels)
                return@launch
            }

            val usedLabels = getUsedLabels(userId, items)

            val previousSelection = labels.getLabelSelectionState(usedLabels)
            val labelAsData = state.value.bottomSheetState?.contentState as? LabelAsBottomSheetState.Data
                ?: throw IllegalStateException("BottomSheetState is not LabelAsBottomSheetState.Data")

            val updatedSelection = labelAsData.getLabelSelectionState()
            val viewMode = getViewModeForCurrentLocation(selectedMailLabelId.flow.value)
            if (archiveSelected) {
                when (viewMode) {
                    ViewMode.ConversationGrouping ->
                        moveConversations(
                            userId,
                            items.map { ConversationId(it) },
                            SystemLabelId.Archive.labelId
                        )

                    ViewMode.NoConversationGrouping ->
                        moveMessages(
                            userId,
                            items.map { MessageId(it) },
                            SystemLabelId.Archive.labelId
                        )
                }
            }
            val operation = handleRelabelOperation(
                userId = userId,
                viewMode = viewMode,
                selectedItems = items,
                currentSelectionList = previousSelection,
                updatedSelectionList = updatedSelection,
                archiveSelected = archiveSelected,
                entryPoint = entryPoint
            )
            emitNewStateFrom(operation)
        }
    }

    private fun showMoveToBottomSheetAndLoadData(operation: MailboxViewAction) {
        val entryPoint = when (operation) {
            is MailboxViewAction.RequestMoveToBottomSheet -> {
                val selectionMode = state.value.mailboxListState as? MailboxListState.Data.SelectionMode
                if (selectionMode == null) {
                    Timber.d("MailboxListState is not in SelectionMode")
                    return
                }
                MoveToBottomSheetEntryPoint.SelectionMode
            }

            is MailboxViewAction.SwipeMoveToAction ->
                MoveToBottomSheetEntryPoint.MoveToSwipeAction(operation.itemId)

            else -> {
                Timber.d("Unsupported operation: $operation")
                return
            }
        }

        viewModelScope.launch {
            emitNewStateFrom(operation)

            val userId = primaryUserId.filterNotNull().first()
            val destinationFolder = observeDestinationMailLabels(userId).firstOrNull()
            val color = observeFolderColorSettings(userId).firstOrNull()

            if (destinationFolder == null) {
                emitNewStateFrom(MailboxEvent.ErrorRetrievingDestinationMailFolders)
                return@launch
            }

            if (color == null) {
                emitNewStateFrom(MailboxEvent.ErrorRetrievingFolderColorSettings)
                return@launch
            }

            val event = MailboxEvent.MailboxBottomSheetEvent(
                MoveToBottomSheetState.MoveToBottomSheetEvent.ActionData(
                    destinationFolder.toUiModels(color).let { it.folders + it.systems }.toImmutableList(),
                    entryPoint
                )
            )
            emitNewStateFrom(event)
        }
    }

    private suspend fun onMoveToConfirmed(entryPoint: MoveToBottomSheetEntryPoint) {
        val items = getMoveToItems(entryPoint) ?: return
        val bottomSheetState = mutableState.value.bottomSheetState?.contentState
        if (bottomSheetState !is MoveToBottomSheetState.Data) {
            Timber.d("BottomSheetState is not MoveToBottomSheetState.Data")
            return
        }
        val userId = primaryUserId.filterNotNull().first()
        val selectedFolder = bottomSheetState.selected
        if (selectedFolder == null) {
            Timber.d("Selected folder is null")
            return
        }
        handleMoveOperation(userId, items, selectedFolder.id.labelId, entryPoint)
    }

    private fun getMoveToItems(entryPoint: MoveToBottomSheetEntryPoint): Set<String>? = when (entryPoint) {
        is MoveToBottomSheetEntryPoint.SelectionMode -> {
            val selectionMode = state.value.mailboxListState as? MailboxListState.Data.SelectionMode
            if (selectionMode == null) {
                Timber.d("MailboxListState is not in SelectionMode")
            }
            selectionMode?.selectedMailboxItems?.map { it.id }?.toSet()
        }

        is MoveToBottomSheetEntryPoint.MoveToSwipeAction -> {
            setOf(entryPoint.itemId)
        }

        else -> {
            Timber.d("Unsupported entry point: $entryPoint")
            null
        }
    }

    private suspend fun handleMoveToAction(viewAction: MailboxViewAction) {
        val selectionState = state.value.mailboxListState as? MailboxListState.Data.SelectionMode
        if (selectionState == null) {
            Timber.d("MailboxListState is not in SelectionMode")
            return
        }
        val userId = primaryUserId.filterNotNull().first()
        val targetLabel = when (viewAction) {
            is MailboxViewAction.MoveToArchive -> SystemLabelId.Archive.labelId
            is MailboxViewAction.MoveToSpam -> SystemLabelId.Spam.labelId
            else -> {
                Timber.e("Unsupported action: $viewAction")
                return
            }
        }
        handleMoveOperation(
            userId,
            selectionState.selectedMailboxItems.map { it.id }.toSet(),
            targetLabel,
            MoveToBottomSheetEntryPoint.SelectionMode
        )
    }

    private suspend fun handleMoveOperation(
        userId: UserId,
        items: Set<String>,
        targetLabelId: LabelId,
        entryPoint: MoveToBottomSheetEntryPoint
    ) {
        val viewMode = getViewModeForCurrentLocation(selectedMailLabelId.flow.value)
        when (viewMode) {
            ViewMode.ConversationGrouping -> moveConversations(
                userId = userId,
                conversationIds = items.map { ConversationId(it) },
                labelId = targetLabelId
            )

            ViewMode.NoConversationGrouping -> moveMessages(
                userId = userId,
                messageIds = items.map { MessageId(it) },
                labelId = targetLabelId
            )
        }.fold(
            ifLeft = { MailboxEvent.ErrorMoving },
            ifRight = { MailboxViewAction.MoveToConfirmed(entryPoint) }
        ).let { emitNewStateFrom(it) }
    }

    private fun showMoreBottomSheet(operation: MailboxViewAction) {
        val selectionState = state.value.mailboxListState as? MailboxListState.Data.SelectionMode
        if (selectionState == null) {
            Timber.d("MailboxListState is not in SelectionMode")
            return
        }
        emitNewStateFrom(operation)

        val currentMailLabel = selectedMailLabelId.flow.value.labelId
        emitNewStateFrom(
            MailboxEvent.MailboxBottomSheetEvent(
                MailboxMoreActionsBottomSheetState.MailboxMoreActionsBottomSheetEvent.ActionData(
                    getMailboxBottomSheetActions(currentMailLabel)
                        .map { actionUiModelMapper.toUiModel(it) }
                        .toImmutableList()
                )
            )
        )
    }

    private suspend fun getUsedLabels(userId: UserId, selectedItems: Set<String>): List<UsedLabels> {
        return when (getViewModeForCurrentLocation(selectedMailLabelId.flow.value)) {
            ViewMode.ConversationGrouping ->
                getConversationsWithLabels(userId, selectedItems.map { ConversationId(it) }).map { list ->
                    list.map { UsedLabels(it.labels) }
                }

            ViewMode.NoConversationGrouping ->
                getMessagesWithLabels(userId, selectedItems.map { MessageId(it) }).map { list ->
                    list.map { UsedLabels(it.labels) }
                }
        }.onLeft { Timber.e("Error while observing messages with labels") }.getOrElse { emptyList() }
    }

    private suspend fun handleRelabelOperation(
        userId: UserId,
        viewMode: ViewMode,
        selectedItems: Set<String>,
        currentSelectionList: LabelSelectionList,
        updatedSelectionList: LabelSelectionList,
        archiveSelected: Boolean,
        entryPoint: LabelAsBottomSheetEntryPoint
    ): MailboxOperation {
        return when (viewMode) {
            ViewMode.ConversationGrouping -> relabelConversations(
                userId = userId,
                conversationIds = selectedItems.map { ConversationId(it) },
                currentSelections = currentSelectionList,
                updatedSelections = updatedSelectionList
            )

            ViewMode.NoConversationGrouping -> relabelMessages(
                userId = userId,
                messageIds = selectedItems.map { MessageId(it) },
                currentSelections = currentSelectionList,
                updatedSelections = updatedSelectionList
            )
        }.fold(
            ifLeft = { MailboxEvent.ErrorLabeling },
            ifRight = { MailboxViewAction.LabelAsConfirmed(archiveSelected, entryPoint) }
        )
    }

    private suspend fun handleTrashAction() {
        val selectionModeDataState = state.value.mailboxListState as? MailboxListState.Data.SelectionMode
        if (selectionModeDataState == null) {
            Timber.d("MailboxListState is not in SelectionMode")
            return
        }
        val userId = primaryUserId.filterNotNull().first()
        val viewMode = getViewModeForCurrentLocation(selectedMailLabelId.flow.value)
        when (viewMode) {
            ViewMode.ConversationGrouping -> moveConversations(
                userId = userId,
                conversationIds = selectionModeDataState.selectedMailboxItems.map { ConversationId(it.id) },
                labelId = SystemLabelId.Trash.labelId
            )

            ViewMode.NoConversationGrouping -> moveMessages(
                userId = userId,
                messageIds = selectionModeDataState.selectedMailboxItems.map { MessageId(it.id) },
                labelId = SystemLabelId.Trash.labelId
            )
        }
        emitNewStateFrom(MailboxEvent.Trash(viewMode, selectionModeDataState.selectedMailboxItems.size))
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

        val userId = primaryUserId.filterNotNull().first()
        val viewMode = getViewModeForCurrentLocation(selectedMailLabelId.flow.value)
        when (viewMode) {
            ViewMode.ConversationGrouping -> {
                deleteConversations(
                    userId = userId,
                    conversationIds = selectionModeDataState.selectedMailboxItems.map { ConversationId(it.id) },
                    currentLabelId = selectionModeDataState.currentMailLabel.id.labelId
                )
            }

            ViewMode.NoConversationGrouping -> {
                deleteMessages(
                    userId = userId,
                    messageIds = selectionModeDataState.selectedMailboxItems.map { MessageId(it.id) },
                    currentLabelId = selectionModeDataState.currentMailLabel.id.labelId
                )
            }
        }
        emitNewStateFrom(MailboxEvent.DeleteConfirmed(viewMode, selectionModeDataState.selectedMailboxItems.size))
    }

    private fun handleClearAllDialogDismissed(viewAction: MailboxViewAction) {
        emitNewStateFrom(viewAction)
    }

    private suspend fun handleClearAllAction() {
        val currentMailLabel = selectedMailLabelId.flow.value.labelId
        if (currentMailLabel != SystemLabelId.Trash.labelId && currentMailLabel != SystemLabelId.Spam.labelId) {
            Timber.e("Clear all action is only supported for Trash and Spam")
            return
        }
        emitNewStateFrom(
            MailboxEvent.DeleteAll(getViewModeForCurrentLocation(selectedMailLabelId.flow.value), currentMailLabel)
        )
    }

    private suspend fun handleClearAllConfirmedAction() {
        val currentMailLabel = selectedMailLabelId.flow.value.labelId
        if (currentMailLabel != SystemLabelId.Trash.labelId && currentMailLabel != SystemLabelId.Spam.labelId) {
            Timber.e("Clear all action is only supported for Trash and Spam")
            emitNewStateFrom(MailboxViewAction.DeleteAllDialogDismissed)
            return
        }
        val userId = primaryUserId.filterNotNull().first()
        val viewMode = getViewModeForCurrentLocation(selectedMailLabelId.flow.value)

        val emptyLabelId = EmptyLabelId(currentMailLabel.id)
        emptyLabelInProgressSignal.emitOperationSignal(emptyLabelId)
        emitNewStateFrom(MailboxEvent.DeleteAllConfirmed(viewMode))

        when (viewMode) {
            ViewMode.ConversationGrouping -> deleteConversations(userId, currentMailLabel)
            ViewMode.NoConversationGrouping -> deleteMessages(userId, currentMailLabel)
        }
    }

    private suspend fun handleStarAction(viewAction: MailboxViewAction) {
        val selectionModeDataState = state.value.mailboxListState as? MailboxListState.Data.SelectionMode
        if (selectionModeDataState == null) {
            Timber.d("MailboxListState is not in SelectionMode")
            return
        }
        val userId = primaryUserId.filterNotNull().first()
        val viewMode = getViewModeForCurrentLocation(selectedMailLabelId.flow.value)
        when (viewMode) {
            ViewMode.ConversationGrouping -> {
                starConversations(userId, selectionModeDataState.selectedMailboxItems.map { ConversationId(it.id) })
            }

            ViewMode.NoConversationGrouping -> {
                starMessages(userId, selectionModeDataState.selectedMailboxItems.map { MessageId(it.id) })
            }
        }

        emitNewStateFrom(viewAction)
    }

    private suspend fun handleUnStarAction(viewAction: MailboxViewAction) {
        val selectionModeDataState = state.value.mailboxListState as? MailboxListState.Data.SelectionMode
        if (selectionModeDataState == null) {
            Timber.d("MailboxListState is not in SelectionMode")
            return
        }
        val userId = primaryUserId.filterNotNull().first()
        val viewMode = getViewModeForCurrentLocation(selectedMailLabelId.flow.value)
        when (viewMode) {
            ViewMode.ConversationGrouping -> {
                unStarConversations(userId, selectionModeDataState.selectedMailboxItems.map { ConversationId(it.id) })
            }

            ViewMode.NoConversationGrouping -> {
                unStarMessages(userId, selectionModeDataState.selectedMailboxItems.map { MessageId(it.id) })
            }
        }
        emitNewStateFrom(viewAction)
    }

    private fun handleDeleteDialogDismissed() {
        emitNewStateFrom(MailboxViewAction.DeleteDialogDismissed)
    }

    private fun showUpsellingBottomSheet(operation: MailboxViewAction.RequestUpsellingBottomSheet) {
        viewModelScope.launch {
            emitNewStateFrom(operation)

            emitNewStateFrom(
                MailboxEvent.MailboxBottomSheetEvent(
                    UpsellingBottomSheetState.UpsellingBottomSheetEvent.Ready(operation.entryPoint)
                )
            )
        }
    }

    private fun observeCurrentMailLabel() = observeMailLabels()
        .map { mailLabels ->
            mailLabels.allById[selectedMailLabelId.flow.value]
        }

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
            observeMailLabels(userId, respectSettings = true)
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

    private suspend fun getViewModeForCurrentLocation(currentMailLabel: MailLabelId): ViewMode {
        val userId = primaryUserId.firstOrNull()

        return if (userId == null || state.value.isInSearchMode()) {
            ObserveCurrentViewMode.DefaultViewMode
        } else {
            observeCurrentViewMode(userId, currentMailLabel).first()
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
        val state = mailboxReducer.newStateFrom(state.value, operation)
        mutableState.value = state
    }

    private fun Flow<MailboxState>.observeUnreadFilterState() =
        this.map { it.unreadFilterState as? UnreadFilterState.Data }
            .mapNotNull { it?.isFilterEnabled }
            .distinctUntilChanged()

    private fun Flow<MailboxState>.observeMailLabelChanges() =
        this.map { it.mailboxListState as? MailboxListState.Data.ViewMode }
            .mapNotNull { it?.currentMailLabel }
            .distinctUntilChanged()

    private fun Flow<MailboxState>.observeSearchState() = this.map { it.mailboxListState as? MailboxListState.Data }
        .mapNotNull { it?.searchState }
        .distinctUntilChangedBy { it.isSearchingAllMail to it.searchQuery }

    private fun MailboxState.isInSearchMode() =
        this.mailboxListState is MailboxListState.Data && this.mailboxListState.searchState.isInSearch()

    private fun Flow<MailboxState>.observeSelectedMailboxItems() =
        this.map { it.mailboxListState as? MailboxListState.Data.SelectionMode }
            .mapNotNull { it?.selectedMailboxItems }
            .distinctUntilChanged()

    private fun List<MailLabel.Custom>.getLabelSelectionState(mappedLabels: List<UsedLabels>): LabelSelectionList {
        val previousSelectedLabels = mutableListOf<LabelId>()
        val previousPartiallySelectedLabels = mutableListOf<LabelId>()
        this.forEach { label ->
            if (mappedLabels.allContainsLabel(label.id.labelId)) {
                previousSelectedLabels.add(label.id.labelId)
            } else if (mappedLabels.partiallyContainsLabel(label.id.labelId)) {
                previousPartiallySelectedLabels.add(label.id.labelId)
            }
        }
        return LabelSelectionList(
            selectedLabels = previousSelectedLabels,
            partiallySelectionLabels = previousPartiallySelectedLabels
        )
    }

    private fun LabelAsBottomSheetState.Data.getLabelSelectionState(): LabelSelectionList {
        val selectedLabels = this.labelUiModelsWithSelectedState
            .filter { it.selectedState == LabelSelectedState.Selected }
            .map { it.labelUiModel.id.labelId }

        val partiallySelectedLabels = this.labelUiModelsWithSelectedState
            .filter { it.selectedState == LabelSelectedState.PartiallySelected }
            .map { it.labelUiModel.id.labelId }
        return LabelSelectionList(
            selectedLabels = selectedLabels,
            partiallySelectionLabels = partiallySelectedLabels
        )
    }

    private fun List<UsedLabels>.allContainsLabel(labelId: LabelId): Boolean {
        return this.all { usedLabels ->
            usedLabels.labels.any { it.labelId == labelId }
        }
    }

    private fun List<UsedLabels>.partiallyContainsLabel(labelId: LabelId): Boolean {
        return this.any { messageWithLabel ->
            messageWithLabel.labels.any { it.labelId == labelId }
        }
    }

    private fun isActionAllowedForCurrentLabel(labelId: LabelId): Boolean {
        return state.value.mailboxListState.let { usedLabels ->
            usedLabels is MailboxListState.Data && usedLabels.currentMailLabel.id.labelId != labelId
        }
    }

    private fun showRatingBooster(operation: MailboxViewAction.ShowRatingBooster) {
        try {
            showRatingBooster(operation.context as Activity)
        } catch (exception: ClassCastException) {
            Timber.d("Showing the rating booster was unsuccessful", exception)
        }
    }

    companion object {

        val initialState = MailboxState(
            mailboxListState = MailboxListState.Loading,
            topAppBarState = MailboxTopAppBarState.Loading,
            upgradeStorageState = UpgradeStorageState(notificationDotVisible = false),
            unreadFilterState = UnreadFilterState.Loading,
            bottomAppBarState = BottomBarState.Data.Hidden(emptyList<ActionUiModel>().toImmutableList()),
            deleteDialogState = DeleteDialogState.Hidden,
            deleteAllDialogState = DeleteDialogState.Hidden,
            autoDeleteSettingState = AutoDeleteSettingState.Loading,
            storageLimitState = StorageLimitState.None,
            bottomSheetState = null,
            actionResult = Effect.empty(),
            error = Effect.empty(),
            showRatingBooster = Effect.empty(),
            showNPSFeedback = Effect.empty()
        )
    }
}
