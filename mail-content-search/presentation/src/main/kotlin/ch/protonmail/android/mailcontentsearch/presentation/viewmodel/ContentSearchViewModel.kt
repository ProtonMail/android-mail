/*
 * Copyright (c) 2026 Proton Technologies AG
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

package ch.protonmail.android.mailcontentsearch.presentation.viewmodel

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.map
import arrow.core.Either
import ch.protonmail.android.mailattachments.domain.model.AttachmentId
import ch.protonmail.android.mailattachments.domain.model.AttachmentOpenMode
import ch.protonmail.android.mailattachments.domain.usecase.GetAttachmentIntentValues
import ch.protonmail.android.mailcommon.domain.model.Action
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcommon.presentation.mapper.ActionUiModelMapper
import ch.protonmail.android.mailcommon.presentation.model.ActionResult
import ch.protonmail.android.mailcommon.presentation.model.AvatarUiModel
import ch.protonmail.android.mailcommon.presentation.model.BottomBarState
import ch.protonmail.android.mailcommon.presentation.model.BottomBarTarget
import ch.protonmail.android.mailcommon.presentation.model.BottomSheetOperation
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailcontentsearch.presentation.model.ContentSearchOperation
import ch.protonmail.android.mailcontentsearch.presentation.model.ContentSearchResultUiModel
import ch.protonmail.android.mailcontentsearch.presentation.model.ContentSearchState
import ch.protonmail.android.mailcontentsearch.presentation.model.ContentSearchViewAction
import ch.protonmail.android.mailcontentsearch.presentation.model.isInTrashOrSpam
import ch.protonmail.android.mailcontentsearch.presentation.reducer.ContentSearchReducer
import ch.protonmail.android.maillabel.domain.model.LabelId
import ch.protonmail.android.maillabel.domain.model.MailLabelId
import ch.protonmail.android.maillabel.domain.model.SystemLabelId
import ch.protonmail.android.maillabel.domain.model.ViewMode
import ch.protonmail.android.maillabel.domain.usecase.FindLocalSystemLabelId
import ch.protonmail.android.maillabel.domain.usecase.GetCurrentViewModeForLabel
import ch.protonmail.android.maillabel.presentation.bottomsheet.LabelAsBottomSheetEntryPoint
import ch.protonmail.android.maillabel.presentation.bottomsheet.LabelAsItemId
import ch.protonmail.android.maillabel.presentation.bottomsheet.moveto.MoveToBottomSheetEntryPoint
import ch.protonmail.android.maillabel.presentation.bottomsheet.moveto.MoveToItemId
import ch.protonmail.android.maillabel.presentation.model.MailLabelText
import ch.protonmail.android.mailmailbox.domain.model.MailboxItemId
import ch.protonmail.android.mailmailbox.domain.model.MailboxItemType
import ch.protonmail.android.mailmailbox.domain.usecase.GetBottomSheetActions
import ch.protonmail.android.mailmailbox.presentation.mailbox.mapper.MailboxItemUiModelMapper
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxItemUiModel
import ch.protonmail.android.mailmailbox.presentation.mailbox.usecase.BottomBarStateFactory
import ch.protonmail.android.mailmailbox.presentation.mailbox.usecase.BulkActionMessageFactory
import ch.protonmail.android.mailmailbox.presentation.mailbox.usecase.MailboxActionExecutor
import ch.protonmail.android.mailmailbox.presentation.mailbox.usecase.UpdateIncludeFilter
import ch.protonmail.android.mailmailbox.presentation.paging.MailboxPagerFactory
import ch.protonmail.android.mailmessage.domain.usecase.HandleAvatarImageLoadingFailure
import ch.protonmail.android.mailmessage.domain.usecase.LoadAvatarImage
import ch.protonmail.android.mailmessage.domain.usecase.ObserveAvatarImageStates
import ch.protonmail.android.mailmessage.presentation.mapper.AvatarImageUiModelMapper
import ch.protonmail.android.mailmessage.presentation.model.AvatarImagesUiModel
import ch.protonmail.android.mailmessage.presentation.model.bottomsheet.LabelAsBottomSheetState
import ch.protonmail.android.mailmessage.presentation.model.bottomsheet.MailboxMoreActionsBottomSheetState
import ch.protonmail.android.mailmessage.presentation.model.bottomsheet.MoveToBottomSheetState
import ch.protonmail.android.mailmessage.presentation.reducer.BottomSheetReducer
import ch.protonmail.android.mailpagination.domain.model.IncludeFilter
import ch.protonmail.android.mailpagination.domain.usecase.ObservePageInvalidationEvents
import ch.protonmail.android.mailsession.domain.usecase.ObservePrimaryUserIdWithValidSession
import ch.protonmail.android.mailsettings.domain.model.ToolbarActionsRefreshSignal
import ch.protonmail.android.mailsettings.domain.usecase.ObserveFolderColorSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.proton.core.domain.entity.UserId
import me.proton.core.util.kotlin.DispatcherProvider
import javax.inject.Inject
import ch.protonmail.android.maillabel.presentation.R as labelR
import ch.protonmail.android.mailmailbox.presentation.R as mailboxR

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
@SuppressWarnings("LongParameterList", "TooManyFunctions")
class ContentSearchViewModel @Inject constructor(
    private val pagerFactory: MailboxPagerFactory,
    private val itemMapper: MailboxItemUiModelMapper,
    private val observePrimaryUserId: ObservePrimaryUserIdWithValidSession,
    private val observeFolderColorSettings: ObserveFolderColorSettings,
    private val getCurrentViewModeForLabel: GetCurrentViewModeForLabel,
    private val findLocalSystemLabelId: FindLocalSystemLabelId,
    private val observePageInvalidationEvents: ObservePageInvalidationEvents,
    private val mailboxActionExecutor: MailboxActionExecutor,
    private val bottomBarStateFactory: BottomBarStateFactory,
    private val getBottomSheetActions: GetBottomSheetActions,
    private val getAttachmentIntentValues: GetAttachmentIntentValues,
    private val actionUiModelMapper: ActionUiModelMapper,
    private val bulkActionMessageFactory: BulkActionMessageFactory,
    private val toolbarActionsRefreshSignal: ToolbarActionsRefreshSignal,
    private val reducer: ContentSearchReducer,
    private val bottomSheetReducer: BottomSheetReducer,
    private val updateIncludeFilter: UpdateIncludeFilter,
    private val observeAvatarImageStates: ObserveAvatarImageStates,
    private val loadAvatarImage: LoadAvatarImage,
    private val handleAvatarImageLoadingFailure: HandleAvatarImageLoadingFailure,
    private val avatarImageUiModelMapper: AvatarImageUiModelMapper,
    private val dispatchers: DispatcherProvider,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val mutableState = MutableStateFlow(ContentSearchState.Initial)
    val state: StateFlow<ContentSearchState> = mutableState.asStateFlow()

    /**
     * The search field's text, owned here rather than by the field so that the history chips can write
     * into it (see [setQuery]) without the field having to be re-created to pick up a new value.
     *
     * Only the raw text is persisted across process death — restoring the caret too isn't worth
     * depending on [TextFieldState]'s saver internals.
     */
    val queryState = TextFieldState(initialText = savedStateHandle.get<String>(KEY_QUERY).orEmpty())

    private val submittedQuery = MutableStateFlow(savedStateHandle.get<String>(KEY_SUBMITTED_QUERY).orEmpty())

    private var hasAutoFocusedSearchField = false

    private var attachmentDownloadJob: Job? = null

    private var allMailLocalLabelId: LabelId = SystemLabelId.AllMail.labelId
    private var almostAllMailLocalLabelId: LabelId = SystemLabelId.AlmostAllMail.labelId

    val items: Flow<PagingData<ContentSearchResultUiModel>> = combine(
        observePrimaryUserId().filterNotNull(),
        submittedQuery
    ) { userId, query -> userId to query }
        .distinctUntilChanged()
        .flatMapLatest { (userId, query) ->
            if (query.isBlank()) {
                mutableState.update { it.copy(query = "", phase = ContentSearchState.Phase.Idle) }
                flowOf(PagingData.empty())
            } else {
                // The displayed query is advanced here, where its search launches, so the results and
                // their highlight always describe the same query.
                mutableState.update { it.copy(query = query, phase = ContentSearchState.Phase.Loading) }
                buildSearchPagingFlow(userId, query)
            }
        }
        .cachedIn(viewModelScope)

    init {
        observePageInvalidationEvents()
            .onEach { applyOperation(ContentSearchOperation.ReloadResults) }
            .launchIn(viewModelScope)

        // Kept off the [items] flow so editing the field has these effects even while nothing is
        // collecting results (the idle history page). The field's starting value isn't an edit, so
        // dropping it keeps a restored query from being re-saved or read as the user emptying the field.
        snapshotFlow { queryState.text.toString() }
            .distinctUntilChanged()
            .drop(1)
            .onEach { text ->
                savedStateHandle[KEY_QUERY] = text
                // Emptying the field is the one edit that acts without the search key: it takes the
                // screen back to the history page and drops the search that was showing behind it.
                if (text.isBlank()) resetToIdle()
            }
            .launchIn(viewModelScope)

        // Recompute the bottom toolbar whenever the selection (and its read/starred flags), the
        // searched location, or the global toolbar-customization signal change. Results are always
        // messages, so we never branch on conversation grouping here.
        // Sender images live in one global in-memory store, so this single subscription serves both
        // the results list and the search-history rows.
        observeAvatarImageStates()
            .onEach { states ->
                applyOperation(
                    ContentSearchOperation.AvatarImagesUpdated(
                        AvatarImagesUiModel(
                            states = states.states.mapValues { (_, state) ->
                                avatarImageUiModelMapper.toUiModel(state)
                            }
                        )
                    )
                )
            }
            .launchIn(viewModelScope)

        combine(
            mutableState.map { it.selectionState.selectedItems to it.openedFromLocation }.distinctUntilChanged(),
            toolbarActionsRefreshSignal.refreshEvents.onStart { emit(Unit) }
        ) { (selectedItems, labelId), _ -> selectedItems to labelId }
            .onEach { (selectedItems, labelId) -> refreshBottomBarActions(selectedItems.isEmpty(), labelId) }
            .launchIn(viewModelScope)

        viewModelScope.launch {
            val userId = observePrimaryUserId().filterNotNull().first()
            allMailLocalLabelId = findLocalSystemLabelId(userId, SystemLabelId.AllMail)?.labelId
                ?: allMailLocalLabelId
            almostAllMailLocalLabelId = findLocalSystemLabelId(userId, SystemLabelId.AlmostAllMail)?.labelId
                ?: almostAllMailLocalLabelId
            val viewMode = getCurrentViewModeForLabel(userId, allMailLocalLabelId)
            mutableState.update {
                it.copy(
                    isConversationGrouping = viewMode == ViewMode.ConversationGrouping,
                    openedFromLocation = resolveOpenedFromLocation(it.includeFilter),
                    // Carried in state so the history can open against this account's own All Mail
                    // label rather than the default id.
                    allMailLocation = allMailLocalLabelId
                )
            }
        }
    }

    // There is no label covering "almost all mail plus one of spam/trash", so as soon as either
    // scope is included the actions on a result target All Mail.
    private fun resolveOpenedFromLocation(includeFilter: IncludeFilter): LabelId =
        if (includeFilter.includesAny) allMailLocalLabelId else almostAllMailLocalLabelId

    private suspend fun buildSearchPagingFlow(
        userId: UserId,
        query: String
    ): Flow<PagingData<ContentSearchResultUiModel>> {
        val folderColorSettings = observeFolderColorSettings(userId).first()

        return pagerFactory.create(
            userId = userId,
            selectedMailLabelId = AllMailSelection,
            type = MailboxItemType.Message,
            searchQuery = query
        ).flow.map { paging ->
            paging.map { item ->
                withContext(dispatchers.Comp) {
                    ContentSearchResultUiModel(
                        item = itemMapper.toUiModel(
                            userId = userId,
                            mailboxItem = item,
                            folderColorSettings = folderColorSettings,
                            isShowingSearchResults = true
                        ),
                        // Read while the domain item is still in hand: the mapped row keeps the location
                        // icons but not the label ids they were derived from.
                        isInTrashOrSpam = item.isInTrashOrSpam()
                    )
                }
            }
        }
    }

    /**
     * Whether the search field should take focus, and so raise the keyboard, right now. True only the
     * first time the screen is shown: coming back from a message, leaving selection mode or rotating all
     * re-create the field, and re-focusing then would cover the results the user came back to read.
     *
     * Kept here rather than in the composition because navigating to a message tears the screen's
     * composition (and any `remember`) down, while this ViewModel outlives it on the back stack.
     */
    fun consumeSearchFieldAutoFocus(): Boolean {
        if (hasAutoFocusedSearchField) return false
        hasAutoFocusedSearchField = true
        return true
    }

    fun submit(action: ContentSearchViewAction) {
        when (action) {
            ContentSearchViewAction.Search -> submitSearch(queryState.text.toString())
            is ContentSearchViewAction.SuggestionSelected -> setQuery(action.query)
            ContentSearchViewAction.ClearQuery -> clearQuery()
            ContentSearchViewAction.ToggleIncludeSpam -> toggleInclude { it.copy(includeSpam = !it.includeSpam) }
            ContentSearchViewAction.ToggleIncludeTrash -> toggleInclude { it.copy(includeTrash = !it.includeTrash) }
            is ContentSearchViewAction.ResultsLoaded -> onResultsLoaded(action.count)
            is ContentSearchViewAction.StarAction -> handleStarAction(action)
            is ContentSearchViewAction.AvatarImageLoadRequested -> action.item.participantAvatar()?.let { avatar ->
                viewModelScope.launch { loadAvatarImage(avatar.address, avatar.bimiSelector) }
            }
            is ContentSearchViewAction.AvatarImageLoadFailed -> action.item.participantAvatar()?.let { avatar ->
                viewModelScope.launch { handleAvatarImageLoadingFailure(avatar.address, avatar.bimiSelector) }
            }

            is ContentSearchViewAction.RequestAttachment -> handleRequestAttachment(action)

            is ContentSearchViewAction.ItemLongClicked -> applyOperation(
                if (mutableState.value.inSelectionMode) {
                    ContentSearchOperation.ToggleSelection(action.item)
                } else {
                    ContentSearchOperation.EnterSelectionMode(action.item)
                }
            )
            is ContentSearchViewAction.ToggleItemSelection ->
                applyOperation(ContentSearchOperation.ToggleSelection(action.item))
            ContentSearchViewAction.ExitSelectionMode -> applyOperation(ContentSearchOperation.ExitSelectionMode)
            is ContentSearchViewAction.ItemsRemovedFromSelection ->
                applyOperation(ContentSearchOperation.ItemsRemovedFromSelection(action.itemIds))

            ContentSearchViewAction.MarkAsRead -> handleMarkAsRead()
            ContentSearchViewAction.MarkAsUnread -> handleMarkAsUnread()
            ContentSearchViewAction.Star -> handleStarSelection()
            ContentSearchViewAction.UnStar -> handleUnStarSelection()
            ContentSearchViewAction.Trash -> handleMoveTo(SystemLabelId.Trash)
            ContentSearchViewAction.MoveToArchive -> handleMoveTo(SystemLabelId.Archive)
            ContentSearchViewAction.MoveToSpam -> handleMoveTo(SystemLabelId.Spam)
            ContentSearchViewAction.MoveToInbox -> handleMoveTo(SystemLabelId.Inbox)
            ContentSearchViewAction.Delete -> applyOperation(ContentSearchOperation.ShowDeleteDialog)
            ContentSearchViewAction.DeleteConfirmed -> handleDeleteConfirmed()
            ContentSearchViewAction.DeleteDialogDismissed ->
                applyOperation(ContentSearchOperation.DismissDeleteDialog)

            ContentSearchViewAction.RequestMoveToBottomSheet -> requestMoveToBottomSheet()
            ContentSearchViewAction.RequestLabelAsBottomSheet -> requestLabelAsBottomSheet()
            ContentSearchViewAction.RequestMoreActionsBottomSheet -> showMoreActionsBottomSheet()
            ContentSearchViewAction.DismissBottomSheet -> dismissBottomSheet()
            is ContentSearchViewAction.MoveToCompleted -> completeMoveToAction(action.destination, action.itemCount)
            is ContentSearchViewAction.LabelAsCompleted -> completeLabelAsAction(action.archived, action.itemCount)
        }
    }

    private fun applyOperation(operation: ContentSearchOperation) {
        mutableState.update { reducer.newStateFrom(it, operation) }
    }

    // Only participant avatars have an address to fetch an image for; group and drafts avatars do not.
    private fun MailboxItemUiModel.participantAvatar() = avatar as? AvatarUiModel.ParticipantAvatar

    private fun selectedItemIds(): List<String> = mutableState.value.selectionState.selectedItems.map { it.id }

    private suspend fun refreshBottomBarActions(selectionEmpty: Boolean, labelId: LabelId) {
        if (selectionEmpty) {
            applyOperation(
                ContentSearchOperation.BottomBarUpdated(
                    BottomBarState.Data.Hidden(BottomBarTarget.Mailbox, persistentListOf())
                )
            )
            return
        }
        val userId = observePrimaryUserId().filterNotNull().first()
        val mailboxItemIds = mutableState.value.selectionState.selectedItems.map { MailboxItemId(it.id) }
        val bottomBarState = bottomBarStateFactory.create(
            userId = userId,
            labelId = labelId,
            itemIds = mailboxItemIds,
            viewMode = MessageViewMode
        )
        applyOperation(ContentSearchOperation.BottomBarUpdated(bottomBarState))
    }

    /**
     * Downloads the tapped attachment and hands the file to the screen to open, mirroring the mailbox:
     * one download at a time, a second tap only warns instead of queueing.
     */
    private fun handleRequestAttachment(action: ContentSearchViewAction.RequestAttachment) {
        if (mutableState.value.downloadingAttachmentId != null) {
            applyOperation(ContentSearchOperation.AttachmentDownloadInProgress)
            return
        }
        applyOperation(ContentSearchOperation.AttachmentDownloadStarted(action.attachmentId))
        attachmentDownloadJob = viewModelScope.launch {
            val userId = observePrimaryUserId().filterNotNull().first()
            getAttachmentIntentValues(userId, AttachmentOpenMode.Open, AttachmentId(action.attachmentId.value))
                .onLeft { applyOperation(ContentSearchOperation.AttachmentDownloadFailed) }
                .onRight { applyOperation(ContentSearchOperation.AttachmentReady(it)) }
        }
    }

    private fun handleStarAction(action: ContentSearchViewAction.StarAction) {
        viewModelScope.launch {
            val userId = observePrimaryUserId().filterNotNull().first()
            val itemIds = listOf(action.itemId)
            if (action.isStarred) {
                mailboxActionExecutor.unStar(userId, MessageViewMode, itemIds)
            } else {
                mailboxActionExecutor.star(userId, MessageViewMode, itemIds)
            }
        }
    }

    private fun handleMarkAsRead() = runOnSelection(ContentSearchOperation.MarkSelectionAsRead) { userId, ids ->
        mailboxActionExecutor.markRead(userId, MessageViewMode, ids, mutableState.value.openedFromLocation)
    }

    private fun handleMarkAsUnread() = runOnSelection(ContentSearchOperation.MarkSelectionAsUnread) { userId, ids ->
        mailboxActionExecutor.markUnread(userId, MessageViewMode, ids, mutableState.value.openedFromLocation)
    }

    private fun handleStarSelection() = runOnSelection(ContentSearchOperation.StarSelection) { userId, ids ->
        mailboxActionExecutor.star(userId, MessageViewMode, ids)
    }

    private fun handleUnStarSelection() = runOnSelection(ContentSearchOperation.UnStarSelection) { userId, ids ->
        mailboxActionExecutor.unStar(userId, MessageViewMode, ids)
    }

    private fun runOnSelection(
        successOperation: ContentSearchOperation,
        block: suspend (UserId, List<String>) -> Either<DataError, Unit>
    ) {
        val itemIds = selectedItemIds()
        if (itemIds.isEmpty()) return
        viewModelScope.launch {
            val userId = observePrimaryUserId().filterNotNull().first()
            block(userId, itemIds).onRight { applyOperation(successOperation) }
        }
    }

    private fun handleMoveTo(systemLabelId: SystemLabelId) {
        val itemIds = selectedItemIds()
        if (itemIds.isEmpty()) return
        val itemCount = itemIds.size
        viewModelScope.launch {
            val userId = observePrimaryUserId().filterNotNull().first()
            mailboxActionExecutor.move(userId, MessageViewMode, itemIds, systemLabelId).onRight {
                val actionResult = moveActionResult(systemLabelId.title(), itemCount)
                applyOperation(ContentSearchOperation.ShowActionMessage(actionResult))
                applyOperation(ContentSearchOperation.ExitSelectionMode)
            }.onLeft {
                applyOperation(
                    ContentSearchOperation.ShowError(TextUiModel(mailboxR.string.mailbox_action_move_messages_failed))
                )
            }
        }
    }

    private fun handleDeleteConfirmed() {
        val itemIds = selectedItemIds()
        if (itemIds.isEmpty()) {
            applyOperation(ContentSearchOperation.ExitSelectionMode)
            return
        }
        val itemCount = itemIds.size
        viewModelScope.launch {
            val userId = observePrimaryUserId().filterNotNull().first()
            mailboxActionExecutor.delete(
                userId = userId,
                viewMode = MessageViewMode,
                itemIds = itemIds,
                currentLabelId = mutableState.value.openedFromLocation
            ).onRight {
                applyOperation(
                    ContentSearchOperation.ShowActionMessage(
                        bulkActionMessageFactory.deleteResult(itemCount, MessageViewMode)
                    )
                )
                applyOperation(ContentSearchOperation.ExitSelectionMode)
            }.onLeft {
                applyOperation(
                    ContentSearchOperation.ShowError(TextUiModel(mailboxR.string.mailbox_action_delete_failed))
                )
            }
        }
    }

    // Content-search results are always messages, so the move snackbar is built with the message view mode.
    private fun moveActionResult(destination: MailLabelText, itemCount: Int): ActionResult =
        bulkActionMessageFactory.moveResult(destination, itemCount, MessageViewMode)

    private fun SystemLabelId.title(): MailLabelText = when (this) {
        SystemLabelId.Trash -> MailLabelText(labelR.string.label_title_trash)
        SystemLabelId.Archive -> MailLabelText(labelR.string.label_title_archive)
        SystemLabelId.Spam -> MailLabelText(labelR.string.label_title_spam)
        else -> MailLabelText(labelR.string.label_title_inbox)
    }

    // Bottom sheets reuse the mailbox/maillabel plumbing. Results are always messages, so the entry
    // points are the message-only NoConversationGrouping selection-mode variants.
    private fun requestMoveToBottomSheet() {
        val selectedItems = mutableState.value.selectionState.selectedItems
        if (selectedItems.isEmpty()) return
        viewModelScope.launch {
            val userId = observePrimaryUserId().filterNotNull().first()
            applyBottomSheetOperation(
                MoveToBottomSheetState.MoveToBottomSheetEvent.Ready(
                    userId = userId,
                    currentLabel = mutableState.value.openedFromLocation,
                    itemIds = selectedItems.map { MoveToItemId(it.id) },
                    entryPoint = MoveToBottomSheetEntryPoint.Mailbox.SelectionMode(
                        selectedItems.size,
                        MessageViewMode
                    )
                )
            )
        }
    }

    private fun requestLabelAsBottomSheet() {
        val selectedItems = mutableState.value.selectionState.selectedItems
        if (selectedItems.isEmpty()) return
        viewModelScope.launch {
            val userId = observePrimaryUserId().filterNotNull().first()
            applyBottomSheetOperation(
                LabelAsBottomSheetState.LabelAsBottomSheetEvent.Ready(
                    userId = userId,
                    currentLabel = mutableState.value.openedFromLocation,
                    itemIds = selectedItems.map { LabelAsItemId(it.id) },
                    entryPoint = LabelAsBottomSheetEntryPoint.Mailbox.SelectionMode(
                        selectedItems.size,
                        MessageViewMode
                    )
                )
            )
        }
    }

    private fun showMoreActionsBottomSheet() {
        val selectedItems = mutableState.value.selectionState.selectedItems
        if (selectedItems.isEmpty()) return
        // Open the sheet first (the More reducer's ActionData only preserves the current visibility
        // effect, it never raises Show), then fill it once the actions load.
        applyBottomSheetOperation(BottomSheetOperation.Requested)
        viewModelScope.launch {
            val userId = observePrimaryUserId().filterNotNull().first()
            val mailboxItemIds = selectedItems.map { MailboxItemId(it.id) }
            getBottomSheetActions(
                userId,
                mutableState.value.openedFromLocation,
                mailboxItemIds,
                MessageViewMode
            ).onRight { actions ->
                applyBottomSheetOperation(
                    MailboxMoreActionsBottomSheetState.MailboxMoreActionsBottomSheetEvent.ActionData(
                        hiddenActionUiModels = actions.hiddenActions.map { actionUiModelMapper.toUiModel(it) }
                            .toImmutableList(),
                        visibleActionUiModels = actions.visibleActions.map { actionUiModelMapper.toUiModel(it) }
                            .toImmutableList(),
                        customizeToolbarActionUiModel = actionUiModelMapper.toUiModel(Action.CustomizeToolbar),
                        selectedCount = selectedItems.size
                    )
                )
            }
        }
    }

    private fun dismissBottomSheet() {
        applyBottomSheetOperation(BottomSheetOperation.Dismiss)
    }

    // Move-to / label-as are applied by the maillabel sheet's own view model; here we close the sheet,
    // surface the confirmation snackbar from the data it reports, and leave selection mode.
    private fun completeMoveToAction(destination: MailLabelText, itemCount: Int) {
        applyBottomSheetOperation(BottomSheetOperation.Dismiss)
        applyOperation(ContentSearchOperation.ShowActionMessage(moveActionResult(destination, itemCount)))
        applyOperation(ContentSearchOperation.ExitSelectionMode)
    }

    private fun completeLabelAsAction(archived: Boolean, itemCount: Int) {
        applyBottomSheetOperation(BottomSheetOperation.Dismiss)
        // Mirror the mailbox: labeling only confirms with a snackbar when it also archives (a move).
        if (archived) {
            applyOperation(
                ContentSearchOperation.ShowActionMessage(
                    moveActionResult(MailLabelText(labelR.string.label_title_archive), itemCount)
                )
            )
        }
        applyOperation(ContentSearchOperation.ExitSelectionMode)
    }

    private fun applyBottomSheetOperation(operation: BottomSheetOperation) {
        mutableState.update {
            it.copy(bottomSheetState = bottomSheetReducer.newStateFrom(it.bottomSheetState, operation))
        }
    }

    /**
     * Runs the given query, which is the only way results ever change: editing the field leaves the
     * previous results (and their highlight) alone until the user asks for the new ones.
     *
     * A blank query is not a search, there is nothing to look for, and the field is emptied via
     * [clearQuery] instead.
     */
    private fun submitSearch(query: String) {
        val trimmedQuery = query.trim()
        if (trimmedQuery.isBlank()) return
        cancelAttachmentDownload()
        // Asking for the query that is already running reloads it rather than doing nothing, which is how
        // the user retries a search that failed or has gone stale.
        if (trimmedQuery == submittedQuery.value) {
            applyOperation(ContentSearchOperation.ReloadResults)
            return
        }
        savedStateHandle[KEY_SUBMITTED_QUERY] = trimmedQuery
        submittedQuery.value = trimmedQuery
    }

    /**
     * Fills the field from a suggestion or history chip and searches it straight away: picking a term is
     * itself the user asking for its results, so it doesn't also need the search key.
     */
    private fun setQuery(query: String) {
        queryState.setTextAndPlaceCursorAtEnd(query)
        submitSearch(query)
    }

    private fun clearQuery() {
        resetToIdle()
        queryState.clearText()
    }

    private fun resetToIdle() {
        cancelAttachmentDownload()
        savedStateHandle[KEY_SUBMITTED_QUERY] = ""
        submittedQuery.value = ""
        // Reset the phase here rather than waiting for the dropped query to travel back through the
        // paging flow, so the history page doesn't flash the previous results on its way in.
        mutableState.update { it.copy(query = "", phase = ContentSearchState.Phase.Idle) }
    }

    // The row a download was started from is about to be replaced by another result set, so drop it
    // rather than pop a file viewer over results the user has moved on from.
    private fun cancelAttachmentDownload() {
        if (attachmentDownloadJob == null) return
        attachmentDownloadJob?.cancel()
        attachmentDownloadJob = null
        applyOperation(ContentSearchOperation.AttachmentDownloadCancelled)
    }

    // Spam and trash are independent scopes: each menu item flips only its own flag and the whole
    // filter is handed to the live search scroller, which re-emits the results in place.
    private fun toggleInclude(transform: (IncludeFilter) -> IncludeFilter) {
        val updatedFilter = transform(mutableState.value.includeFilter)
        mutableState.update {
            it.copy(
                includeFilter = updatedFilter,
                openedFromLocation = resolveOpenedFromLocation(updatedFilter)
            )
        }

        viewModelScope.launch {
            updateIncludeFilter(updatedFilter, MessageViewMode)
        }
    }

    private fun onResultsLoaded(count: Int) {
        mutableState.update { current ->
            val phase = when {
                current.query.isBlank() -> ContentSearchState.Phase.Idle
                else -> ContentSearchState.Phase.Results(count = count)
            }
            current.copy(phase = phase)
        }
    }

    companion object {

        private const val KEY_QUERY = "contentSearchQuery"

        private const val KEY_SUBMITTED_QUERY = "contentSearchSubmittedQuery"

        private val AllMailSelection = MailLabelId.System(SystemLabelId.AllMail.labelId)

        /** Results are always messages, so every action and snackbar runs in the message view mode. */
        private val MessageViewMode = ViewMode.NoConversationGrouping
    }
}
