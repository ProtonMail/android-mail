import org.jetbrains.kotlin.gradle.dsl.JvmTarget

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

plugins {
    id("com.android.library")
    kotlin("android")
    kotlin("kapt")
    kotlin("plugin.serialization")
    id("dagger.hilt.android.plugin")
    id("org.jetbrains.kotlin.plugin.compose")
    id("app-config-plugin")
}

android {
    namespace = "ch.protonmail.android.mailupselling.presentation"
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

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    kapt(libs.bundles.app.annotationProcessors)
    implementation(libs.dagger.hilt.android)

    implementation(project(":mail-common:domain"))
    implementation(project(":mail-events:domain"))
    implementation(project(":mail-featureflags:domain"))
    implementation(project(":mail-session:domain"))
    implementation(project(":mail-upselling:domain"))
    implementation(project(":mail-common:presentation"))
    implementation(project(":design-system"))
    implementation(project(":presentation-compose"))
    implementation(project(":uicomponents"))
    implementation(project(":shared:core:payment:domain"))
    implementation(project(":shared:core:payment:presentation"))
    implementation(project(":shared:core:payment-google:domain"))
    implementation(project(":shared:core:payment-google:presentation"))

    debugImplementation(libs.bundles.compose.debug)

    implementation(libs.proton.core.user.domain)
    implementation(libs.bundles.compose)
    implementation(libs.androidx.hilt.lifecycle.viewmodel)
    implementation(libs.arrow.core)
    implementation(libs.coil.compose)
    implementation(libs.haze)
    implementation(libs.timber)
    implementation(libs.proton.core.presentationCompose)

    testImplementation(project(":test:test-data"))
    testImplementation(project(":test:utils"))
    testImplementation(libs.bundles.test)
}
