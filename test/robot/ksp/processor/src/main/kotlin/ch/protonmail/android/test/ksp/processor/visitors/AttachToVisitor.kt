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

package ch.protonmail.android.test.ksp.processor.visitors

import ch.protonmail.android.test.ksp.processor.generation.generateAttachToExtension
import ch.protonmail.android.test.robot.ProtonMailSectionRobot
import com.google.devtools.ksp.getAllSuperTypes
import com.google.devtools.ksp.isAbstract
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSVisitorVoid
import com.squareup.kotlinpoet.asTypeName
import com.squareup.kotlinpoet.ksp.toTypeName

internal class AttachToVisitor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger
) : KSVisitorVoid() {

    override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {
        if (classDeclaration.classKind != ClassKind.CLASS) {
            logger.error("Annotated object is not a class.", classDeclaration)
            return
        }

        if (classDeclaration.isAbstract()) {
            logger.error("Annotated object cannot be an abstract class.", classDeclaration)
            return
        }

        // Use `getAllSuperTypes`, as the actual annotated class might not implement the interface directly.
        classDeclaration.getAllSuperTypes().find {
            it.toTypeName() == ProtonMailSectionRobot::class.asTypeName()
        } ?: run {
            logger.error("Annotated object needs to be a ProtonMailSectionRobot.")
            return
        }

        generateAttachToExtension(classDeclaration, codeGenerator, logger)
    }
}
