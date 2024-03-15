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

import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask

buildscript {
    repositories {
        google()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:${Versions.Gradle.androidGradlePlugin}")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:${Versions.Gradle.kotlinGradlePlugin}")
        classpath("com.google.dagger:hilt-android-gradle-plugin:${Versions.Gradle.hiltAndroidGradlePlugin}")
        classpath("com.google.gms:google-services:${Versions.Gradle.googleServicesPlugin}")
        classpath("org.jacoco:org.jacoco.core:${Versions.Gradle.jacocoGradlePlugin}")
        classpath("io.sentry:sentry-android-gradle-plugin:${Versions.Gradle.sentryGradlePlugin}")
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
    }
}

plugins {
    id("me.proton.core.gradle-plugins.detekt") version Versions.Proton.corePlugin
    id("me.proton.core.gradle-plugins.jacoco") version Versions.Proton.corePlugin
    id("com.github.ben-manes.versions") version Versions.Gradle.benManesVersionsPlugin
    id("com.google.devtools.ksp") version "1.9.22-1.0.17" apply false
}

subprojects {
    if (project.findProperty("enableComposeCompilerReports") == "true") {
        kotlinCompilerArgs(
            "-P", "plugin:androidx.compose.compiler.plugins.kotlin:reportsDestination=" +
                project.buildDir.absolutePath + "/compose_reports",
            "-P", "plugin:androidx.compose.compiler.plugins.kotlin:metricsDestination=" +
                project.buildDir.absolutePath + "/compose_metrics"
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

protonCoverageMultiModuleOptions {
    runTestTasksBefore = false
    sharedExcludes = listOf("**/me/proton/core/**")
    coverageConversionScript = { "$rootDir/../proton-libs/plugins/jacoco/scripts/cover2cover.py" }
}

protonDetekt {
    threshold = 0
}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}

setupDependenciesPlugin()
setupTests()

kotlinCompilerArgs(
    "-Xopt-in=kotlin.RequiresOptIn",
    // Enables experimental Coroutines (runBlockingTest).
    "-Xopt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
    // Enables experimental Time (Turbine).
    "-Xopt-in=kotlin.time.ExperimentalTime"
)

fun Project.kotlinCompilerArgs(vararg extraCompilerArgs: String) {
    for (sub in subprojects) {
        sub.tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
            kotlinOptions { freeCompilerArgs = freeCompilerArgs + extraCompilerArgs }
        }
    }
}

fun Project.setupDependenciesPlugin() {
    // https://github.com/ben-manes/gradle-versions-plugin
    tasks.withType<DependencyUpdatesTask> {
        rejectVersionIf {
            isNonStable(candidate.version) && !isNonStable(currentVersion)
        }
    }
}

fun isNonStable(version: String): Boolean {
    val stableKeyword = listOf("RELEASE", "FINAL", "GA").any { version.toUpperCase().contains(it) }
    val regex = "^[0-9,.v-]+(-r)?$".toRegex()
    val isStable = stableKeyword || regex.matches(version)
    return isStable.not()
}

fun Project.setupTests() {
    for (sub in subprojects) {
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
