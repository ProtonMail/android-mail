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

buildscript {
    repositories {
        google()
    }
    dependencies {
        classpath(libs.android.tools.build)
        classpath(libs.kotlin.gradle)
        classpath(libs.hilt.android.gradle)
        classpath(libs.google.services)
        classpath(libs.sentry.gradle)
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
    }
}

plugins {
    alias(libs.plugins.google.devtools.ksp) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.proton.core.detekt)
    alias(libs.plugins.proton.core.coverage.config)
    alias(libs.plugins.proton.core.coverage) apply false
    alias(libs.plugins.proton.core.global.coverage) apply false
    alias(libs.plugins.compose.compiler) apply false
}

subprojects {
    if (project.findProperty("enableComposeCompilerReports") == "true") {
        kotlinCompilerArgs(
            "-P",
            "plugin:androidx.compose.compiler.plugins.kotlin:reportsDestination=" +
                project.layout.buildDirectory.asFile.get().absolutePath + "/compose_reports",
            "-P",
            "plugin:androidx.compose.compiler.plugins.kotlin:metricsDestination=" +
                project.layout.buildDirectory.asFile.get().absolutePath + "/compose_metrics"
        )
    }

    afterEvaluate {
        dependencies {
            configurations.findByName("detektPlugins")?.let {
                add("detektPlugins", project(":detekt-rules"))
            }
        }
        tasks.findByName("detekt")?.dependsOn(":detekt-rules:assemble")
    }
}

protonDetekt {
    threshold = 0
}

tasks.register("clean", Delete::class) {
    delete(rootProject.layout.buildDirectory)
}

setupTests()

kotlinCompilerArgs(
    "-opt-in=kotlin.RequiresOptIn",
    // Enables experimental Coroutines API.
    "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
    // Enables experimental Time (Turbine).
    "-opt-in=kotlin.time.ExperimentalTime"
)

fun Project.kotlinCompilerArgs(vararg extraCompilerArgs: String) {
    for (sub in subprojects) {
        sub.tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
            kotlinOptions { freeCompilerArgs = freeCompilerArgs + extraCompilerArgs }
        }
    }
}


fun Project.setupTests() {
    fun Project.isRootProject() = this@isRootProject.subprojects.size != 0

    for (sub in subprojects) {

        // Apply coverage plugin to non subprojects.
        if (!sub.isRootProject()) {
            sub.afterEvaluate { pluginManager.apply("me.proton.core.gradle-plugins.coverage") }
        }

        sub.tasks.withType<Test> {
            // Test logging
            testLogging {
                exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
            }

            // Additional JVM args to bypass strong encapsulation (needed for mocking)
            jvmArgs(
                "--add-opens", "java.base/java.util=ALL-UNNAMED"
            )
        }
    }
}

