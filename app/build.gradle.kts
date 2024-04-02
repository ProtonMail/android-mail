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

import java.util.Properties
import configuration.extensions.protonEnvironment

plugins {
    id("com.android.application")
    id("com.google.gms.google-services")
    kotlin("android")
    kotlin("kapt")
    id("com.google.devtools.ksp")
    id("dagger.hilt.android.plugin")
    id("io.sentry.android.gradle")
    id("org.jetbrains.kotlinx.kover")
    id("me.proton.core.gradle-plugins.environment-config") version Versions.Proton.corePlugin
}

setAsHiltModule()

val privateProperties = Properties().apply {
    @Suppress("SwallowedException")
    try {
        load(rootDir.resolve("private.properties").inputStream())
    } catch (exception: java.io.FileNotFoundException) {
        // Provide empty properties to allow the app to be built without secrets
        Properties()
    }
}

val accountSentryDSN: String = privateProperties.getProperty("accountSentryDSN") ?: ""
val sentryDSN: String = privateProperties.getProperty("sentryDSN") ?: ""
val proxyToken: String? = privateProperties.getProperty("PROXY_TOKEN")

android {
    namespace = "ch.protonmail.android"
    compileSdk = Config.compileSdk

    defaultConfig {
        applicationId = Config.applicationId
        minSdk = Config.minSdk
        targetSdk = Config.targetSdk
        versionCode = Config.versionCode
        versionName = Config.versionName
        testInstrumentationRunner = Config.testInstrumentationRunner
        testInstrumentationRunnerArguments["clearPackageData"] = "true"

        javaCompileOptions {
            annotationProcessorOptions {
                arguments["room.schemaLocation"] = "$projectDir/schemas"
                arguments["room.incremental"] = "true"
            }
        }

        protonEnvironment {
            apiPrefix = "mail-api"
        }

        buildConfigField("String", "SENTRY_DSN", sentryDSN.toBuildConfigValue())
        buildConfigField("String", "ACCOUNT_SENTRY_DSN", accountSentryDSN.toBuildConfigValue())
        buildConfigField("String", "PROXY_TOKEN", proxyToken.toBuildConfigValue())
    }

    testOptions {
        execution = "ANDROIDX_TEST_ORCHESTRATOR"
    }

    signingConfigs {
        // Signing config for debug uses a dummy shared keystore to allow Firebase to work on any internal machine.
        getByName("debug") {
            storeFile = file("$rootDir/keystore/debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }

        register("release") {
            storeFile = file("$rootDir/keystore/ProtonMail.keystore")
            storePassword = "${privateProperties["keyStorePassword"]}"
            keyAlias = "ProtonMail"
            keyPassword = "${privateProperties["keyStoreKeyPassword"]}"
        }
    }

    buildTypes {
        // In UI Tests, we don't want the FCM service to be instantiated.
        val isFcmServiceEnabled = properties["enableFcmService"] ?: true

        debug {
            isDebuggable = true
            enableUnitTestCoverage = false
            postprocessing {
                isObfuscate = false
                isOptimizeCode = false
                isRemoveUnusedCode = false
                isRemoveUnusedResources = false
            }
            manifestPlaceholders["isFcmServiceEnabled"] = isFcmServiceEnabled
        }
        release {
            isDebuggable = false
            enableUnitTestCoverage = false
            postprocessing {
                isObfuscate = false
                isOptimizeCode = true
                isRemoveUnusedCode = true
                isRemoveUnusedResources = true
                file("proguard").listFiles()?.forEach { proguardFile(it) }
            }
            manifestPlaceholders["isFcmServiceEnabled"] = isFcmServiceEnabled
            signingConfig = signingConfigs["release"]
        }
        create("benchmark") {
            initWith(getByName("release"))
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += listOf("release")
            postprocessing {
                isObfuscate = false
            }
            manifestPlaceholders["isFcmServiceEnabled"] = false
            defaultConfig {
                testInstrumentationRunnerArguments["androidx.benchmark.fullTracing.enable"] = "true"
            }
        }
    }

    flavorDimensions.add("default")
    productFlavors {
        val gitHash = "git rev-parse --short HEAD".runCommand(workingDir = rootDir)
        create("dev") {
            applicationIdSuffix = ".dev"
            versionNameSuffix = "-dev+$gitHash"
            buildConfigField("Boolean", "USE_DEFAULT_PINS", "false")

            protonEnvironment {
                host = "proton.black"
                apiPrefix = "mail-api"
            }
        }
        create("alpha") {
            applicationIdSuffix = ".alpha"
            versionNameSuffix = "-alpha+$gitHash"
            buildConfigField("Boolean", "USE_DEFAULT_PINS", "true")
        }
        create("prod") {
            buildConfigField("Boolean", "USE_DEFAULT_PINS", "true")
        }
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true

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

    hilt {
        enableAggregatingTask = true
    }

    packaging {
        resources.excludes.add("MANIFEST.MF")
        resources.excludes.add("META-INF/LICENSE*")
        resources.excludes.add("META-INF/licenses/**")
        resources.excludes.add("META-INF/AL2.0")
        resources.excludes.add("META-INF/LGPL2.1")
        resources.excludes.add("META-INF/gradle/incremental.annotation.processors")
    }

    sourceSets {
        getByName("main").java.srcDirs("src/main/kotlin")
        getByName("test").java.srcDirs("src/test/kotlin")
        getByName("androidTest").java.srcDirs("src/androidTest/kotlin", "src/uiTest/kotlin")
        getByName("androidTest").assets.srcDirs("src/uiTest/assets", "schemas")
        getByName("androidTest").res.srcDirs("src/uiTest/res")
        getByName("dev").res.srcDirs("src/dev/res")
        getByName("alpha").res.srcDirs("src/alpha/res")
    }
}

configurations {
    // Remove duplicate classes (keep "org.jetbrains").
    implementation.get().exclude(mapOf("group" to "com.intellij", "module" to "annotations"))
    implementation.get().exclude(mapOf("group" to "org.intellij", "module" to "annotations"))
}

dependencies {
    implementation(files("../../proton-libs/gopenpgp/gopenpgp.aar"))
    implementation(Dependencies.appLibs)
    implementation(KotlinX.immutableCollections)
    implementation(Proton.Core.proguardRules)
    implementation(AndroidX.Biometrics.biometric)

    implementation(project(":mail-common"))
    implementation(project(":mail-composer"))
    implementation(project(":mail-contact"))
    implementation(project(":mail-conversation"))
    implementation(project(":mail-detail"))
    implementation(project(":mail-label"))
    implementation(project(":mail-mailbox"))
    implementation(project(":mail-message"))
    implementation(project(":mail-notifications"))
    implementation(project(":mail-pagination"))
    implementation(project(":mail-settings"))
    implementation(project(":uicomponents"))

    debugImplementation(Dependencies.appDebug)

    // Environment configuration
    releaseImplementation(Proton.Core.configDaggerStatic)
    debugImplementation(Proton.Core.configDaggerContentProvider)

    kapt(Dependencies.appAnnotationProcessors)

    coreLibraryDesugaring(AndroidTools.desugarJdkLibs)

    // To see the traces as results we need to include Perfetto.
    // We should not include in the production as it increases the APK size.
    val benchmarkImplementation by configurations
    benchmarkImplementation(AndroidX.Profile.Tracing.tracing)
    benchmarkImplementation(AndroidX.Profile.ComposeTracing.composeTracing)
    benchmarkImplementation(AndroidX.Profile.Perfetto.perfetto)
    benchmarkImplementation(AndroidX.Profile.Perfetto.perfettoBinary)
    // Also include configDaggerStatic to provide the required Hilt bindings in benchmark.
    benchmarkImplementation(Proton.Core.configDaggerStatic)

    testImplementation(Dependencies.testLibs)
    testImplementation(project(":test:test-data"))
    testImplementation(project(":test:utils"))

    androidTestImplementation(Dependencies.androidTestLibs)
    androidTestImplementation(Proton.Core.accountManagerPresentationCompose)
    androidTestImplementation(Proton.Core.accountRecoveryTest)
    androidTestImplementation(Proton.Core.authTest)
    androidTestImplementation(Proton.Core.planTest)
    androidTestImplementation(Proton.Core.reportTest)
    androidTestImplementation(project(":test:annotations"))
    androidTestImplementation(project(":test:idlingresources"))
    androidTestImplementation(project(":test:robot:core"))
    androidTestImplementation(project(":test:robot:ksp:annotations"))
    androidTestImplementation(project(":test:test-data"))
    androidTestImplementation(project(":test:network-mocks"))
    androidTestImplementation(project(":test:utils"))
    androidTestImplementation(project(":uicomponents")) // Needed for shared test tags.

    androidTestUtil(AndroidX.Test.orchestrator)
    kspAndroidTest(project(":test:robot:ksp:processor"))
}

fun String?.toBuildConfigValue() = if (this != null) "\"$this\"" else "null"
