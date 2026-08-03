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
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Test

internal class SelectionStateReducerTest {

    private val reducer = SelectionStateReducer()

    private fun item(
        id: String,
        isRead: Boolean = false,
        isStarred: Boolean = false
    ): SelectableMailItem = SelectedMailItem(id = id, isRead = isRead, isStarred = isStarred)

    @Test
    fun `enter selection carries the item read and starred flags`() {
        val result = reducer.enterSelection(item("1", isRead = true, isStarred = true))

        assertEquals(setOf(SelectedMailItem("1", isRead = true, isStarred = true)), result.selectedItems)
        assertFalse(result.areAllItemsSelected)
        assertTrue(result.inSelectionMode)
    }

    @Test
    fun `add to selection appends the item`() {
        val start = reducer.enterSelection(item("1"))

        val result = reducer.addToSelection(start, item("2"))

        assertEquals(setOf("1", "2"), result.selectedItems.map { it.id }.toSet())
    }

    @Test
    fun `remove from selection drops the item and clears all-selected`() {
        val start = SelectionState(
            selectedItems = setOf(SelectedMailItem("1", isRead = false, isStarred = false)),
            areAllItemsSelected = true
        )

        val result = reducer.removeFromSelection(start, "1")

        assertTrue(result.selectedItems.isEmpty())
        assertFalse(result.areAllItemsSelected)
    }

    @Test
    fun `select all keeps existing selection and tops up without duplicates`() {
        val start = reducer.enterSelection(item("1"))
        val allItems = listOf(item("1"), item("2"), item("3"))

        val result = reducer.selectAll(start, allItems)

        assertEquals(setOf("1", "2", "3"), result.selectedItems.map { it.id }.toSet())
        assertTrue(result.areAllItemsSelected)
    }

    @Test
    fun `select all never exceeds the maximum selection limit`() {
        val allItems = (0 until SelectionState.MaxItemSelectionLimit + 25).map { item("$it") }

        val result = reducer.selectAll(SelectionState.None, allItems)

        assertEquals(SelectionState.MaxItemSelectionLimit, result.selectedItems.size)
    }

    @Test
    fun `select all tops up the remaining capacity when some are already selected`() {
        val preselected = reducer.enterSelection(item("already"))
        val allItems = (0 until SelectionState.MaxItemSelectionLimit + 25).map { item("$it") }

        val result = reducer.selectAll(preselected, allItems)

        assertEquals(SelectionState.MaxItemSelectionLimit, result.selectedItems.size)
        assertTrue(result.selectedItems.any { it.id == "already" })
    }

    @Test
    fun `mark read flips the cached read flag on every selected item`() {
        val start = reducer.addToSelection(reducer.enterSelection(item("1")), item("2"))

        val result = reducer.markRead(start, isRead = true)

        assertTrue(result.selectedItems.all { it.isRead })
    }

    @Test
    fun `mark starred flips the cached starred flag on every selected item`() {
        val start = reducer.addToSelection(reducer.enterSelection(item("1")), item("2"))

        val result = reducer.markStarred(start, isStarred = true)

        assertTrue(result.selectedItems.all { it.isStarred })
    }

    @Test
    fun `deselect all resets to none`() {
        assertEquals(SelectionState.None, reducer.deselectAll())
    }
}
