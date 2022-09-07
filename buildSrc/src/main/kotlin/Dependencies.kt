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

import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit
import org.gradle.api.artifacts.dsl.DependencyHandler

object Dependencies {

    val hiltAnnotationProcessors = mutableListOf<String>().apply {
        add(AndroidX.Hilt.compiler)
        add(Dagger.hiltDaggerCompiler)
    }

    val composeLibs = mutableListOf<String>().apply {
        add(AndroidX.Activity.compose)
        add(AndroidX.Compose.foundation)
        add(AndroidX.Compose.foundationLayout)
        add(AndroidX.Compose.material)
        add(AndroidX.Compose.runtime)
        add(AndroidX.Compose.ui)
        add(AndroidX.Compose.uiToolingPreview)
        add(AndroidX.ConstraintLayoutCompose.constraintLayoutCompose)
    }

    val composeDebugLibs = mutableListOf<String>().apply {
        add(AndroidX.Compose.uiTestManifest)
        add(AndroidX.Compose.uiTooling)
        // CustomView: https://issuetracker.google.com/issues/227767363, fixed on AS Dolphin, remove once stable
        add(AndroidX.CustomView.customView)
        add(AndroidX.CustomView.poolingContainer)
    }

    val appLibs = mutableListOf<String>().apply {
        add(Accompanist.insets)
        add(Accompanist.pager)
        add(Accompanist.systemUiController)
        add(AndroidX.Activity.ktx)
        add(AndroidX.AppCompat.appCompat)
        add(AndroidX.Core.splashscreen)
        add(AndroidX.Hilt.compiler)
        add(AndroidX.Hilt.navigationCompose)
        add(AndroidX.Hilt.work)
        add(AndroidX.Navigation.compose)
        add(AndroidX.Paging.compose)
        add(AndroidX.Paging.runtime)
        add(AndroidX.Room.ktx)
        add(AndroidX.Work.runtimeKtx)
        add(Arrow.core)
        add(JakeWharton.timber)
        add(Material.material)
        add(Proton.Core.account)
        add(Proton.Core.accountManager)
        add(Proton.Core.auth)
        add(Proton.Core.challenge)
        add(Proton.Core.contact)
        add(Proton.Core.country)
        add(Proton.Core.crypto)
        add(Proton.Core.cryptoValidator)
        add(Proton.Core.data)
        add(Proton.Core.dataRoom)
        add(Proton.Core.domain)
        add(Proton.Core.eventManager)
        add(Proton.Core.featureFlag)
        add(Proton.Core.humanVerification)
        add(Proton.Core.key)
        add(Proton.Core.label)
        add(Proton.Core.mailSettings)
        add(Proton.Core.network)
        add(Proton.Core.payment)
        add(Proton.Core.plan)
        add(Proton.Core.presentation)
        add(Proton.Core.presentationCompose)
        add(Proton.Core.report)
        add(Proton.Core.user)
        add(Proton.Core.userSettings)
        add(Proton.Core.utilAndroidDagger)
        add(Proton.Core.utilAndroidStrictMode)
        add(Proton.Core.utilKotlin)
        add(Sentry.compose)
        add(Sentry.sentry)
        add(Squareup.okhttp)
        add(Squareup.plumber)
        addAll(composeLibs)
    }

    val moduleDataLibs = mutableListOf<String>().apply {
        add(AndroidX.DataStore.preferences)
        add(AndroidX.Room.ktx)
        add(AndroidX.Work.runtimeKtx)
        add(Arrow.core)
        add(JakeWharton.timber)
        add(JavaX.inject)
        add(KotlinX.serializationJson)
        add(Proton.Core.data)
        add(Proton.Core.dataRoom)
        add(Proton.Core.domain)
        add(Proton.Core.network)
        add(Proton.Core.utilKotlin)
        add(Squareup.okhttp)
        add(Squareup.retrofit)
    }

    val modulePresentationLibs = mutableListOf<String>().apply {
        add(Accompanist.swipeRefresh)
        add(AndroidX.Activity.ktx)
        add(AndroidX.Hilt.navigationCompose)
        add(AndroidX.Navigation.compose)
        add(AndroidX.Paging.compose)
        add(AndroidX.Paging.runtime)
        add(AndroidX.Room.ktx)
        add(Arrow.core)
        add(JakeWharton.timber)
        add(JavaX.inject)
        add(Material.material)
        add(Proton.Core.accountManager)
        add(Proton.Core.domain)
        add(Proton.Core.presentation)
        add(Proton.Core.presentationCompose)
        add(Proton.Core.utilKotlin)
        addAll(composeLibs)
    }

    val moduleDomainLibs = mutableListOf<String>().apply {
        add(Arrow.core)
        add(JavaX.inject)
        add(KotlinX.coroutinesCore)
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
        add(Cash.turbine)
        add(Junit.junit)
        add(Kotlin.test)
        add(KotlinX.coroutinesTest)
        add(Mockk.mockk)
        add(Proton.Core.testKotlin)
    }
    val androidTestLibs = mutableListOf<String>().apply {
        add(AndroidX.Compose.uiTest)
        add(AndroidX.Compose.uiTestJUnit)
        add(AndroidX.Test.core)
        add(AndroidX.Test.coreKtx)
        add(AndroidX.Test.espresso)
        add(AndroidX.Test.rules)
        add(AndroidX.Test.runner)
        add(AndroidX.Test.uiautomator)
        add(Cash.turbine)
        add(Kotlin.test)
        add(Mockk.mockkAndroid)
        add(Proton.Core.paymentIap)
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
