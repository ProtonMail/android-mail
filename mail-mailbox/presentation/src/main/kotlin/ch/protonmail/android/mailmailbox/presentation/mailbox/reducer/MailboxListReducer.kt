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

package ch.protonmail.android.mailmailbox.presentation.mailbox.reducer

import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailcommon.presentation.model.SelectionState
import ch.protonmail.android.mailcommon.presentation.reducer.SelectionStateReducer
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailmailbox.domain.model.MailboxItemId
import ch.protonmail.android.mailmailbox.domain.model.OpenMailboxItemRequest
import ch.protonmail.android.mailmailbox.presentation.R
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.LoadingBarUiState
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxEvent
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxItemUiModel
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxListState
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxOperation
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxSearchMode
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxSearchState
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxViewAction
import ch.protonmail.android.mailmessage.domain.model.AvatarImageStates
import ch.protonmail.android.mailmessage.presentation.mapper.AvatarImageUiModelMapper
import ch.protonmail.android.mailmessage.presentation.model.AvatarImagesUiModel
import javax.inject.Inject

@Suppress("TooManyFunctions", "LargeClass")
class MailboxListReducer @Inject constructor(
    private val avatarImageUiModelMapper: AvatarImageUiModelMapper,
    private val selectionStateReducer: SelectionStateReducer
) {

    @Suppress("ComplexMethod")
    internal fun newStateFrom(
        currentState: MailboxListState,
        operation: MailboxOperation.AffectingMailboxList
    ): MailboxListState {
        return when (operation) {
            is MailboxEvent.SelectedLabelChanged -> reduceSelectedLabelChanged(operation, currentState)
            is MailboxEvent.NewLabelSelected -> reduceNewLabelSelected(operation, currentState)
            is MailboxEvent.CategoryChanged -> reduceCategoryChanged(currentState)
            is MailboxEvent.SwipeActionsChanged -> reduceSwipeActionsChanged(operation, currentState)
            is MailboxEvent.ItemClicked.ItemDetailsOpened -> reduceItemDetailOpened(operation, currentState)
            is MailboxEvent.ItemClicked.OpenComposer -> reduceOpenComposer(operation, currentState)
            is MailboxEvent.EnterSelectionMode -> reduceEnterSelectionMode(operation.item, currentState)
            is MailboxEvent.ItemClicked.ItemAddedToSelection -> reduceItemAddedToSelection(operation, currentState)
            is MailboxEvent.ItemClicked.ItemRemovedFromSelection -> reduceItemRemovedFromSelection(
                operation,
                currentState
            )

            is MailboxEvent.ItemsRemovedFromSelection -> reduceItemsRemovedFromSelection(operation, currentState)
            is MailboxEvent.AllItemsSelected -> reduceAllItemsSelected(operation, currentState)
            is MailboxEvent.AllItemsDeselected -> reduceAllItemsDeselected(currentState)
            is MailboxEvent.DeleteConfirmed,
            is MailboxEvent.MoveToConfirmed,
            is MailboxEvent.LabelAsConfirmed,
            is MailboxViewAction.MoveToArchive,
            is MailboxViewAction.MoveToSpam,
            is MailboxViewAction.MoveToInbox,
            is MailboxViewAction.SnoozeDismissed -> reduceExitSelectionMode(currentState)

            is MailboxViewAction.OnOfflineWithData -> reduceOfflineWithData(currentState)
            is MailboxViewAction.OnErrorWithData -> reduceErrorWithData(currentState)
            is MailboxViewAction.Refresh -> reduceRefresh(currentState)
            is MailboxEvent.RefreshCompleted -> reduceRefreshCompleted(currentState)
            is MailboxViewAction.ExitSelectionMode -> reduceExitSelectionMode(currentState)
            is MailboxViewAction.MarkAsRead -> reduceMarkAsRead(currentState)
            is MailboxViewAction.MarkAsUnread -> reduceMarkAsUnread(currentState)
            is MailboxViewAction.Star -> reduceStar(currentState)
            is MailboxViewAction.UnStar -> reduceUnStar(currentState)
            is MailboxViewAction.EnterSearchMode -> reduceEnterSearchMode(currentState)
            is MailboxViewAction.SearchQuery -> reduceSearchQuery(operation, currentState)
            is MailboxViewAction.SearchResult -> reduceSearchResult(currentState)
            is MailboxViewAction.ExitSearchMode -> reduceExitSearchMode(currentState)
            is MailboxEvent.AvatarImageStatesUpdated -> reduceAvatarImageStatesUpdated(operation, currentState)
            is MailboxEvent.AttachmentDownloadStartedEvent -> reduceAttachmentDownloadStarted(operation, currentState)
            is MailboxEvent.AttachmentDownloadInProgressEvent -> reduceAttachmentDownloadInProgress(currentState)
            is MailboxEvent.AttachmentReadyEvent -> reduceAttachmentReady(operation, currentState)
            is MailboxEvent.AttachmentErrorEvent -> reduceAttachmentDownloadError(currentState)
            is MailboxEvent.PaginatorInvalidated -> reducePaginatorInvalidated(operation, currentState)
            is MailboxEvent.CouldNotLoadUserSession -> reduceCouldNotLoadUserSession()
            is MailboxEvent.LoadingBarStateUpdated -> reduceLoadingBarStateUpdated(operation, currentState)
            is MailboxEvent.FirstPageLoadingChanged -> reduceFirstPageLoadingChanged(operation, currentState)
        }
    }

    private fun reduceFirstPageLoadingChanged(
        operation: MailboxEvent.FirstPageLoadingChanged,
        currentState: MailboxListState
    ): MailboxListState {
        // Only the start edge matters: bump the counter so the UI can edge-detect it even through the
        // conflated StateFlow. The end edge is ignored — the skeleton is dismissed by the paging refresh
        // settling, not by this event. See ET-6553 and MailboxListState.Data.firstPageLoadingStartCount.
        if (!operation.isLoading) return currentState
        return when (currentState) {
            is MailboxListState.Data.ViewMode -> currentState.copy(
                firstPageLoadingStartCount = currentState.firstPageLoadingStartCount + 1
            )

            is MailboxListState.Data.SelectionMode -> currentState.copy(
                firstPageLoadingStartCount = currentState.firstPageLoadingStartCount + 1
            )

            else -> currentState
        }
    }

    private fun reduceLoadingBarStateUpdated(
        operation: MailboxEvent.LoadingBarStateUpdated,
        currentState: MailboxListState
    ): MailboxListState {
        return when (currentState) {
            is MailboxListState.Data.ViewMode -> currentState.copy(
                loadingBarState = operation.state
            )

            is MailboxListState.Data.SelectionMode -> currentState.copy(
                loadingBarState = operation.state
            )

            else -> currentState
        }
    }

    private fun reducePaginatorInvalidated(
        operation: MailboxEvent.PaginatorInvalidated,
        currentState: MailboxListState
    ): MailboxListState {
        return when (currentState) {
            is MailboxListState.Data.ViewMode -> currentState.copy(
                paginatorInvalidationEffect = Effect.of(operation.event)
            )

            is MailboxListState.Data.SelectionMode -> currentState.copy(
                paginatorInvalidationEffect = Effect.of(operation.event)
            )

            else -> currentState
        }
    }

    private fun reduceAttachmentDownloadStarted(
        event: MailboxEvent.AttachmentDownloadStartedEvent,
        currentState: MailboxListState
    ): MailboxListState {
        return when (currentState) {
            is MailboxListState.Data.ViewMode -> currentState.copy(
                downloadingAttachmentId = event.attachmentId
            )
            else -> currentState
        }
    }

    private fun reduceAttachmentDownloadInProgress(currentState: MailboxListState): MailboxListState {
        return when (currentState) {
            is MailboxListState.Data.ViewMode -> {
                val errorMessage = TextUiModel.TextRes(R.string.mailbox_attachment_download_in_progress)
                currentState.copy(displayAttachmentError = Effect.of(errorMessage))
            }

            else -> currentState
        }
    }

    private fun reduceAttachmentReady(
        event: MailboxEvent.AttachmentReadyEvent,
        currentState: MailboxListState
    ): MailboxListState {
        return when (currentState) {
            is MailboxListState.Data.ViewMode -> currentState.copy(
                downloadingAttachmentId = null,
                displayAttachment = Effect.of(event.openAttachmentIntentValues)
            )

            else -> currentState
        }
    }

    private fun reduceAttachmentDownloadError(currentState: MailboxListState): MailboxListState {
        return when (currentState) {
            is MailboxListState.Data.ViewMode -> currentState.copy(
                downloadingAttachmentId = null,
                displayAttachmentError = Effect.of(TextUiModel.TextRes(R.string.mailbox_attachment_download_error))
            )

            else -> currentState
        }
    }

    private fun reduceAvatarImageStatesUpdated(
        event: MailboxEvent.AvatarImageStatesUpdated,
        currentState: MailboxListState
    ): MailboxListState {
        return when (currentState) {
            is MailboxListState.Data.ViewMode -> currentState.copy(
                avatarImagesUiModel = mapAvatarImageStatesToUiModel(event.avatarImageStates)
            )

            is MailboxListState.Data.SelectionMode -> currentState.copy(
                avatarImagesUiModel = mapAvatarImageStatesToUiModel(event.avatarImageStates)
            )

            else -> currentState
        }
    }

    private fun mapAvatarImageStatesToUiModel(avatarImageStates: AvatarImageStates): AvatarImagesUiModel {
        return AvatarImagesUiModel(
            states = avatarImageStates.states.mapValues { (_, state) ->
                avatarImageUiModelMapper.toUiModel(state)
            }
        )
    }

    private fun reduceEnterSearchMode(currentState: MailboxListState): MailboxListState {
        return when (currentState) {
            is MailboxListState.Data.ViewMode -> currentState.copy(
                searchState = MailboxSearchState(
                    searchMode = MailboxSearchMode.NewSearch,
                    searchQuery = ""
                ),
                shouldShowFab = false
            )

            else -> currentState
        }
    }

    private fun reduceSearchQuery(
        operation: MailboxViewAction.SearchQuery,
        currentState: MailboxListState
    ): MailboxListState {
        return when (currentState) {
            is MailboxListState.Data.ViewMode ->
                if (currentState.searchState.searchMode == MailboxSearchMode.NewSearch)
                    currentState.copy(
                        searchState = currentState.searchState.copy(
                            searchQuery = operation.query,
                            searchMode = MailboxSearchMode.NewSearchLoading
                        )
                    )
                else
                    currentState.copy(
                        searchState = currentState.searchState.copy(
                            searchQuery = operation.query,
                            searchMode = MailboxSearchMode.SearchData
                        )
                    )

            else -> currentState
        }
    }

    private fun reduceSearchResult(currentState: MailboxListState): MailboxListState {
        return when (currentState) {
            is MailboxListState.Data.ViewMode -> currentState.copy(
                searchState = currentState.searchState.copy(
                    searchMode = MailboxSearchMode.SearchData
                )
            )

            else -> currentState
        }
    }

    private fun reduceExitSearchMode(currentState: MailboxListState): MailboxListState {
        return when (currentState) {
            is MailboxListState.Data.ViewMode -> currentState.copy(
                searchState = MailboxSearchState.NotSearching,
                shouldShowFab = true
            )

            is MailboxListState.Data.SelectionMode -> reduceExitSelectionMode(
                currentState.copy(
                    searchState = MailboxSearchState.NotSearching
                )
            )

            else -> currentState
        }
    }

    private fun reduceSelectedLabelChanged(
        operation: MailboxEvent.SelectedLabelChanged,
        currentState: MailboxListState
    ): MailboxListState.Data {
        val currentMailLabel = operation.selectedLabel
        return when (currentState) {
            is MailboxListState.CouldNotLoadUserSession,
            is MailboxListState.Loading -> MailboxListState.Data.ViewMode(
                currentMailLabel,
                openItemEffect = Effect.empty(),
                scrollToMailboxTop = Effect.empty(),
                refreshErrorEffect = Effect.empty(),
                refreshOngoing = false,
                loadingBarState = LoadingBarUiState.Hide,
                swipeActions = null,
                searchState = MailboxSearchState.NotSearching,
                shouldShowFab = true,
                avatarImagesUiModel = AvatarImagesUiModel.Empty,

                displayAttachment = Effect.empty(),
                displayAttachmentError = Effect.empty()
            )

            is MailboxListState.Data.SelectionMode -> currentState.copy(
                currentMailLabel = currentMailLabel
            )

            is MailboxListState.Data.ViewMode -> currentState.copy(
                currentMailLabel = currentMailLabel,
                downloadingAttachmentId = null
            )
        }
    }

    private fun reduceNewLabelSelected(
        operation: MailboxEvent.NewLabelSelected,
        currentState: MailboxListState
    ): MailboxListState.Data {
        val currentMailLabel = operation.selectedLabel
        return when (currentState) {
            is MailboxListState.CouldNotLoadUserSession,
            is MailboxListState.Loading -> MailboxListState.Data.ViewMode(
                currentMailLabel,
                openItemEffect = Effect.empty(),
                scrollToMailboxTop = Effect.empty(),
                refreshErrorEffect = Effect.empty(),
                refreshOngoing = false,
                loadingBarState = LoadingBarUiState.Hide,
                swipeActions = null,
                searchState = MailboxSearchState.NotSearching,
                shouldShowFab = true,
                avatarImagesUiModel = AvatarImagesUiModel.Empty,

                displayAttachment = Effect.empty(),
                displayAttachmentError = Effect.empty()
            )

            is MailboxListState.Data.ViewMode -> currentState.copy(
                currentMailLabel = currentMailLabel,
                scrollToMailboxTop = Effect.of(currentMailLabel.id),
                downloadingAttachmentId = null
            )

            is MailboxListState.Data.SelectionMode -> currentState.copy(
                currentMailLabel = currentMailLabel
            )
        }
    }

    private fun reduceCategoryChanged(currentState: MailboxListState): MailboxListState {
        return when (currentState) {
            is MailboxListState.Data.ViewMode -> currentState.copy(
                scrollToMailboxTop = Effect.of(currentState.currentMailLabel.id)
            )

            else -> currentState
        }
    }

    private fun reduceSwipeActionsChanged(
        operation: MailboxEvent.SwipeActionsChanged,
        currentState: MailboxListState
    ): MailboxListState {
        return when (currentState) {
            is MailboxListState.Data.ViewMode -> currentState.copy(
                swipeActions = operation.swipeActionsPreference
            )

            else -> currentState
        }
    }

    private fun reduceItemDetailOpened(
        operation: MailboxEvent.ItemClicked.ItemDetailsOpened,
        currentState: MailboxListState
    ): MailboxListState {
        val currentLocation = operation.contextLabel

        val request = OpenMailboxItemRequest(
            itemId = MailboxItemId(operation.item.conversationId.id),
            shouldOpenInComposer = false,
            subItemId = operation.subitemId?.let { MailboxItemId(operation.subitemId) },
            openedFromLocation = currentLocation,
            openedFromCategory = operation.openedFromCategory,
            locationViewModeIsConversation = operation.viewModeIsConversationGrouping,
            searchQuery = operation.searchQuery
        )

        return when (currentState) {
            is MailboxListState.Data.ViewMode -> currentState.copy(openItemEffect = Effect.of(request))
            else -> currentState
        }
    }

    private fun reduceOfflineWithData(currentState: MailboxListState) = when (currentState) {
        is MailboxListState.Data.ViewMode -> {
            if (currentState.refreshOngoing) {
                currentState.copy(refreshOngoing = false)
            } else {
                currentState
            }
        }

        else -> currentState
    }

    private fun reduceRefresh(currentState: MailboxListState) = when (currentState) {
        is MailboxListState.Data.ViewMode -> currentState.copy(refreshOngoing = true)
        is MailboxListState.Data.SelectionMode -> currentState.copy(refreshOngoing = true)
        else -> currentState
    }

    private fun reduceRefreshCompleted(currentState: MailboxListState) = when (currentState) {
        is MailboxListState.Data.ViewMode -> currentState.copy(refreshOngoing = false)
        is MailboxListState.Data.SelectionMode -> currentState.copy(refreshOngoing = false)
        else -> currentState
    }

    private fun reduceErrorWithData(currentState: MailboxListState) = when (currentState) {
        is MailboxListState.Data.ViewMode -> {
            if (currentState.refreshOngoing) {
                currentState.copy(refreshErrorEffect = Effect.of(Unit), refreshOngoing = false)
            } else {
                currentState
            }
        }

        else -> currentState
    }

    // Selection-set transitions are delegated to the shared SelectionStateReducer; this reducer only
    // adapts them into the mailbox's ViewMode <-> SelectionMode fork, which additionally carries the
    // FAB visibility and the attachment effects that must be reset on exit.
    private fun reduceEnterSelectionMode(item: MailboxItemUiModel, currentState: MailboxListState) =
        when (currentState) {
            is MailboxListState.Data.ViewMode -> currentState.toSelectionMode(
                selectionStateReducer.enterSelection(item)
            )

            else -> currentState
        }

    private fun reduceExitSelectionMode(currentState: MailboxListState) = when (currentState) {
        is MailboxListState.Data.SelectionMode -> MailboxListState.Data.ViewMode(
            currentMailLabel = currentState.currentMailLabel,
            openItemEffect = Effect.empty(),
            scrollToMailboxTop = Effect.empty(),
            refreshErrorEffect = Effect.empty(),
            refreshOngoing = currentState.refreshOngoing,
            loadingBarState = currentState.loadingBarState,
            swipeActions = currentState.swipeActions,
            searchState = currentState.searchState,
            shouldShowFab = !currentState.searchState.isInSearch(),
            avatarImagesUiModel = currentState.avatarImagesUiModel,
            displayAttachment = Effect.empty(),
            displayAttachmentError = Effect.empty()
        )

        else -> currentState
    }

    private fun reduceItemAddedToSelection(
        operation: MailboxEvent.ItemClicked.ItemAddedToSelection,
        currentState: MailboxListState
    ) = currentState.mapSelection { selectionStateReducer.addToSelection(it, operation.item) }

    private fun reduceItemRemovedFromSelection(
        operation: MailboxEvent.ItemClicked.ItemRemovedFromSelection,
        currentState: MailboxListState
    ) = currentState.mapSelection { selectionStateReducer.removeFromSelection(it, operation.item.id) }

    private fun reduceItemsRemovedFromSelection(
        operation: MailboxEvent.ItemsRemovedFromSelection,
        currentState: MailboxListState
    ) = currentState.mapSelection { selectionStateReducer.removeFromSelection(it, operation.itemIds) }

    private fun reduceAllItemsSelected(operation: MailboxEvent.AllItemsSelected, currentState: MailboxListState) =
        currentState.mapSelection { selectionStateReducer.selectAll(it, operation.allItems) }

    private fun reduceAllItemsDeselected(currentState: MailboxListState) =
        currentState.mapSelection { selectionStateReducer.deselectAll() }

    private fun reduceMarkAsRead(currentState: MailboxListState) =
        currentState.mapSelection { selectionStateReducer.markRead(it, isRead = true) }

    private fun reduceMarkAsUnread(currentState: MailboxListState) =
        currentState.mapSelection { selectionStateReducer.markRead(it, isRead = false) }

    private fun reduceStar(currentState: MailboxListState) =
        currentState.mapSelection { selectionStateReducer.markStarred(it, isStarred = true) }

    private fun reduceUnStar(currentState: MailboxListState) =
        currentState.mapSelection { selectionStateReducer.markStarred(it, isStarred = false) }

    private fun reduceOpenComposer(operation: MailboxEvent.ItemClicked.OpenComposer, currentState: MailboxListState) =
        when (currentState) {
            is MailboxListState.Data.ViewMode -> currentState.copy(
                openItemEffect = Effect.of(
                    OpenMailboxItemRequest(
                        itemId = MailboxItemId(operation.item.id),
                        shouldOpenInComposer = true,
                        openedFromLocation = currentState.currentMailLabel.id.labelId
                    )
                )
            )

            else -> currentState
        }

    private fun reduceCouldNotLoadUserSession(): MailboxListState = MailboxListState.CouldNotLoadUserSession
}

/**
 * Adapters between the mailbox's ViewMode <-> SelectionMode fork and the screen-agnostic
 * [SelectionState] the shared reducer operates on.
 */
private fun MailboxListState.mapSelection(transition: (SelectionState) -> SelectionState): MailboxListState =
    when (this) {
        is MailboxListState.Data.SelectionMode -> withSelection(transition(selection))
        else -> this
    }

private val MailboxListState.Data.SelectionMode.selection: SelectionState
    get() = SelectionState(selectedItems = selectedMailboxItems, areAllItemsSelected = areAllItemsSelected)

private fun MailboxListState.Data.SelectionMode.withSelection(selection: SelectionState) = copy(
    selectedMailboxItems = selection.selectedItems,
    areAllItemsSelected = selection.areAllItemsSelected
)

private fun MailboxListState.Data.ViewMode.toSelectionMode(selection: SelectionState) =
    MailboxListState.Data.SelectionMode(
        currentMailLabel = currentMailLabel,
        selectedMailboxItems = selection.selectedItems,
        swipeActions = swipeActions,
        searchState = searchState,
        avatarImagesUiModel = avatarImagesUiModel,
        shouldShowFab = false,
        areAllItemsSelected = selection.areAllItemsSelected,
        refreshOngoing = refreshOngoing,
        loadingBarState = loadingBarState
    )
