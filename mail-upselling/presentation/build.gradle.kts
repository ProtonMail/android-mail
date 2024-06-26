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
    namespace = "ch.protonmail.android.mailupselling.presentation"
    compileSdk = Config.compileSdk

    defaultConfig {
        minSdk = Config.minSdk
        lint.targetSdk = Config.targetSdk
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
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = Versions.AndroidX.composeCompiler
    }
}

dependencies {
    implementation(project(":mail-upselling:domain"))
    implementation(project(":mail-common:domain"))
    implementation(project(":mail-common:presentation"))
    implementation(project(":uicomponents"))

    debugImplementation(AndroidX.Compose.uiTooling)
    debugImplementation(AndroidX.Compose.uiToolingPreview)

    implementation(AndroidX.Hilt.navigationCompose)
    implementation(Arrow.core)
    implementation(Coil.coil)
    implementation(Dependencies.composeLibs)
    implementation(JakeWharton.timber)
    implementation(Proton.Core.presentation)
    implementation(Proton.Core.presentationCompose)
    implementation(Proton.Core.plan)

    testImplementation(project(":test:test-data"))

    testImplementation(Cash.turbine)
    testImplementation(Kotlin.test)
    testImplementation(Kotlin.testJunit)
    testImplementation(KotlinX.coroutinesTest)
    testImplementation(Mockk.mockk)
}
