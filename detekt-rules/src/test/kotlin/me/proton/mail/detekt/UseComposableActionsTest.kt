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

package me.proton.mail.detekt

import io.gitlab.arturbosch.detekt.test.lint
import kotlin.test.Test
import kotlin.test.assertEquals

internal class UseComposableActionsTest {

    private val rule = UseComposableActions()

    @Test
    fun `reports Composable with lambda parameters above or same as the threshold`() {
        // given
        val expected = 1
        val code = """
            @Composable
            fun SomeScreen(
                first: () -> Unit,
                second: () -> Unit,
                third: () -> Unit,
                fourth: () -> Unit
            )
        """.trimIndent()

        // when
        val findings = rule.lint(code)

        // then
        assertEquals(expected, findings.size)
    }

    @Test
    fun `does not report Composable with lambda parameters below the threshold`() {
        // given
        val expected = 0
        val code = """
            @Composable
            fun SomeScreen(
                onBack: () -> Unit,
                onNext: () -> Unit
            )
        """.trimIndent()

        // when
        val findings = rule.lint(code)

        // then
        assertEquals(expected, findings.size)
    }

    @Test
    fun `does not report not Composable with lambda parameters above or same as the threshold`() {
        // given
        val expected = 0
        val code = """
            fun NotComposable(
                first: () -> Unit,
                second: () -> Unit,
                third: () -> Unit,
                fourth: () -> Unit
            )
        """.trimIndent()

        // when
        val findings = rule.lint(code)

        // then
        assertEquals(expected, findings.size)
    }

    @Test
    fun `ignores lambda annotated as Composable`() {
        // given
        val expected = 0
        val code = """
            @Composable
            fun SomeScreen(
                first: () -> Unit,
                second: @Composable () -> Unit,
                third: @Composable () -> Unit,
                fourth: @Composable () -> Unit
            )
        """.trimIndent()

        // when
        val findings = rule.lint(code)

        // then
        assertEquals(expected, findings.size)
    }
}
