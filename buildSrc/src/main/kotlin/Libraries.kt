object Accompanist {
    private const val version = Versions.Accompanist.accompanist

    const val insets = "com.google.accompanist:accompanist-insets:$version"
    const val pager = "com.google.accompanist:accompanist-pager:$version"
    const val systemUiController = "com.google.accompanist:accompanist-systemuicontroller:$version"
    const val swipeRefresh = "com.google.accompanist:accompanist-swiperefresh:$version"
}

object AndroidX {

    object Activity {
        private const val version = Versions.AndroidX.activity

        const val ktx = "androidx.activity:activity-ktx:$version"
        const val compose = "androidx.activity:activity-compose:$version"
    }

    object Compose {
        private const val version = Versions.AndroidX.compose

        const val foundation = "androidx.compose.foundation:foundation:$version"
        const val foundationLayout = "androidx.compose.foundation:foundation-layout:$version"
        const val material = "androidx.compose.material:material:$version"
        const val runtime = "androidx.compose.runtime:runtime:$version"
        const val ui = "androidx.compose.ui:ui:$version"
        const val uiTooling = "androidx.compose.ui:ui-tooling:$version"
        const val uiTest = "androidx.compose.ui:ui-test:$version"
        const val uiTestJUnit = "androidx.compose.ui:ui-test-junit4:$version"
        const val uiTestManifest = "androidx.compose.ui:ui-test-manifest:$version"
    }

    object Core {
        private const val version = Versions.AndroidX.annotation

        const val annotation = "androidx.annotation:annotation:$version"
    }

    object Hilt {
        private const val version = Versions.AndroidX.hilt
        const val versionNavigationCompose = Versions.AndroidX.hiltNavigationCompose

        const val compiler = "androidx.hilt:hilt-compiler:$version"
        const val navigationCompose = "androidx.hilt:hilt-navigation-compose:$versionNavigationCompose"
        const val work = "androidx.hilt:hilt-work:$version"
    }

    object Lifecycle {
        private const val version = Versions.AndroidX.lifecycle

        const val viewmodelKtx = "androidx.lifecycle:lifecycle-viewmodel-ktx:$version"
        const val viewmodelCompose = "androidx.lifecycle:lifecycle-viewmodel-compose:$version"
        const val process = "androidx.lifecycle:lifecycle-process:$version"
        const val runtimeTesting = "androidx.lifecycle:lifecycle-runtime-testing:$version"
    }

    object Navigation {
        private const val version = Versions.AndroidX.navigation

        const val compose = "androidx.navigation:navigation-compose:${Versions.AndroidX.navigationCompose}"
    }

    object Paging {
        private const val version = Versions.AndroidX.paging

        const val runtime = "androidx.paging:paging-runtime:$version"
        const val common = "androidx.paging:paging-common:$version"
        const val compose = "androidx.paging:paging-compose:${Versions.AndroidX.pagingCompose}"
    }

    object Room {
        private const val version = Versions.AndroidX.room

        const val ktx = "androidx.room:room-ktx:$version"
        const val compiler = "androidx.room:room-compiler:$version"
    }

    object Test {
        private const val version = Versions.AndroidX.test

        const val core = "androidx.test:core:$version"
        const val coreKtx = "androidx.test:core-ktx:$version"
        const val runner = "androidx.test:runner:$version"
        const val rules = "androidx.test:rules:$version"
        const val espresso = "androidx.test.espresso:espresso-core:${Versions.AndroidX.testEspresso}"
        const val extJunit = "androidx.test.ext:junit:${Versions.AndroidX.testExtJunit}"
    }

    object Work {
        private const val version = Versions.AndroidX.work

        const val runtimeKtx = "androidx.work:work-runtime-ktx:$version"
    }
}

object Core {
    val account = coreArtifact("account", Versions.Core.account)
    val accountManager = coreArtifact("account-manager", Versions.Core.accountManager)
    val auth = coreArtifact("auth", Versions.Core.auth)
    val contact = coreArtifact("contact", Versions.Core.contact)
    val country = coreArtifact("country", Versions.Core.country)
    val crypto = coreArtifact("crypto", Versions.Core.crypto)
    val data = coreArtifact("data", Versions.Core.data)
    val dataRoom = coreArtifact("data-room", Versions.Core.dataRoom)
    val domain = coreArtifact("domain", Versions.Core.domain)
    val eventManager = coreArtifact("event-manager", Versions.Core.eventManager)
    val humanVerification = coreArtifact("human-verification", Versions.Core.humanVerification)
    val key = coreArtifact("key", Versions.Core.key)
    val mailSettings = coreArtifact("mail-settings", Versions.Core.mailSettings)
    val network = coreArtifact("network", Versions.Core.network)
    val payment = coreArtifact("payment", Versions.Core.payment)
    val plan = coreArtifact("plan", Versions.Core.plan)
    val presentation = coreArtifact("presentation", Versions.Core.presentation)
    val user = coreArtifact("user", Versions.Core.user)
    val userSettings = coreArtifact("user-settings", Versions.Core.userSettings)
    val utilKotlin = coreArtifact("util-kotlin", Versions.Core.utilKotlin)
    val testKotlin = coreArtifact("test-kotlin", Versions.Core.testKotlin)
    val testAndroid = coreArtifact("test-android", Versions.Core.testAndroid)
    val testAndroidInstrumented = coreArtifact(
        "test-android-instrumented",
        Versions.Core.testAndroidInstrumented
    )
}

object Dagger {
    private const val version = Versions.Dagger.dagger

    const val hiltAndroid = "com.google.dagger:hilt-android:$version"
    const val hiltDaggerCompiler = "com.google.dagger:hilt-compiler:$version"
}

object Gotev {
    const val cookieStore = "net.gotev:cookie-store:${Versions.Gotev.cookieStore}"
}

object JakeWharton {
    const val timber = "com.jakewharton.timber:timber:${Versions.JakeWharton.timber}"
}

object Junit {
    const val junit = "junit:junit:${Versions.Junit.junit}"
}

object Kotlin {
    private const val version = Versions.Kotlin.kotlin
}

object KotlinX {
    const val coroutinesCore = "org.jetbrains.kotlinx:kotlinx-coroutines-core:${Versions.KotlinX.coroutines}"
    const val coroutinesTest = "org.jetbrains.kotlinx:kotlinx-coroutines-test:${Versions.KotlinX.coroutines}"
    const val serializationJson = "org.jetbrains.kotlinx:kotlinx-serialization-json:${Versions.KotlinX.serializationJson}"
}

object Material {
    const val material = "com.google.android.material:material:${Versions.Android.material}"
}

object Mockk {
    const val mockk = "io.mockk:mockk:${Versions.Mockk.mockk}"
    const val mockkAndroid = "io.mockk:mockk-android:${Versions.Mockk.mockk}"
}

object Squareup {
    const val leakCanary = "com.squareup.leakcanary:leakcanary-android:${Versions.Squareup.leakCanary}"
    const val okhttp = "com.squareup.okhttp3:okhttp:${Versions.Squareup.okhttp}"
    const val plumber = "com.squareup.leakcanary:plumber-android:${Versions.Squareup.leakCanary}"
    const val retrofit = "com.squareup.retrofit2:retrofit:${Versions.Squareup.retrofit}"
}

object Sentry {
    const val sentry = "io.sentry:sentry-android:${Versions.Sentry.sentry}"
}

fun coreArtifact(name: String, version: String) = "me.proton.core:$name:$version"
