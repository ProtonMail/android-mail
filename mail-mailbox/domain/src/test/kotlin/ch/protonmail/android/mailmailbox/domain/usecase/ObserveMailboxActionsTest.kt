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

import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.Action
import ch.protonmail.android.maillabel.domain.model.MailLabel
import ch.protonmail.android.maillabel.domain.model.MailLabelId
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ObserveMailboxActionsTest {

    private val observeMailboxActions = GetMailboxActions()

    @Test
    fun `returns default actions for non-trash or non-spam labels`() = runTest {
        // Given
        val currentMailLabel = MailLabel.System(MailLabelId.System.Inbox)
        val expected = listOf(Action.MarkUnread, Action.Trash, Action.Move, Action.Label, Action.More)

        // When
        val actions = observeMailboxActions(currentMailLabel)

        // Then
        assertEquals(expected.right(), actions)
    }

    @Test
    fun `returns delete actions for trash label`() = runTest {
        // Given
        val currentMailLabel = MailLabel.System(MailLabelId.System.Trash)
        val expected = listOf(Action.MarkUnread, Action.Delete, Action.Move, Action.Label, Action.More)

        // When
        val actions = observeMailboxActions(currentMailLabel)

        // Then
        assertEquals(expected.right(), actions)
    }

    @Test
    fun `returns delete actions for spam label`() = runTest {
        // Given
        val currentMailLabel = MailLabel.System(MailLabelId.System.Spam)
        val expected = listOf(Action.MarkUnread, Action.Delete, Action.Move, Action.Label, Action.More)

        // When
        val actions = observeMailboxActions(currentMailLabel)

        // Then
        assertEquals(expected.right(), actions)
    }
}
