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
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.LambdaTypeName
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.TypeVariableName
import com.squareup.kotlinpoet.UNIT
import com.squareup.kotlinpoet.ksp.addOriginatingKSFile
import com.squareup.kotlinpoet.ksp.writeTo

internal fun generateAsDslExtension(
    annotatedElement: KSClassDeclaration,
    codeGenerator: CodeGenerator,
    logger: KSPLogger
) {
    val (annotatedClass, annotatedClassPkg) = annotatedElement.destructured

    logger.info("Generating shorthand DSL helper for $annotatedClass.")

    val originatingFile = requireNotNull(annotatedElement.containingFile) {
        logger.error("Originating file for $annotatedClass is null.")
    }

    val shortHandDslExtensionFunction = FunSpec.builder(annotatedClass.replaceFirstChar { it.lowercase() })
        .addOriginatingKSFile(originatingFile)
        .addModifiers(KModifier.INTERNAL)
        .addParameter(
            ParameterSpec.builder(
                name = "block",
                type = LambdaTypeName.get(
                    TypeVariableName.invoke(annotatedClass),
                    parameters = emptyList(),
                    returnType = UNIT
                )
            ).build()
        )
        .addStatement("""return %L().apply(block)""", annotatedClass)
        .returns(TypeVariableName.invoke(annotatedClass))
        .build()

    val targetFile = FileSpec.builder(
        packageName = annotatedClassPkg,
        fileName = "$annotatedClass\$AsDslExtension"
    )
        .addFunction(shortHandDslExtensionFunction)
        .build()

    targetFile.writeTo(codeGenerator, aggregating = false).also {
        logger.info("Annotation processed -> ${targetFile.name}.")
    }
}
