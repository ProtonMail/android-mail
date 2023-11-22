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

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat.startActivity
import ch.protonmail.android.mailsettings.presentation.R
import me.proton.core.compose.component.ProtonRawListItem
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.defaultNorm

@Composable
fun NotificationSettingsItemButton(modifier: Modifier = Modifier) {
    val context = LocalContext.current

    fun launchNotificationSettingsIntent() {
        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
        intent.putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        startActivity(context, intent, null)
    }

    ProtonRawListItem(
        modifier = modifier
            .clickable { launchNotificationSettingsIntent() }
            .padding(ProtonDimens.DefaultSpacing),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            modifier = Modifier.weight(1f),
            text = stringResource(id = R.string.mail_settings_notifications_notifications_settings),
            color = ProtonTheme.colors.textNorm,
            style = ProtonTheme.typography.defaultNorm
        )
        Icon(
            painter = painterResource(id = R.drawable.ic_proton_arrow_out_square),
            tint = ProtonTheme.colors.iconWeak,
            contentDescription = stringResource(id = R.string.mail_settings_notifications_notifications_settings_hint)
        )
    }
}
