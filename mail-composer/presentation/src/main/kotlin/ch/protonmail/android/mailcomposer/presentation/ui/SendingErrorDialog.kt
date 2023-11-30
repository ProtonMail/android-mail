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

package ch.protonmail.android.mailcomposer.presentation.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import ch.protonmail.android.mailcomposer.presentation.R
import me.proton.core.compose.component.ProtonAlertDialog
import me.proton.core.compose.component.ProtonAlertDialogButton
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.ProtonTheme3
import me.proton.core.compose.theme.defaultWeak

@Composable
fun SendingErrorDialog(
    errorMessage: String,
    onDismissClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    ProtonAlertDialog(
        modifier = modifier,
        titleResId = R.string.message_sending_error_dialog_header,
        text = {
            Column {
                Text(
                    text = stringResource(id = R.string.message_sending_error_dialog_text_error),
                    style = ProtonTheme.typography.defaultWeak,
                    modifier = modifier
                )
                Spacer(modifier = Modifier.height(ProtonDimens.DefaultSpacing))
                Text(
                    text = errorMessage,
                    style = ProtonTheme.typography.defaultWeak,
                    modifier = modifier
                )
            }
        },
        dismissButton = {
            ProtonAlertDialogButton(R.string.message_sending_error_dialog_button_dismiss) { onDismissClicked() }
        },
        confirmButton = { },
        onDismissRequest = { onDismissClicked() }
    )
}

@Preview
@Composable
private fun SendingErrorDialogPreview() {
    ProtonTheme3 {
        ProtonTheme {
            SendingErrorDialog(
                "This is error message",
                {}
            )
        }
    }
}
