/*
 * Copyright (c) 2026 Proton Technologies AG
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

package ch.protonmail.android.mailsettings.presentation.settings.appicon

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/** Reads and toggles the app's launcher activity aliases, addressed by class name. */
internal class ActivityAliasProxy @Inject constructor(
    @ApplicationContext private val appContext: Context
) {

    fun isEnabled(className: String): Boolean = appContext.packageManager.getComponentEnabledSetting(
        ComponentName(appContext, className)
    ) == PackageManager.COMPONENT_ENABLED_STATE_ENABLED

    fun setEnabled(className: String, enabled: Boolean) {
        appContext.packageManager.setComponentEnabledSetting(
            ComponentName(appContext, className),
            if (enabled) {
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            } else {
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            },
            PackageManager.DONT_KILL_APP
        )
    }
}
