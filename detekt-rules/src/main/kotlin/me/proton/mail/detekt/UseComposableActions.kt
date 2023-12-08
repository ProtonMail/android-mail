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

import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity
import me.proton.mail.detekt.UseComposableActions.Companion.Threshold
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtParameter

/**
 * Reports functions annotated as `@Composable` with [Threshold] or more lambda parameters
 * ```kotlin
 * // compliant code
 * @Composable
 * fun SomeComposable(
 *     onBack: () -> Unit
 * )
 *
 * // compliant code
 * fun NotComposable(
 *     first: () -> Unit,
 *     second: () -> Unit,
 *     third: () -> Unit,
 *     fourth: () -> Unit,
 * )
 *
 * // non-compliant code
 * @Composable
 * fun SomeComposable(
 *     onBack: () -> Unit,
 *     second: () -> Unit,
 *     third: () -> Unit,
 *     fourth: () -> Unit,
 * )
 * ```
 */
class UseComposableActions : Rule() {

    override val issue = Issue(
        javaClass.simpleName,
        Severity.Maintainability,
        Description,
        Debt.FIVE_MINS
    )

    override fun visitNamedFunction(function: KtNamedFunction) {
        super.visitNamedFunction(function)

        val annotationNames = function.annotationEntries.map { annotation -> annotation.shortName.toString() }
        if ("Composable" in annotationNames) {

            val lambdaParametersCount = function.valueParameters.count(::isNotComposableLambda)
            if (lambdaParametersCount >= Threshold) {
                report(CodeSmell(issue, Entity.atName(function), Message))
            }
        }
    }

    private fun isLambda(parameter: KtParameter) = ") -> " in parameter.text
    private fun isNotComposableLambda(parameter: KtParameter) = isLambda(parameter) && "@Composable" !in parameter.text
    private companion object {

        const val Description = "This rule reports a Composable functions with too many lambda parameters."
        const val Message = "Too many lambda parameters: wrap them into an Actions class instead."
        const val Threshold = 3
    }
}
