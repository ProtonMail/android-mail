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

package ch.protonmail.android.mailsettings.presentation.settings.notifications.ui

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import ch.protonmail.android.mailcommon.presentation.ui.MailDivider
import ch.protonmail.android.mailsettings.presentation.R
import ch.protonmail.android.mailsettings.presentation.settings.notifications.model.PushNotificationsSettingsState
import ch.protonmail.android.uicomponents.settings.SettingsToggleItem

@Composable
fun PushNotificationsSettingsScreenList(
    modifier: Modifier = Modifier,
    state: PushNotificationsSettingsState.DataLoaded,
    actions: PushNotificationsSettingsScreenList.Actions
) {
    LazyColumn(modifier = modifier) {
        item { MailDivider() }
        item {
            SettingsToggleItem(
                name = stringResource(id = R.string.mail_settings_notifications_extended_notifications_title),
                value = state.extendedNotificationState.model.enabled,
                hint = stringResource(id = R.string.mail_settings_notifications_extended_notifications_summary),
                onToggle = { actions.onExtendedNotificationsTap(it) }
            )
        }
        item { MailDivider() }
        item { NotificationSettingsItemButton() }
    }
}

object PushNotificationsSettingsScreenList {
    data class Actions(
        val onExtendedNotificationsTap: (Boolean) -> Unit
    )
}
