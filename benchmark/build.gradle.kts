import java.util.Properties

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
    id("com.android.test")
    id("org.jetbrains.kotlin.android")
}

private val benchmarkProperties = Properties().apply {
    @Suppress("SwallowedException")
    try {
        load(projectDir.resolve("benchmark.properties").inputStream())
    } catch (exception: java.io.FileNotFoundException) {
        Properties()
    }
}

private val benchmarkUsername = benchmarkProperties["username"].toString()
private val benchmarkPassword = benchmarkProperties["password"].toString()

android {
    namespace = "ch.protonmail.android.benchmark"
    compileSdk = Config.compileSdk

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_17.toString()
    }

    defaultConfig {
        minSdk = Config.minSdk
        targetSdk = Config.targetSdk

        missingDimensionStrategy("default", "alpha")

        buildConfigField("String", "DEFAULT_LOGIN", benchmarkUsername.toBuildConfigValue())
        buildConfigField("String", "DEFAULT_PASSWORD", benchmarkPassword.toBuildConfigValue())

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        // This benchmark buildType is used for benchmarking, and should function like the
        // release build (for example, with minification on). It's signed with a debug key
        // for easy local/CI testing.
        create("benchmark") {
            isDebuggable = true
            signingConfig = getByName("debug").signingConfig
            matchingFallbacks += listOf("release")
        }
    }

    buildFeatures {
        buildConfig = true
    }

    targetProjectPath = ":app"
    experimentalProperties["android.experimental.self-instrumenting"] = true
}

dependencies {
    implementation(libs.androidx.test.androidjunit)
    implementation(libs.androidx.test.espresso.core)
    implementation(libs.androidx.test.uiautomator)
    implementation(libs.androidx.test.macrobenchmark)
}

androidComponents {
    beforeVariants(selector().all()) {
        it.enable = it.buildType == "benchmark"
    }
}

fun String?.toBuildConfigValue() = if (this != null) "\"$this\"" else "null"
