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
import android.os.StrictMode
import androidx.startup.Initializer
import ch.protonmail.android.BuildConfig

class StrictModeInitializer : Initializer<Unit> {

    override fun create(context: Context) {
        if (BuildConfig.DEBUG) {
            enableStrictMode()
        }
    }

    override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()

    private fun enableStrictMode() {
        val threadPolicyBuilder = StrictMode.ThreadPolicy.Builder()
            .detectAll()
            .penaltyFlashScreen()
            .penaltyLog()
        val vmPolicyBuilder = StrictMode.VmPolicy.Builder()
            .detectAll()
            .penaltyLog()

        StrictMode.setThreadPolicy(threadPolicyBuilder.build())
        StrictMode.setVmPolicy(vmPolicyBuilder.build())
    }
}
