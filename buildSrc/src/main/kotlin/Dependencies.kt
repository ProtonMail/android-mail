import org.gradle.api.artifacts.dsl.DependencyHandler
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

object Dependencies {

    val hiltAnnotationProcessors = mutableListOf<String>().apply {
        add(AndroidX.Hilt.compiler)
        add(Dagger.hiltDaggerCompiler)
    }

    val composeLibs = mutableListOf<String>().apply {
        add(AndroidX.Activity.compose)
        add(AndroidX.Compose.foundation)
        add(AndroidX.Compose.material)
        add(AndroidX.Compose.runtime)
        add(AndroidX.Compose.ui)
        add(AndroidX.Compose.uiTooling)
    }

    val composeDebugLibs = mutableListOf<String>().apply {
        add(AndroidX.Compose.uiTestManifest)
    }

    val appLibs = mutableListOf<String>().apply {
        add(Accompanist.insets)
        add(Accompanist.pager)
        add(Accompanist.systemUiController)
        add(AndroidX.Activity.ktx)
        add(AndroidX.Compose.foundationLayout)
        add(AndroidX.Hilt.compiler)
        add(AndroidX.Hilt.navigationCompose)
        add(AndroidX.Hilt.work)
        add(AndroidX.Navigation.compose)
        add(AndroidX.Paging.compose)
        add(AndroidX.Paging.runtime)
        add(AndroidX.Room.ktx)
        add(AndroidX.Work.runtimeKtx)
        addAll(composeLibs)
        add(Gotev.cookieStore)
        add(JakeWharton.timber)
        add(Material.material)
        add(Proton.Core.account)
        add(Proton.Core.accountManager)
        add(Proton.Core.auth)
        add(Proton.Core.contact)
        add(Proton.Core.country)
        add(Proton.Core.crypto)
        add(Proton.Core.data)
        add(Proton.Core.dataRoom)
        add(Proton.Core.domain)
        add(Proton.Core.eventManager)
        add(Proton.Core.humanVerification)
        add(Proton.Core.key)
        add(Proton.Core.mailSettings)
        add(Proton.Core.network)
        add(Proton.Core.payment)
        add(Proton.Core.plan)
        add(Proton.Core.presentation)
        add(Proton.Core.user)
        add(Proton.Core.userSettings)
        add(Proton.Core.utilKotlin)
        add(Sentry.sentry)
        add(Squareup.okhttp)
        add(Squareup.plumber)
    }

    val moduleDataLibs = mutableListOf<String>().apply {
        add(AndroidX.Room.ktx)
        add(AndroidX.Work.runtimeKtx)
        add(Proton.Core.data)
        add(Proton.Core.dataRoom)
        add(Proton.Core.domain)
        add(Proton.Core.utilKotlin)
        add(Squareup.okhttp)
    }

    val modulePresentationLibs = mutableListOf<String>().apply {
        add(AndroidX.Activity.ktx)
        addAll(composeLibs)
        add(Material.material)
        add(Proton.Core.presentation)
        add(Proton.Core.utilKotlin)
    }

    val moduleDomainLibs = mutableListOf<String>().apply {
        add(Proton.Core.domain)
        add(Proton.Core.utilKotlin)
    }

    val appAnnotationProcessors = mutableListOf<String>().apply {
        add(AndroidX.Room.compiler)
        addAll(hiltAnnotationProcessors)
    }

    val appDebug = mutableListOf<String>().apply {
        add(Squareup.leakCanary)
    }

    val testLibs = mutableListOf<String>().apply {
        add(KotlinX.coroutinesTest)
        add(Junit.junit)
        add(Mockk.mockk)
        add(Proton.Core.testKotlin)
    }
    val androidTestLibs = mutableListOf<String>().apply {
        add(AndroidX.Compose.uiTest)
        add(AndroidX.Compose.uiTestJUnit)
        add(AndroidX.Test.core)
        add(AndroidX.Test.coreKtx)
        add(AndroidX.Test.runner)
        add(AndroidX.Test.rules)
        add(AndroidX.Test.espresso)
        add(Mockk.mockkAndroid)
        add(Proton.Core.testAndroidInstrumented)
    }
}

// Util functions for adding the different type dependencies from build.gradle.kts file.
fun DependencyHandler.kapt(list: List<String>) {
    list.forEach { dependency ->
        add("kapt", dependency)
    }
}

fun DependencyHandler.implementation(list: List<String>) {
    list.forEach { dependency ->
        add("implementation", dependency)
    }
}

fun DependencyHandler.debugImplementation(list: List<String>) {
    list.forEach { dependency ->
        add("debugImplementation", dependency)
    }
}

fun DependencyHandler.androidTestImplementation(list: List<String>) {
    list.forEach { dependency ->
        add("androidTestImplementation", dependency)
    }
}

fun DependencyHandler.testImplementation(list: List<String>) {
    list.forEach { dependency ->
        add("testImplementation", dependency)
    }
}

fun String.runCommand(
    workingDir: File = File("."),
    timeoutAmount: Long = 60,
    timeoutUnit: TimeUnit = TimeUnit.SECONDS
): String = ProcessBuilder(split("\\s(?=(?:[^'\"`]*(['\"`])[^'\"`]*\\1)*[^'\"`]*$)".toRegex()))
    .directory(workingDir)
    .redirectOutput(ProcessBuilder.Redirect.PIPE)
    .redirectError(ProcessBuilder.Redirect.PIPE)
    .start()
    .apply { waitFor(timeoutAmount, timeoutUnit) }
    .run {
        val error = errorStream.bufferedReader().readText().trim()
        if (error.isNotEmpty()) {
            throw IOException(error)
        }
        inputStream.bufferedReader().readText().trim()
    }
