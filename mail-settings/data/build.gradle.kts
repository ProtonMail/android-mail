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
}

android {
    namespace = "ch.protonmail.android.mailsettings.data"
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
}

dependencies {
    kapt(libs.bundles.app.annotationProcessors)

    implementation(libs.bundles.module.data)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.hilt.work)
    implementation(libs.androidx.biometrics)
    implementation(libs.proton.core.user)
    implementation(libs.proton.core.mailSettings)

    implementation(project(":mail-common:data"))
    implementation(project(":mail-common:domain"))
    implementation(project(":mail-settings:domain"))
    implementation(project(":mail-message:data"))
    implementation(project(":mail-pagination:data"))
    implementation(project(":mail-conversation:domain"))

    testImplementation(libs.bundles.test)
}
