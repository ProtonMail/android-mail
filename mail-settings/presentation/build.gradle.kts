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

setAsHiltModule()

android {
    namespace = "ch.protonmail.android.mailsettings.presentation"
    compileSdk = Config.compileSdk

    defaultConfig {
        minSdk = Config.minSdk
        targetSdk = Config.targetSdk
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

    packagingOptions {
        resources.excludes.add("META-INF/licenses/**")
        resources.excludes.add("META-INF/AL2.0")
        resources.excludes.add("META-INF/LGPL2.1")
    }
}

dependencies {
    debugImplementation(Dependencies.composeDebugLibs)

    implementation(Dependencies.modulePresentationLibs)
    implementation(Proton.Core.account)
    implementation(Proton.Core.key)
    implementation(Proton.Core.user)
    implementation(Proton.Core.userSettings)
    implementation(Proton.Core.mailSettings)

    implementation(project(":mail-settings:domain"))
    implementation(project(":mail-common:domain"))
    implementation(project(":mail-common:presentation"))

    testImplementation(Dependencies.testLibs)
    testImplementation(Proton.Core.network)
    testImplementation(project(":test:test-data"))
}
