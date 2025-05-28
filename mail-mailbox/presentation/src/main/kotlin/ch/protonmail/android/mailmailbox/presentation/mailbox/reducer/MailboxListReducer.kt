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
import ch.protonmail.android.mailcommon.presentation.ui.AutoDeleteBannerUiModel
import ch.protonmail.android.maillabel.domain.model.MailLabel
import ch.protonmail.android.maillabel.domain.model.MailLabelId
import ch.protonmail.android.mailmailbox.domain.model.MailboxItemId
import ch.protonmail.android.mailmailbox.domain.model.MailboxItemType
import ch.protonmail.android.mailmailbox.domain.model.OpenMailboxItemRequest
import ch.protonmail.android.mailmailbox.presentation.R
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxEvent
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxItemUiModel
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxListState
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxListState.Data.SelectionMode.SelectedMailboxItem
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxOperation
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxSearchMode
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxSearchState
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxViewAction
import ch.protonmail.android.mailsettings.domain.model.AutoDeleteSetting
import me.proton.core.mailsettings.domain.entity.ViewMode
import javax.inject.Inject

@Suppress("TooManyFunctions")
class MailboxListReducer @Inject constructor() {

    @Suppress("ComplexMethod")
    internal fun newStateFrom(
        currentState: MailboxListState,
        operation: MailboxOperation.AffectingMailboxList
    ): MailboxListState {
        return when (operation) {
            is MailboxEvent.SelectedLabelChanged -> reduceSelectedLabelChanged(operation, currentState)
            is MailboxEvent.NewLabelSelected -> reduceNewLabelSelected(operation, currentState)
            is MailboxEvent.SwipeActionsChanged -> reduceSwipeActionsChanged(operation, currentState)
            is MailboxEvent.ItemClicked.ItemDetailsOpenedInViewMode -> reduceItemDetailOpened(operation, currentState)
            is MailboxEvent.ItemClicked.OpenComposer -> reduceOpenComposer(operation, currentState)
            is MailboxEvent.EnterSelectionMode -> reduceEnterSelectionMode(operation.item, currentState)
            is MailboxEvent.ItemClicked.ItemAddedToSelection -> reduceItemAddedToSelection(operation, currentState)
            is MailboxEvent.ItemClicked.ItemRemovedFromSelection -> reduceItemRemovedFromSelection(
                operation,
                currentState
            )

            is MailboxEvent.ItemsRemovedFromSelection -> reduceItemsRemovedFromSelection(operation, currentState)

            is MailboxEvent.DeleteConfirmed,
            is MailboxViewAction.MoveToConfirmed,
            is MailboxViewAction.MoveToArchive,
            is MailboxViewAction.MoveToSpam,
            is MailboxEvent.Trash -> reduceExitSelectionMode(currentState)

            is MailboxViewAction.OnOfflineWithData -> reduceOfflineWithData(currentState)
            is MailboxViewAction.OnErrorWithData -> reduceErrorWithData(currentState)
            is MailboxViewAction.Refresh -> reduceRefresh(currentState)
            is MailboxViewAction.RefreshCompleted -> reduceRefreshCompleted(currentState)
            is MailboxViewAction.ExitSelectionMode -> reduceExitSelectionMode(currentState)
            is MailboxViewAction.MarkAsRead -> reduceMarkAsRead(currentState)
            is MailboxViewAction.MarkAsUnread -> reduceMarkAsUnread(currentState)
            is MailboxViewAction.Star -> reduceStar(currentState)
            is MailboxViewAction.UnStar -> reduceUnStar(currentState)
            is MailboxViewAction.EnterSearchMode -> reduceEnterSearchMode(currentState)
            is MailboxViewAction.SearchQuery -> reduceSearchQuery(operation, currentState)
            is MailboxViewAction.SearchResultsReady -> reduceSearchResult(operation, currentState)
            is MailboxViewAction.IncludeAllClicked -> reduceIncludeAll(currentState)
            is MailboxViewAction.ExitSearchMode -> reduceExitSearchMode(currentState)
            is MailboxEvent.ClearAllOperationStatus -> reduceClearState(operation, currentState)
            is MailboxEvent.AutoDeleteStateChanged -> reduceAutoDeleteBannerState(operation, currentState)
        }
    }

    private fun reduceEnterSearchMode(currentState: MailboxListState): MailboxListState {
        return when (currentState) {
            is MailboxListState.Data.ViewMode -> currentState.copy(
                swipingEnabled = false,
                searchState = MailboxSearchState(
                    searchMode = MailboxSearchMode.NewSearch,
                    searchQuery = "",
                    showIncludeSpamTrashButton = false,
                    isSearchingAllMail = false
                )
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

    private fun reduceSearchResult(
        operation: MailboxViewAction.SearchResultsReady,
        currentState: MailboxListState
    ): MailboxListState {
        return when (currentState) {
            is MailboxListState.Data.ViewMode -> currentState.copy(
                searchState = currentState.searchState.copy(
                    searchMode = MailboxSearchMode.SearchData,
                    showIncludeSpamTrashButton = operation.almostAllMailSetting &&
                        currentState.searchState.isSearchingAllMail.not()
                )
            )

            else -> currentState
        }
    }

    private fun reduceIncludeAll(currentState: MailboxListState): MailboxListState {
        return when (currentState) {
            is MailboxListState.Data.ViewMode -> currentState.copy(
                searchState = currentState.searchState.copy(
                    searchMode = MailboxSearchMode.SearchData,
                    showIncludeSpamTrashButton = false,
                    isSearchingAllMail = true
                )
            )

            else -> currentState
        }
    }

    private fun reduceExitSearchMode(currentState: MailboxListState): MailboxListState {
        return when (currentState) {
            is MailboxListState.Data.ViewMode -> currentState.copy(
                swipingEnabled = currentState.swipeActions?.atLastOneActionSet() ?: false,
                searchState = MailboxSearchState.NotSearching
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

        val autoDeleteBannerState = when (currentMailLabel.id) {
            MailLabelId.System.Trash -> {
                MailboxListState.Data.AutoDeleteBannerState.Visible(
                    uiModel = AutoDeleteBannerUiModel.Activate.Trash
                )
            }

            MailLabelId.System.Spam -> {
                MailboxListState.Data.AutoDeleteBannerState.Visible(
                    uiModel = AutoDeleteBannerUiModel.Activate.Spam
                )
            }

            else -> {
                MailboxListState.Data.AutoDeleteBannerState.Hidden
            }
        }

        return when (currentState) {
            is MailboxListState.Loading -> MailboxListState.Data.ViewMode(
                currentMailLabel,
                openItemEffect = Effect.empty(),
                scrollToMailboxTop = Effect.empty(),
                offlineEffect = Effect.empty(),
                refreshErrorEffect = Effect.empty(),
                refreshRequested = false,
                swipingEnabled = false,
                swipeActions = null,
                searchState = MailboxSearchState.NotSearching,
                clearState = MailboxListState.Data.ClearState.Hidden,
                autoDeleteBannerState = autoDeleteBannerState
            )

            is MailboxListState.Data.SelectionMode -> currentState.copy(
                currentMailLabel = currentMailLabel,
                clearState = MailboxListState.Data.ClearState.Hidden
            )

            is MailboxListState.Data.ViewMode -> currentState.copy(
                currentMailLabel = currentMailLabel,
                clearState = MailboxListState.Data.ClearState.Hidden
            )
        }
    }

    private fun reduceNewLabelSelected(
        operation: MailboxEvent.NewLabelSelected,
        currentState: MailboxListState
    ): MailboxListState.Data {
        val currentMailLabel = operation.selectedLabel
        return when (currentState) {
            is MailboxListState.Loading -> MailboxListState.Data.ViewMode(
                currentMailLabel,
                openItemEffect = Effect.empty(),
                scrollToMailboxTop = Effect.empty(),
                offlineEffect = Effect.empty(),
                refreshErrorEffect = Effect.empty(),
                refreshRequested = false,
                swipingEnabled = false,
                swipeActions = null,
                searchState = MailboxSearchState.NotSearching,
                clearState = MailboxListState.Data.ClearState.Hidden,
                autoDeleteBannerState = MailboxListState.Data.AutoDeleteBannerState.Hidden
            )

            is MailboxListState.Data.ViewMode -> currentState.copy(
                currentMailLabel = currentMailLabel,
                scrollToMailboxTop = Effect.of(currentMailLabel.id),
                clearState = MailboxListState.Data.ClearState.Hidden
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
                swipingEnabled = operation.swipeActionsPreference.atLastOneActionSet(),
                swipeActions = operation.swipeActionsPreference
            )

            else -> currentState
        }
    }

    private fun reduceItemDetailOpened(
        operation: MailboxEvent.ItemClicked.ItemDetailsOpenedInViewMode,
        currentState: MailboxListState
    ): MailboxListState {
        val request = when (operation.preferredViewMode) {
            ViewMode.ConversationGrouping -> {

                val currentLocation = if (currentState is MailboxListState.Data) {
                    currentState.currentMailLabel
                } else {
                    null
                }

                val destinationMessageId = when {
                    // In search mode, subItemId is set to scroll to the searched item
                    currentState is MailboxListState.Data.ViewMode &&
                        currentState.searchState.isInSearch() -> MailboxItemId(operation.item.id)

                    // Sent/AllSent folder always show items as messages, never as conversations.
                    // If the user selects a message from the list, they should be redirected to the correct message
                    // in the conversation and not to the latest in the location.
                    currentState is MailboxListState.Data.ViewMode &&
                        currentState.currentMailLabel.id in listOf(
                            MailLabelId.System.Sent,
                            MailLabelId.System.AllSent
                        )
                    -> MailboxItemId(operation.item.id)

                    else -> null
                }

                val isSentMessageWithNoAssignedConvId = operation.item.conversationId.id.isEmpty()
                if (isSentMessageWithNoAssignedConvId) {
                    OpenMailboxItemRequest(
                        itemId = MailboxItemId(operation.item.id),
                        itemType = MailboxItemType.Message,
                        shouldOpenInComposer = false,
                        subItemId = destinationMessageId,
                        filterByLocation = currentLocation
                    )
                } else {
                    OpenMailboxItemRequest(
                        itemId = MailboxItemId(operation.item.conversationId.id),
                        itemType = MailboxItemType.Conversation,
                        shouldOpenInComposer = false,
                        subItemId = destinationMessageId,
                        filterByLocation = currentLocation
                    )
                }
            }

            ViewMode.NoConversationGrouping -> {
                OpenMailboxItemRequest(
                    itemId = MailboxItemId(operation.item.id),
                    itemType = operation.item.type,
                    shouldOpenInComposer = false
                )
            }
        }

        return when (currentState) {
            is MailboxListState.Data.ViewMode -> currentState.copy(openItemEffect = Effect.of(request))
            else -> currentState
        }
    }

    private fun reduceOfflineWithData(currentState: MailboxListState) = when (currentState) {
        is MailboxListState.Data.ViewMode -> {
            if (currentState.refreshRequested) {
                currentState.copy(offlineEffect = Effect.of(Unit), refreshRequested = false)
            } else {
                currentState
            }
        }

        else -> currentState
    }

    private fun reduceRefresh(currentState: MailboxListState) = when (currentState) {
        is MailboxListState.Data.ViewMode -> currentState.copy(refreshRequested = true)
        else -> currentState
    }

    private fun reduceRefreshCompleted(currentState: MailboxListState) = when (currentState) {
        is MailboxListState.Data.ViewMode -> currentState.copy(refreshRequested = false)
        else -> currentState
    }

    private fun reduceErrorWithData(currentState: MailboxListState) = when (currentState) {
        is MailboxListState.Data.ViewMode -> {
            if (currentState.refreshRequested) {
                currentState.copy(refreshErrorEffect = Effect.of(Unit), refreshRequested = false)
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
                selectedMailboxItems = setOf(SelectedMailboxItem(item.userId, item.id, item.isRead, item.showStar)),
                swipingEnabled = false,
                swipeActions = currentState.swipeActions,
                clearState = currentState.clearState,
                searchState = currentState.searchState,
                autoDeleteBannerState = currentState.autoDeleteBannerState
            )

            else -> currentState
        }

    private fun reduceExitSelectionMode(currentState: MailboxListState) = when (currentState) {
        is MailboxListState.Data.SelectionMode -> MailboxListState.Data.ViewMode(
            currentMailLabel = currentState.currentMailLabel,
            openItemEffect = Effect.empty(),
            scrollToMailboxTop = Effect.empty(),
            offlineEffect = Effect.empty(),
            refreshErrorEffect = Effect.empty(),
            refreshRequested = false,
            swipingEnabled = currentState.swipeActions?.atLastOneActionSet() ?: false,
            swipeActions = currentState.swipeActions,
            searchState = currentState.searchState,
            clearState = currentState.clearState,
            autoDeleteBannerState = currentState.autoDeleteBannerState
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
                        operation.item.userId,
                        operation.item.id,
                        operation.item.isRead,
                        operation.item.showStar
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
            selectedMailboxItems = currentState.selectedMailboxItems
                .filterNot { operation.itemIds.contains(it.id) }
                .toSet()
        )

        else -> currentState
    }

    private fun reduceOpenComposer(operation: MailboxEvent.ItemClicked.OpenComposer, currentState: MailboxListState) =
        when (currentState) {
            is MailboxListState.Data.ViewMode -> currentState.copy(
                openItemEffect = Effect.of(
                    OpenMailboxItemRequest(
                        itemId = MailboxItemId(operation.item.id),
                        itemType = operation.item.type,
                        shouldOpenInComposer = true
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

    private fun reduceClearState(operation: MailboxEvent.ClearAllOperationStatus, currentState: MailboxListState) =
        when (currentState) {
            is MailboxListState.Data.ViewMode -> {
                if (currentState.currentMailLabel.isClearableLocation()) {
                    currentState.copy(
                        clearState = if (operation.isClearing) {
                            MailboxListState.Data.ClearState.Visible.Banner
                        } else {
                            MailboxListState.Data.ClearState.Visible.Button(
                                when (currentState.currentMailLabel.id) {
                                    MailLabelId.System.Trash -> TextUiModel(R.string.mailbox_action_button_clear_trash)
                                    MailLabelId.System.Spam -> TextUiModel(R.string.mailbox_action_button_clear_spam)
                                    else -> TextUiModel.Text("")
                                }
                            )
                        }
                    )
                } else {
                    currentState
                }
            }

            else -> currentState
        }

    private fun reduceAutoDeleteBannerState(
        operation: MailboxEvent.AutoDeleteStateChanged,
        currentState: MailboxListState
    ): MailboxListState {
        val currentMailLabel = operation.currentLabelId
        val autoDeleteBannerUiModel =
            if (currentMailLabel == MailLabelId.System.Trash || currentMailLabel == MailLabelId.System.Spam) {
                when (operation.autoDeleteSetting) {
                    AutoDeleteSetting.Disabled -> null
                    AutoDeleteSetting.Enabled -> AutoDeleteBannerUiModel.Info
                    AutoDeleteSetting.NotSet.PaidUser -> {
                        if (currentMailLabel == MailLabelId.System.Trash) {
                            AutoDeleteBannerUiModel.Activate.Trash
                        } else AutoDeleteBannerUiModel.Activate.Spam
                    }

                    AutoDeleteSetting.NotSet.FreeUser.UpsellingOff -> null
                    AutoDeleteSetting.NotSet.FreeUser.UpsellingOn -> AutoDeleteBannerUiModel.Upgrade
                }
            } else {
                null
            }

        val autoDeleteBannerState = autoDeleteBannerUiModel?.let {
            MailboxListState.Data.AutoDeleteBannerState.Visible(it)
        } ?: MailboxListState.Data.AutoDeleteBannerState.Hidden

        return when (currentState) {
            is MailboxListState.Data.ViewMode -> {
                currentState.copy(autoDeleteBannerState = autoDeleteBannerState)
            }

            is MailboxListState.Data.SelectionMode -> {
                currentState.copy(autoDeleteBannerState = autoDeleteBannerState)
            }

            else -> currentState
        }
    }

    private fun MailLabel.isClearableLocation() =
        this.id == MailLabelId.System.Trash || this.id == MailLabelId.System.Spam
}
