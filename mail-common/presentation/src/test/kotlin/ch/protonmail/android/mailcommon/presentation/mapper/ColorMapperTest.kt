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

import androidx.compose.ui.graphics.Color
import arrow.core.Either
import arrow.core.left
import arrow.core.right
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters
import kotlin.test.Test
import kotlin.test.assertEquals

@RunWith(Parameterized::class)
internal class ColorMapperTest(
    private val testName: String,
    private val input: String,
    private val expected: Either<String, Color>
) {

    @Test
    fun test() {
        println("Running $testName, Input: $input, Expected: $expected")
        assertEquals(expected, ColorMapper().toColor(input))
    }

    data class Params(
        val testName: String,
        val input: String,
        val expected: Either<String, Color>
    )

    companion object {

        @JvmStatic
        @Parameters(name = "{0}")
        fun data() = listOf(

            Params(
                testName = "success from #aarrggbb string",
                input = "#80000000",
                expected = Color.Black.copy(alpha = 0.5f).right()
            ),

            Params(
                testName = "success from aarrggbb string",
                input = "80000000",
                expected = Color.Black.copy(alpha = 0.5f).right()
            ),

            Params(
                testName = "success from #rrggbb string",
                input = "#000000",
                expected = Color.Black.right()
            ),

            Params(
                testName = "success from rrggbb string",
                input = "000000",
                expected = Color.Black.right()
            ),

            Params(
                testName = "success from #rgb string",
                input = "#000",
                expected = Color.Black.right()
            ),

            Params(
                testName = "success from rgb string",
                input = "000",
                expected = Color.Black.right()
            ),

            Params(
                testName = "success for red color",
                input = "ff0000",
                expected = Color.Red.right()
            ),

            Params(
                testName = "success for green color",
                input = "00ff00",
                expected = Color.Green.right()
            ),

            Params(
                testName = "success for blue color",
                input = "0000ff",
                expected = Color.Blue.right()
            ),

            Params(
                testName = "from invalid string",
                input = "invalid",
                expected = "invalid".left()
            )

        ).map { arrayOf(it.testName, it.input, it.expected) }
    }
}
