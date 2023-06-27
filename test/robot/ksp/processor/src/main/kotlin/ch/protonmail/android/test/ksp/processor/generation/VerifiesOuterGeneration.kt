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
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.Modifier
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.LambdaTypeName
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.TypeVariableName
import com.squareup.kotlinpoet.UNIT
import com.squareup.kotlinpoet.ksp.addOriginatingKSFile
import com.squareup.kotlinpoet.ksp.writeTo

internal fun generateVerifyOuterExtension(
    annotatedElement: KSClassDeclaration,
    codeGenerator: CodeGenerator,
    logger: KSPLogger
) {
    val (annotatedClass, annotatedClassPkg) = annotatedElement.destructured
    val outerClass = requireNotNull(annotatedElement.asStarProjectedType().outerType).declaration

    val targetClass = outerClass.stringClassName
    val extensionIdentifier = annotatedClass.replaceFirstChar { it.lowercase() }

    logger.info("Attaching outer verification to $targetClass.")

    val originatingFile = requireNotNull(annotatedElement.containingFile) {
        logger.error("Originating file for $annotatedClass on $targetClass is null.")
    }

    val verifiesOuterExtensionFunction = FunSpec.builder(extensionIdentifier)
        .addOriginatingKSFile(originatingFile)
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
            """%L().apply(block)""",
            TypeVariableName.invoke(annotatedClass)
        )
        .also {
            // If outer class is abstract, do not return a new instance of it.
            if (!outerClass.isAbstract()) {
                it.addStatement(
                    """return %L()""",
                    TypeVariableName.invoke(targetClass)
                )
                    .returns(
                        ClassName(
                            packageName = annotatedClassPkg,
                            targetClass
                        )
                    )
            } else {
                logger.info("$targetClass is abstract, extension function won't return a new instance of it.")
            }
        }
        .build()

    val targetFile = FileSpec.Companion.builder(
        packageName = annotatedClassPkg,
        fileName = "$targetClass\$$annotatedClass\$VerifyOuterExtension"
    )
        .addFunction(verifiesOuterExtensionFunction)
        .build()

    targetFile.writeTo(codeGenerator, aggregating = false).also {
        logger.info("Annotation processed -> ${targetFile.name}.")
    }
}

private fun KSDeclaration.isAbstract(): Boolean = modifiers.any { it == Modifier.ABSTRACT }

