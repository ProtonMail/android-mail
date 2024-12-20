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
}

android {
    namespace = "ch.protonmail.android.mailnotifications"
    compileSdk = Config.compileSdk

    defaultConfig {
        minSdk = Config.minSdk
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
        buildConfig = true
        compose = true
    }

    packaging {
        resources.excludes.add("MANIFEST.MF")
        resources.excludes.add("META-INF/LICENSE*")
        resources.excludes.add("META-INF/licenses/**")
        resources.excludes.add("META-INF/AL2.0")
        resources.excludes.add("META-INF/LGPL2.1")
        resources.excludes.add("META-INF/gradle/incremental.annotation.processors")
    }
}

kapt {
    correctErrorTypes = true
}

dependencies {
    kapt(libs.bundles.app.annotationProcessors)

    api(platform(libs.firebase.bom))
    api(libs.firebase.messaging) {
        exclude(group = "com.google.firebase", module = "firebase-core")
        exclude(group = "com.google.firebase", module = "firebase-analytics")
        exclude(group = "com.google.firebase", module = "firebase-measurement-connector")
    }

    implementation(project(":mail-common"))
    implementation(project(":mail-label:domain"))
    implementation(project(":mail-detail:domain"))
    implementation(project(":mail-pagination:domain"))
    implementation(project(":mail-message:domain"))
    implementation(project(":mail-settings:domain"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.compose.ui.tooling)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.hilt.work)
    implementation(libs.androidx.work.runtimeKtx)
    implementation(libs.arrow.core)
    implementation(libs.dagger.hilt.android)
    implementation(libs.timber)
    implementation(libs.proton.core.accountManager)
    implementation(libs.proton.core.label)

    testImplementation(project(":test:test-data"))
    testImplementation(libs.bundles.test)
    androidTestImplementation(libs.bundles.test.androidTest)
    androidTestImplementation(project(":test:annotations"))
}
