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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import ch.protonmail.android.R
import ch.protonmail.android.design.compose.component.ProtonAlertDialog
import ch.protonmail.android.design.compose.component.ProtonAlertDialogButton
import ch.protonmail.android.design.compose.theme.ProtonDimens
import ch.protonmail.android.design.compose.theme.ProtonTheme
import ch.protonmail.android.mailcommon.presentation.extension.copyTextToClipboard

@Composable
internal fun TelemetryDebugEventDialog(event: TelemetryDebugEventUiModel, onDismiss: () -> Unit) {
    val context = LocalContext.current

    ProtonAlertDialog(
        onDismissRequest = onDismiss,
        title = event.name,
        text = {
            Column {
                Text(
                    text = event.measurementGroup,
                    style = ProtonTheme.typography.bodySmall,
                    color = ProtonTheme.colors.textWeak,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = event.id,
                    style = ProtonTheme.typography.bodySmall,
                    color = ProtonTheme.colors.textHint,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(top = ProtonDimens.Spacing.Small)
                )
            }
        },
        confirmButton = {
            Column(modifier = Modifier.fillMaxWidth()) {
                ProtonAlertDialogButton(titleResId = R.string.app_debug_screen_telemetry_copy_payload) {
                    context.copyTextToClipboard(label = event.name, text = event.payload)
                    onDismiss()
                }
                ProtonAlertDialogButton(titleResId = R.string.app_debug_screen_telemetry_copy_id) {
                    context.copyTextToClipboard(label = event.name, text = event.id)
                    onDismiss()
                }
                ProtonAlertDialogButton(
                    titleResId = R.string.app_debug_screen_telemetry_dialog_close,
                    textColor = ProtonTheme.colors.textWeak,
                    onClick = onDismiss
                )
            }
        }
    )
}
