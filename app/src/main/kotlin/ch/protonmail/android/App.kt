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
import androidx.lifecycle.ProcessLifecycleOwner
import ch.protonmail.android.callbacks.AutoLockLifecycleCallbacks
import ch.protonmail.android.callbacks.SecureActivityLifecycleCallbacks
import ch.protonmail.android.initializer.MainInitializer
import ch.protonmail.android.logging.LogsFileHandlerLifecycleObserver
import ch.protonmail.android.mailbugreport.domain.LogsExportFeatureSetting
import ch.protonmail.android.mailbugreport.domain.annotations.LogsExportFeatureSettingValue
import ch.protonmail.android.mailcommon.domain.benchmark.BenchmarkTracer
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import javax.inject.Provider

@HiltAndroidApp
internal class App : Application() {

    @Inject
    lateinit var secureActivityLifecycleCallbacks: SecureActivityLifecycleCallbacks

    @Inject
    lateinit var lockScreenLifecycleCallbacks: AutoLockLifecycleCallbacks

    @Inject
    lateinit var benchmarkTracer: BenchmarkTracer

    @Inject
    @LogsExportFeatureSettingValue
    lateinit var logsExportFeatureSetting: Provider<LogsExportFeatureSetting>

    override fun onCreate() {
        super.onCreate()

        benchmarkTracer.begin("proton-app-init")

        MainInitializer.init(this)
        registerActivityLifecycleCallbacks(secureActivityLifecycleCallbacks)
        registerActivityLifecycleCallbacks(lockScreenLifecycleCallbacks)

        addLogsFileHandlerObserver()

        benchmarkTracer.end()
    }

    private fun addLogsFileHandlerObserver() {
        if (logsExportFeatureSetting.get().isEnabled) {
            ProcessLifecycleOwner.get().lifecycle.addObserver(LogsFileHandlerLifecycleObserver(this))
        }
    }
}
