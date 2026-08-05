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

package ch.protonmail.android.mailcontentsearch.presentation.reducer

import ch.protonmail.android.mailattachments.presentation.reducer.AttachmentDownloadReducer
import ch.protonmail.android.mailcommon.presentation.model.BottomBarState
import ch.protonmail.android.mailcommon.presentation.model.BottomBarTarget
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailcommon.presentation.reducer.SelectionStateReducer
import ch.protonmail.android.mailcommon.presentation.model.SelectionState
import ch.protonmail.android.mailcontentsearch.presentation.model.ContentSearchOperation
import ch.protonmail.android.mailcontentsearch.presentation.model.ContentSearchState
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxItemUiModel
import ch.protonmail.android.mailmailbox.presentation.mailbox.usecase.BulkActionMessageFactory
import ch.protonmail.android.mailmessage.presentation.mapper.MailLabelTextMapper
import io.mockk.every
import io.mockk.mockk
import kotlinx.collections.immutable.persistentListOf
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

internal class ContentSearchReducerTest {

    // The max-selection message is a plain string, so the label mapper is never consulted here.
    private val reducer = ContentSearchReducer(
        SelectionStateReducer(),
        AttachmentDownloadReducer(),
        BulkActionMessageFactory(mockk<MailLabelTextMapper>())
    )

    private fun item(
        id: String,
        isRead: Boolean = false,
        isStarred: Boolean = false
    ): MailboxItemUiModel = mockk {
        every { this@mockk.id } returns id
        every { this@mockk.isRead } returns isRead
        every { this@mockk.isStarred } returns isStarred
    }

    @Test
    fun `enter selection mode selects the long-clicked item`() {
        val result = reducer.newStateFrom(
            ContentSearchState.Initial,
            ContentSearchOperation.EnterSelectionMode(item("1", isRead = true, isStarred = true))
        )

        assertTrue(result.inSelectionMode)
        assertEquals(setOf("1"), result.selectionState.selectedItems.map { it.id }.toSet())
        assertTrue(result.selectionState.selectedItems.single().isRead)
        assertTrue(result.selectionState.selectedItems.single().isStarred)
    }

    @Test
    fun `toggling an unselected item adds it`() {
        val start = reducer.newStateFrom(
            ContentSearchState.Initial,
            ContentSearchOperation.EnterSelectionMode(item("1"))
        )

        val result = reducer.newStateFrom(start, ContentSearchOperation.ToggleSelection(item("2")))

        assertEquals(setOf("1", "2"), result.selectionState.selectedItems.map { it.id }.toSet())
    }

    @Test
    fun `toggling the last selected item leaves selection mode`() {
        val start = reducer.newStateFrom(
            ContentSearchState.Initial,
            ContentSearchOperation.EnterSelectionMode(item("1"))
        )

        val result = reducer.newStateFrom(start, ContentSearchOperation.ToggleSelection(item("1")))

        assertFalse(result.inSelectionMode)
    }

    @Test
    fun `exit selection clears items and dismisses delete dialog`() {
        val start = reducer
            .newStateFrom(ContentSearchState.Initial, ContentSearchOperation.EnterSelectionMode(item("1")))
            .let { reducer.newStateFrom(it, ContentSearchOperation.ShowDeleteDialog) }

        val result = reducer.newStateFrom(start, ContentSearchOperation.ExitSelectionMode)

        assertFalse(result.inSelectionMode)
        assertFalse(result.showDeleteDialog)
    }

    @Test
    fun `mark selection as read flips the cached read flag without exiting`() {
        val start = reducer.newStateFrom(
            ContentSearchState.Initial,
            ContentSearchOperation.EnterSelectionMode(item("1", isRead = false))
        )

        val result = reducer.newStateFrom(start, ContentSearchOperation.MarkSelectionAsRead)

        assertTrue(result.inSelectionMode)
        assertTrue(result.selectionState.selectedItems.all { it.isRead })
    }

    @Test
    fun `items removed from selection are dropped`() {
        val start = reducer
            .newStateFrom(ContentSearchState.Initial, ContentSearchOperation.EnterSelectionMode(item("1")))
            .let { reducer.newStateFrom(it, ContentSearchOperation.ToggleSelection(item("2"))) }

        val result = reducer.newStateFrom(start, ContentSearchOperation.ItemsRemovedFromSelection(listOf("1")))

        assertEquals(setOf("2"), result.selectionState.selectedItems.map { it.id }.toSet())
    }

    @Test
    fun `toggling past the selection limit refuses the item and reports the cap`() {
        val atLimit = ContentSearchState.Initial.copy(
            selectionState = SelectionState(
                selectedItems = (1..SelectionState.MaxItemSelectionLimit).map {
                    SelectionState.SelectedMailItem(id = "$it", isRead = false, isStarred = false)
                }.toSet(),
                areAllItemsSelected = false
            )
        )

        val result = reducer.newStateFrom(atLimit, ContentSearchOperation.ToggleSelection(item("overflow")))

        assertEquals(SelectionState.MaxItemSelectionLimit, result.selectionState.selectedItems.size)
        assertFalse(result.selectionState.selectedItems.any { it.id == "overflow" })
        assertNotNull(result.actionMessage.consume())
    }

    @Test
    fun `toggling an already selected item still works at the limit`() {
        val selected = (1..SelectionState.MaxItemSelectionLimit).map {
            SelectionState.SelectedMailItem(id = "$it", isRead = false, isStarred = false)
        }.toSet()
        val atLimit = ContentSearchState.Initial.copy(
            selectionState = SelectionState(selectedItems = selected, areAllItemsSelected = false)
        )

        val result = reducer.newStateFrom(atLimit, ContentSearchOperation.ToggleSelection(item("1")))

        assertEquals(SelectionState.MaxItemSelectionLimit - 1, result.selectionState.selectedItems.size)
    }

    @Test
    fun `show error surfaces the message`() {
        val message = TextUiModel("Move operation failed")

        val result = reducer.newStateFrom(ContentSearchState.Initial, ContentSearchOperation.ShowError(message))

        assertEquals(message, result.errorMessage.consume())
    }

    @Test
    fun `bottom bar updated replaces the bottom bar state`() {
        val shown = BottomBarState.Data.Shown(BottomBarTarget.Mailbox, persistentListOf())

        val result = reducer.newStateFrom(ContentSearchState.Initial, ContentSearchOperation.BottomBarUpdated(shown))

        assertEquals(shown, result.bottomBarState)
    }
}
