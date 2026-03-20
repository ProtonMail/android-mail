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

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.LoadState
import androidx.paging.LoadStates
import androidx.paging.Pager
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.map
import arrow.core.Either
import arrow.core.flatMap
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailattachments.domain.model.AttachmentId
import ch.protonmail.android.mailattachments.domain.model.AttachmentOpenMode
import ch.protonmail.android.mailattachments.domain.usecase.GetAttachmentIntentValues
import ch.protonmail.android.mailcommon.domain.coroutines.AppScope
import ch.protonmail.android.mailcommon.domain.model.Action
import ch.protonmail.android.mailcommon.domain.model.ConversationId
import ch.protonmail.android.mailcommon.domain.model.CursorId
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailcommon.presentation.mapper.ActionUiModelMapper
import ch.protonmail.android.mailcommon.presentation.model.ActionUiModel
import ch.protonmail.android.mailcommon.presentation.model.AvatarUiModel
import ch.protonmail.android.mailcommon.presentation.model.BottomBarEvent
import ch.protonmail.android.mailcommon.presentation.model.BottomBarState
import ch.protonmail.android.mailcommon.presentation.model.BottomBarTarget
import ch.protonmail.android.mailcommon.presentation.ui.delete.DeleteDialogState
import ch.protonmail.android.mailconversation.domain.usecase.DeleteConversations
import ch.protonmail.android.mailconversation.domain.usecase.IsExpandableLocation
import ch.protonmail.android.mailconversation.domain.usecase.MarkConversationsAsRead
import ch.protonmail.android.mailconversation.domain.usecase.MarkConversationsAsUnread
import ch.protonmail.android.mailconversation.domain.usecase.MoveConversations
import ch.protonmail.android.mailconversation.domain.usecase.StarConversations
import ch.protonmail.android.mailconversation.domain.usecase.TerminateConversationPaginator
import ch.protonmail.android.mailconversation.domain.usecase.UnStarConversations
import ch.protonmail.android.maillabel.domain.extension.isOutbox
import ch.protonmail.android.maillabel.domain.model.LabelId
import ch.protonmail.android.maillabel.domain.model.MailLabel
import ch.protonmail.android.maillabel.domain.model.MailLabelId
import ch.protonmail.android.maillabel.domain.model.SystemLabelId
import ch.protonmail.android.maillabel.domain.model.ViewMode
import ch.protonmail.android.maillabel.domain.usecase.FindLocalSystemLabelId
import ch.protonmail.android.maillabel.domain.usecase.GetCurrentViewModeForLabel
import ch.protonmail.android.maillabel.domain.usecase.GetSelectedMailLabelId
import ch.protonmail.android.maillabel.domain.usecase.ObserveLoadedMailLabelId
import ch.protonmail.android.maillabel.domain.usecase.ObserveMailLabels
import ch.protonmail.android.maillabel.domain.usecase.ObserveSelectedMailLabelId
import ch.protonmail.android.maillabel.domain.usecase.SelectMailLabelId
import ch.protonmail.android.maillabel.presentation.bottomsheet.LabelAsBottomSheetEntryPoint
import ch.protonmail.android.maillabel.presentation.bottomsheet.LabelAsItemId
import ch.protonmail.android.maillabel.presentation.bottomsheet.moveto.MoveToBottomSheetEntryPoint
import ch.protonmail.android.maillabel.presentation.bottomsheet.moveto.MoveToItemId
import ch.protonmail.android.mailmailbox.domain.model.MailboxItem
import ch.protonmail.android.mailmailbox.domain.model.MailboxItemId
import ch.protonmail.android.mailmailbox.domain.model.MailboxItemType
import ch.protonmail.android.mailmailbox.domain.model.MailboxPageKey
import ch.protonmail.android.mailmailbox.domain.model.SpamOrTrash
import ch.protonmail.android.mailmailbox.domain.model.toMailboxItemType
import ch.protonmail.android.mailmailbox.domain.usecase.GetBottomBarActions
import ch.protonmail.android.mailmailbox.domain.usecase.GetBottomSheetActions
import ch.protonmail.android.mailmailbox.domain.usecase.ObserveMailboxFetchNewStatus
import ch.protonmail.android.mailmailbox.domain.usecase.ObserveUnreadCounters
import ch.protonmail.android.mailmailbox.domain.usecase.SetEphemeralMailboxCursor
import ch.protonmail.android.mailmailbox.presentation.mailbox.mapper.MailboxItemUiModelMapper
import ch.protonmail.android.mailmailbox.presentation.mailbox.mapper.SwipeActionsMapper
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxComposerNavigationState
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxEvent
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxItemUiModel
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxListState
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxOperation
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxState
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxTopAppBarState
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxViewAction
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MoveResult
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.ShowSpamTrashIncludeFilterState
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.UnreadFilterState
import ch.protonmail.android.mailmailbox.presentation.mailbox.reducer.MailboxReducer
import ch.protonmail.android.mailmailbox.presentation.mailbox.usecase.ObserveValidSenderAddress
import ch.protonmail.android.mailmailbox.presentation.mailbox.usecase.ObserveViewModeChanged
import ch.protonmail.android.mailmailbox.presentation.mailbox.usecase.RecordRatingBoosterTriggered
import ch.protonmail.android.mailmailbox.presentation.mailbox.usecase.ShouldShowRatingBooster
import ch.protonmail.android.mailmailbox.presentation.mailbox.usecase.UpdateShowSpamTrashFilter
import ch.protonmail.android.mailmailbox.presentation.mailbox.usecase.UpdateUnreadFilter
import ch.protonmail.android.mailmailbox.presentation.paging.MailboxPagerFactory
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailmessage.domain.model.UnreadCounter
import ch.protonmail.android.mailmessage.domain.usecase.DeleteAllMessagesInLocation
import ch.protonmail.android.mailmessage.domain.usecase.DeleteMessages
import ch.protonmail.android.mailmessage.domain.usecase.HandleAvatarImageLoadingFailure
import ch.protonmail.android.mailmessage.domain.usecase.LoadAvatarImage
import ch.protonmail.android.mailmessage.domain.usecase.MarkMessagesAsRead
import ch.protonmail.android.mailmessage.domain.usecase.MarkMessagesAsUnread
import ch.protonmail.android.mailmessage.domain.usecase.MoveMessages
import ch.protonmail.android.mailmessage.domain.usecase.ObserveAvatarImageStates
import ch.protonmail.android.mailmessage.domain.usecase.StarMessages
import ch.protonmail.android.mailmessage.domain.usecase.UnStarMessages
import ch.protonmail.android.mailmessage.presentation.model.bottomsheet.LabelAsBottomSheetState
import ch.protonmail.android.mailmessage.presentation.model.bottomsheet.MailboxMoreActionsBottomSheetState
import ch.protonmail.android.mailmessage.presentation.model.bottomsheet.ManageAccountSheetState
import ch.protonmail.android.mailmessage.presentation.model.bottomsheet.MoveToBottomSheetState
import ch.protonmail.android.mailmessage.presentation.model.bottomsheet.SnoozeSheetState
import ch.protonmail.android.mailpagination.domain.usecase.ObservePageInvalidationEvents
import ch.protonmail.android.mailsession.domain.repository.EventLoopRepository
import ch.protonmail.android.mailsession.domain.usecase.HasValidUserSession
import ch.protonmail.android.mailsession.domain.usecase.ObservePrimaryUserIdWithValidSession
import ch.protonmail.android.mailsettings.domain.model.ToolbarActionsRefreshSignal
import ch.protonmail.android.mailsettings.domain.usecase.ObserveFolderColorSettings
import ch.protonmail.android.mailsettings.domain.usecase.ObserveSwipeActionsPreference
import ch.protonmail.android.mailsnooze.presentation.model.SnoozeConversationId
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import me.proton.android.core.accountmanager.domain.usecase.ObservePrimaryAccountAvatarItem
import me.proton.core.domain.entity.UserId
import me.proton.core.util.kotlin.DispatcherProvider
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
@SuppressWarnings("LongParameterList", "TooManyFunctions", "LargeClass")
class MailboxViewModel @Inject constructor(
    @AppScope private val appScope: CoroutineScope,
    private val mailboxPagerFactory: MailboxPagerFactory,
    private val getCurrentViewModeForLabel: GetCurrentViewModeForLabel,
    private val observePrimaryUserIdWithValidSession: ObservePrimaryUserIdWithValidSession,
    private val observeMailLabels: ObserveMailLabels,
    private val getUserHasValidSession: HasValidUserSession,
    private val observeSwipeActionsPreference: ObserveSwipeActionsPreference,
    private val observeSelectedMailLabelId: ObserveSelectedMailLabelId,
    private val observeLoadedMailLabelId: ObserveLoadedMailLabelId,
    private val getSelectedMailLabelId: GetSelectedMailLabelId,
    private val selectMailLabelId: SelectMailLabelId,
    private val observeUnreadCounters: ObserveUnreadCounters,
    private val observeFolderColorSettings: ObserveFolderColorSettings,
    private val getBottomBarActions: GetBottomBarActions,
    private val getBottomSheetActions: GetBottomSheetActions,
    private val actionUiModelMapper: ActionUiModelMapper,
    private val mailboxItemMapper: MailboxItemUiModelMapper,
    private val swipeActionsMapper: SwipeActionsMapper,
    private val markConversationsAsRead: MarkConversationsAsRead,
    private val markConversationsAsUnread: MarkConversationsAsUnread,
    private val markMessagesAsRead: MarkMessagesAsRead,
    private val markMessagesAsUnread: MarkMessagesAsUnread,
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
    private val findLocalSystemLabelId: FindLocalSystemLabelId,
    private val loadAvatarImage: LoadAvatarImage,
    private val handleAvatarImageLoadingFailure: HandleAvatarImageLoadingFailure,
    private val observeAvatarImageStates: ObserveAvatarImageStates,
    private val observePrimaryAccountAvatarItem: ObservePrimaryAccountAvatarItem,
    private val getAttachmentIntentValues: GetAttachmentIntentValues,
    private val deleteAllMessagesInLocation: DeleteAllMessagesInLocation,
    private val observePageInvalidationEvents: ObservePageInvalidationEvents,
    private val observeViewModeChanged: ObserveViewModeChanged,
    private val toolbarRefreshSignal: ToolbarActionsRefreshSignal,
    private val terminateConversationPaginator: TerminateConversationPaginator,
    private val isExpandableLocation: IsExpandableLocation,
    private val eventLoopRepository: EventLoopRepository,
    private val updateUnreadFilter: UpdateUnreadFilter,
    private val updateShowSpamTrashFilter: UpdateShowSpamTrashFilter,
    private val setEphemeralMailboxCursor: SetEphemeralMailboxCursor,
    private val observeMailboxFetchNewStatus: ObserveMailboxFetchNewStatus,
    private val observeValidSenderAddress: ObserveValidSenderAddress,
    private val loadingBarControllerFactory: MailboxLoadingBarControllerFactory,
    private val shouldShowRatingBooster: ShouldShowRatingBooster,
    private val recordRatingBoosterTriggered: RecordRatingBoosterTriggered
) : ViewModel() {

    private val primaryUserId = observePrimaryUserIdWithValidSession().filterNotNull()
    private val mutableState = MutableStateFlow(initialState)
    private val itemIdsMutex = Mutex()
    private val itemIds = mutableListOf<String>()
    private val folderColorSettings = primaryUserId.flatMapLatest {
        observeFolderColorSettings(it).distinctUntilChanged()
    }
    private var refreshJob: Job? = null
    private var attachmentDownloadJob: Job? = null

    private val loadingBarController = loadingBarControllerFactory.create(viewModelScope)

    val state: StateFlow<MailboxState> = mutableState.asStateFlow()
    val items: Flow<PagingData<MailboxItemUiModel>> = observePagingData().cachedIn(viewModelScope)

    init {
        observeCurrentMailLabel()
            .onEach { currentMailLabel ->
                currentMailLabel?.let {
                    cancelAttachmentDownload()
                    emitNewStateFrom(
                        MailboxEvent.SelectedLabelChanged(
                            currentMailLabel
                        )
                    )
                }
            }
            .filterNotNull()
            .launchIn(viewModelScope)

        observeMailLabelChangeRequests()
            .pairWithCurrentLabelCount()
            .combine(primaryUserId.filterNotNull()) { labelWithCount, userId ->
                Triple(labelWithCount.first, labelWithCount.second, userId)
            }
            .onEach { (currentMailLabel, currentLabelCount, _) ->
                itemIdsMutex.withLock { itemIds.clear() }
                cancelAttachmentDownload()
                emitNewStateFrom(
                    MailboxEvent.NewLabelSelected(
                        currentMailLabel, currentLabelCount
                    )
                )
            }
            .flatMapLatest { (currentMailLabel, _, userId) ->
                handleSwipeActionPreferences(userId, currentMailLabel)
            }
            .onEach {
                emitNewStateFrom(it)
            }
            .launchIn(viewModelScope)

        combine(
            observeLoadedMailLabelId().mapToExistingLabel(),
            state.observeSelectedMailboxItems(),
            toolbarRefreshSignal.refreshEvents.onStart { emit(Unit) }
        ) { selectedMailLabel, selectedMailboxItems, _ ->
            getBottomBarActions(
                primaryUserId.filterNotNull().first(),
                selectedMailLabel.id.labelId,
                selectedMailboxItems.map { MailboxItemId(it.id) },
                getViewModeForCurrentLocation(selectedMailLabel.id)
            ).fold(
                ifLeft = { MailboxEvent.MessageBottomBarEvent(BottomBarEvent.ErrorLoadingActions) },
                ifRight = { actions ->
                    MailboxEvent.MessageBottomBarEvent(
                        BottomBarEvent.ActionsData(
                            BottomBarTarget.Mailbox,
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

        observeAvatarImageStates()
            .onEach { avatarImageStates ->
                emitNewStateFrom(MailboxEvent.AvatarImageStatesUpdated(avatarImageStates))
            }
            .launchIn(viewModelScope)

        observePrimaryAccountAvatarItem().onEach { item ->
            emitNewStateFrom(MailboxEvent.PrimaryAccountAvatarChanged(item))
        }.launchIn(viewModelScope)

        observePageInvalidationEvents()
            .onEach { event ->
                Timber.d("Paginator: Received page invalidation event with id: ${event.id}")
                emitNewStateFrom(MailboxEvent.PaginatorInvalidated(event))
            }
            .launchIn(viewModelScope)

        loadingBarController.observeState().onEach {
            emitNewStateFrom(MailboxEvent.LoadingBarStateUpdated(it))
        }.launchIn(viewModelScope)

        observeMailboxFetchNewStatus().onEach {
            loadingBarController.onMailboxFetchNewStatus(it)
        }.launchIn(viewModelScope)

        primaryUserId.flatMapLatest {
            observeValidSenderAddress(it)
        }.onEach {
            emitNewStateFrom(MailboxEvent.SenderHasValidAddressUpdated(isValid = it))
        }.launchIn(viewModelScope)

        primaryUserId.flatMapLatest { userId ->
            shouldShowRatingBooster(userId)
        }.distinctUntilChanged()
            .filter { it }
            .onEach { shouldShowRatingBooster ->
                recordRatingBoosterTriggered()
                emitNewStateFrom(MailboxEvent.ShowRatingBooster)

            }
            .launchIn(viewModelScope)
    }

    override fun onCleared() {
        Timber.d("MailboxViewModel onCleared")
        cleanupOnCleared()
        super.onCleared()
    }

    @VisibleForTesting
    internal fun cleanupOnCleared() {
        appScope.launch {
            val userId = primaryUserId.first()
            val viewMode = getViewModeForCurrentLocation(getSelectedMailLabelId())
            when (viewMode) {
                ViewMode.ConversationGrouping -> terminateConversationPaginator(userId)
                ViewMode.NoConversationGrouping -> {
                    Timber.d("Skip terminating message paginator (singleton scope)")
                }
            }
        }
    }

    private fun handleSwipeActionPreferences(userId: UserId, currentMailLabel: MailLabel): Flow<MailboxEvent> {
        return observeSwipeActionsPreference(userId)
            .map { swipeActionsPreference ->
                val swipeActions = swipeActionsMapper(currentMailLabel.resolveId(), swipeActionsPreference)
                MailboxEvent.SwipeActionsChanged(swipeActions)
            }
            .distinctUntilChanged()
    }

    private fun MailLabel.resolveId() = when (this) {
        is MailLabel.Custom -> this.id.labelId
        is MailLabel.System -> this.systemLabelId.labelId
    }

    @SuppressWarnings("ComplexMethod", "LongMethod")
    internal fun submit(viewAction: MailboxViewAction) {
        viewModelScope.launch {
            when (viewAction) {
                is MailboxViewAction.ExitSelectionMode,
                is MailboxViewAction.DisableUnreadFilter,
                is MailboxViewAction.EnableUnreadFilter,
                is MailboxViewAction.EnableShowSpamTrashFilter,
                is MailboxViewAction.DisableShowSpamTrashFilter -> emitNewStateFrom(viewAction)

                is MailboxViewAction.MailboxItemsChanged -> handleMailboxItemChanged(viewAction.itemIds)
                is MailboxViewAction.OnItemAvatarClicked -> handleOnAvatarClicked(viewAction.item)
                is MailboxViewAction.OnAvatarImageLoadRequested -> handleOnAvatarImageLoadRequested(viewAction.item)
                is MailboxViewAction.OnAvatarImageLoadFailed -> handleOnAvatarImageLoadFailed(viewAction.item)
                is MailboxViewAction.OnItemLongClicked -> handleItemLongClick(viewAction.item)
                is MailboxViewAction.Refresh -> handlePullToRefresh(viewAction)
                is MailboxViewAction.ItemClicked -> handleItemClick(viewAction.item)
                is MailboxViewAction.OnOfflineWithData -> emitNewStateFrom(viewAction)
                is MailboxViewAction.OnErrorWithData -> emitNewStateFrom(viewAction)
                is MailboxViewAction.MarkAsRead -> handleMarkAsReadAction(viewAction)
                is MailboxViewAction.MarkAsUnread -> handleMarkAsUnreadAction(viewAction)
                is MailboxViewAction.SwipeReadAction -> handleSwipeReadAction(viewAction)
                is MailboxViewAction.SwipeArchiveAction -> handleSwipeArchiveAction(viewAction)
                is MailboxViewAction.SwipeSpamAction -> handleSwipeSpamAction(viewAction)
                is MailboxViewAction.SwipeTrashAction -> handleSwipeTrashAction(viewAction)
                is MailboxViewAction.StarAction -> handleSwipeStarAction(viewAction)
                is MailboxViewAction.SwipeLabelAsAction -> requestLabelAsBottomSheet(viewAction)
                is MailboxViewAction.SwipeMoveToAction -> requestMoveToBottomSheet(viewAction)
                is MailboxViewAction.Trash -> handleTrashAction()
                is MailboxViewAction.Delete -> handleDeleteAction()
                is MailboxViewAction.MoveToInbox -> handleMoveToInboxAction()
                is MailboxViewAction.DeleteConfirmed -> handleDeleteConfirmedAction()
                is MailboxViewAction.DeleteDialogDismissed -> handleDeleteDialogDismissed()
                is MailboxViewAction.RequestLabelAsBottomSheet -> requestLabelAsBottomSheet(viewAction)

                is MailboxViewAction.RequestMoveToBottomSheet -> requestMoveToBottomSheet(viewAction)
                is MailboxViewAction.RequestMoreActionsBottomSheet -> showMoreBottomSheet(viewAction)
                is MailboxViewAction.RequestManageAccountsBottomSheet -> showAccountManagerBottomSheet(viewAction)
                is MailboxViewAction.DismissBottomSheet -> emitNewStateFrom(viewAction)
                is MailboxViewAction.Star -> handleStarAction(viewAction)
                is MailboxViewAction.UnStar -> handleUnStarAction(viewAction)
                is MailboxViewAction.MoveToArchive -> handleMoveToArchiveAction()
                is MailboxViewAction.MoveToSpam -> handleMoveToSpamAction()
                is MailboxViewAction.EnterSearchMode -> emitNewStateFrom(viewAction)
                is MailboxViewAction.ExitSearchMode -> emitNewStateFrom(viewAction)
                is MailboxViewAction.SearchQuery -> emitNewStateFrom(viewAction)
                is MailboxViewAction.SearchResult -> emitNewStateFrom(viewAction)
                is MailboxViewAction.NavigateToInboxLabel -> handleNavigateToInbox()
                is MailboxViewAction.SelectAll -> handleSelectAllAction(viewAction)
                is MailboxViewAction.DeselectAll -> handleDeselectAllAction()
                is MailboxViewAction.CustomizeToolbar -> handleCustomizeToolbar(viewAction)
                is MailboxViewAction.RequestAttachment -> handleRequestAttachment(viewAction)
                is MailboxViewAction.ClearAll -> handleClearAll()
                is MailboxViewAction.ClearAllConfirmed -> handleClearAllConfirmed(viewAction)
                is MailboxViewAction.ClearAllDismissed -> emitNewStateFrom(viewAction)
                is MailboxViewAction.SnoozeDismissed -> handleSnoozeCompletedAction(viewAction)
                is MailboxViewAction.RequestSnoozeBottomSheet -> requestSnoozeBottomSheet(viewAction)
                is MailboxViewAction.SignalMoveToCompleted -> handleMoveToCompleted(viewAction)
                is MailboxViewAction.SignalLabelAsCompleted -> handleLabelAsCompleted(viewAction)
                is MailboxViewAction.ValidateUserSession -> handleValidateUserSession()
                is MailboxViewAction.NavigateToComposer -> handleNavigateToComposer()
            }
        }
    }

    private fun handleNavigateToComposer() {
        if (state.value.composerNavigationState is MailboxComposerNavigationState.Disabled) {
            emitNewStateFrom(MailboxEvent.ErrorComposing)
        } else {
            emitNewStateFrom(MailboxEvent.NavigateToComposer)
        }
    }

    private fun handlePullToRefresh(viewAction: MailboxViewAction.Refresh) {
        if (refreshJob?.isActive == true) return

        refreshJob = viewModelScope.launch {
            Timber.d("Pull to refresh started")
            emitNewStateFrom(viewAction)

            eventLoopRepository.triggerAndWait(primaryUserId.first())

            emitNewStateFrom(MailboxEvent.RefreshCompleted)

            Timber.d("Pull to refresh completed")
        }
    }

    private fun handleRequestAttachment(action: MailboxViewAction.RequestAttachment) {
        val currentListState = state.value.mailboxListState as? MailboxListState.Data.ViewMode
        if (currentListState?.downloadingAttachmentId != null) {
            emitNewStateFrom(MailboxEvent.AttachmentDownloadInProgressEvent)
            return
        }
        emitNewStateFrom(MailboxEvent.AttachmentDownloadStartedEvent(action.attachmentId))
        attachmentDownloadJob = viewModelScope.launch {
            val domainAttachmentId = AttachmentId(action.attachmentId.value)
            val attachmentIntentValues =
                getAttachmentIntentValues(primaryUserId.first(), AttachmentOpenMode.Open, domainAttachmentId)
                    .getOrElse { return@launch emitNewStateFrom(MailboxEvent.AttachmentErrorEvent) }

            emitNewStateFrom(MailboxEvent.AttachmentReadyEvent(attachmentIntentValues))
        }
    }

    private fun cancelAttachmentDownload() {
        attachmentDownloadJob?.cancel()
        attachmentDownloadJob = null
    }

    private suspend fun handleNavigateToInbox() {
        val inboxLabel = findLocalSystemLabelId(primaryUserId.first(), SystemLabelId.Inbox)
            ?: return Timber.e("Unable to find Inbox system label")

        selectMailLabelId(inboxLabel)
    }

    private fun handleCustomizeToolbar(viewAction: MailboxViewAction) {
        emitNewStateFrom(viewAction)
    }

    private fun handleSelectAllAction(action: MailboxViewAction.SelectAll) {
        emitNewStateFrom(MailboxEvent.AllItemsSelected(action.allItems))
    }

    private fun handleDeselectAllAction() {
        emitNewStateFrom(MailboxEvent.AllItemsDeselected)
    }

    private suspend fun handleMailboxItemChanged(updatedItemIds: List<String>) {
        withContext(dispatchersProvider.Comp) {
            itemIdsMutex.withLock {
                val removedItems = itemIds.filterNot { updatedItemIds.contains(it) }
                itemIds.clear()
                itemIds.addAll(updatedItemIds)
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
    }

    private suspend fun handleItemClick(item: MailboxItemUiModel) {
        when (state.value.mailboxListState) {
            is MailboxListState.Data.SelectionMode -> handleItemClickInSelectionMode(item)
            is MailboxListState.Data.ViewMode -> handleItemClickInViewMode(item)
            is MailboxListState.CouldNotLoadUserSession,
            is MailboxListState.Loading -> {
                Timber.d("Loading state can't handle item clicks")
            }
        }
    }

    private suspend fun handleItemClickInViewMode(item: MailboxItemUiModel) {
        if (item.shouldOpenInComposer) {
            emitNewStateFrom(MailboxEvent.ItemClicked.OpenComposer(item))
        } else {
            val user = primaryUserId.filterNotNull().first()
            val viewMode = getViewModeForCurrentLocation(getSelectedMailLabelId())
            val isInSearchMode = state.value.isInSearchMode()
            val subItemId = if (item.type == MailboxItemType.Message || isInSearchMode) {
                item.id
            } else {
                null
            }
            // We are leaving mailbox screen, however other screens may still need the cursor in order to navigate
            // messages. For example the paging view detail screen that uses a cursor to swipe messages.  In this case
            // we set an ephemeral cursor (transient/ short lived) that can be joined by the following screen
            val labelId = getFromLabelIdSearchAware()
            setEphemeralMailboxCursor(
                userId = user, viewModeIsConversation = viewMode == ViewMode.ConversationGrouping,
                cursorId = CursorId(
                    item.conversationId, subItemId
                ),
                labelId = labelId
            )
            val isConversationGrouping =
                getViewModeForCurrentLocation(getSelectedMailLabelId()) == ViewMode.ConversationGrouping

            emitNewStateFrom(
                MailboxEvent.ItemClicked.ItemDetailsOpened(
                    item, labelId,
                    isConversationGrouping, subItemId
                )
            )
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

    private fun handleOnAvatarImageLoadRequested(item: MailboxItemUiModel) {
        (item.avatar as? AvatarUiModel.ParticipantAvatar)?.let { avatar ->
            viewModelScope.launch {
                loadAvatarImage(avatar.address, avatar.bimiSelector)
            }
        }
    }

    private fun handleOnAvatarImageLoadFailed(item: MailboxItemUiModel) {
        (item.avatar as? AvatarUiModel.ParticipantAvatar)?.let { avatar ->
            viewModelScope.launch {
                handleAvatarImageLoadingFailure(avatar.address, avatar.bimiSelector)
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
            if (selectionMode.selectedMailboxItems.size >= MailboxListState.maxItemSelectionLimit) {
                MailboxEvent.MaxSelectionLimitReached
            } else {
                MailboxEvent.ItemClicked.ItemAddedToSelection(item)
            }
        }

        emitNewStateFrom(event)
    }

    private fun enterSelectionMode(item: MailboxItemUiModel) {
        val state = state.value.mailboxListState
        if (state is MailboxListState.Data.ViewMode && state.currentMailLabel.resolveId()
                .selectionModeEnabledForLabel()
        ) {
            cancelAttachmentDownload()
            emitNewStateFrom(
                MailboxEvent.EnterSelectionMode(item)
            )
        } else {
            Timber.d("Cannot enter selection mode from state: $state")
        }
    }

    private fun LabelId.selectionModeEnabledForLabel() = this.isOutbox().not()

    /**
     * Creates a [MailboxPagerFactory] and observes the emitted paging data. The Pager is re-created, when:
     * - The selected Mail Label (ie. Mailbox location) changes
     * - Entering/exiting search mode)
     *
     * This method keeps track of the "selected mail label" and the "search mode state" it's been called
     * to be able to hide the displayed items and show a loader as soon as such params change (avoiding
     * the old location's items from being displayed till the new ones are loaded).
     * This is achieved through `pagingDataFlow.emit(PagingData.empty())`
     */
    @Suppress("LongMethod")
    private fun observePagingData(): Flow<PagingData<MailboxItemUiModel>> {
        val pagingDataFlow = MutableStateFlow<PagingData<MailboxItemUiModel>>(PagingData.empty())
        var currentMailLabel: MailLabel? = null
        var currentSearchModeState: Boolean? = null

        primaryUserId.filterNotNull().flatMapLatest { userId ->

            // Reset state and emit empty data when userId changes
            currentMailLabel = null
            currentSearchModeState = null
            pagingDataFlow.tryEmit(
                PagingData.empty(
                    LoadStates(LoadState.Loading, LoadState.Loading, LoadState.Loading)
                )
            )

            // Create new pager when mail label, search mode or view mode changes. Changes in
            // search query, unread filter and showSpamTrash will be handled by the existing pager.
            combine(
                observeMailLabelChangeRequests(),
                state.observeSearchQuery(),
                observeViewModeChanged(userId)
            ) { selectedMailLabel, query, _ ->

                val isInSearchMode = state.value.isInSearchMode()
                if (selectedMailLabel != currentMailLabel || currentSearchModeState != isInSearchMode) {
                    pagingDataFlow.emit(
                        PagingData.empty(
                            LoadStates(LoadState.Loading, LoadState.Loading, LoadState.Loading)
                        )
                    )
                    currentMailLabel = selectedMailLabel
                    currentSearchModeState = isInSearchMode
                }

                val viewMode = getViewModeForCurrentLocation(selectedMailLabel.id)

                mailboxPagerFactory.create(
                    userId = userId,
                    selectedMailLabelId = selectedMailLabel.id,
                    type = if (!isInSearchMode) viewMode.toMailboxItemType() else MailboxItemType.Message,
                    searchQuery = query
                ) to (query to viewMode)
            }
                .flatMapLatest { (pager, searchAndViewMode) ->
                    val (query, viewMode) = searchAndViewMode

                    channelFlow {
                        Timber.d(
                            "New Pager created for label=${currentMailLabel?.id?.labelId?.id} " +
                                "query=$query viewMode=$viewMode"
                        )

                        // Start paging immediately and send items downstream.
                        // Also signal when the first page has been emitted.
                        val firstPageArrived = CompletableDeferred<Unit>()
                        val pagingJob = launch {
                            var emittedOnce = false
                            mapPagingData(userId, pager)
                                .onEach { page ->
                                    if (!emittedOnce) {
                                        emittedOnce = true
                                        firstPageArrived.complete(Unit)

                                        currentMailLabel?.let { labelLoaded ->
                                            Timber.d("Setting loaded label id to: ${labelLoaded.id}")
                                            selectMailLabelId.setLocationAsLoaded(labelLoaded.id)
                                        }

                                        Timber.d("First page arrived for label=${getSelectedMailLabelId()}")
                                    }
                                    send(page)
                                }
                                .collect()
                        }

                        // Start observers after the first page and skip their initial snapshots.
                        // This will ensure that new Rust scroller has been created and is ready to
                        // accept updates to the filters and search query.
                        firstPageArrived.await()

                        val jobs = mutableListOf<Job>()

                        jobs += launch {
                            state.observeUnreadFilterState()
                                .distinctUntilChanged()
                                .drop(1) // ignore initial value
                                .collect { unreadEnabled ->
                                    Timber.d("Updating unread filter: $unreadEnabled")
                                    updateUnreadFilter(unreadEnabled, viewMode)
                                }
                        }

                        jobs += launch {
                            state.observeShowSpamTrashFilterState()
                                .distinctUntilChanged()
                                .drop(1) // ignore initial value
                                .collect { showSpamTrash ->
                                    Timber.d("Updating showSpamTrash filter: $showSpamTrash")
                                    updateShowSpamTrashFilter(showSpamTrash, viewMode)
                                }
                        }

                        jobs += launch {
                            combine(
                                observeLoadedMailLabelId()
                                    .mapToExistingLabel()
                                    .distinctUntilChanged(),
                                state.observeSearchDataReady()
                            ) { loadedMailLabel, dataReady ->
                                val userId = primaryUserId.filterNotNull().first()
                                val viewMode = if (dataReady) {
                                    ViewMode.NoConversationGrouping
                                } else {
                                    getCurrentViewModeForLabel(userId, loadedMailLabel.id.labelId)
                                }

                                if (isExpandableLocation(viewMode)) {
                                    MailboxEvent.ShowSpamTrashFilter
                                } else {
                                    MailboxEvent.HideSpamTrashFilter
                                }
                            }
                                .distinctUntilChanged()
                                .onEach { emitNewStateFrom(it) }
                                .collect()
                        }

                        awaitClose {
                            Timber.d("Cancelling pager")
                            jobs.forEach { it.cancel() }
                            pagingJob.cancel()
                        }
                    }
                }
        }
            .onEach {
                pagingDataFlow.emit(it)
            }
            .launchIn(viewModelScope)

        return pagingDataFlow
    }

    private suspend fun mapPagingData(
        userId: UserId,
        pager: Pager<MailboxPageKey, MailboxItem>
    ): Flow<PagingData<MailboxItemUiModel>> {
        return withContext(dispatchersProvider.Comp) {
            val folderColorSettingsValue = folderColorSettings.first()
            pager.flow.mapLatest { pagingData ->
                pagingData.map {
                    withContext(dispatchersProvider.Comp) {
                        mailboxItemMapper.toUiModel(
                            userId, it, folderColorSettingsValue,
                            state.value.isInSearchMode()
                        )
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
        val viewMode = getViewModeForCurrentLocation(getSelectedMailLabelId())
        when (viewMode) {
            ViewMode.ConversationGrouping -> markConversationsAsRead(
                userId = user,
                labelId = getFromLabelIdSearchAware(),
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
        val viewMode = getViewModeForCurrentLocation(getSelectedMailLabelId())
        when (viewMode) {
            ViewMode.ConversationGrouping -> markConversationsAsUnread(
                userId = userId,
                labelId = getFromLabelIdSearchAware(),
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
            when (getViewModeForCurrentLocation(getSelectedMailLabelId())) {
                ViewMode.ConversationGrouping -> markConversationsAsUnread(
                    userId = primaryUserId.filterNotNull().first(),
                    labelId = getFromLabelIdSearchAware(),
                    conversationIds = listOf(ConversationId(swipeReadAction.itemId))
                )

                ViewMode.NoConversationGrouping -> markMessagesAsUnread(
                    userId = primaryUserId.filterNotNull().first(),
                    messageIds = listOf(MessageId(swipeReadAction.itemId))
                )
            }
        } else {
            when (getViewModeForCurrentLocation(getSelectedMailLabelId())) {
                ViewMode.ConversationGrouping -> markConversationsAsRead(
                    userId = primaryUserId.filterNotNull().first(),
                    labelId = getFromLabelIdSearchAware(),
                    conversationIds = listOf(ConversationId(swipeReadAction.itemId))
                )

                ViewMode.NoConversationGrouping -> markMessagesAsRead(
                    userId = primaryUserId.filterNotNull().first(),
                    messageIds = listOf(MessageId(swipeReadAction.itemId))
                )
            }
        }
        emitNewStateFrom(swipeReadAction)
    }

    private suspend fun handleSwipeStarAction(swipeStarAction: MailboxViewAction.StarAction) {
        if (swipeStarAction.isStarred) {
            when (getViewModeForCurrentLocation(getSelectedMailLabelId())) {
                ViewMode.ConversationGrouping -> unStarConversations(
                    userId = primaryUserId.filterNotNull().first(),
                    conversationIds = listOf(ConversationId(swipeStarAction.itemId))
                )

                ViewMode.NoConversationGrouping -> unStarMessages(
                    userId = primaryUserId.filterNotNull().first(),
                    messageIds = listOf(MessageId(swipeStarAction.itemId))
                )
            }
        } else {
            when (getViewModeForCurrentLocation(getSelectedMailLabelId())) {
                ViewMode.ConversationGrouping -> starConversations(
                    userId = primaryUserId.filterNotNull().first(),
                    conversationIds = listOf(ConversationId(swipeStarAction.itemId))
                )

                ViewMode.NoConversationGrouping -> starMessages(
                    userId = primaryUserId.filterNotNull().first(),
                    messageIds = listOf(MessageId(swipeStarAction.itemId))
                )
            }
        }
        emitNewStateFrom(swipeStarAction)
    }

    private suspend fun handleSwipeArchiveAction(swipeArchiveAction: MailboxViewAction.SwipeArchiveAction) {
        if (isActionAllowedForCurrentLabel(SystemLabelId.Archive.labelId)) {
            val viewMode = getViewModeForCurrentLocation(getSelectedMailLabelId())
            val userId = primaryUserId.filterNotNull().first()
            moveSingleItemToDestination(userId, swipeArchiveAction.itemId, SystemLabelId.Archive, viewMode)
            emitNewStateFrom(swipeArchiveAction)
        }
    }

    private suspend fun handleSwipeSpamAction(swipeSpamAction: MailboxViewAction.SwipeSpamAction) {
        if (isActionAllowedForCurrentLabel(SystemLabelId.Spam.labelId)) {
            val viewMode = getViewModeForCurrentLocation(getSelectedMailLabelId())
            val userId = primaryUserId.filterNotNull().first()
            moveSingleItemToDestination(userId, swipeSpamAction.itemId, SystemLabelId.Spam, viewMode)
            emitNewStateFrom(swipeSpamAction)
        }
    }

    private suspend fun handleSwipeTrashAction(swipeTrashAction: MailboxViewAction.SwipeTrashAction) {
        if (isActionAllowedForCurrentLabel(SystemLabelId.Trash.labelId)) {
            val viewMode = getViewModeForCurrentLocation(getSelectedMailLabelId())
            val userId = primaryUserId.filterNotNull().first()
            moveSingleItemToDestination(userId, swipeTrashAction.itemId, SystemLabelId.Trash, viewMode)
            emitNewStateFrom(swipeTrashAction)
        }
    }

    private suspend fun moveSingleItemToDestination(
        userId: UserId,
        itemId: String,
        systemLabelId: SystemLabelId,
        viewMode: ViewMode
    ) {
        when (viewMode) {
            ViewMode.ConversationGrouping -> moveConversations(
                userId = userId,
                conversationIds = listOf(ConversationId(itemId)),
                systemLabelId = systemLabelId
            )

            ViewMode.NoConversationGrouping -> moveMessages(
                userId = userId,
                messageIds = listOf(MessageId(itemId)),
                systemLabelId = systemLabelId
            )
        }
    }

    private fun requestLabelAsBottomSheet(operation: MailboxViewAction) {
        val items = when (operation) {
            is MailboxViewAction.RequestLabelAsBottomSheet -> {
                val selectionMode = state.value.mailboxListState as? MailboxListState.Data.SelectionMode
                if (selectionMode == null) {
                    Timber.d("MailboxListState is not in SelectionMode")
                    return
                }
                selectionMode.selectedMailboxItems.map { LabelAsItemId(it.id) }
            }

            is MailboxViewAction.SwipeLabelAsAction -> {
                listOf(operation.itemId)
            }

            else -> return
        }

        viewModelScope.launch {
            val viewMode = getViewModeForCurrentLocation(getSelectedMailLabelId())
            val entryPoint = when (operation) {
                is MailboxViewAction.RequestLabelAsBottomSheet ->
                    LabelAsBottomSheetEntryPoint.Mailbox.SelectionMode(items.count(), viewMode)

                is MailboxViewAction.SwipeLabelAsAction ->
                    LabelAsBottomSheetEntryPoint.Mailbox.LabelAsSwipeAction(viewMode, operation.itemId)

                else -> {
                    Timber.e("Unsupported operation: $operation")
                    return@launch
                }
            }

            val event = LabelAsBottomSheetState.LabelAsBottomSheetEvent.Ready(
                userId = primaryUserId.first(),
                currentLabel = getSelectedMailLabelId().labelId,
                itemIds = items,
                entryPoint = entryPoint
            )
            emitNewStateFrom(MailboxEvent.MailboxBottomSheetEvent(event))
        }
    }

    private fun requestMoveToBottomSheet(operation: MailboxViewAction) {
        viewModelScope.launch {
            val userId = primaryUserId.filterNotNull().first()
            val currentMailLabel = getSelectedMailLabelId()
            val viewMode = getViewModeForCurrentLocation(currentMailLabel)

            val (entryPoint, selectedItemIds) = when (operation) {
                is MailboxViewAction.RequestMoveToBottomSheet -> {
                    val selectionMode = state.value.mailboxListState as? MailboxListState.Data.SelectionMode
                    if (selectionMode == null) {
                        Timber.d("MailboxListState is not in SelectionMode")
                        return@launch
                    }

                    val itemCount = selectionMode.selectedMailboxItems.size
                    Pair(
                        MoveToBottomSheetEntryPoint.Mailbox.SelectionMode(itemCount, viewMode),
                        selectionMode.selectedMailboxItems.map { MoveToItemId(it.id) }
                    )
                }

                is MailboxViewAction.SwipeMoveToAction -> Pair(
                    MoveToBottomSheetEntryPoint.Mailbox.MoveToSwipeAction(viewMode, operation.itemId),
                    listOf(operation.itemId)
                )

                else -> {
                    Timber.d("Unsupported operation: $operation")
                    return@launch
                }
            }

            val event = MoveToBottomSheetState.MoveToBottomSheetEvent.Ready(
                userId = userId,
                currentLabel = currentMailLabel.labelId,
                itemIds = selectedItemIds,
                entryPoint = entryPoint
            )

            emitNewStateFrom(MailboxEvent.MailboxBottomSheetEvent(event))
        }
    }

    private fun requestSnoozeBottomSheet(operation: MailboxViewAction) {
        viewModelScope.launch {
            val userId = primaryUserId.filterNotNull().first()
            val selectedItemIds = when (operation) {
                is MailboxViewAction.RequestSnoozeBottomSheet -> {
                    val selectionMode = state.value.mailboxListState as? MailboxListState.Data.SelectionMode
                    if (selectionMode == null) {
                        Timber.d("MailboxListState is not in SelectionMode")
                        return@launch
                    }
                    selectionMode.selectedMailboxItems.map { it.id }
                }

                else -> {
                    Timber.d("Unsupported operation: $operation")
                    return@launch
                }
            }
            val selectedLabelId = getSelectedMailLabelId().labelId
            val event = SnoozeSheetState.SnoozeOptionsBottomSheetEvent.Ready(
                userId = userId,
                labelId = selectedLabelId,
                itemIds = selectedItemIds.map { SnoozeConversationId(it) }
            )

            emitNewStateFrom(MailboxEvent.MailboxBottomSheetEvent(event))
        }
    }

    private fun showAccountManagerBottomSheet(operation: MailboxViewAction) {
        emitNewStateFrom(operation)
        emitNewStateFrom(
            MailboxEvent.MailboxBottomSheetEvent(
                ManageAccountSheetState.ManageAccountsBottomSheetEvent.Ready
            )
        )
    }

    private suspend fun showMoreBottomSheet(operation: MailboxViewAction) {
        val selectionState = state.value.mailboxListState as? MailboxListState.Data.SelectionMode
        if (selectionState == null) {
            Timber.d("MailboxListState is not in SelectionMode")
            return
        }
        emitNewStateFrom(operation)

        val userId = primaryUserId.filterNotNull().first()
        val currentMailLabel = getSelectedMailLabelId()
        val viewMode = getViewModeForCurrentLocation(currentMailLabel)
        val selectedItemIds: List<MailboxItemId> = selectionState.selectedMailboxItems.map { MailboxItemId(it.id) }

        val actions = getBottomSheetActions(userId, currentMailLabel.labelId, selectedItemIds, viewMode)
            .getOrElse {
                Timber.e("Mailbox failed to load the bottom-sheet actions: $it")
                return
            }

        emitNewStateFrom(
            MailboxEvent.MailboxBottomSheetEvent(
                MailboxMoreActionsBottomSheetState.MailboxMoreActionsBottomSheetEvent.ActionData(
                    hiddenActionUiModels = actions.hiddenActions
                        .map { actionUiModelMapper.toUiModel(it) }
                        .toImmutableList(),
                    visibleActionUiModels = actions.visibleActions
                        .map { actionUiModelMapper.toUiModel(it) }
                        .toImmutableList(),
                    customizeToolbarActionUiModel = actionUiModelMapper.toUiModel(Action.CustomizeToolbar),
                    selectedCount = selectionState.selectedMailboxItems.size
                )
            )
        )
    }

    private suspend fun handleTrashAction() {
        moveSelectedMailboxItemsTo(SystemLabelId.Trash).onRight {
            emitNewStateFrom(MailboxEvent.MoveToConfirmed.Trash(it.viewMode, it.itemsMoved))
        }.onLeft {
            emitNewStateFrom(MailboxEvent.ErrorMoving)
        }
    }

    private suspend fun handleMoveToInboxAction() {
        moveSelectedMailboxItemsTo(SystemLabelId.Inbox).onRight {
            emitNewStateFrom(MailboxEvent.MoveToConfirmed.Inbox(it.viewMode, it.itemsMoved))
        }.onLeft {
            emitNewStateFrom(MailboxEvent.ErrorMoving)
        }
    }

    private suspend fun handleMoveToArchiveAction() {
        moveSelectedMailboxItemsTo(SystemLabelId.Archive).onRight {
            emitNewStateFrom(MailboxEvent.MoveToConfirmed.Archive(it.viewMode, it.itemsMoved))
        }.onLeft {
            emitNewStateFrom(MailboxEvent.ErrorMoving)
        }
    }

    private suspend fun handleMoveToSpamAction() {
        moveSelectedMailboxItemsTo(SystemLabelId.Spam).onRight {
            emitNewStateFrom(MailboxEvent.MoveToConfirmed.Spam(it.viewMode, it.itemsMoved))
        }.onLeft {
            emitNewStateFrom(MailboxEvent.ErrorMoving)
        }
    }

    private suspend fun moveSelectedMailboxItemsTo(systemLabelId: SystemLabelId): Either<DataError, MoveResult> {
        val selectionModeDataState = state.value.mailboxListState as? MailboxListState.Data.SelectionMode
        if (selectionModeDataState == null) {
            Timber.d("MailboxListState is not in SelectionMode")
            return DataError.Local.IllegalStateError.left()
        }
        val userId = primaryUserId.filterNotNull().first()
        val viewMode = getViewModeForCurrentLocation(getSelectedMailLabelId())
        return when (viewMode) {
            ViewMode.ConversationGrouping -> moveConversations(
                userId = userId,
                conversationIds = selectionModeDataState.selectedMailboxItems.map { ConversationId(it.id) },
                systemLabelId = systemLabelId
            )

            ViewMode.NoConversationGrouping -> moveMessages(
                userId = userId,
                messageIds = selectionModeDataState.selectedMailboxItems.map { MessageId(it.id) },
                systemLabelId = systemLabelId
            )
        }.flatMap {
            MoveResult(viewMode, selectionModeDataState.selectedMailboxItems.size).right()
        }
    }

    private fun handleMoveToCompleted(action: MailboxViewAction.SignalMoveToCompleted) {
        emitNewStateFrom(
            MailboxEvent.MoveToConfirmed.Custom(
                viewMode = action.entryPoint.viewMode,
                itemCount = action.entryPoint.itemCount,
                label = action.label
            )
        )
    }

    private fun handleLabelAsCompleted(action: MailboxViewAction.SignalLabelAsCompleted) {
        emitNewStateFrom(
            MailboxEvent.LabelAsConfirmed(
                alsoArchived = action.alsoArchive,
                viewMode = action.entryPoint.viewMode,
                itemCount = action.entryPoint.itemCount
            )
        )
    }

    private suspend fun handleDeleteAction() {
        val selectionModeDataState = state.value.mailboxListState as? MailboxListState.Data.SelectionMode
        if (selectionModeDataState == null) {
            Timber.d("MailboxListState is not in SelectionMode")
            return
        }
        val event = MailboxEvent.Delete(
            viewMode = getViewModeForCurrentLocation(getSelectedMailLabelId()),
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
        val viewMode = getViewModeForCurrentLocation(getSelectedMailLabelId())
        when (viewMode) {
            ViewMode.ConversationGrouping -> {
                deleteConversations(
                    userId = userId,
                    conversationIds = selectionModeDataState.selectedMailboxItems.map { ConversationId(it.id) }
                )
            }

            ViewMode.NoConversationGrouping -> {
                deleteMessages(
                    userId = userId,
                    messageIds = selectionModeDataState.selectedMailboxItems.map { MessageId(it.id) },
                    currentLabelId = selectionModeDataState.currentMailLabel.id.labelId
                )
            }
        }.onLeft {
            emitNewStateFrom(MailboxEvent.ErrorDeleting)
        }.onRight {
            emitNewStateFrom(MailboxEvent.DeleteConfirmed(viewMode, selectionModeDataState.selectedMailboxItems.size))
        }
    }

    private fun handleClearAll() = viewModelScope.launch {
        val spamOrTrash = when (val label = observeCurrentMailLabel().first()) {
            is MailLabel.System -> when (label.systemLabelId) {
                SystemLabelId.Trash -> SpamOrTrash.Trash
                SystemLabelId.Spam -> SpamOrTrash.Spam
                else -> null
            }

            else -> null
        }
        spamOrTrash?.let { emitNewStateFrom(MailboxEvent.ClearAll(spamOrTrash = it)) }
    }

    private fun handleClearAllConfirmed(action: MailboxViewAction.ClearAllConfirmed) = viewModelScope.launch {
        deleteAllMessagesInLocation(primaryUserId.first(), getSelectedMailLabelId().labelId)
        emitNewStateFrom(action)
    }

    private fun handleSnoozeCompletedAction(action: MailboxViewAction.SnoozeDismissed) = viewModelScope.launch {
        emitNewStateFrom(action)
    }

    private suspend fun handleStarAction(viewAction: MailboxViewAction) {
        val selectionModeDataState = state.value.mailboxListState as? MailboxListState.Data.SelectionMode
        if (selectionModeDataState == null) {
            Timber.d("MailboxListState is not in SelectionMode")
            return
        }
        val userId = primaryUserId.filterNotNull().first()
        val viewMode = getViewModeForCurrentLocation(getSelectedMailLabelId())
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
        val viewMode = getViewModeForCurrentLocation(getSelectedMailLabelId())
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

    private fun observeCurrentMailLabel() = observeMailLabels()
        .map { mailLabels ->
            selectMailLabelId.selectInitialLocationIfNeeded(primaryUserId.first(), mailLabels.allById.keys)
            mailLabels.allById[getSelectedMailLabelId()]
        }

    private fun Flow<MailLabelId>.mapToExistingLabel(): Flow<MailLabel> = flatMapLatest { labelId ->
        observeMailLabels()
            .mapNotNull { mailLabels ->
                mailLabels.allById[labelId]
            }
    }

    private fun observeUnreadCounters(): Flow<List<UnreadCounter>> = primaryUserId.flatMapLatest { userId ->
        observeUnreadCounters(userId)
    }

    private fun observeMailLabels() = primaryUserId.flatMapLatest { userId ->
        observeMailLabels(userId)
    }

    private suspend fun getViewModeForCurrentLocation(currentMailLabel: MailLabelId): ViewMode {
        val userId = primaryUserId.firstOrNull()

        return if (userId == null || state.value.isInSearchMode()) {
            GetCurrentViewModeForLabel.DefaultViewMode
        } else {
            getCurrentViewModeForLabel(userId, currentMailLabel.labelId)
        }
    }

    private fun Flow<MailLabel>.pairWithCurrentLabelCount() = map { currentLabel ->
        val currentLabelCount = observeUnreadCounters().firstOrNull()
            ?.find { it.labelId == currentLabel.id.labelId }
            ?.count
        Pair(currentLabel, currentLabelCount)
    }

    private fun Flow<List<UnreadCounter>>.mapToCurrentLabelCount() = map { unreadCounters ->
        val currentMailLabelId = getSelectedMailLabelId()
        unreadCounters.find { it.labelId == currentMailLabelId.labelId }?.count
    }

    private fun emitNewStateFrom(operation: MailboxOperation) {
        val state = mailboxReducer.newStateFrom(state.value, operation)
        mutableState.value = state
    }

    // A user can be logged in but not have a valid user session, in this case the app can't function. User sessions
    // are cached, but in rare cases such as migration RUST will not have even cached the session and therefore network
    // connection is obligatory and we MUST load the session before the app can recover
    private suspend fun handleValidateUserSession() {
        if (state.value.mailboxListState is MailboxListState.Loading && !getUserHasValidSession()) {
            emitNewStateFrom(MailboxEvent.CouldNotLoadUserSession)
        }
    }

    private fun Flow<MailboxState>.observeUnreadFilterState() =
        this.map { it.unreadFilterState as? UnreadFilterState.Data }
            .mapNotNull { it?.isFilterEnabled }
            .distinctUntilChanged()

    private fun Flow<MailboxState>.observeShowSpamTrashFilterState() =
        this.map { it.showSpamTrashIncludeFilterState as? ShowSpamTrashIncludeFilterState.Data }
            .map { (it as? ShowSpamTrashIncludeFilterState.Data.Shown)?.enabled ?: false }
            .distinctUntilChanged()

    private fun observeMailLabelChangeRequests(): Flow<MailLabel> = observeSelectedMailLabelId()
        .mapToExistingLabel()
        .distinctUntilChanged()

    private fun Flow<MailboxState>.observeSearchQuery() = this.map { it.mailboxListState as? MailboxListState.Data }
        .mapNotNull { it?.searchState?.searchQuery }
        .distinctUntilChanged()

    private fun Flow<MailboxState>.observeSearchDataReady() = this.map { it.mailboxListState as? MailboxListState.Data }
        .map { it?.searchState?.hasData() ?: false }
        .distinctUntilChanged()

    private fun MailboxState.isInSearchMode() =
        this.mailboxListState is MailboxListState.Data && this.mailboxListState.searchState.isInSearch()

    private fun Flow<MailboxState>.observeSelectedMailboxItems() =
        this.map { it.mailboxListState as? MailboxListState.Data.SelectionMode }
            .mapNotNull { it?.selectedMailboxItems }
            .distinctUntilChanged()

    private suspend fun isActionAllowedForCurrentLabel(labelId: LabelId): Boolean {
        return when (val mailLabel = observeCurrentMailLabel().first()) {
            is MailLabel.System -> mailLabel.systemLabelId.labelId != labelId
            else -> true
        }
    }

    private suspend fun getFromLabelIdSearchAware(): LabelId {
        val currentLabelId = getSelectedMailLabelId().labelId
        val userId = primaryUserId.filterNotNull().first()

        val systemLabelId = when {
            !state.value.isInSearchMode() && !isSpamTrashFilterEnabled() -> return currentLabelId
            isSpamTrashFilterEnabled() || isSpamTrashFilterHidden() -> SystemLabelId.AllMail
            else -> SystemLabelId.AlmostAllMail
        }

        return findLocalSystemLabelId(userId, systemLabelId)?.labelId ?: currentLabelId
    }

    private fun isSpamTrashFilterHidden() =
        state.value.showSpamTrashIncludeFilterState is ShowSpamTrashIncludeFilterState.Data.Hidden

    private fun isSpamTrashFilterEnabled() =
        (state.value.showSpamTrashIncludeFilterState as? ShowSpamTrashIncludeFilterState.Data.Shown)?.enabled == true

    companion object {

        val initialState = MailboxState(
            mailboxListState = MailboxListState.Loading,
            topAppBarState = MailboxTopAppBarState.Loading,
            unreadFilterState = UnreadFilterState.Loading,
            showSpamTrashIncludeFilterState = ShowSpamTrashIncludeFilterState.Loading,
            bottomAppBarState = BottomBarState.Data.Hidden(
                BottomBarTarget.Mailbox,
                emptyList<ActionUiModel>().toImmutableList()
            ),
            deleteDialogState = DeleteDialogState.Hidden,
            clearAllDialogState = DeleteDialogState.Hidden,
            bottomSheetState = null,
            actionResult = Effect.empty(),
            composerNavigationState = MailboxComposerNavigationState.Enabled(),
            error = Effect.empty(),
            showRatingBooster = Effect.empty()
        )
    }
}
