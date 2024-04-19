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

package ch.protonmail.upselling.presentation.extension

import ch.protonmail.android.mailupselling.presentation.extension.toDecimalString
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.test.assertEquals

@RunWith(Parameterized::class)
class FloatExtensionTest(private val testInput: TestInput) {

    @Test
    fun `should format to the expected string value`() = with(testInput) {
        // When
        val actual = toFormat.toDecimalString()

        // Then
        assertEquals(expected, actual)
    }

    companion object {

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data() = arrayOf(
            TestInput(
                toFormat = 10.00f,
                expected = "10"
            ),
            TestInput(
                toFormat = 10.01f,
                expected = "10.01"
            ),
            TestInput(
                toFormat = 10.123123f,
                expected = "10.12"
            ),
            TestInput(
                toFormat = 10.127123f,
                expected = "10.13"
            ),
            TestInput(
                toFormat = 10f,
                expected = "10"
            )
        )
    }

    data class TestInput(
        val toFormat: Float,
        val expected: String
    )
}
