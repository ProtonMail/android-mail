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
    id("org.jetbrains.kotlin.plugin.compose")
    id("app-config-plugin")
}

android {
    namespace = "ch.protonmail.android.maillabel.presentation"
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
        compose = true
    }

    packaging {
        resources.excludes.add("META-INF/licenses/**")
        resources.excludes.add("META-INF/AL2.0")
        resources.excludes.add("META-INF/LGPL2.1")
    }
}

dependencies {
    kapt(libs.bundles.app.annotationProcessors)
    debugImplementation(libs.bundles.compose.debug)

    implementation(libs.bundles.module.presentation)
    implementation(libs.androidx.constraintlayout.compose)

    implementation(project(":mail-common:presentation"))
    implementation(project(":mail-common:domain"))
    implementation(project(":mail-conversation:domain"))
    implementation(project(":mail-category-view:domain"))
    implementation(project(":mail-category-view:presentation"))
    implementation(project(":mail-label:domain"))
    implementation(project(":mail-message:domain"))
    implementation(project(":mail-session:domain"))
    implementation(project(":mail-settings:domain"))
    implementation(project(":uicomponents"))
    implementation(project(":design-system"))
    implementation(project(":presentation-compose"))

    testImplementation(libs.bundles.test)
    testImplementation(project(":test:test-data"))
    testImplementation(project(":test:utils"))

    androidTestImplementation(libs.bundles.test.androidTest)
    androidTestImplementation(project(":test:annotations"))
}
