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

package ch.protonmail.android.mailcommon.presentation.model

import androidx.compose.runtime.Stable

/**
 * Screen-agnostic multi-select state for a list of [SelectableMailItem]s. The transition logic lives in
 * [ch.protonmail.android.mailcommon.presentation.reducer.SelectionStateReducer] so any selection-capable
 * screen (mailbox, content search, …) reuses it instead of re-implementing it. Each screen embeds (or
 * maps) this value into its own state container; nothing here knows about a specific screen.
 */
@Stable
data class SelectionState(
    val selectedItems: Set<SelectedMailItem>,
    val areAllItemsSelected: Boolean
) {

    val inSelectionMode: Boolean get() = selectedItems.isNotEmpty()

    /**
     * Whether no further item can be added.
     * [ch.protonmail.android.mailcommon.presentation.reducer.SelectionStateReducer.selectAll] already tops
     * up only to [MaxItemSelectionLimit]; screens adding one item at a time check this so they can say why.
     */
    val isAtLimit: Boolean get() = selectedItems.size >= MaxItemSelectionLimit

    data class SelectedMailItem(
        override val id: String,
        override val isRead: Boolean,
        override val isStarred: Boolean
    ) : SelectableMailItem

    companion object {

        const val MaxItemSelectionLimit = 100

        val None = SelectionState(selectedItems = emptySet(), areAllItemsSelected = false)
    }
}
