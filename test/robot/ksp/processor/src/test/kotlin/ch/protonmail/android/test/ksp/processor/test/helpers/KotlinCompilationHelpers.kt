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

package ch.protonmail.android.test.ksp.processor.test.helpers

import java.io.File
import ch.protonmail.android.test.ksp.processor.UITestSymbolProcessorProvider
import com.tschuchort.compiletesting.JvmCompilationResult
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import com.tschuchort.compiletesting.configureKsp
import com.tschuchort.compiletesting.kspWithCompilation
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi

@OptIn(ExperimentalCompilerApi::class)
internal fun JvmCompilationResult.getGeneratedSourceFile(path: String): File {
    return outputDirectory.parentFile!!
        .resolve("ksp")
        .resolve("sources")
        .resolve("kotlin")
        .resolve(path)
}

@OptIn(ExperimentalCompilerApi::class)
internal fun getKotlinCompilation(kotlinSources: List<SourceFile>): KotlinCompilation {
    return KotlinCompilation().apply {
        configureKsp(useKsp2 = true) {
            symbolProcessorProviders += UITestSymbolProcessorProvider()
        }
        sources = kotlinSources
        inheritClassPath = true
        messageOutputStream = System.out
        kspWithCompilation = true
    }
}
