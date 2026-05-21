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
import ch.protonmail.android.mailsettings.presentation.R

internal object AppSettingsScreenPreviewData {

    val Data = AppSettingsState.Data(
        settings = AppSettingsUiModel(
            autoLockEnabled = false,
            alternativeRoutingEnabled = false,
            isEmailCategoriesEnabled = true,
            customLanguage = null,
            deviceContactsEnabled = false,
            theme = TextUiModel.TextRes(R.string.mail_settings_system_default),
            notificationsEnabledStatus = TextUiModel("On"),
            appIconName = TextUiModel.Text("Default"),
            swipeNextEnabled = false
        )
    )

    val Actions = AppSettingsScreen.Actions(
        onThemeClick = {},
        onPushNotificationsClick = {},
        onAutoLockClick = {},
        onAppLanguageClick = {},
        onSwipeToNextEmailClick = {},
        onSwipeActionsClick = {},
        onNavigateToUpselling = { _, _ -> },
        onNavigateToSignatureSettings = {},
        onCustomizeToolbarClick = {},
        onEmailCategoriesClick = {},
        onViewApplicationLogsClick = {},
        onAppIconSettingsClick = {},
        onBackClick = {}
    )
}
