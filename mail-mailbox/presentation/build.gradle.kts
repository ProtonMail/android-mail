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
    id("kotlin-parcelize")
    id("dagger.hilt.android.plugin")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "ch.protonmail.android.mailmailbox.presentation"
    compileSdk = Config.compileSdk

    defaultConfig {
        minSdk = Config.minSdk
        lint.targetSdk = Config.targetSdk
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        resources.excludes.add("MANIFEST.MF")
        resources.excludes.add("META-INF/LICENSE*")
        resources.excludes.add("META-INF/licenses/**")
        resources.excludes.add("META-INF/AL2.0")
        resources.excludes.add("META-INF/LGPL2.1")
    }
}

dependencies {
    kapt(libs.bundles.app.annotationProcessors)
    debugImplementation(libs.bundles.compose.debug)

    implementation(libs.bundles.module.presentation)
    implementation(libs.bundles.compose)
    implementation(libs.androidx.paging.runtime)
    implementation(libs.androidx.paging.compose)
    implementation(libs.androidx.room.ktx)
    implementation(libs.proton.core.account)
    implementation(libs.proton.core.accountManager)
    implementation(libs.proton.core.contact)
    implementation(libs.proton.core.featureFlag)
    implementation(libs.proton.core.label)
    implementation(libs.proton.core.mailSettings)
    implementation(libs.proton.core.plan.presentationCompose)
    implementation(libs.google.play.review)
    implementation(libs.google.play.reviewKtx)
    implementation(libs.proton.core.user)

    implementation(project(":mail-common:domain"))
    implementation(project(":mail-common:presentation"))
    implementation(project(":mail-contact:domain"))
    implementation(project(":mail-conversation:domain"))
    implementation(project(":mail-label:domain"))
    implementation(project(":mail-label:presentation"))
    implementation(project(":mail-mailbox:domain"))
    implementation(project(":mail-message:domain"))
    implementation(project(":mail-message:presentation"))
    implementation(project(":mail-pagination:domain"))
    implementation(project(":mail-pagination:presentation"))
    implementation(project(":mail-settings:data"))
    implementation(project(":mail-settings:domain"))
    implementation(project(":mail-settings:presentation"))
    implementation(project(":mail-upselling:presentation"))
    implementation(project(":mail-upselling:domain"))
    implementation(project(":mail-sidebar:presentation"))
    implementation(project(":uicomponents"))

    testImplementation(libs.bundles.test)
    testImplementation(libs.androidx.paging.testing)
    testImplementation(project(":test:test-data"))

    androidTestImplementation(libs.bundles.test.androidTest)
    androidTestImplementation(project(":test:annotations"))
}
