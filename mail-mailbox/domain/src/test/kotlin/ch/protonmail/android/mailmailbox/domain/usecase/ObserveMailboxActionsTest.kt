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

package ch.protonmail.android.mailmailbox.domain.usecase

import app.cash.turbine.test
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.Action
import ch.protonmail.android.mailcommon.domain.sample.UserIdSample
import ch.protonmail.android.maillabel.domain.model.MailLabel
import ch.protonmail.android.maillabel.domain.model.MailLabelId
import ch.protonmail.android.mailsettings.domain.usecase.ObserveMailMessageToolbarSettings
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ObserveMailboxActionsTest {

    private val userId = UserIdSample.Primary
    private val observeToolbarActions = mockk<ObserveMailMessageToolbarSettings> {
        every { this@mockk.invoke(userId, true) } returns flowOf(null)
    }
    private val observeMailboxActions = GetMailboxActions(observeToolbarActions)

    @Test
    fun `returns default actions for non-trash or non-spam labels`() = runTest {
        // Given
        val currentMailLabel = MailLabel.System(MailLabelId.System.Inbox)
        val expected = listOf(Action.MarkUnread, Action.Trash, Action.Move, Action.Label, Action.More)

        // When
        val actions = observeMailboxActions(
            currentMailLabel, areAllItemsUnread = false, areAllItemsStarred = false, userId
        )

        // Then
        assertEquals(expected.right(), actions.first())
    }

    @Test
    fun `returns preferences when available`() = runTest {
        // Given
        val currentMailLabel = MailLabel.System(MailLabelId.System.Inbox)
        every { observeToolbarActions.invoke(userId, true) } returns flowOf(
            listOf(
                Action.Move, Action.Trash, Action.Forward
            )
        )
        val expected = listOf(Action.Move, Action.Trash, Action.Forward, Action.More)

        // When
        val actions = observeMailboxActions(
            currentMailLabel, areAllItemsUnread = false, areAllItemsStarred = false, userId
        )

        // Then
        assertEquals(expected.right(), actions.first())
    }

    @Test
    fun `returns a flow of preferences as they change`() = runTest {
        // Given
        val currentMailLabel = MailLabel.System(MailLabelId.System.Inbox)
        every { observeToolbarActions.invoke(userId, true) } returns flowOf(
            listOf(
                Action.Move, Action.Trash, Action.Forward
            ),
            listOf(
                Action.Label, Action.Star
            )
        )

        // When
        observeMailboxActions(currentMailLabel, areAllItemsUnread = false, areAllItemsStarred = false, userId).test {
            // Then
            assertEquals(listOf(Action.Move, Action.Trash, Action.Forward, Action.More).right(), awaitItem())
            assertEquals(listOf(Action.Label, Action.Star, Action.More).right(), awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `returns delete actions for trash label`() = runTest {
        // Given
        val currentMailLabel = MailLabel.System(MailLabelId.System.Trash)
        val expected = listOf(Action.MarkUnread, Action.Delete, Action.Move, Action.Label, Action.More)

        // When
        val actions = observeMailboxActions(
            currentMailLabel, areAllItemsUnread = false, areAllItemsStarred = false, userId
        )

        // Then
        assertEquals(expected.right(), actions.first())
    }

    @Test
    fun `returns delete actions for spam label`() = runTest {
        // Given
        val currentMailLabel = MailLabel.System(MailLabelId.System.Spam)
        val expected = listOf(Action.MarkUnread, Action.Delete, Action.Move, Action.Label, Action.More)

        // When
        val actions = observeMailboxActions(
            currentMailLabel, areAllItemsUnread = false, areAllItemsStarred = false, userId
        )

        // Then
        assertEquals(expected.right(), actions.first())
    }

    @Test
    fun `returns mark as read when all items are unread`() = runTest {
        // Given
        val currentMailLabel = MailLabel.System(MailLabelId.System.Inbox)
        val expected = listOf(Action.MarkRead, Action.Trash, Action.Move, Action.Label, Action.More)

        // When
        val actions = observeMailboxActions(
            currentMailLabel, areAllItemsUnread = true, areAllItemsStarred = false, userId
        )

        // Then
        assertEquals(expected.right(), actions.first())
    }

    @Test
    fun `returns delete and mark as read when all items are unread in spam`() = runTest {
        // Given
        val currentMailLabel = MailLabel.System(MailLabelId.System.Spam)
        val expected = listOf(Action.MarkRead, Action.Delete, Action.Move, Action.Label, Action.More)

        // When
        val actions = observeMailboxActions(
            currentMailLabel, areAllItemsUnread = true, areAllItemsStarred = false, userId
        )

        // Then
        assertEquals(expected.right(), actions.first())
    }

    @Test
    fun `returns delete and mark as read when all items are unread in trash`() = runTest {
        // Given
        val currentMailLabel = MailLabel.System(MailLabelId.System.Trash)
        val expected = listOf(Action.MarkRead, Action.Delete, Action.Move, Action.Label, Action.More)

        // When
        val actions = observeMailboxActions(
            currentMailLabel, areAllItemsUnread = true, areAllItemsStarred = false, userId
        )

        // Then
        assertEquals(expected.right(), actions.first())
    }

    @Test
    fun `returns unstar when all items are starred`() = runTest {
        // Given
        val currentMailLabel = MailLabel.System(MailLabelId.System.Inbox)
        every { observeToolbarActions.invoke(userId, true) } returns flowOf(
            listOf(
                Action.Star, Action.Trash, Action.Forward
            )
        )
        val expected = listOf(Action.Unstar, Action.Trash, Action.Forward, Action.More)

        // When
        val actions = observeMailboxActions(
            currentMailLabel, areAllItemsUnread = false, areAllItemsStarred = true, userId
        )

        // Then
        assertEquals(expected.right(), actions.first())
    }
}
