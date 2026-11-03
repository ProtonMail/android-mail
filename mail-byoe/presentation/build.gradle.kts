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
    id("org.jetbrains.kotlin.plugin.compose")
    id("app-config-plugin")
}

android {
    namespace = "ch.protonmail.android.mailbyoe.presentation"
    compileSdk = AppConfiguration.compileSdk.get()

    defaultConfig {
        minSdk = AppConfiguration.minSdk.get()
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        testInstrumentationRunnerArguments["clearPackageData"] = "true"

        buildConfigField("String", "WEBVIEW_APP_VERSION", "\"android-mail@${AppConfiguration.versionName.get()}\"")
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
    debugImplementation(libs.bundles.compose.debug)

    implementation(libs.bundles.module.presentation)
    implementation(libs.androidx.browser)
    implementation(libs.lottie.compose)

    implementation(project(":mail-common:domain"))
    implementation(project(":mail-common:presentation"))
    implementation(project(":mail-byoe:domain"))
    implementation(project(":mail-label:domain"))
    implementation(project(":mail-session:domain"))
    implementation(project(":mail-upselling:domain"))
    implementation(project(":mail-upselling:presentation"))
    implementation(project(":uicomponents"))
    implementation(project(":design-system"))
    implementation(project(":presentation-compose"))

    testImplementation(libs.bundles.test)
    testImplementation(project(":test:utils"))
}
