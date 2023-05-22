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

package ch.protonmail.android.test.ksp.processor

import ch.protonmail.android.test.ksp.annotations.AsDsl
import ch.protonmail.android.test.ksp.annotations.AttachTo
import ch.protonmail.android.test.ksp.annotations.VerifiesOuter
import ch.protonmail.android.test.ksp.processor.visitors.AsDslVisitor
import ch.protonmail.android.test.ksp.processor.visitors.AttachToVisitor
import ch.protonmail.android.test.ksp.processor.visitors.VerifiesOuterVisitor
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.validate

internal class UITestSymbolProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger
) : SymbolProcessor {

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val asDslDeclarations = resolver.getSymbolsWithAnnotation(AsDsl::class.qualifiedName!!)
        val attachToDeclarations = resolver.getSymbolsWithAnnotation(AttachTo::class.qualifiedName!!)
        val outerVerifiesDeclarations = resolver.getSymbolsWithAnnotation(VerifiesOuter::class.qualifiedName!!)

        asDslDeclarations.filtered.forEach { it.accept(AsDslVisitor(codeGenerator, logger), Unit) }
        attachToDeclarations.filtered.forEach { it.accept(AttachToVisitor(codeGenerator, logger), Unit) }
        outerVerifiesDeclarations.filtered.forEach { it.accept(VerifiesOuterVisitor(codeGenerator, logger), Unit) }

        return asDslDeclarations.filterNot { it.validate() }
            .plus(attachToDeclarations.filterNot { it.validate() })
            .plus(outerVerifiesDeclarations.filterNot { it.validate() })
            .toList()
    }
}

private val Sequence<KSAnnotated>.filtered
    get() = filter { it is KSClassDeclaration && it.validate() }
