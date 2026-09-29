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

package ch.protonmail.android.navigation

import ch.protonmail.android.navigation.model.Destination.Screen
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class NpsTriggerTest {

    @Test
    fun `given the message reader was open, when the user goes back to the mailbox, then NPS is triggered`() {
        // When
        val result = isReturnToMailboxFromReader(Screen.Conversation.route, Screen.Mailbox.route)

        // Then
        assertTrue(result)
    }

    @Test
    fun `given the app just started, when the mailbox is shown, then NPS is not triggered`() {
        // When
        val result = isReturnToMailboxFromReader(null, Screen.Mailbox.route)

        // Then
        assertFalse(result)
    }

    @Test
    fun `given another screen was open, when the user goes back to the mailbox, then NPS is not triggered`() {
        // When
        val result = isReturnToMailboxFromReader(Screen.ContentSearch.route, Screen.Mailbox.route)

        // Then
        assertFalse(result)
    }

    @Test
    fun `given the mailbox was open, when the user opens the message reader, then NPS is not triggered`() {
        // When
        val result = isReturnToMailboxFromReader(Screen.Mailbox.route, Screen.Conversation.route)

        // Then
        assertFalse(result)
    }
}
