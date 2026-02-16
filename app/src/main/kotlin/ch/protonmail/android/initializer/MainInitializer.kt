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
import androidx.startup.AppInitializer
import androidx.startup.Initializer
import ch.protonmail.android.BuildConfig
import ch.protonmail.android.initializer.background.BackgroundExecutionInitializer
import ch.protonmail.android.initializer.strictmode.StrictModeInitializer
import ch.protonmail.android.legacymigration.domain.initializer.LegacyAppCleanupInitializer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import me.proton.android.core.humanverification.presentation.ChallengeInitializer

class MainInitializer : Initializer<Unit> {

    // create a nested class to initialise some of the non-essential time consuming dependencies in a background thread
    class MainAsyncInitializer : Initializer<Unit> {
        override fun create(context: Context) {
            // No-op needed
        }

        override fun dependencies() = mailDependencies() + releaseOnlyDependenciesIfNeeded()

        private fun mailDependencies(): List<Class<out Initializer<*>?>> = emptyList()

        private fun releaseOnlyDependenciesIfNeeded() =
            if (BuildConfig.DEBUG) emptyList() else listOf(SentryInitializer::class.java)
    }

    override fun create(context: Context) {
        // No-op needed
    }

    override fun dependencies() = mailDependencies()

    private fun mailDependencies() = listOf(
        LoggerInitializer::class.java,
        StrictModeInitializer::class.java,
        ThemeObserverInitializer::class.java,
        AutoLockInitializer::class.java,
        NotificationInitializer::class.java,
        NotificationHandlersInitializer::class.java,
        RustMailCommonInitializer::class.java,
        ChallengeInitializer::class.java,
        BackgroundExecutionInitializer::class.java,
        RustNetworkObserverInitializer::class.java,
        WebViewCrashExceptionHandlerInitializer::class.java,
        EventsStartupInitializer::class.java
    )

    companion object {

        fun init(appContext: Context) {
            with(AppInitializer.getInstance(appContext)) {
                // WorkManager need to be initialized before any other dependant initializer.
                initializeComponent(WorkManagerInitializer::class.java)

                // Cleanup legacy app data if needed
                initializeComponent(LegacyAppCleanupInitializer::class.java)

                initializeComponent(MainInitializer::class.java)
            }

            // Initialize some non-essential initializers in a background thread. They are taking most
            // time to initialize. This line must be after initialisation of MainInitializer above, because
            // AppInitializer has an internal lock, which prevents simultaneous initialisation
            CoroutineScope(Dispatchers.Default + SupervisorJob()).launch {
                AppInitializer.getInstance(appContext).initializeComponent(MainAsyncInitializer::class.java)
            }
        }
    }
}
