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

package ch.protonmail.android.initializer

import android.content.Context
import androidx.startup.Initializer
import ch.protonmail.android.BuildConfig
import ch.protonmail.android.mailbugreport.data.FileLoggingTree
import ch.protonmail.android.mailbugreport.domain.LogsExportFeatureSetting
import ch.protonmail.android.mailbugreport.domain.LogsFileHandler
import ch.protonmail.android.mailbugreport.domain.annotations.LogsExportFeatureSettingValue
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import me.proton.core.util.android.sentry.TimberLogger
import me.proton.core.util.kotlin.CoreLogger
import timber.log.Timber
import javax.inject.Provider

class LoggerInitializer : Initializer<Unit> {

    override fun create(context: Context) {
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        // Forward Core Logs to Timber, using TimberLogger.
        CoreLogger.set(TimberLogger)

        val accessors = EntryPointAccessors.fromApplication(
            context.applicationContext,
            LoggerInitializerEntryPoint::class.java
        )

        val isLoggingEnabled = accessors.logsExportFeatureSetting().get().isEnabled
        if (isLoggingEnabled.not()) return

        val logsFileHandler = accessors.logsFileHandlerProvider()
        Timber.plant(FileLoggingTree(logsFileHandler))
    }

    override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface LoggerInitializerEntryPoint {

        fun logsFileHandlerProvider(): LogsFileHandler

        @LogsExportFeatureSettingValue
        fun logsExportFeatureSetting(): Provider<LogsExportFeatureSetting>
    }
}
