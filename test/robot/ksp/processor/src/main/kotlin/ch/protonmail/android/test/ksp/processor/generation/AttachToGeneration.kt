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

import ch.protonmail.android.test.ksp.annotations.AttachTo
import ch.protonmail.android.test.ksp.processor.destructured
import com.google.devtools.ksp.containingFile
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
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

internal fun generateAttachToExtension(
    annotatedElement: KSClassDeclaration,
    codeGenerator: CodeGenerator,
    logger: KSPLogger
) {
    val annotationElement = annotatedElement.annotations.first {
        it.shortName.asString() == AttachTo::class.simpleName
    }

    val identifier = requireNotNull(annotationElement.getArgument<String>(AttachTo::identifier.name))
    val targets = requireNotNull(annotationElement.getArgument<ArrayList<KSType>>(AttachTo::targets.name))
    val (annotatedClass, annotatedClassPkg) = annotatedElement.destructured

    targets.forEach { target ->
        val (targetClass, targetPkg) = target.declaration.destructured
        val extensionIdentifier = identifier.ifEmpty { annotatedClass }.replaceFirstChar { it.lowercase() }

        logger.info("Attaching $annotatedClass to $targetClass.")

        val originatingFile = requireNotNull(annotationElement.containingFile) {
            logger.error("Originating file for $annotatedClass on $targetClass is null.")
        }

        val attachToExtensionFunction = FunSpec.builder(extensionIdentifier)
            .addOriginatingKSFile(originatingFile)
            .addModifiers(KModifier.INTERNAL)
            .receiver(ClassName(targetPkg, targetClass))
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
            .returns(ClassName(annotatedClassPkg, annotatedClass))
            .build()

        val targetFile = FileSpec.builder(
            packageName = annotatedClassPkg,
            fileName = "$targetClass\$${annotatedClass}\$AttachToExtension"
        )
            .addFunction(attachToExtensionFunction)
            .build()

        targetFile.writeTo(codeGenerator, aggregating = false).also {
            logger.info("Annotation processed -> ${targetFile.name}.")
        }
    }
}

private inline fun <reified T> KSAnnotation.getArgument(rawName: String): T? =
    arguments.firstOrNull { it.name?.asString() == rawName }?.value as? T
