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
        // add(Core.account)
        // add(Core.accountManager)
        // add(Core.auth)
        // add(Core.country)
        // add(Core.crypto)
        // add(Core.data)
        // add(Core.dataRoom)
        // add(Core.domain)
        // add(Core.humanVerification)
        // add(Core.key)
        // add(Core.network)
        // add(Core.payment)
        // add(Core.plan)
        add(Core.presentation)
        // add(Core.user)
        // add(Core.userSettings)
        // add(Core.utilKotlin)
        add(Gotev.cookieStore)
        add(JakeWharton.timber)
        add(Material.material)
        add(Squareup.okhttp)
        add(Squareup.plumber)
    }

    val moduleDataLibs = mutableListOf<String>().apply {
        add(AndroidX.Room.ktx)
        add(AndroidX.Work.runtimeKtx)
        add(Core.data)
        add(Core.dataRoom)
        add(Core.domain)
        add(Core.utilKotlin)
        add(Squareup.okhttp)
    }

    val modulePresentationLibs = mutableListOf<String>().apply {
        add(AndroidX.Activity.ktx)
        addAll(composeLibs)
        add(Core.presentation)
        add(Core.utilKotlin)
        add(Material.material)
    }

    val moduleDomainLibs = mutableListOf<String>().apply {
        add(Core.domain)
        add(Core.utilKotlin)
    }

    val appAnnotationProcessors = mutableListOf<String>().apply {
        add(AndroidX.Room.compiler)
        addAll(hiltAnnotationProcessors)
    }

    val appDebug = mutableListOf<String>().apply {
        add(Squareup.leakCanary)
    }

    val testLibs = mutableListOf<String>().apply {
        add(Test.junit)
    }
    val androidTestLibs = mutableListOf<String>().apply {
        add(Test.core)
        add(Test.coreKtx)
        add(Test.runner)
        add(Test.rules)
        add(Test.espresso)
        add(AndroidX.Compose.uiTest)
        add(AndroidX.Compose.uiTestJUnit)
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
