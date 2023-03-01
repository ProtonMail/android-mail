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

import timber.log.Timber
import java.lang.reflect.Method

/**
 * Special array list that skip additions for matching ViolationInfo instances as per
 * hack described in https://atscaleconference.com/videos/eliminating-long-tail-jank-with-strictmode/
 */
class StrictModeHackArrayList : ArrayList<Any>() {

    private val whitelistedViolations = listOf(
        // Violations observed only in Firebase tests
        "android.graphics.HwTypefaceUtil.getMultiWeightHwFamily",
        "android.graphics.HwTypefaceUtil.updateFont",
        // AppLanguageRepository reading locale from file through
        // AppCompatDelegate (due to `autoStoreLocales` manifest metadata)
        "androidx.appcompat.app.AppLocalesStorageHelper.readLocales",
        // Firebase tests initialization
        "androidx.test.runner.MonitoringInstrumentation.specifyDexMakerCacheProperty",
        // Reading from file
        "ch.protonmail.android.initializer.SentryInitializer.create",
        // Reading from SharedPreferences
        "me.proton.core.util.android.sharedpreferences.ExtensionsKt.nullableGet"
    )

    override fun add(element: Any): Boolean {
        val hasDeclaredMethod = element.javaClass.declaredMethods.any { it.name == "getStackTrace" }
        if (!hasDeclaredMethod) {
            // call super to continue with standard violation reporting
            return super.add(element)
        }

        val crashInfoMethod: Method = element.javaClass.getDeclaredMethod("getStackTrace")
        crashInfoMethod.invoke(element)?.let { crashInfoStackTrace ->
            for (whitelistedStacktraceCall in whitelistedViolations) {
                if (crashInfoStackTrace.toString().contains(whitelistedStacktraceCall)) {
                    Timber.d("Skipping whitelisted StrictMode violation: $whitelistedStacktraceCall")
                    return false
                }
            }
        }
        // call super to continue with standard violation reporting
        return super.add(element)
    }
}
