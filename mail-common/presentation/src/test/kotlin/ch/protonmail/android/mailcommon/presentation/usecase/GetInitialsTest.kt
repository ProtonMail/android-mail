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

package ch.protonmail.android.mailcommon.presentation.usecase

import org.junit.Test
import kotlin.test.assertEquals

class GetInitialsTest {

    private val getInitials = GetInitials()

    @Test
    fun `get initials for empty string`() {
        // Given
        val name = ""
        val expectedInitials = ""

        // When
        val actual = getInitials(name)

        // Then
        assertEquals(actual, expectedInitials)
    }

    @Test
    fun `get initials for one word lowercase name`() {
        // Given
        val name = "aaa"
        val expectedInitials = "A"

        // When
        val actual = getInitials(name)

        // Then
        assertEquals(actual, expectedInitials)
    }

    @Test
    fun `get initials for multiple word lowercase name`() {
        // Given
        val name = "aaa bbb ccc ddd"
        val expectedInitials = "AD"

        // When
        val actual = getInitials(name)

        // Then
        assertEquals(actual, expectedInitials)
    }

    @Test
    fun `get initials for multiple word name starting with numbers`() {
        // Given
        val name = "0 1 2 3"
        val expectedInitials = "03"

        // When
        val actual = getInitials(name)

        // Then
        assertEquals(actual, expectedInitials)
    }

    @Test
    fun `get initials for multiple word name starting with non-letter, non-digit characters`() {
        // Given
        val name = "#abc !def %ghi"
        val expectedInitials = "#%"

        // When
        val actual = getInitials(name)

        // Then
        assertEquals(actual, expectedInitials)
    }

    @Test
    fun `get initials for name with emojis only`() {
        // Given
        val name = "\uD83D\uDE0A\uD83D\uDE0D"
        val expectedInitials = "\uD83D\uDE0A"

        // When
        val actual = getInitials(name)

        // Then
        assertEquals(actual, expectedInitials)
    }
}
