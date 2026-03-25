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
import ch.protonmail.android.logging.RustLogsAttachmentProcessor
import ch.protonmail.android.logging.SentryUserObserver
import ch.protonmail.android.logging.SentryWebViewInfoProvider
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import io.sentry.SentryLevel
import io.sentry.SentryOptions
import io.sentry.android.core.SentryAndroid
import io.sentry.android.timber.SentryTimberIntegration
import kotlinx.coroutines.flow.MutableStateFlow
import me.proton.core.configuration.EnvironmentConfigurationDefaults

class SentryInitializer : Initializer<Unit> {

    override fun create(context: Context) {
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            SentryInitializerEntryPoint::class.java
        )
        val isCrashReportsEnabled = MutableStateFlow(true)

        SentryAndroid.init(context.applicationContext) { options: SentryOptions ->
            options.dsn = BuildConfig.SENTRY_DSN
            options.release = BuildConfig.VERSION_NAME
            options.environment = EnvironmentConfigurationDefaults.host
            options.addIntegration(
                SentryTimberIntegration(
                    minEventLevel = SentryLevel.WARNING,
                    minBreadcrumbLevel = SentryLevel.INFO
                )
            )
            options.addEventProcessor(entryPoint.rustLogsAttachmentProcessor())
            options.setBeforeSend { event, _ ->
                if (isCrashReportsEnabled.value) event else null
            }
        }

        entryPoint.observer().start {
            isCrashReportsEnabled.value = it
        }

        entryPoint.webViewInfoProvider().setWebViewTags()
    }

    override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface SentryInitializerEntryPoint {

        fun observer(): SentryUserObserver
        fun rustLogsAttachmentProcessor(): RustLogsAttachmentProcessor
        fun webViewInfoProvider(): SentryWebViewInfoProvider
    }
}
