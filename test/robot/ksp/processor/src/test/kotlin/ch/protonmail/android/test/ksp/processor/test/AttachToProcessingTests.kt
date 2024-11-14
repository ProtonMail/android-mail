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
internal class AttachToProcessingTests {

    @Test
    fun `test @AttachTo annotation processing, same package between Robot and Section`() {
        // GIVEN
        val robotSource = SourceFile.kotlin(
            "MyRobot.kt",
            """
                |package test.packageName
                |
                |import ch.protonmail.android.test.robot.ProtonMailRobot
                |
                |internal class MyRobot : ProtonMailRobot
            """.trimMargin()
        )

        val extensionSource = SourceFile.kotlin(
            "SectionRobot.kt",
            """
                |package test.packageName
                |
                |import ch.protonmail.android.test.ksp.annotations.AttachTo
                |import ch.protonmail.android.test.robot.ProtonMailSectionRobot
                |
                |@AttachTo(targets = [MyRobot::class], identifier = "customId")
                |internal class SectionRobot: ProtonMailSectionRobot
            """.trimMargin()
        )

        val targetSourceFileWithPath = "test/packageName/MyRobot\$SectionRobot\$AttachToExtension.kt"

        // WHEN
        val result = getKotlinCompilation(listOf(robotSource, extensionSource)).compile()
        val sourceCode = result.getGeneratedSourceFile(targetSourceFileWithPath).readText()

        // THEN
        assertEquals(result.exitCode, KotlinCompilation.ExitCode.OK)
        assertEquals(
            """
                |package test.packageName
                |
                |import kotlin.Unit
                |
                |internal fun MyRobot.customId(block: SectionRobot.() -> Unit): SectionRobot = SectionRobot().apply(block)
                |
            """.trimMargin(),
            sourceCode
        )
    }

    @Test
    fun `test @AttachTo annotation processing, different package between Robot and Section`() {
        // GIVEN
        val robotSource = SourceFile.kotlin(
            "MyRobot.kt",
            """
                |package test.different.packageName
                |
                |import ch.protonmail.android.test.robot.ProtonMailRobot
                |
                |internal class MyRobot : ProtonMailRobot
            """.trimMargin()
        )

        val extensionSource = SourceFile.kotlin(
            "SectionRobot.kt",
            """
                |package test.packageName.section
                |
                |import ch.protonmail.android.test.ksp.annotations.AttachTo
                |import ch.protonmail.android.test.robot.ProtonMailSectionRobot
                |import test.different.packageName.MyRobot
                |
                |@AttachTo(targets = [MyRobot::class], identifier = "customId")
                |internal class SectionRobot: ProtonMailSectionRobot
            """.trimMargin()
        )

        val targetSourceFileWithPath = "test/packageName/section/MyRobot\$SectionRobot\$AttachToExtension.kt"

        // WHEN
        val result = getKotlinCompilation(listOf(robotSource, extensionSource)).compile()
        val sourceCode = result.getGeneratedSourceFile(targetSourceFileWithPath).readText()

        // THEN
        assertEquals(result.exitCode, KotlinCompilation.ExitCode.OK)
        assertEquals(
            """
                |package test.packageName.section
                |
                |import kotlin.Unit
                |import test.different.packageName.MyRobot
                |
                |internal fun MyRobot.customId(block: SectionRobot.() -> Unit): SectionRobot = SectionRobot().apply(block)
                |
            """.trimMargin(),
            sourceCode
        )
    }

    @Test
    fun `test @AttachTo annotation processing with default identifier`() {
        // GIVEN
        val robotSource = SourceFile.kotlin(
            "MyRobot.kt",
            """
                |package test.different.packageName
                |
                |import ch.protonmail.android.test.robot.ProtonMailRobot
                |
                |internal class MyRobot : ProtonMailRobot
            """.trimMargin()
        )

        val extensionSource = SourceFile.kotlin(
            "SectionRobot.kt",
            """
                |package test.packageName.section
                |
                |import ch.protonmail.android.test.ksp.annotations.AttachTo
                |import ch.protonmail.android.test.robot.ProtonMailSectionRobot
                |import test.different.packageName.MyRobot
                |
                |@AttachTo(targets = [MyRobot::class])
                |internal class SectionRobot: ProtonMailSectionRobot
            """.trimMargin()
        )

        val targetSourceFileWithPath = "test/packageName/section/MyRobot\$SectionRobot\$AttachToExtension.kt"

        // WHEN
        val result = getKotlinCompilation(listOf(robotSource, extensionSource)).compile()
        val sourceCode = result.getGeneratedSourceFile(targetSourceFileWithPath).readText()

        // THEN
        assertEquals(result.exitCode, KotlinCompilation.ExitCode.OK)
        assertEquals(
            """
                |package test.packageName.section
                |
                |import kotlin.Unit
                |import test.different.packageName.MyRobot
                |
                |internal fun MyRobot.sectionRobot(block: SectionRobot.() -> Unit): SectionRobot = SectionRobot().apply(block)
                |
            """.trimMargin(),
            sourceCode
        )
    }

    @Test
    fun `test @AttachTo annotation processing failure when applied on an interface`() {
        // GIVEN
        val robotSource = SourceFile.kotlin(
            "MyRobot.kt",
            """
                |package test.different.packageName
                |
                |import ch.protonmail.android.test.robot.ProtonMailRobot
                |
                |internal class MyRobot : ProtonMailRobot
            """.trimMargin()
        )

        val extensionSource = SourceFile.kotlin(
            "SectionRobot.kt",
            """
                |package test.packageName.section
                |
                |import ch.protonmail.android.test.ksp.annotations.AttachTo
                |import ch.protonmail.android.test.robot.ProtonMailSectionRobot
                |import test.different.packageName.MyRobot
                |
                |@AttachTo(targets = [MyRobot::class])
                |internal interface SectionRobot: ProtonMailSectionRobot
            """.trimMargin()
        )

        val expectedErrorMessage = "Annotated object is not a class."

        // WHEN
        val result = getKotlinCompilation(listOf(robotSource, extensionSource)).compile()

        // THEN
        assertEquals(result.exitCode, KotlinCompilation.ExitCode.COMPILATION_ERROR)
        assertTrue(result.messages.contains(expectedErrorMessage))
    }

    @Test
    fun `test @AttachTo annotation processing failure when applied on an abstract class`() {
        // GIVEN
        val robotSource = SourceFile.kotlin(
            "MyRobot.kt",
            """
                |package test.different.packageName
                |
                |import ch.protonmail.android.test.robot.ProtonMailRobot
                |
                |internal class MyRobot : ProtonMailRobot
            """.trimMargin()
        )

        val extensionSource = SourceFile.kotlin(
            "SectionRobot.kt",
            """
                |package test.packageName.section
                |
                |import ch.protonmail.android.test.ksp.annotations.AttachTo
                |import ch.protonmail.android.test.robot.ProtonMailSectionRobot
                |import test.different.packageName.MyRobot
                |
                |@AttachTo(targets = [MyRobot::class])
                |internal abstract class SectionRobot: ProtonMailSectionRobot
            """.trimMargin()
        )

        val expectedErrorMessage = "Annotated object cannot be an abstract class."

        // WHEN
        val result = getKotlinCompilation(listOf(robotSource, extensionSource)).compile()

        // THEN
        assertEquals(result.exitCode, KotlinCompilation.ExitCode.COMPILATION_ERROR)
        assertTrue(result.messages.contains(expectedErrorMessage))
    }

    @Test
    fun `test @AttachTo annotation processing failure when the annotated class is not a ProtonMailSectionRobot`() {
        // GIVEN
        val robotSource = SourceFile.kotlin(
            "MyRobot.kt",
            """
                |package test.different.packageName
                |
                |import ch.protonmail.android.test.robot.ProtonMailRobot
                |
                |internal class MyRobot : ProtonMailRobot
            """.trimMargin()
        )

        val extensionSource = SourceFile.kotlin(
            "SectionRobot.kt",
            """
                |package test.packageName.section
                |
                |import ch.protonmail.android.test.ksp.annotations.AttachTo
                |import test.different.packageName.MyRobot
                |
                |@AttachTo(targets = [MyRobot::class])
                |internal class SectionRobot
            """.trimMargin()
        )

        val expectedErrorMessage = "Annotated object needs to be a ProtonMailSectionRobot"

        // WHEN
        val result = getKotlinCompilation(listOf(robotSource, extensionSource)).compile()

        // THEN
        assertEquals(result.exitCode, KotlinCompilation.ExitCode.COMPILATION_ERROR)
        assertTrue(result.messages.contains(expectedErrorMessage))
    }
}
