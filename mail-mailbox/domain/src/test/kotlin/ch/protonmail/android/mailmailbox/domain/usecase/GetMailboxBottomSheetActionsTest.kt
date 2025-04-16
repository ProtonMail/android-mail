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
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import javax.inject.Provider
import kotlin.test.assertEquals

class GetMailboxBottomSheetActionsTest {

    private val provideIsCustomizeToolbarEnabled = mockk<Provider<Boolean>>()

    private val sut by lazy {
        GetMailboxBottomSheetActions(
            provideIsCustomizeToolbarEnabled.get()
        )
    }

    @Test
    fun `return normal actions for non-trash or spam folders`() = runTest {
        // Given
        customizeToolbarFeatureEnabled(false)

        // When
        val actual = sut(SystemLabelId.Sent.labelId)

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
                Action.Archive
            ),
            actual
        )
    }

    @Test
    fun `return normal actions with customize toolbar action if FF enabled`() = runTest {
        // Given
        customizeToolbarFeatureEnabled(true)

        // When
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
    fun `return trash actions for toolbar FF disabled`() = runTest {
        // Given
        customizeToolbarFeatureEnabled(false)

        // When
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
                Action.Archive
            ),
            actual
        )
    }

    @Test
    fun `return spam actions for toolbar FF disabled`() = runTest {
        // Given
        customizeToolbarFeatureEnabled(false)

        // When
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
                Action.Spam,
                Action.Star,
                Action.Unstar,
                Action.Archive
            ),
            actual
        )
    }

    @Test
    fun `return trash actions with customize toolbar action for toolbar FF enabled`() = runTest {
        // Given
        customizeToolbarFeatureEnabled(true)

        // When
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
    fun `return spam actions with customize toolbar action for toolbar FF enabled`() = runTest {
        // Given
        customizeToolbarFeatureEnabled(true)

        // When
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
                Action.Spam,
                Action.Star,
                Action.Unstar,
                Action.Archive,
                Action.OpenCustomizeToolbar
            ),
            actual
        )
    }

    private fun customizeToolbarFeatureEnabled(value: Boolean) {
        every {
            provideIsCustomizeToolbarEnabled.get()
        } returns value
    }
}
