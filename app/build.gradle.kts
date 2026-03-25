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

import com.android.build.api.dsl.VariantDimension
import configuration.extensions.protonEnvironment
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    id("com.google.gms.google-services")
    kotlin("android")
    kotlin("kapt")
    id("com.google.devtools.ksp")
    id("dagger.hilt.android.plugin")
    id("io.sentry.android.gradle")
    id("org.jetbrains.kotlinx.kover")
    id("me.proton.core.gradle-plugins.environment-config") version libs.versions.proton.core.plugin.get()
    id("org.jetbrains.kotlin.plugin.compose")
    id("app-config-plugin")
}

val accountSentryDSN: String = System.getenv("SENTRY_DSN_ACCOUNT") ?: ""
val sentryDSN: String = System.getenv("SENTRY_DSN_MAIL") ?: ""

val gitHashProvider: Provider<String> = providers.exec {
    commandLine("git", "rev-parse", "--short=7", "HEAD")
    isIgnoreExitValue = false
}.standardOutput.asText.map { it.trim() }

val gitHash: String by lazy {
    gitHashProvider.get()
}

android {
    namespace = "ch.protonmail.android"
    compileSdk = AppConfiguration.compileSdk.get()

    defaultConfig {
        applicationId = AppConfiguration.applicationId.get()
        minSdk = AppConfiguration.minSdk.get()
        targetSdk = AppConfiguration.targetSdk.get()
        ndkVersion = AppConfiguration.ndkVersion.get()
        versionCode = AppConfiguration.versionCode.get()
        versionName = AppConfiguration.versionName.get()

        testInstrumentationRunner = AppConfiguration.testInstrumentationRunner.get()
        testInstrumentationRunnerArguments["clearPackageData"] = "true"

        protonEnvironment {
            apiPrefix = "mail-api"
        }

        buildConfigField("String", "SENTRY_DSN", sentryDSN.toBuildConfigValue())
        buildConfigField("String", "ACCOUNT_SENTRY_DSN", accountSentryDSN.toBuildConfigValue())
        buildConfigField("String", "RUST_SDK_VERSION", "\"${libs.versions.proton.rust.core.get()}\"")

        setAssetLinksResValue("proton.me")
    }

    testOptions {
        execution = "ANDROIDX_TEST_ORCHESTRATOR"
    }

    signingConfigs {
        getByName("debug") {
            val debugKeystore = file("$rootDir/keystore/debug.keystore")

            // Use the shared keystore if present (either CI or internal usage).
            if (debugKeystore.exists()) {
                storeFile = file("$rootDir/keystore/debug.keystore")
                storePassword = "android"
                keyAlias = "androiddebugkey"
                keyPassword = "android"
            }
        }
    }

    buildTypes {
        // In UI Tests, we don't want the FCM service to be instantiated.
        val isFcmServiceEnabled = properties["enableFcmService"] ?: true

        debug {
            isDebuggable = true
            enableUnitTestCoverage = false
            isMinifyEnabled = false
            manifestPlaceholders["isFcmServiceEnabled"] = isFcmServiceEnabled
        }
        release {
            isDebuggable = false
            enableUnitTestCoverage = false
            isMinifyEnabled = true

            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                *fileTree("proguard").files.toTypedArray()
            )

            manifestPlaceholders["isFcmServiceEnabled"] = isFcmServiceEnabled
            signingConfig = signingConfigs["debug"]
        }
        create("benchmark") {
            initWith(getByName("release"))
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += listOf("release")
            manifestPlaceholders["isFcmServiceEnabled"] = false
            defaultConfig {
                testInstrumentationRunnerArguments["androidx.benchmark.fullTracing.enable"] = "true"
            }
        }
    }

    flavorDimensions.add("default")
    productFlavors {
        create("dev") {
            applicationIdSuffix = ".dev"
            versionNameSuffix = "-dev+$gitHash"
            buildConfigField("Boolean", "USE_DEFAULT_PINS", "false")

            val protonHost = "proton.black"
            protonEnvironment {
                host = protonHost
                apiPrefix = "mail-api"
            }
            setAssetLinksResValue(protonHost)
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
        jvmTarget = JvmTarget.fromTarget("17").target
    }

    buildFeatures {
        compose = true
        buildConfig = true
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
        getByName("androidTest").assets.srcDirs("src/uiTest/assets")
        getByName("androidTest").res.srcDirs("src/uiTest/res")
        getByName("dev").res.srcDirs("src/dev/res")
        getByName("alpha").res.srcDirs("src/alpha/res")
    }
}

configurations {
    // Remove duplicate classes (keep "org.jetbrains").
    implementation.get().exclude(mapOf("group" to "com.intellij", "module" to "annotations"))
    implementation.get().exclude(mapOf("group" to "org.intellij", "module" to "annotations"))

    // Proton Core 36.0.0+ pulls `me.proton.crypto:android-golib` transitively via `me.proton.core:crypto-android`.
    // It includes native libs that inflate APK size and is not needed by this app.
    configureEach {
        exclude(group = "me.proton.crypto", module = "android-golib")
    }
}

dependencies {
    implementation(project(":shared:core:account:dagger"))
    implementation(project(":shared:core:account-manager:dagger"))
    implementation(project(":shared:core:account-manager:presentation"))
    implementation(project(":shared:core:account-recovery:presentation"))
    implementation(project(":shared:core:humanverification:dagger"))
    implementation(project(":shared:core:humanverification:presentation"))
    implementation(project(":shared:core:auth:dagger"))
    implementation(project(":shared:core:auth:presentation"))
    implementation(project(":shared:core:payment:dagger"))
    implementation(project(":shared:core:payment:presentation"))
    implementation(project(":shared:core:payment-google:dagger"))
    implementation(project(":shared:core:payment-google:presentation"))

    implementation(libs.bundles.appLibs)
    implementation(libs.bundles.module.legacyCore)
    implementation(libs.java.jna) { artifact { type = "aar" } }

    implementation(project(":mail-attachments"))
    implementation(project(":mail-tracking-protection"))
    implementation(project(":mail-bugreport"))
    implementation(project(":mail-common"))
    implementation(project(":mail-composer"))
    implementation(project(":mail-contact"))
    implementation(project(":mail-conversation"))
    implementation(project(":mail-detail"))
    implementation(project(":mail-events"))
    implementation(project(":mail-featureflags"))
    implementation(project(":mail-label"))
    implementation(project(":mail-mailbox"))
    implementation(project(":mail-message"))
    implementation(project(":mail-notifications"))
    implementation(project(":mail-pagination"))
    implementation(project(":mail-settings"))
    implementation(project(":mail-pin-lock"))
    implementation(project(":mail-onboarding"))
    implementation(project(":mail-crash-record"))
    implementation(project(":mail-sidebar"))
    implementation(project(":mail-session"))
    implementation(project(":mail-spotlight"))
    implementation(project(":mail-legacy-migration"))
    implementation(project(":mail-upselling"))
    implementation(project(":mail-snooze"))
    implementation(project(":mail-padlocks"))
    implementation(project(":uicomponents"))
    implementation(project(":design-system"))
    implementation(project(":presentation-compose"))

    implementation(libs.play.review.core)
    implementation(libs.play.review.ext)
    implementation(libs.androidx.compose.animation)

    debugImplementation(libs.bundles.app.debug)

    // Environment configuration
    releaseImplementation(libs.proton.core.configuration.dagger.static)
    debugImplementation(libs.proton.core.configuration.dagger.contentProvider)

    kapt(libs.bundles.app.annotationProcessors)

    coreLibraryDesugaring(libs.android.tools.desugarJdkLibs)

    // To see the traces as results we need to include Perfetto.
    // We should not include in the production as it increases the APK size.
    val benchmarkImplementation by configurations
    benchmarkImplementation(libs.androidx.tracing)
    benchmarkImplementation(libs.androidx.tracing.compose.runtime)
    benchmarkImplementation(libs.androidx.tracing.perfetto)
    benchmarkImplementation(libs.androidx.tracing.perfetto.binary)
    // Also include configDaggerStatic to provide the required Hilt bindings in benchmark.
    benchmarkImplementation(libs.proton.core.configuration.dagger.static)

    testImplementation(libs.bundles.test)
    testImplementation(project(":test:test-data"))
    testImplementation(project(":test:utils"))

    androidTestImplementation(libs.bundles.test.androidTest)
    androidTestImplementation(project(":test:annotations"))
    androidTestImplementation(project(":test:robot:core"))
    androidTestImplementation(project(":test:robot:ksp:annotations"))
    androidTestImplementation(project(":test:test-data"))
    androidTestImplementation(project(":test:network-mocks"))
    androidTestImplementation(project(":test:utils"))
    androidTestImplementation(project(":uicomponents")) // Needed for shared test tags.

    androidTestUtil(libs.androidx.test.orchestrator)
    kaptAndroidTest(libs.dagger.hilt.compiler)
    kspAndroidTest(project(":test:robot:ksp:processor"))
}

fun isSentryAutoUploadEnabled(): Boolean = gradle.startParameter.taskNames.any {
    it.contains("release", true)
}

sentry {
    autoInstallation {
        sentryVersion = libs.versions.sentry.asProvider()
        autoUploadProguardMapping = isSentryAutoUploadEnabled()
        uploadNativeSymbols = isSentryAutoUploadEnabled()
    }

    tracingInstrumentation {
        enabled = false
    }
}

fun String?.toBuildConfigValue() = if (this != null) "\"$this\"" else "null"

fun VariantDimension.setAssetLinksResValue(host: String) {
    resValue(
        type = "string", name = "asset_statements",
        value = """
            [{
              "relation": ["delegate_permission/common.handle_all_urls", "delegate_permission/common.get_login_creds"],
              "target": { "namespace": "web", "site": "https://$host" }
            }]
        """.trimIndent()
    )
}
