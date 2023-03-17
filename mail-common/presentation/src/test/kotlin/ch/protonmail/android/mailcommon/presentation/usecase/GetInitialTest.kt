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
import kotlin.test.assertNull

internal class GetInitialTest {

    private val getInitial = GetInitial()

    @Test
    fun `returns emoji if the first letter of the given string is an emoji`() {
        // Given
        val input = "\uD83D\uDC7D Test"
        val expectedResult = "\uD83D\uDC7D"
        // When
        val result = getInitial(input)
        // Then
        assertEquals(expectedResult, result)
    }

    @Test
    fun `returns first char of the given string to uppercase`() {
        // Given
        val input = "any normal string"
        val expectedResult = "A"
        // When
        val result = getInitial(input)
        // Then
        assertEquals(expectedResult, result)
    }

    @Test
    fun `returns null when given string is empty`() {
        // Given
        val input = ""
        // When
        val result = getInitial(input)
        // Then
        assertNull(result)
    }

    @Test
    fun `returns null when given string is blank`() {
        // Given
        val input = "  "
        // When
        val result = getInitial(input)
        // Then
        assertNull(result)
    }

    @Test
    fun `returns first char only when input is high surrogate not followed by another char`() {
        // Given
        val input = "\uD83D"
        val expected = "\uD83D"
        // When
        val result = getInitial(input)
        // Then
        assertEquals(expected, result)
    }
}
