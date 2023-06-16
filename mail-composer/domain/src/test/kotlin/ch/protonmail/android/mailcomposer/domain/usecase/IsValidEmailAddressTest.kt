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

package ch.protonmail.android.mailcomposer.domain.usecase

import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class IsValidEmailAddressTest {

    @Test
    fun `Empty email address should not be valid`() {
        // Given
        val emailAddress = ""

        // When
        val isValid = IsValidEmailAddress().invoke(emailAddress)

        // Then
        assertFalse(isValid)
    }

    @Test
    fun `Email address without @ should not be valid`() {
        // Given
        val emailAddress = "test"

        // When
        val isValid = IsValidEmailAddress().invoke(emailAddress)

        // Then
        assertFalse(isValid)
    }

    @Test
    fun `Email address without domain should not be valid`() {
        // Given
        val emailAddress = "test@"

        // When
        val isValid = IsValidEmailAddress().invoke(emailAddress)

        // Then
        assertFalse(isValid)
    }

    @Test
    fun `Email address without local part should not be valid`() {
        // Given
        val emailAddress = "@test.com"

        // When
        val isValid = IsValidEmailAddress().invoke(emailAddress)

        // Then
        assertFalse(isValid)
    }

    @Test
    fun `A valid email address should be valid`() {
        // Given
        val emailAddress = "a@b.c"

        // When
        val isValid = IsValidEmailAddress().invoke(emailAddress)

        // Then
        assertTrue(isValid)
    }
}
