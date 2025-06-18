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
import com.android.build.api.dsl.VariantDimension
import configuration.extensions.protonEnvironment
import kotlin.collections.forEach
import kotlin.collections.listOf
import kotlin.collections.mapOf
import kotlin.collections.plusAssign
import kotlin.collections.set

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
}

val privateProperties = Properties().apply {
    @Suppress("SwallowedException")
    try {
        load(rootDir.resolve("private.properties").inputStream())
    } catch (exception: java.io.FileNotFoundException) {
        // Provide empty properties to allow the app to be built without secrets
        Properties()
    }
}

val accountSentryDSN: String = System.getenv("SENTRY_DSN_ACCOUNT") ?: ""
val sentryDSN: String = System.getenv("SENTRY_DSN_MAIL") ?: ""
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
            signingConfig = signingConfigs["debug"]
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
        jvmTarget = JavaVersion.VERSION_17.toString()
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

sentry {
    autoInstallation {
        sentryVersion.set(libs.versions.sentry.asProvider())
    }
}

dependencies {
    implementation(files("../../proton-libs/gopenpgp/gopenpgp.aar"))

    implementation(libs.bundles.appLibs)
    implementation(libs.proton.core.proguardRules)

    implementation(project(":mail-bugreport"))
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
    implementation(project(":mail-upselling"))
    implementation(project(":mail-onboarding"))
    implementation(project(":mail-sidebar"))
    implementation(project(":uicomponents"))

    val devImplementation by configurations
    devImplementation(libs.bundles.app.debug)

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
    androidTestImplementation(libs.proton.core.accountManagerPresentationCompose)
    androidTestImplementation(libs.proton.core.accountRecoveryTest)
    androidTestImplementation(libs.proton.core.authTest)
    androidTestImplementation(libs.proton.core.planTest)
    androidTestImplementation(libs.proton.core.reportTest)
    androidTestImplementation(libs.proton.core.userRecoveryTest)
    androidTestImplementation(libs.proton.core.testRule)
    androidTestImplementation(project(":test:annotations"))
    androidTestImplementation(project(":test:idlingresources"))
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
