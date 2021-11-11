/*
 * Copyright (c) 2021 Proton Technologies AG
 * This file is part of Proton Technologies AG and ProtonCore.
 *
 * ProtonCore is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ProtonCore is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with ProtonCore.  If not, see <https://www.gnu.org/licenses/>.
 */

package ch.protonmail.android.init

import android.content.Context
import android.util.Log
import androidx.startup.Initializer
import ch.protonmail.android.BuildConfig
import ch.protonmail.android.log.AppLogger
import me.proton.core.util.kotlin.CoreLogger
import timber.log.Timber

class LoggerInitializer : Initializer<Unit> {

    override fun create(context: Context) {
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        } else {
            Timber.plant(CrashReportingTree())
        }

        // Forward Core Logs to Timber, using AppLogger.
        CoreLogger.set(AppLogger())
    }

    override fun dependencies(): List<Class<out Initializer<*>>> {
        return emptyList()
    }

    private class CrashReportingTree : Timber.Tree() {
        override fun log(priority: Int, tag: String?, message: String, e: Throwable?) {
            when (priority) {
                Log.VERBOSE,
                Log.DEBUG -> Unit
                Log.ERROR -> Unit // CrashLibrary.logError()
                Log.WARN -> Unit // CrashLibrary.logWarning()
                else -> Unit // CrashLibrary.log()
            }
        }
    }
}
