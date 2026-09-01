/*
 * Copyright (c) 2025 Proton Technologies AG
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

import org.jetbrains.kotlin.gradle.dsl.JvmTarget

/*
 * Internal-only telemetry inspection, consumed by the debug menu on dev/alpha builds.
 *
 * Deliberately a single module rather than the usual domain/data/dagger split: it is wired into the
 * app as a `dev`/`alpha`-only dependency (see app/build.gradle.kts), so none of it reaches a prod
 * APK. Nothing in the product may depend on it.
 */
plugins {
    id("com.android.library")
    kotlin("android")
    kotlin("kapt")
    id("dagger.hilt.android.plugin")
    id("app-config-plugin")
}

android {
    namespace = "ch.protonmail.android.mailtelemetry.debug"
    compileSdk = AppConfiguration.compileSdk.get()

    defaultConfig {
        minSdk = AppConfiguration.minSdk.get()
        lint.targetSdk = AppConfiguration.targetSdk.get()
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget = JvmTarget.fromTarget("17")
        }
    }
}

dependencies {
    kapt(libs.bundles.app.annotationProcessors)

    compileOnly(libs.proton.rust.core)

    implementation(libs.bundles.module.data)
    implementation(libs.proton.core.domain)

    implementation(project(":mail-common:domain"))
    implementation(project(":mail-session:data"))
    implementation(project(":mail-session:domain"))

    testImplementation(libs.proton.rust.core)
    testImplementation(libs.bundles.test)
    testImplementation(project(":test:utils"))
    testImplementation(project(":test:test-data"))
}
