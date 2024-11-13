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
    kotlin("kapt")
    kotlin("android")
    kotlin("plugin.serialization")
    id("dagger.hilt.android.plugin")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "ch.protonmail.android.mailcomposer.presentation"
    compileSdk = Config.compileSdk

    defaultConfig {
        minSdk = Config.minSdk
        lint.targetSdk = Config.targetSdk
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        testInstrumentationRunnerArguments["clearPackageData"] = "true"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_17.toString()
    }

    buildFeatures {
        compose = true
    }

    packaging {
        resources.excludes.add("META-INF/licenses/**")
        resources.excludes.add("META-INF/LICENSE*")
        resources.excludes.add("META-INF/AL2.0")
        resources.excludes.add("META-INF/LGPL2.1")
    }
}

dependencies {
    kapt(libs.bundles.app.annotationProcessors)
    implementation(libs.dagger.hilt.android)

    implementation(libs.bundles.module.presentation)
    implementation(libs.accompanist.permissions)

    implementation(libs.proton.core.contact)
    implementation(libs.proton.core.user)

    implementation(project(":mail-common:domain"))
    implementation(project(":mail-common:presentation"))
    implementation(project(":mail-composer:domain"))
    implementation(project(":mail-contact:domain"))
    implementation(project(":mail-message:domain"))
    implementation(project(":mail-message:presentation"))
    implementation(project(":mail-pagination:domain"))
    implementation(project(":mail-settings:domain"))
    implementation(project(":mail-settings:presentation"))
    implementation(project(":test:idlingresources"))
    implementation(project(":uicomponents"))

    debugImplementation(libs.bundles.compose.debug)

    testImplementation(libs.bundles.test)
    testImplementation(libs.proton.core.label.domain)
    testImplementation(project(":test:test-data"))
    testImplementation(project(":test:utils"))
    testImplementation(project(":mail-detail:presentation"))
    androidTestImplementation(libs.bundles.test.androidTest)
}
