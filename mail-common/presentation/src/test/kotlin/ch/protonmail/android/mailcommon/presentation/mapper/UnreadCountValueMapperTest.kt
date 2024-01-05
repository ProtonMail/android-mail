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

package ch.protonmail.android.mailcommon.presentation.mapper

import org.junit.Assert.assertEquals
import org.junit.Test

internal class UnreadCountValueMapperTest {

    private val mapper = UnreadCountValueMapper

    @Test
    fun `should return the value as is if it is less than 9999`() {
        // Given
        val value = 19
        val expectedString = value.toString()

        // When
        val actual = mapper.toCappedValue(value)

        // Then
        assertEquals(expectedString, actual)
    }

    @Test
    fun `should return the value as is if it equals to 9999`() {
        // Given
        val value = 9999
        val expectedString = value.toString()

        // When
        val actual = mapper.toCappedValue(value)

        // Then
        assertEquals(expectedString, actual)
    }

    @Test
    fun `should cap the value if it is greater than 9999`() {
        // Given
        val value = 10_000
        val expectedString = "9999+"

        // When
        val actual = mapper.toCappedValue(value)

        // Then
        assertEquals(expectedString, actual)
    }
}
