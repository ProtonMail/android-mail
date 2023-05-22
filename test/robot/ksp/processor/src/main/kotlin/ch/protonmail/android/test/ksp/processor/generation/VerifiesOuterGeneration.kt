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

package ch.protonmail.android.test.ksp.processor.generation

import ch.protonmail.android.test.ksp.processor.destructured
import ch.protonmail.android.test.ksp.processor.stringClassName
import com.google.devtools.ksp.outerType
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.LambdaTypeName
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.TypeVariableName
import com.squareup.kotlinpoet.UNIT
import com.squareup.kotlinpoet.ksp.writeTo

internal fun generateVerifyOuterExtension(annotatedElement: KSClassDeclaration, codeGenerator: CodeGenerator) {
    val (annotatedClass, annotatedClassPkg) = annotatedElement.destructured
    val outerClass = requireNotNull(annotatedElement.asStarProjectedType().outerType).declaration

    val targetClass = outerClass.stringClassName
    val extensionIdentifier = annotatedClass.replaceFirstChar { it.lowercase() }

    val verifiesOuterExtensionFunction = FunSpec.builder(extensionIdentifier)
        .addModifiers(KModifier.INTERNAL)
        .receiver(
            TypeVariableName.invoke(targetClass)
        )
        .addParameter(
            ParameterSpec.builder(
                name = "block",
                type = LambdaTypeName.get(
                    ClassName(
                        packageName = annotatedClassPkg,
                        simpleNames = listOf(targetClass, annotatedClass)
                    ),
                    parameters = emptyList(),
                    returnType = UNIT
                )
            ).build()
        )
        .addStatement(
            """return %L().apply(block)""",
            TypeVariableName.invoke(annotatedClass)
        )
        .returns(
            ClassName(
                packageName = annotatedClassPkg,
                simpleNames = listOf(targetClass, annotatedClass)
            )
        )
        .build()

    val targetFile = FileSpec.Companion.builder(
        packageName = annotatedClassPkg,
        fileName = "$targetClass\$$annotatedClass\$VerifyOuterExtension"
    )
        .addFunction(verifiesOuterExtensionFunction)
        .build()

    targetFile.writeTo(codeGenerator, true)
}
