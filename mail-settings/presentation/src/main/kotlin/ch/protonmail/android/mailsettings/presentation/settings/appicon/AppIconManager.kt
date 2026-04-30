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

package ch.protonmail.android.mailsettings.presentation.settings.appicon

import android.content.Context
import android.content.pm.PackageManager
import ch.protonmail.android.mailcommon.domain.AppInformation
import ch.protonmail.android.mailnotifications.domain.proxy.NotificationManagerCompatProxy
import ch.protonmail.android.mailsettings.presentation.settings.appicon.model.AppIconData
import ch.protonmail.android.mailsettings.presentation.settings.appicon.model.getComponentName
import ch.protonmail.android.mailsettings.presentation.settings.appicon.usecase.CreateLaunchIntent
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class AppIconManager @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val appInformation: AppInformation,
    private val createLaunchIntent: CreateLaunchIntent,
    private val notificationManagerCompatProxy: NotificationManagerCompatProxy
) {

    private val currentIconState by lazy { MutableStateFlow(getCurrentIconData()) }

    fun setNewAppIcon(desiredAppIcon: AppIconData) {
        // Dismiss all notifications
        notificationManagerCompatProxy.dismissAllNotifications()

        val activityAliasPrefix = activityAliasPrefix()

        // Disable current icon
        getCurrentIconData().let { currentIcon ->
            appContext.packageManager.setComponentEnabledSetting(
                currentIcon.getComponentName(activityAliasPrefix, appContext),
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP
            )
        }

        // Enable new icon
        appContext.packageManager.setComponentEnabledSetting(
            desiredAppIcon.getComponentName(activityAliasPrefix, appContext),
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP
        )

        currentIconState.value = desiredAppIcon
        createLaunchIntent.invalidateCache()
    }

    fun getCurrentIconData(): AppIconData {
        val activityAliasPrefix = activityAliasPrefix()
        val activeIcon = AppIconData.ALL_ICONS.firstOrNull { iconData ->
            appContext.packageManager.getComponentEnabledSetting(
                iconData.getComponentName(activityAliasPrefix, appContext)
            ) == PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        }

        return activeIcon ?: AppIconData.DEFAULT
    }

    fun getAvailableIcons(): List<AppIconData> = AppIconData.ALL_ICONS

    private fun activityAliasPrefix() = when (appInformation.appBuildFlavor) {
        "dev" -> "ch.protonmail.android.dev"
        "alpha" -> "ch.protonmail.android.alpha"
        else -> "ch.protonmail.android"
    }
}
