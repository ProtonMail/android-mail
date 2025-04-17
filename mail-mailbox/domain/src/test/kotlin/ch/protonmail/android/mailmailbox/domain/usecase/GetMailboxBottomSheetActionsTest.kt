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

import ch.protonmail.android.mailcommon.domain.model.Action
import ch.protonmail.android.maillabel.domain.model.SystemLabelId
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals

class GetMailboxBottomSheetActionsTest {

    private val sut by lazy {
        GetMailboxBottomSheetActions()
    }

    @Test
    fun `return normal actions for non-trash or spam folders`() = runTest {
        // Given + When
        val actual = sut(SystemLabelId.Inbox.labelId)

        // Then
        assertEquals(
            listOf(
                Action.MarkRead,
                Action.MarkUnread,
                Action.Trash,
                Action.Move,
                Action.Label,
                Action.Spam,
                Action.Star,
                Action.Unstar,
                Action.Archive,
                Action.OpenCustomizeToolbar
            ),
            actual
        )
    }

    @Test
    fun `return trash actions`() = runTest {
        // Given + When
        val actual = sut(SystemLabelId.Trash.labelId)

        // Then
        assertEquals(
            listOf(
                Action.MarkRead,
                Action.MarkUnread,
                Action.Delete,
                Action.Move,
                Action.Label,
                Action.Spam,
                Action.Star,
                Action.Unstar,
                Action.Archive,
                Action.OpenCustomizeToolbar
            ),
            actual
        )
    }

    @Test
    fun `return spam actions`() = runTest {
        // Given + When
        val actual = sut(SystemLabelId.Spam.labelId)

        // Then
        assertEquals(
            listOf(
                Action.MarkRead,
                Action.MarkUnread,
                Action.Trash,
                Action.Delete,
                Action.Move,
                Action.Label,
                Action.Star,
                Action.Unstar,
                Action.Archive,
                Action.OpenCustomizeToolbar
            ),
            actual
        )
    }
}
