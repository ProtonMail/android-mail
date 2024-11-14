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

package ch.protonmail.android.test.ksp.processor.test

import ch.protonmail.android.test.ksp.processor.test.helpers.getGeneratedSourceFile
import ch.protonmail.android.test.ksp.processor.test.helpers.getKotlinCompilation
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCompilerApi::class)
internal class AsDslProcessingTests {

    @Test
    fun `test @AsDsl annotation processing`() {
        // GIVEN
        val kotlinSource = SourceFile.kotlin(
            "KClass.kt",
            """
                |package test.packageName
                |
                |import ch.protonmail.android.test.ksp.annotations.AsDsl
                | 
                |@AsDsl
                |internal class MyRobot
            """.trimMargin()
        )
        val targetSourceFileWithPath = "test/packageName/MyRobot\$AsDslExtension.kt"

        // WHEN
        val result = getKotlinCompilation(listOf(kotlinSource)).compile()
        val sourceCode = result.getGeneratedSourceFile(targetSourceFileWithPath).readText()

        // THEN
        assertEquals(result.exitCode, KotlinCompilation.ExitCode.OK)
        assertEquals(
            """
                |package test.packageName
                |
                |import kotlin.Unit
                |
                |internal fun myRobot(block: MyRobot.() -> Unit): MyRobot = MyRobot().apply(block)
                |
            """.trimMargin(),
            sourceCode
        )
    }

    @Test
    fun `test @AsDsl annotation processing failure when applied on an interface`() {
        // GIVEN
        val kotlinSource = SourceFile.kotlin(
            "KClass.kt",
            """
                |package test.packageName
                |
                |import ch.protonmail.android.test.ksp.annotations.AsDsl
                | 
                |@AsDsl
                |internal interface MyRobot
            """.trimMargin()
        )
        val expectedError = "Annotated object is not a class."

        // WHEN
        val result = getKotlinCompilation(listOf(kotlinSource)).compile()

        // THEN
        assertEquals(result.exitCode, KotlinCompilation.ExitCode.COMPILATION_ERROR)
        assertTrue(result.messages.contains(expectedError))
    }

    @Test
    fun `test @AsDsl annotation processing failure when applied on an abstract class`() {
        // GIVEN
        val kotlinSource = SourceFile.kotlin(
            "KClass.kt",
            """
                |package test.packageName
                |
                |import ch.protonmail.android.test.ksp.annotations.AsDsl
                | 
                |@AsDsl
                |internal abstract class MyRobot
            """.trimMargin()
        )
        val expectedError = "Annotated object cannot be an abstract class."

        // WHEN
        val result = getKotlinCompilation(listOf(kotlinSource)).compile()

        // THEN
        assertEquals(result.exitCode, KotlinCompilation.ExitCode.COMPILATION_ERROR)
        assertTrue(result.messages.contains(expectedError))
    }
}
