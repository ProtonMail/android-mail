import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

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
        classpath(libs.paparazzi.plugin)
    }
}

val kotlinVersion = libs.versions.kotlin.asProvider().get()

allprojects {
    repositories {
        google()
        mavenCentral()
        mavenLocal()
    }

    // pin stdlib to the compiler version as Payments SDK pulls a newer one.
    configurations.configureEach {
        resolutionStrategy {
            force(
                "org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion",
                "org.jetbrains.kotlin:kotlin-stdlib-common:$kotlinVersion",
                "org.jetbrains.kotlin:kotlin-stdlib-jdk7:$kotlinVersion",
                "org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlinVersion"
            )
        }
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
        plugins.withId("org.jetbrains.kotlin.android") {
            tasks.withType<KotlinCompile>().configureEach {
                val reportsPrefix =
                    "plugin:androidx.compose.compiler.plugins.kotlin:reportsDestination="
                val metricsPrefix =
                    "plugin:androidx.compose.compiler.plugins.kotlin:metricsDestination="

                val projectDir = project.layout.buildDirectory.asFile.get().absolutePath
                val currentArgs = compilerOptions.freeCompilerArgs.get()

                // Avoid adding them twice if this block runs multiple times
                if (currentArgs.none { it.startsWith(reportsPrefix) } &&
                    currentArgs.none { it.startsWith(metricsPrefix) }
                ) {
                    compilerOptions.freeCompilerArgs.set(
                        currentArgs + listOf(
                            "-P",
                            "$reportsPrefix$projectDir/compose_reports",
                            "-P",
                            "$metricsPrefix$projectDir/compose_metrics"
                        )
                    )
                }
            }
        }
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
    "-opt-in=kotlin.time.ExperimentalTime",
    // https://youtrack.jetbrains.com/issue/KT-73255
    "-Xannotation-default-target=param-property",
    // Global Compose stability configuration
    "-P",
    "plugin:androidx.compose.compiler.plugins.kotlin:stabilityConfigurationPath=" +
        rootProject.layout.projectDirectory
            .file("compose-stability.conf")
            .asFile
            .absolutePath
)

fun Project.kotlinCompilerArgs(vararg extraCompilerArgs: String) {
    for (sub in subprojects) {
        sub.tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
            compilerOptions {
                freeCompilerArgs.addAll(extraCompilerArgs.toList())
            }
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

