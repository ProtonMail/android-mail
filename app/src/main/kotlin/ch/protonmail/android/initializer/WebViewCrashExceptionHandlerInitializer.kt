/*
 * Copyright (c) 2025 Proton Technologies AG
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
import ch.protonmail.android.mailcrashrecord.domain.usecase.SaveMessageBodyWebViewCrash
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.runBlocking
import timber.log.Timber

class WebViewCrashExceptionHandlerInitializer : Initializer<Unit> {

    override fun create(context: Context) {
        // Save the original handler so the system still handles the crash
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            // The layer-allocation crash is thrown from native WebView code and reaches the
            // uncaught handler wrapped in a Chromium exception (e.g. JniAndroid$UncaughtExceptionException),
            // so we walk the cause chain instead of checking only the top-level type.
            val layerCrash = throwable.findLayerCrash()

            if (layerCrash != null) {
                runBlocking {
                    EntryPointAccessors.fromApplication(
                        context.applicationContext,
                        WebViewCrashExceptionHandlerEntryPoint::class.java
                    ).saveMessageBodyWebViewCrash().invoke()
                }
                Timber.e(layerCrash, "WebView layer crash captured")
            }

            // Pass the exception onward so the app still crashes as normal
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    private fun Throwable.findLayerCrash(): Throwable? = generateSequence(this) { it.cause }
        .firstOrNull { it.message?.contains(LAYER_CRASH_MARKER) == true }

    override fun dependencies(): List<Class<out Initializer<*>?>?> = emptyList()

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface WebViewCrashExceptionHandlerEntryPoint {
        fun saveMessageBodyWebViewCrash(): SaveMessageBodyWebViewCrash
    }

    private companion object {

        const val LAYER_CRASH_MARKER = "Unable to create layer"
    }
}
