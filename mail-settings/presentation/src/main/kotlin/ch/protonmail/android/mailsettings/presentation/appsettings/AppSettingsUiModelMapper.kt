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

package ch.protonmail.android.mailsettings.presentation.appsettings

import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailsettings.domain.model.AppSettings
import ch.protonmail.android.mailsettings.domain.model.Theme
import ch.protonmail.android.mailsettings.presentation.R

internal object AppSettingsUiModelMapper {

    fun toUiModel(
        appSettings: AppSettings,
        notificationsEnabled: Boolean,
        appIconDescription: TextUiModel
    ): AppSettingsUiModel {
        return AppSettingsUiModel(
            autoLockEnabled = appSettings.hasAutoLock,
            alternativeRoutingEnabled = appSettings.hasAlternativeRouting,
            customLanguage = appSettings.customAppLanguage,
            theme = appSettings.theme.toTextUiModel(),
            deviceContactsEnabled = appSettings.hasCombinedContactsEnabled,
            notificationsEnabledStatus = getNotificationStatus(notificationsEnabled),
            appIconName = appIconDescription,
            swipeNextEnabled = appSettings.swipeNextPreference.enabled
        )
    }

    private fun getNotificationStatus(on: Boolean) =
        TextUiModel(if (on) R.string.notifications_on else R.string.notifications_off)

    private fun Theme.toTextUiModel(): TextUiModel {
        val textRes = when (this) {
            Theme.SYSTEM_DEFAULT -> R.string.mail_settings_system_default
            Theme.LIGHT -> R.string.mail_settings_theme_light
            Theme.DARK -> R.string.mail_settings_theme_dark
        }

        return TextUiModel.TextRes(textRes)
    }
}
