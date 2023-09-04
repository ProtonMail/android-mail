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
import ch.protonmail.android.mailmailbox.domain.model.MailboxItemId
import ch.protonmail.android.mailmailbox.domain.model.MailboxItemType
import ch.protonmail.android.mailmailbox.domain.model.OpenMailboxItemRequest
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxEvent
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxItemUiModel
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxListState
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxListState.Data.SelectionMode.SelectedMailboxItem
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxOperation
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxViewAction
import me.proton.core.mailsettings.domain.entity.ViewMode
import javax.inject.Inject

class MailboxListReducer @Inject constructor() {

    @Suppress("ComplexMethod")
    internal fun newStateFrom(
        currentState: MailboxListState,
        operation: MailboxOperation.AffectingMailboxList
    ): MailboxListState {
        return when (operation) {
            is MailboxEvent.SelectedLabelChanged -> reduceSelectedLabelChanged(operation, currentState)
            is MailboxEvent.NewLabelSelected -> reduceNewLabelSelected(operation, currentState)
            is MailboxEvent.ItemClicked.ItemDetailsOpenedInViewMode -> reduceItemDetailOpened(operation, currentState)
            is MailboxEvent.ItemClicked.OpenComposer -> reduceOpenComposer(operation, currentState)
            is MailboxEvent.SelectionModeEnabledChanged -> reduceSelectionModeEnabledChanged(operation, currentState)
            is MailboxEvent.EnterSelectionMode -> reduceEnterSelectionMode(operation.item, currentState)
            is MailboxEvent.ItemClicked.ItemAddedToSelection -> reduceItemAddedToSelection(operation, currentState)
            is MailboxEvent.ItemClicked.ItemRemovedFromSelection -> reduceItemRemovedFromSelection(
                operation,
                currentState
            )

            is MailboxViewAction.OnOfflineWithData -> reduceOfflineWithData(currentState)
            is MailboxViewAction.OnErrorWithData -> reduceErrorWithData(currentState)
            is MailboxViewAction.Refresh -> reduceRefresh(currentState)
            is MailboxViewAction.ExitSelectionMode -> reduceExitSelectionMode(currentState)
            is MailboxViewAction.MarkAsRead -> reduceMarkAsRead(currentState)
            is MailboxViewAction.MarkAsUnread -> reduceMarkAsUnread(currentState)
        }
    }

    private fun reduceSelectedLabelChanged(
        operation: MailboxEvent.SelectedLabelChanged,
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
                selectionModeEnabled = currentState.selectionModeEnabled
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
            is MailboxListState.Loading -> MailboxListState.Data.ViewMode(
                currentMailLabel,
                openItemEffect = Effect.empty(),
                scrollToMailboxTop = Effect.empty(),
                offlineEffect = Effect.empty(),
                refreshErrorEffect = Effect.empty(),
                refreshRequested = false,
                selectionModeEnabled = currentState.selectionModeEnabled
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

    private fun reduceItemDetailOpened(
        operation: MailboxEvent.ItemClicked.ItemDetailsOpenedInViewMode,
        currentState: MailboxListState
    ): MailboxListState {
        val request = when (operation.preferredViewMode) {
            ViewMode.ConversationGrouping -> {
                OpenMailboxItemRequest(
                    itemId = MailboxItemId(operation.item.conversationId.id),
                    itemType = MailboxItemType.Conversation,
                    shouldOpenInComposer = false
                )
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
                selectedMailboxItems = setOf(SelectedMailboxItem(item.id, item.isRead)),
                selectionModeEnabled = currentState.selectionModeEnabled
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
            selectionModeEnabled = currentState.selectionModeEnabled
        )

        else -> currentState
    }

    private fun reduceSelectionModeEnabledChanged(
        operation: MailboxEvent.SelectionModeEnabledChanged,
        currentState: MailboxListState
    ) = with(currentState) {
        when (this) {
            is MailboxListState.Data.SelectionMode -> copy(selectionModeEnabled = operation.selectionModeEnabled)
            is MailboxListState.Data.ViewMode -> copy(selectionModeEnabled = operation.selectionModeEnabled)
            is MailboxListState.Loading -> copy(selectionModeEnabled = operation.selectionModeEnabled)
        }
    }

    private fun reduceItemAddedToSelection(
        operation: MailboxEvent.ItemClicked.ItemAddedToSelection,
        currentState: MailboxListState
    ) = when (currentState) {
        is MailboxListState.Data.SelectionMode ->
            currentState.copy(
                selectedMailboxItems = currentState.selectedMailboxItems +
                    SelectedMailboxItem(operation.item.id, operation.item.isRead)
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
}
