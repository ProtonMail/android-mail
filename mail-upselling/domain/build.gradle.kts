import org.jetbrains.kotlin.gradle.dsl.JvmTarget

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

plugins {
    id("com.android.library")
    kotlin("android")
    kotlin("plugin.serialization")
    id("app-config-plugin")
}

android {
    namespace = "ch.protonmail.android.mailupselling.domain"
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

    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    implementation(libs.bundles.module.domain)

    implementation(project(":mail-common:domain"))
    implementation(project(":mail-common:presentation"))
    implementation(project(":mail-featureflags:domain"))
    implementation(project(":mail-session:domain"))
    implementation(project(":uicomponents"))
    implementation(project(":shared:core:payment:domain"))

    implementation(libs.proton.android.payment)

    implementation(libs.kotlinx.datetime)
    implementation(libs.timber)
    implementation(libs.proton.core.user.domain)

    testImplementation(project(":test:test-data"))
    testImplementation(project(":test:utils"))
    testImplementation(libs.bundles.test)
}
