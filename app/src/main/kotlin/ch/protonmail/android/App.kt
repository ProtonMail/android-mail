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

package ch.protonmail.android

import android.app.Application
import androidx.compose.runtime.Composer
import androidx.compose.runtime.ExperimentalComposeRuntimeApi
import androidx.lifecycle.ProcessLifecycleOwner
import ch.protonmail.android.callbacks.SecureActivityLifecycleCallbacks
import ch.protonmail.android.initializer.MainInitializer
import ch.protonmail.android.initializer.background.RustEventLoopErrorLifecycleObserver
import ch.protonmail.android.logging.LogsFileHandlerLifecycleObserver
import ch.protonmail.android.mailbugreport.domain.LogsExportFeatureSetting
import ch.protonmail.android.mailbugreport.domain.annotations.LogsExportFeatureSettingValue
import ch.protonmail.android.mailcommon.domain.benchmark.BenchmarkTracer
import ch.protonmail.android.mailcrashrecord.domain.usecase.SaveMessageBodyWebViewCrash
import ch.protonmail.android.mailevents.presentation.AppOpenLifecycleObserver
import ch.protonmail.android.mailnotifications.domain.FirebaseMessagingTokenLifecycleObserver
import ch.protonmail.android.mailsession.data.initializer.DatabaseLifecycleObserver
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import javax.inject.Provider

@HiltAndroidApp
internal class App : Application() {

    @Inject
    lateinit var secureActivityLifecycleCallbacks: SecureActivityLifecycleCallbacks

    @Inject
    lateinit var benchmarkTracer: BenchmarkTracer

    @Inject
    @LogsExportFeatureSettingValue
    lateinit var logsExportFeatureSetting: Provider<LogsExportFeatureSetting>

    @Inject
    lateinit var databaseLifecycleObserver: Provider<DatabaseLifecycleObserver>

    @Inject
    lateinit var firebaseLifecycleObserver: Provider<FirebaseMessagingTokenLifecycleObserver>

    @Inject
    lateinit var eventLoopLifecycleObserver: Provider<RustEventLoopErrorLifecycleObserver>

    @Inject
    lateinit var appOpenLifecycleObserver: Provider<AppOpenLifecycleObserver>

    @Inject
    lateinit var saveMessageBodyWebViewCrash: SaveMessageBodyWebViewCrash

    @OptIn(ExperimentalComposeRuntimeApi::class)
    override fun onCreate() {
        super.onCreate()

        // Richer Compose-related stack traces. Does not work on prod builds nor it is recommended to do so there.
        Composer.setDiagnosticStackTraceEnabled(BuildConfig.DEBUG)

        benchmarkTracer.begin("proton-app-init")

        MainInitializer.init(this)
        registerActivityLifecycleCallbacks(secureActivityLifecycleCallbacks)

        addLogsFileHandlerObserver()
        addDatabaseObserver()
        addFirebaseTokenLifecycleObserver()
        addEventLoopObserver()
        addAppOpenLifecycleObserver()

        benchmarkTracer.end()
    }

    private fun addLogsFileHandlerObserver() {
        if (logsExportFeatureSetting.get().isEnabled) {
            ProcessLifecycleOwner.get().lifecycle.addObserver(LogsFileHandlerLifecycleObserver(this))
        }
    }

    private fun addDatabaseObserver() {
        ProcessLifecycleOwner.get().lifecycle.addObserver(databaseLifecycleObserver.get())
    }

    private fun addFirebaseTokenLifecycleObserver() {
        ProcessLifecycleOwner.get().lifecycle.addObserver(firebaseLifecycleObserver.get())
    }

    private fun addEventLoopObserver() {
        ProcessLifecycleOwner.get().lifecycle.addObserver(eventLoopLifecycleObserver.get())
    }

    private fun addAppOpenLifecycleObserver() {
        ProcessLifecycleOwner.get().lifecycle.addObserver(appOpenLifecycleObserver.get())
    }
}
