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
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailmailbox.domain.model.MailboxItemId
import ch.protonmail.android.mailmailbox.domain.model.OpenMailboxItemRequest
import ch.protonmail.android.mailmailbox.presentation.R
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.LoadingBarUiState
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxEvent
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxItemUiModel
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxListState
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxListState.Data.SelectionMode.SelectedMailboxItem
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxOperation
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxSearchMode
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxSearchState
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxViewAction
import ch.protonmail.android.mailmessage.domain.model.AvatarImageStates
import ch.protonmail.android.mailmessage.presentation.mapper.AvatarImageUiModelMapper
import ch.protonmail.android.mailmessage.presentation.model.AvatarImagesUiModel
import javax.inject.Inject

@Suppress("TooManyFunctions")
class MailboxListReducer @Inject constructor(
    private val avatarImageUiModelMapper: AvatarImageUiModelMapper
) {

    @Suppress("ComplexMethod")
    internal fun newStateFrom(
        currentState: MailboxListState,
        operation: MailboxOperation.AffectingMailboxList
    ): MailboxListState {
        return when (operation) {
            is MailboxEvent.SelectedLabelChanged -> reduceSelectedLabelChanged(operation, currentState)
            is MailboxEvent.NewLabelSelected -> reduceNewLabelSelected(operation, currentState)
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
            is MailboxEvent.AttachmentReadyEvent -> reduceAttachmentReady(operation, currentState)
            is MailboxEvent.AttachmentErrorEvent -> reduceAttachmentDownloadError(currentState)
            is MailboxEvent.PaginatorInvalidated -> reducePaginatorInvalidated(operation, currentState)
            is MailboxEvent.CouldNotLoadUserSession -> reduceCouldNotLoadUserSession()
            is MailboxEvent.LoadingBarStateUpdated -> reduceLoadingBarStateUpdated(operation, currentState)
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
                currentMailLabel = currentMailLabel
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
                scrollToMailboxTop = Effect.of(currentMailLabel.id)
            )

            is MailboxListState.Data.SelectionMode -> currentState.copy(
                currentMailLabel = currentMailLabel
            )
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
            locationViewModeIsConversation = operation.viewModeIsConversationGrouping
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

    private fun reduceEnterSelectionMode(item: MailboxItemUiModel, currentState: MailboxListState) =
        when (currentState) {
            is MailboxListState.Data.ViewMode -> MailboxListState.Data.SelectionMode(
                currentMailLabel = currentState.currentMailLabel,
                selectedMailboxItems = setOf(SelectedMailboxItem(item.id, item.isRead, item.isStarred)),
                swipeActions = currentState.swipeActions,
                searchState = currentState.searchState,
                avatarImagesUiModel = currentState.avatarImagesUiModel,
                shouldShowFab = false,
                areAllItemsSelected = false,
                refreshOngoing = currentState.refreshOngoing,
                loadingBarState = currentState.loadingBarState
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
    ) = when (currentState) {
        is MailboxListState.Data.SelectionMode ->
            currentState.copy(
                selectedMailboxItems = currentState.selectedMailboxItems +
                    SelectedMailboxItem(
                        operation.item.id,
                        operation.item.isRead,
                        operation.item.isStarred
                    )
            )

        else -> currentState
    }

    private fun reduceItemRemovedFromSelection(
        operation: MailboxEvent.ItemClicked.ItemRemovedFromSelection,
        currentState: MailboxListState
    ) = when (currentState) {
        is MailboxListState.Data.SelectionMode ->
            currentState.copy(
                areAllItemsSelected = false,
                selectedMailboxItems = currentState.selectedMailboxItems
                    .filterNot { it.id == operation.item.id }
                    .toSet()
            )

        else -> currentState
    }

    private fun reduceItemsRemovedFromSelection(
        operation: MailboxEvent.ItemsRemovedFromSelection,
        currentState: MailboxListState
    ) = when (currentState) {
        is MailboxListState.Data.SelectionMode -> currentState.copy(
            areAllItemsSelected = false,
            selectedMailboxItems = currentState.selectedMailboxItems
                .filterNot { operation.itemIds.contains(it.id) }
                .toSet()
        )

        else -> currentState
    }

    private fun reduceAllItemsSelected(
        operation: MailboxEvent.AllItemsSelected,
        currentState: MailboxListState
    ): MailboxListState {
        val state: MailboxListState
        when (currentState) {
            is MailboxListState.Data.SelectionMode -> {
                val selectedIds = currentState.selectedMailboxItems.map { it.id }
                val limitedSelection = currentState.selectedMailboxItems.toMutableSet()
                limitedSelection.addAll(
                    operation.allItems.let { allItems ->
                        // remove selected id's so we don't add duplicates
                        allItems.filter { !selectedIds.contains(it.id) }.take(
                            // add additional items up to our maximum supported limit
                            0.coerceAtLeast(
                                MailboxListState.maxItemSelectionLimit - selectedIds.size
                            )
                        ).map {
                            SelectedMailboxItem(
                                id = it.id,
                                isRead = it.isRead,
                                isStarred = it.isStarred
                            )
                        }
                    }
                )
                state = currentState.copy(
                    areAllItemsSelected = true,
                    selectedMailboxItems = limitedSelection
                )
            }

            else -> state = currentState
        }
        return state
    }

    private fun reduceAllItemsDeselected(currentState: MailboxListState) = when (currentState) {
        is MailboxListState.Data.SelectionMode -> currentState.copy(
            areAllItemsSelected = false,
            selectedMailboxItems = emptySet()
        )

        else -> currentState
    }

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

    private fun reduceMarkAsRead(currentState: MailboxListState) = when (currentState) {
        is MailboxListState.Data.SelectionMode -> currentState.copy(
            selectedMailboxItems = currentState.selectedMailboxItems.map { currentSelectedItem ->
                currentSelectedItem.copy(isRead = true)
            }.toSet()
        )

        else -> currentState
    }

    private fun reduceMarkAsUnread(currentState: MailboxListState) = when (currentState) {
        is MailboxListState.Data.SelectionMode -> currentState.copy(
            selectedMailboxItems = currentState.selectedMailboxItems.map { currentSelectedItem ->
                currentSelectedItem.copy(isRead = false)
            }.toSet()
        )

        else -> currentState
    }

    private fun reduceStar(currentState: MailboxListState) = when (currentState) {
        is MailboxListState.Data.SelectionMode -> currentState.copy(
            selectedMailboxItems = currentState.selectedMailboxItems.map { currentSelectedItem ->
                currentSelectedItem.copy(isStarred = true)
            }.toSet()
        )

        else -> currentState
    }

    private fun reduceUnStar(currentState: MailboxListState) = when (currentState) {
        is MailboxListState.Data.SelectionMode -> currentState.copy(
            selectedMailboxItems = currentState.selectedMailboxItems.map { currentSelectedItem ->
                currentSelectedItem.copy(isStarred = false)
            }.toSet()
        )

        else -> currentState
    }

    private fun reduceCouldNotLoadUserSession(): MailboxListState = MailboxListState.CouldNotLoadUserSession
}
