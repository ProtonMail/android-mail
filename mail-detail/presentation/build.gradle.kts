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
}

android {
    namespace = "ch.protonmail.android.maildetail.presentation"
    compileSdk = Config.compileSdk

    defaultConfig {
        minSdk = Config.minSdk
        lint.targetSdk = Config.targetSdk
        testInstrumentationRunner = Config.testInstrumentationRunner
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

    composeOptions {
        kotlinCompilerExtensionVersion = Versions.AndroidX.composeCompiler
    }

    packaging {
        resources.excludes.add("META-INF/licenses/**")
        resources.excludes.add("META-INF/AL2.0")
        resources.excludes.add("META-INF/LGPL2.1")
    }
}

dependencies {
    kapt(Dependencies.appAnnotationProcessors)

    debugImplementation(Dependencies.composeDebugLibs)

    implementation(Dependencies.modulePresentationLibs)
    implementation(Proton.Core.contactDomain)
    implementation(Proton.Core.label)

    implementation(project(":mail-detail:domain"))
    implementation(project(":mail-common:domain"))
    implementation(project(":mail-common:presentation"))
    implementation(project(":mail-contact:domain"))
    implementation(project(":mail-conversation:domain"))
    implementation(project(":mail-message:domain"))
    implementation(project(":mail-message:presentation"))
    implementation(project(":mail-label:domain"))
    implementation(project(":mail-label:presentation"))
    implementation(project(":mail-settings:domain"))
    // Needed as PageType is a supertype of Message.
    implementation(project(":mail-pagination:domain"))
    implementation(project(":uicomponents"))

    testImplementation(Dependencies.testLibs)
    testImplementation(project(":test:test-data"))
}
