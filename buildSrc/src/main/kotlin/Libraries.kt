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

object Accompanist {

    private const val version = Versions.Accompanist.accompanist

    const val webview = "com.google.accompanist:accompanist-webview:$version"
    const val permissions = "com.google.accompanist:accompanist-permissions:$version"
}

object AndroidTools {

    private const val version = Versions.AndroidTools.desugarJdkLibs

    const val desugarJdkLibs = "com.android.tools:desugar_jdk_libs:$version"
}

object AndroidX {

    object Activity {

        private const val version = Versions.AndroidX.activity

        const val ktx = "androidx.activity:activity-ktx:$version"
        const val compose = "androidx.activity:activity-compose:$version"
    }

    object AppCompat {

        private const val version = Versions.AndroidX.appCompat

        const val appCompat = "androidx.appcompat:appcompat:$version"
    }

    object ConstraintLayoutCompose {

        private const val version = Versions.AndroidX.constraintLayoutCompose

        const val constraintLayoutCompose = "androidx.constraintlayout:constraintlayout-compose:$version"
    }

    object Compose {

        private const val version = Versions.AndroidX.compose
        private const val foundationVersion = Versions.AndroidX.composeFoundation
        private const val materialVersion = Versions.AndroidX.composeMaterial
        private const val material3Version = Versions.AndroidX.material3

        const val foundation = "androidx.compose.foundation:foundation:$foundationVersion"
        const val foundationLayout = "androidx.compose.foundation:foundation-layout:$foundationVersion"
        const val material = "androidx.compose.material:material:$materialVersion"
        const val material3 = "androidx.compose.material3:material3:$material3Version"
        const val runtime = "androidx.compose.runtime:runtime:$version"
        const val runtimeLiveData = "androidx.compose.runtime:runtime-livedata:$version"
        const val ui = "androidx.compose.ui:ui:$version"
        const val uiTooling = "androidx.compose.ui:ui-tooling:$version"
        const val uiToolingPreview = "androidx.compose.ui:ui-tooling-preview:$version"
        const val uiTest = "androidx.compose.ui:ui-test:$version"
        const val uiTestJUnit = "androidx.compose.ui:ui-test-junit4:$version"
        const val uiTestManifest = "androidx.compose.ui:ui-test-manifest:$version"
    }

    object Core {

        const val core = "androidx.core:core-ktx:${Versions.AndroidX.core}"
        const val annotation = "androidx.annotation:annotation:${Versions.AndroidX.annotation}"
        const val splashscreen = "androidx.core:core-splashscreen:${Versions.AndroidX.splashscreen}"
    }

    // https://issuetracker.google.com/issues/227767363
    object CustomView {

        private const val poolingContainerVersion = Versions.AndroidX.customViewPoolingContainer

        const val customView = "androidx.customview:customview:${Versions.AndroidX.customView}"
        const val poolingContainer = "androidx.customview:customview-poolingcontainer:$poolingContainerVersion"
    }

    object DataStore {

        private const val version = Versions.AndroidX.datastore

        const val preferences = "androidx.datastore:datastore-preferences:$version"
    }

    object Hilt {

        private const val version = Versions.AndroidX.hilt

        const val android = "com.google.dagger:hilt-android:$version"
        const val compiler = "androidx.hilt:hilt-compiler:$version"
        const val navigationCompose = "androidx.hilt:hilt-navigation-compose:$version"
        const val work = "androidx.hilt:hilt-work:$version"
    }

    object Lifecycle {

        private const val version = Versions.AndroidX.lifecycle

        const val process = "androidx.lifecycle:lifecycle-process:$version"
    }

    object Biometrics {

        private const val version = Versions.AndroidX.biometrics

        const val biometric = "androidx.biometric:biometric-ktx:$version"
    }

    object Navigation {

        private const val version = Versions.AndroidX.navigation

        const val compose = "androidx.navigation:navigation-compose:$version"
    }

    object Paging {

        private const val version = Versions.AndroidX.paging

        const val runtime = "androidx.paging:paging-runtime:$version"
        const val common = "androidx.paging:paging-common:$version"
        const val testing = "androidx.paging:paging-testing:${Versions.AndroidX.pagingCompose}"
        const val compose = "androidx.paging:paging-compose:${Versions.AndroidX.pagingCompose}"
    }

    object ProfileInstaller {

        private const val version = Versions.ProfileInstaller.profileInstaller

        const val profileInstaller = "androidx.profileinstaller:profileinstaller:$version"
    }

    object Room {

        private const val version = Versions.AndroidX.room

        const val ktx = "androidx.room:room-ktx:$version"
        const val compiler = "androidx.room:room-compiler:$version"
    }

    object Test {

        const val androidJUnit = "androidx.test.ext:junit:${Versions.AndroidX.testAndroidJUnit}"
        const val core = "androidx.test:core:${Versions.AndroidX.testCore}"
        const val coreKtx = "androidx.test:core-ktx:${Versions.AndroidX.testCore}"
        const val espresso = "androidx.test.espresso:espresso-core:${Versions.AndroidX.testEspresso}"
        const val espressoWeb = "androidx.test.espresso:espresso-web:${Versions.AndroidX.testEspresso}"
        const val espressoIntents = "androidx.test.espresso:espresso-intents:${Versions.AndroidX.testEspresso}"
        const val macroBenchmark = "androidx.benchmark:benchmark-macro-junit4:${Versions.AndroidX.testMacroBenchmark}"
        const val orchestrator = "androidx.test:orchestrator:${Versions.AndroidX.testOrchestrator}"
        const val monitor = "androidx.test:monitor:${Versions.AndroidX.testMonitor}"
        const val runner = "androidx.test:runner:${Versions.AndroidX.testRunner}"
        const val rules = "androidx.test:rules:${Versions.AndroidX.testRules}"
        const val uiautomator = "androidx.test.uiautomator:uiautomator:${Versions.AndroidX.testUiautomator}"
    }

    object Profile{
        object Tracing {

            private const val version = Versions.AndroidX.tracing

            const val tracing = "androidx.tracing:tracing:$version"
        }

        object ComposeTracing {

            private const val version = Versions.AndroidX.composeTracing

            const val composeTracing = "androidx.compose.runtime:runtime-tracing:$version"
        }

        object Perfetto {

            private const val version = Versions.AndroidX.perfetto

            const val perfetto = "androidx.tracing:tracing-perfetto:$version"
            const val perfettoBinary = "androidx.tracing:tracing-perfetto-binary:$version"
        }
    }

    object WebKit {

        private const val version = Versions.AndroidX.webkit

        const val webkit = "androidx.webkit:webkit:$version"
    }

    object Work {

        private const val version = Versions.AndroidX.work

        const val runtimeKtx = "androidx.work:work-runtime-ktx:$version"
    }
}

object Arrow {

    private const val version = Versions.Arrow.core

    const val core = "io.arrow-kt:arrow-core:$version"
}

object Cash {

    private const val version = Versions.Cash.turbine

    const val turbine = "app.cash.turbine:turbine:$version"
}

object Coil {

    private const val version = Versions.Coil.coil

    const val coil = "io.coil-kt:coil-compose:$version"
}

object Dagger {

    private const val version = Versions.Dagger.dagger

    const val hiltAndroid = "com.google.dagger:hilt-android:$version"
    const val hiltAndroidTesting = "com.google.dagger:hilt-android-testing:$version"
    const val hiltCore = "com.google.dagger:hilt-core:$version"
    const val hiltDaggerCompiler = "com.google.dagger:hilt-compiler:$version"
}

object Detekt {

    const val api = "io.gitlab.arturbosch.detekt:detekt-api:${Versions.Detekt.detekt}"
    const val test = "io.gitlab.arturbosch.detekt:detekt-test:${Versions.Detekt.detekt}"
}

object Firebase {

    const val bom = "com.google.firebase:firebase-bom:${Versions.Firebase.bom}"
    const val messaging = "com.google.firebase:firebase-messaging-ktx"
}

object JakeWharton {

    const val timber = "com.jakewharton.timber:timber:${Versions.JakeWharton.timber}"
}

object JavaX {

    const val inject = "javax.inject:javax.inject:${Versions.JavaX.inject}"
}

object Jsoup {

    const val jsoup = "org.jsoup:jsoup:${Versions.Jsoup.jsoup}"
}

object Junit {

    const val junit = "junit:junit:${Versions.Junit.junit}"
}

object Ksp {

    const val symbolProcessingApi = "com.google.devtools.ksp:symbol-processing-api:${Versions.Ksp.symbolProcessingApi}"
}

object Kotlin {

    private const val version = Versions.Kotlin.kotlin
    const val test = "org.jetbrains.kotlin:kotlin-test:$version"
    const val testJunit = "org.jetbrains.kotlin:kotlin-test-junit:$version"
}

object KotlinCompileTesting {

    const val kotlinCompileTesting =
        "com.github.tschuchortdev:kotlin-compile-testing-ksp:${Versions.KotlinCompileTesting.kotlinCompileTesting}"
}

object KotlinX {

    const val coroutinesCore = "org.jetbrains.kotlinx:kotlinx-coroutines-core:${Versions.KotlinX.coroutines}"
    const val coroutinesTest = "org.jetbrains.kotlinx:kotlinx-coroutines-test:${Versions.KotlinX.coroutines}"
    const val immutableCollections = "org.jetbrains.kotlinx:kotlinx-collections-immutable:" +
        "${Versions.KotlinX.immutableCollections}"
    const val serializationJson =
        "org.jetbrains.kotlinx:kotlinx-serialization-json:${Versions.KotlinX.serializationJson}"
}

object Material {

    const val material = "com.google.android.material:material:${Versions.Material.material}"
}

object Mockk {

    const val mockk = "io.mockk:mockk:${Versions.Mockk.mockk}"
    const val mockkAndroid = "io.mockk:mockk-android:${Versions.Mockk.mockk}"
}

object Proton {

    object Core {

        val account = coreArtifact("account", Versions.Proton.core)
        val accountData = coreArtifact("account-data", Versions.Proton.core)
        val accountManager = coreArtifact("account-manager", Versions.Proton.core)
        val accountManagerPresentationCompose =
            coreArtifact("account-manager-presentation-compose", Versions.Proton.core)
        val accountRecovery = coreArtifact("account-recovery", Versions.Proton.core)
        val accountRecoveryTest = coreArtifact("account-recovery-test", Versions.Proton.core)
        val auth = coreArtifact("auth", Versions.Proton.core)
        val authTest = coreArtifact("auth-test", Versions.Proton.core)
        val challenge = coreArtifact("challenge", Versions.Proton.core)
        val contact = coreArtifact("contact", Versions.Proton.core)
        val contactDomain = coreArtifact("contact-domain", Versions.Proton.core)
        val country = coreArtifact("country", Versions.Proton.core)
        val crypto = coreArtifact("crypto", Versions.Proton.core)
        val cryptoValidator = coreArtifact("crypto-validator", Versions.Proton.core)
        val data = coreArtifact("data", Versions.Proton.core)
        val dataRoom = coreArtifact("data-room", Versions.Proton.core)
        val domain = coreArtifact("domain", Versions.Proton.core)
        val eventManager = coreArtifact("event-manager", Versions.Proton.core)
        val featureFlag = coreArtifact("feature-flag", Versions.Proton.core)
        val humanVerification = coreArtifact("human-verification", Versions.Proton.core)
        val key = coreArtifact("key", Versions.Proton.core)
        val keyTransparency = coreArtifact("key-transparency", Versions.Proton.core)
        val label = coreArtifact("label", Versions.Proton.core)
        val labelData = coreArtifact("label-data", Versions.Proton.core)
        val labelDomain = coreArtifact("label-domain", Versions.Proton.core)
        val mailSettings = coreArtifact("mail-settings", Versions.Proton.core)
        val mailSendPreferences = coreArtifact("mail-send-preferences", Versions.Proton.core)
        val network = coreArtifact("network", Versions.Proton.core)
        val notification = coreArtifact("notification", Versions.Proton.core)
        val observability = coreArtifact("observability", Versions.Proton.core)
        val payment = coreArtifact("payment", Versions.Proton.core)
        val paymentIap = coreArtifact("payment-iap", Versions.Proton.core)
        val plan = coreArtifact("plan", Versions.Proton.core)
        val planCompose = coreArtifact("plan-presentation-compose", Versions.Proton.core)
        val planTest = coreArtifact("plan-test", Versions.Proton.core)
        val presentation = coreArtifact("presentation", Versions.Proton.core)
        val presentationCompose = coreArtifact("presentation-compose", Versions.Proton.core)
        val proguardRules = coreArtifact("proguard-rules", Versions.Proton.core)
        val push = coreArtifact("push", Versions.Proton.core)
        val report = coreArtifact("report", Versions.Proton.core)
        val reportTest = coreArtifact("report-test", Versions.Proton.core)
        val telemetry = coreArtifact("telemetry", Versions.Proton.core)
        val user = coreArtifact("user", Versions.Proton.core)
        val userSettings = coreArtifact("user-settings", Versions.Proton.core)
        val configData = coreArtifact("configuration-data", Versions.Proton.core)
        val configDaggerStatic = coreArtifact("configuration-dagger-staticdefaults", Versions.Proton.core)
        val configDaggerContentProvider = coreArtifact("configuration-dagger-content-resolver", Versions.Proton.core)
        val utilAndroidDagger = coreArtifact("util-android-dagger", Versions.Proton.core)
        val utilAndroidSentry = coreArtifact("util-android-sentry", Versions.Proton.core)
        val utilAndroidStrictMode = coreArtifact("util-android-strict-mode", Versions.Proton.core)
        val utilKotlin = coreArtifact("util-kotlin", Versions.Proton.core)
        val testKotlin = coreArtifact("test-kotlin", Versions.Proton.core)
        val testAndroid = coreArtifact("test-android", Versions.Proton.core)
        val testAndroidInstrumented = coreArtifact(
            "test-android-instrumented",
            Versions.Proton.core
        )
        val testQuark = coreArtifact("test-quark", Versions.Proton.core)
    }
}

object Squareup {

    const val kotlinPoetKsp = "com.squareup:kotlinpoet-ksp:${Versions.Squareup.kotlinPoetKsp}"
    const val leakCanary = "com.squareup.leakcanary:leakcanary-android:${Versions.Squareup.leakCanary}"
    const val mockWebServer = "com.squareup.okhttp3:mockwebserver:${Versions.Squareup.okhttp}"
    const val okhttp = "com.squareup.okhttp3:okhttp:${Versions.Squareup.okhttp}"
    const val okhttpTls = "com.squareup.okhttp3:okhttp-tls:${Versions.Squareup.okhttp}"
    const val plumber = "com.squareup.leakcanary:plumber-android:${Versions.Squareup.leakCanary}"
    const val retrofit = "com.squareup.retrofit2:retrofit:${Versions.Squareup.retrofit}"
}

object Sentry {

    const val sentry = "io.sentry:sentry-android:${Versions.Sentry.sentry}"
    const val compose = "io.sentry:sentry-compose-android:${Versions.Sentry.sentry}"
}

fun coreArtifact(name: String, version: String) = "me.proton.core:$name:$version"
