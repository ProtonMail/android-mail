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

package ch.protonmail.android.mailcommon.presentation.reducer

import ch.protonmail.android.mailcommon.presentation.model.SelectableMailItem
import ch.protonmail.android.mailcommon.presentation.model.SelectionState
import ch.protonmail.android.mailcommon.presentation.model.SelectionState.SelectedMailItem
import javax.inject.Inject

class SelectionStateReducer @Inject constructor() {

    fun enterSelection(item: SelectableMailItem): SelectionState =
        SelectionState(selectedItems = setOf(item.toSelectedItem()), areAllItemsSelected = false)

    fun addToSelection(current: SelectionState, item: SelectableMailItem): SelectionState =
        current.copy(selectedItems = current.selectedItems + item.toSelectedItem())

    fun removeFromSelection(current: SelectionState, itemId: String): SelectionState =
        removeFromSelection(current, setOf(itemId))

    fun removeFromSelection(current: SelectionState, itemIds: Collection<String>): SelectionState {
        val remaining = current.selectedItems.filterNot { it.id in itemIds }.toSet()
        return current.copy(selectedItems = remaining, areAllItemsSelected = false)
    }

    /**
     * Merges [allItems] into the current selection, keeping already-selected items and topping up to
     * [SelectionState.MaxItemSelectionLimit] without introducing duplicates.
     */
    fun selectAll(current: SelectionState, allItems: List<SelectableMailItem>): SelectionState {
        val selectedIds = current.selectedItems.mapTo(HashSet()) { it.id }
        val capacity = 0.coerceAtLeast(SelectionState.MaxItemSelectionLimit - selectedIds.size)
        val additions = allItems
            .asSequence()
            .filterNot { it.id in selectedIds }
            .take(capacity)
            .map { it.toSelectedItem() }
        return current.copy(
            selectedItems = current.selectedItems + additions,
            areAllItemsSelected = true
        )
    }

    fun deselectAll(): SelectionState = SelectionState.None

    fun markRead(current: SelectionState, isRead: Boolean): SelectionState =
        current.copy(selectedItems = current.selectedItems.map { it.copy(isRead = isRead) }.toSet())

    fun markStarred(current: SelectionState, isStarred: Boolean): SelectionState =
        current.copy(selectedItems = current.selectedItems.map { it.copy(isStarred = isStarred) }.toSet())

    private fun SelectableMailItem.toSelectedItem() = SelectedMailItem(id = id, isRead = isRead, isStarred = isStarred)
}
