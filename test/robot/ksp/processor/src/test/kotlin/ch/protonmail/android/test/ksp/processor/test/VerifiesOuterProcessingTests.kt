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
internal class VerifiesOuterProcessingTests {

    @Test
    fun `test @VerifiesOuter annotation processing, happy path`() {
        // GIVEN
        val sectionSource = SourceFile.kotlin(
            "Section.kt",
            """
                |package test.packageName.section
                |
                |import ch.protonmail.android.test.robot.ProtonMailSectionRobot
                |import ch.protonmail.android.test.ksp.annotations.VerifiesOuter
                |
                |internal class Section : ProtonMailSectionRobot {
                |
                |    @VerifiesOuter
                |    inner class Verify {
                |        fun someMethod() = Unit
                |    }
                |}
            """.trimMargin()
        )

        val targetSourceFileWithPath = "test/packageName/section/Section\$Verify\$VerifyOuterExtension.kt"

        // WHEN
        val result = getKotlinCompilation(listOf(sectionSource)).compile()
        val sourceCode = result.getGeneratedSourceFile(targetSourceFileWithPath).readText()

        // THEN
        assertEquals(result.exitCode, KotlinCompilation.ExitCode.OK)
        assertEquals(
            """
                |package test.packageName.section
                |
                |import kotlin.Unit
                |
                |internal fun Section.verify(block: Section.Verify.() -> Unit): Section {
                |  Verify().apply(block)
                |  return Section()
                |}
                |
            """.trimMargin(),
            sourceCode
        )
    }

    @Test
    fun `test @VerifiesOuter annotation processing, outer class is abstract`() {
        // GIVEN
        val sectionSource = SourceFile.kotlin(
            "Section.kt",
            """
                |package test.packageName.section
                |
                |import ch.protonmail.android.test.robot.ProtonMailSectionRobot
                |import ch.protonmail.android.test.ksp.annotations.VerifiesOuter
                |
                |internal abstract class Section : ProtonMailSectionRobot {
                |
                |    @VerifiesOuter
                |    inner class Verify {
                |        fun someMethod() = Unit
                |    }
                |}
            """.trimMargin()
        )

        val targetSourceFileWithPath = "test/packageName/section/Section\$Verify\$VerifyOuterExtension.kt"

        // WHEN
        val result = getKotlinCompilation(listOf(sectionSource)).compile()
        val sourceCode = result.getGeneratedSourceFile(targetSourceFileWithPath).readText()

        // THEN
        assertEquals(result.exitCode, KotlinCompilation.ExitCode.OK)
        assertEquals(
            """
                |package test.packageName.section
                |
                |import kotlin.Unit
                |
                |internal fun Section.verify(block: Section.Verify.() -> Unit) {
                |  Verify().apply(block)
                |}
                |
            """.trimMargin(),
            sourceCode
        )
    }

    @Test
    fun `test @VerifiesOuter annotation processing failure when applied on an abstract class`() {
        // GIVEN
        val sectionSource = SourceFile.kotlin(
            "Section.kt",
            """
                |package test.packageName.section
                |
                |import ch.protonmail.android.test.robot.ProtonMailSectionRobot
                |import ch.protonmail.android.test.ksp.annotations.VerifiesOuter
                |
                |internal class Section : ProtonMailSectionRobot {
                |
                |    @VerifiesOuter
                |    abstract inner class Verify {
                |        abstract fun someMethod()
                |    }
                |}
            """.trimMargin()
        )
        val expectedError = "Annotated item cannot be an abstract class."

        // WHEN
        val result = getKotlinCompilation(listOf(sectionSource)).compile()

        // THEN
        assertEquals(result.exitCode, KotlinCompilation.ExitCode.COMPILATION_ERROR)
        assertTrue(result.messages.contains(expectedError))
    }

    @Test
    fun `test @VerifiesOuter annotation processing failure when applied on an interface`() {
        // GIVEN
        val sectionSource = SourceFile.kotlin(
            "Section.kt",
            """
                |package test.packageName.section
                |
                |import ch.protonmail.android.test.robot.ProtonMailSectionRobot
                |import ch.protonmail.android.test.ksp.annotations.VerifiesOuter
                |
                |internal class Section : ProtonMailSectionRobot {
                |
                |    @VerifiesOuter
                |    inner interface Verify {
                |        fun someMethod()
                |    }
                |}
            """.trimMargin()
        )
        val expectedError = "Target object is not a class."

        // WHEN
        val result = getKotlinCompilation(listOf(sectionSource)).compile()

        // THEN
        assertEquals(result.exitCode, KotlinCompilation.ExitCode.COMPILATION_ERROR)
        assertTrue(result.messages.contains(expectedError))
    }

    @Test
    fun `test @VerifiesOuter annotation processing failure when applied on an item without outer class`() {
        // GIVEN
        val sectionSource = SourceFile.kotlin(
            "Section.kt",
            """
                |package test.packageName.section
                |
                |import ch.protonmail.android.test.robot.ProtonMailSectionRobot
                |import ch.protonmail.android.test.ksp.annotations.VerifiesOuter
                |
                |internal class Section : ProtonMailSectionRobot {
                |
                |    @VerifiesOuter
                |    class Verify {
                |        fun someMethod() = Unit
                |    }
                |}
            """.trimMargin()
        )
        val expectedError = "Annotated item has no outer class."

        // WHEN
        val result = getKotlinCompilation(listOf(sectionSource)).compile()

        // THEN
        assertEquals(result.exitCode, KotlinCompilation.ExitCode.COMPILATION_ERROR)
        assertTrue(result.messages.contains(expectedError))
    }
}
