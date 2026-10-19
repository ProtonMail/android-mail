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

import ch.protonmail.android.mailcommon.domain.AppInformation
import ch.protonmail.android.mailnotifications.domain.proxy.NotificationManagerCompatProxy
import ch.protonmail.android.mailsettings.presentation.settings.appicon.model.AppIconData
import ch.protonmail.android.mailsettings.presentation.settings.appicon.model.activityAliasName
import ch.protonmail.android.mailsettings.presentation.settings.appicon.usecase.CreateLaunchIntent
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class AppIconManager @Inject constructor(
    private val activityAliasProxy: ActivityAliasProxy,
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
        activityAliasProxy.setEnabled(getCurrentIconData().activityAliasName(activityAliasPrefix), enabled = false)

        // Enable new icon
        activityAliasProxy.setEnabled(desiredAppIcon.activityAliasName(activityAliasPrefix), enabled = true)

        currentIconState.value = desiredAppIcon
        createLaunchIntent.invalidateCache()
    }

    fun getCurrentIconData(): AppIconData {
        val activityAliasPrefix = activityAliasPrefix()
        val activeIcon = AppIconData.ALL_ICONS.firstOrNull { iconData ->
            activityAliasProxy.isEnabled(iconData.activityAliasName(activityAliasPrefix))
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
