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

package ch.protonmail.android.debug

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import ch.protonmail.android.R
import ch.protonmail.android.design.compose.component.ProtonAlertDialog
import ch.protonmail.android.design.compose.component.ProtonAlertDialogButton
import ch.protonmail.android.design.compose.theme.ProtonDimens
import ch.protonmail.android.design.compose.theme.ProtonTheme

@Composable
internal fun TelemetryDebugLegendDialog(onDismiss: () -> Unit) {
    ProtonAlertDialog(
        onDismissRequest = onDismiss,
        titleResId = R.string.app_debug_screen_telemetry_legend_title,
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(ProtonDimens.Spacing.ExtraLarge)) {
                TelemetryDebugLegendRow(
                    icon = R.drawable.ic_telemetry_capture,
                    tint = ProtonTheme.colors.notificationError,
                    title = R.string.app_debug_screen_telemetry_capture_on,
                    description = R.string.app_debug_screen_telemetry_legend_capture_on
                )
                TelemetryDebugLegendRow(
                    icon = R.drawable.ic_telemetry_capture,
                    tint = ProtonTheme.colors.textHint,
                    title = R.string.app_debug_screen_telemetry_capture_off,
                    description = R.string.app_debug_screen_telemetry_legend_capture_off
                )
                TelemetryDebugLegendRow(
                    icon = R.drawable.ic_telemetry_sending_on,
                    tint = ProtonTheme.colors.textNorm,
                    title = R.string.app_debug_screen_telemetry_sending_on,
                    description = R.string.app_debug_screen_telemetry_legend_sending_on
                )
                TelemetryDebugLegendRow(
                    icon = R.drawable.ic_telemetry_sending_off,
                    tint = ProtonTheme.colors.notificationError,
                    title = R.string.app_debug_screen_telemetry_sending_halted,
                    description = R.string.app_debug_screen_telemetry_legend_sending_halted
                )
            }
        },
        confirmButton = {
            ProtonAlertDialogButton(
                titleResId = R.string.app_debug_screen_telemetry_dialog_close,
                onClick = onDismiss
            )
        }
    )
}

@Composable
private fun TelemetryDebugLegendRow(
    @DrawableRes icon: Int,
    tint: Color,
    @StringRes title: Int,
    @StringRes description: Int
) {
    Row(horizontalArrangement = Arrangement.spacedBy(ProtonDimens.Spacing.Medium)) {
        Icon(painter = painterResource(icon), tint = tint, contentDescription = null)
        Column(verticalArrangement = Arrangement.spacedBy(ProtonDimens.Spacing.Small)) {
            Text(
                text = stringResource(title),
                style = ProtonTheme.typography.titleSmall,
                color = ProtonTheme.colors.textNorm
            )
            Text(
                text = stringResource(description),
                style = ProtonTheme.typography.bodySmall,
                color = ProtonTheme.colors.textWeak
            )
        }
    }
}
