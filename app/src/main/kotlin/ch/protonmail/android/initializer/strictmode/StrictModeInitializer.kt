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

package ch.protonmail.android.initializer.strictmode

import android.content.Context
import android.os.StrictMode
import androidx.startup.Initializer
import ch.protonmail.android.BuildConfig
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import android.os.Build

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
            .penaltyDeath()
        val vmPolicyBuilder = StrictMode.VmPolicy.Builder()
            .detectActivityLeaks()
            .detectCleartextNetwork()
            .detectFileUriExposure()
            .detectLeakedClosableObjects()
            .detectLeakedRegistrationObjects()
            .detectLeakedSqlLiteObjects()
            .penaltyDeathOnFileUriExposure()
            // .detectUntaggedSockets() // Not needed (unless we want to use `android.net.TrafficStats`).
            // .detectNonSdkApiUsage() // Skip: some androidx libraries violate this.
            .apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    detectContentUriWithoutPermission()
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    detectCredentialProtectedWhileLocked()
                    detectImplicitDirectBoot()
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    detectIncorrectContextUse()
                    detectUnsafeIntentLaunch()
                }
            }
            .penaltyDeath()

        StrictMode.setThreadPolicy(threadPolicyBuilder.build())
        StrictMode.setVmPolicy(vmPolicyBuilder.build())
        ignoreWhitelistedWarnings()
    }

    private fun ignoreWhitelistedWarnings() {
        // Source: https://atscaleconference.com/videos/eliminating-long-tail-jank-with-strictmode/
        // On API levels above N, we can use reflection to read the violationsBeingTimed field of strict
        // to get notifications about reported violations
        val field = StrictMode::class.java.getDeclaredField("violationsBeingTimed")
        field.isAccessible = true // Suppress Java language access checking
        // Remove "final" modifier
        val modifiersField = Field::class.java.getDeclaredField("accessFlags")
        modifiersField.isAccessible = true
        modifiersField.setInt(field, field.modifiers and Modifier.FINAL.inv())
        // Override the field with a custom ArrayList, which is able to skip whitelisted violations
        field.set(
            null,
            object : ThreadLocal<ArrayList<out Any>>() {
                override fun get(): ArrayList<out Any> {
                    return StrictModeHackArrayList()
                }
            }
        )
    }
}
