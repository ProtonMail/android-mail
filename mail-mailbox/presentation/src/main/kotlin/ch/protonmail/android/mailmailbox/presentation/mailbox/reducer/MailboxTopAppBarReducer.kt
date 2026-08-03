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

import ch.protonmail.android.mailcommon.presentation.model.SelectionState
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.maillabel.presentation.text
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxEvent
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxEvent.ItemsRemovedFromSelection
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxOperation
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxTopAppBarState
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxTopAppBarState.Data.SelectionMode
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxViewAction
import javax.inject.Inject

class MailboxTopAppBarReducer @Inject constructor() {

    internal fun newStateFrom(
        currentState: MailboxTopAppBarState,
        operation: MailboxOperation.AffectingTopAppBar
    ): MailboxTopAppBarState {
        return when (operation) {
            is MailboxEvent.SelectedLabelChanged -> currentState.toNewStateForSelectedLabelChanged(operation)
            is MailboxEvent.NewLabelSelected -> currentState.toNewStateForNewLabelSelected(operation)
            is MailboxViewAction.ExitSelectionMode -> currentState.toNewStateForExitSelectionMode()
            is MailboxEvent.EnterSelectionMode -> currentState.toNewStateForEnterSelectionMode()
            is MailboxEvent.ItemClicked.ItemAddedToSelection -> currentState.toNewStateForItemAddedToSelection()
            is MailboxEvent.ItemClicked.ItemRemovedFromSelection -> currentState.toNewStateForItemRemovedFromSelection()
            is ItemsRemovedFromSelection -> currentState.toNewStateForItemsRemovedFromSelection(operation)
            is MailboxEvent.DeleteConfirmed,
            is MailboxEvent.MoveToConfirmed,
            is MailboxEvent.LabelAsConfirmed,
            is MailboxViewAction.MoveToArchive,
            is MailboxViewAction.MoveToInbox,
            is MailboxViewAction.MoveToSpam,
            MailboxViewAction.SnoozeDismissed -> currentState.toNewStateForExitSelectionMode()

            is MailboxViewAction.ExitSearchMode -> currentState.toNewStateForExitSearchMode()
            is MailboxViewAction.SearchQuery -> currentState.toSearchModeQuery(operation)
            is MailboxViewAction.EnterSearchMode -> currentState.toNewStateForEnterSearchMode()
            is MailboxEvent.PrimaryAccountAvatarChanged -> currentState.toNewStateForAccountAvatarChanged(operation)
            is MailboxEvent.AllItemsDeselected -> currentState.toNewStateForAllItemsDeselected()
            is MailboxEvent.AllItemsSelected -> currentState.toNewStateForAllItemsSelected(operation)
        }
    }

    private fun MailboxTopAppBarState.toNewStateForSelectedLabelChanged(
        operation: MailboxEvent.SelectedLabelChanged
    ): MailboxTopAppBarState.Data {
        val currentMailLabel = operation.selectedLabel
        return when (this) {
            is MailboxTopAppBarState.Loading ->
                MailboxTopAppBarState.Data.DefaultMode(currentMailLabel.text(), null)

            is MailboxTopAppBarState.Data -> this.with(currentMailLabel.text())
        }
    }

    private fun MailboxTopAppBarState.toNewStateForNewLabelSelected(
        operation: MailboxEvent.NewLabelSelected
    ): MailboxTopAppBarState.Data {
        val currentMailLabel = operation.selectedLabel
        return when (this) {
            is MailboxTopAppBarState.Loading ->
                MailboxTopAppBarState.Data.DefaultMode(currentMailLabel.text(), null)

            is MailboxTopAppBarState.Data -> this.with(currentMailLabel.text())
        }
    }

    private fun MailboxTopAppBarState.toNewStateForEnterSearchMode() = when (this) {
        is MailboxTopAppBarState.Data.DefaultMode ->
            MailboxTopAppBarState.Data.SearchMode(currentLabelName, primaryAvatarItem, "")

        else -> this
    }

    private fun MailboxTopAppBarState.toSearchModeQuery(operation: MailboxViewAction.SearchQuery) = when (this) {
        is MailboxTopAppBarState.Data.SearchMode -> this.copy(searchQuery = operation.query)
        else -> this
    }

    private fun MailboxTopAppBarState.toNewStateForExitSearchMode() = when (this) {
        is MailboxTopAppBarState.Data.SearchMode ->
            MailboxTopAppBarState.Data.DefaultMode(currentLabelName, primaryAvatarItem)

        else -> this
    }

    private fun MailboxTopAppBarState.toNewStateForEnterSelectionMode() = when (this) {

        is MailboxTopAppBarState.Loading -> this
        is MailboxTopAppBarState.Data -> if (this !is MailboxTopAppBarState.Data.SearchMode)
            SelectionMode(
                currentLabelName, primaryAvatarItem, selectedCount = 1
            ) else this
    }

    private fun MailboxTopAppBarState.toNewStateForExitSelectionMode() = when (this) {
        is MailboxTopAppBarState.Loading -> this
        is MailboxTopAppBarState.Data -> if (this !is MailboxTopAppBarState.Data.SearchMode)
            MailboxTopAppBarState.Data.DefaultMode(currentLabelName, primaryAvatarItem)
        else this
    }

    private fun MailboxTopAppBarState.toNewStateForItemAddedToSelection() = when (this) {
        is SelectionMode -> this.copy(selectedCount = this.selectedCount + 1)
        else -> this
    }

    private fun MailboxTopAppBarState.toNewStateForItemRemovedFromSelection() = when (this) {
        is SelectionMode -> this.copy(selectedCount = this.selectedCount - 1)
        else -> this
    }

    private fun MailboxTopAppBarState.toNewStateForItemsRemovedFromSelection(removedItems: ItemsRemovedFromSelection) =
        when (this) {
            is SelectionMode ->
                this.copy(selectedCount = this.selectedCount - removedItems.itemIds.size)

            else -> this
        }

    private fun MailboxTopAppBarState.toNewStateForAccountAvatarChanged(
        operation: MailboxEvent.PrimaryAccountAvatarChanged
    ) = when (this) {
        is MailboxTopAppBarState.Data.DefaultMode -> copy(primaryAvatarItem = operation.item)
        is MailboxTopAppBarState.Data.SearchMode -> copy(primaryAvatarItem = operation.item)
        is SelectionMode -> copy(primaryAvatarItem = operation.item)
        is MailboxTopAppBarState.Loading -> MailboxTopAppBarState.Data.DefaultMode(
            currentLabelName = TextUiModel(""),
            primaryAvatarItem = operation.item
        )
    }

    private fun MailboxTopAppBarState.toNewStateForAllItemsDeselected() = when (this) {
        is SelectionMode -> this.copy(selectedCount = 0)
        else -> this
    }

    private fun MailboxTopAppBarState.toNewStateForAllItemsSelected(operation: MailboxEvent.AllItemsSelected) =
        when (this) {
            is SelectionMode -> this.copy(
                selectedCount = operation.allItems.size.coerceAtMost(
                    SelectionState.MaxItemSelectionLimit
                )
            )

            else -> this
        }

    fun MailboxTopAppBarState.Data.with(currentLabelName: TextUiModel) = when (this) {
        is MailboxTopAppBarState.Data.DefaultMode -> copy(currentLabelName = currentLabelName)
        is MailboxTopAppBarState.Data.SearchMode -> copy(currentLabelName = currentLabelName)
        is SelectionMode -> copy(currentLabelName = currentLabelName)
    }
}
