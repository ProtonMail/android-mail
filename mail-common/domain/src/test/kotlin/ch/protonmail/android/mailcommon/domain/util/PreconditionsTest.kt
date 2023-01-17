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

package ch.protonmail.android.mailcommon.domain.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails

class PreconditionsTest {

    @Test
    fun `require not blank throw exception when string is blank`() {
        // given
        val string = " "
        val expectedMessage = "Required value was blank."

        // when
        val exception = assertFails {
            requireNotBlank(string)
        }

        // then
        assertEquals(expectedMessage, exception.message)
    }

    @Test
    fun `require not blank throw exception when string is null`() {
        // given
        val string: String? = null
        val expectedMessage = "Required value was null."

        // when
        val exception = assertFails {
            requireNotBlank(string)
        }

        // then
        assertEquals(expectedMessage, exception.message)
    }

    @Test
    fun `require not blank throw exception with field name when string is blank`() {
        // given
        val string = " "
        val expectedMessage = "Field abc was blank."

        // when
        val exception = assertFails {
            requireNotBlank(string, fieldName = "Field abc")
        }

        // then
        assertEquals(expectedMessage, exception.message)
    }

    @Test
    fun `require not blank throw exception with field name when string is null`() {
        // given
        val string: String? = null
        val expectedMessage = "Field abc was null."

        // when
        val exception = assertFails {
            requireNotBlank(string, fieldName = "Field abc")
        }

        // then
        assertEquals(expectedMessage, exception.message)
    }

    @Test
    fun `require not empty throw exception when string is empty`() {
        // given
        val string = ""
        val expectedMessage = "Required value was empty."

        // when
        val exception = assertFails {
            requireNotEmpty(string)
        }

        // then
        assertEquals(expectedMessage, exception.message)
    }

    @Test
    fun `require not empty throw exception when string is null`() {
        // given
        val string: String? = null
        val expectedMessage = "Required value was null."

        // when
        val exception = assertFails {
            requireNotEmpty(string)
        }

        // then
        assertEquals(expectedMessage, exception.message)
    }

    @Test
    fun `require not empty throw exception with field name when string is empty`() {
        // given
        val string = ""
        val expectedMessage = "Field abc was empty."

        // when
        val exception = assertFails {
            requireNotEmpty(string, fieldName = "Field abc")
        }

        // then
        assertEquals(expectedMessage, exception.message)
    }

    @Test
    fun `require not empty throw exception with field name when string is null`() {
        // given
        val string: String? = null
        val expectedMessage = "Field abc was null."

        // when
        val exception = assertFails {
            requireNotEmpty(string, fieldName = "Field abc")
        }

        // then
        assertEquals(expectedMessage, exception.message)
    }
}
