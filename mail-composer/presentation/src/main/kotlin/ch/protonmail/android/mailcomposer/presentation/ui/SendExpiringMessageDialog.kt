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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import ch.protonmail.android.mailcomposer.presentation.R
import ch.protonmail.android.mailmessage.domain.model.Recipient
import me.proton.core.compose.component.ProtonAlertDialog
import me.proton.core.compose.component.ProtonAlertDialogButton
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.defaultWeak

@Composable
fun SendExpiringMessageDialog(
    externalRecipients: List<Recipient>,
    onConfirmClicked: () -> Unit,
    onDismissClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    ProtonAlertDialog(
        modifier = modifier,
        titleResId = R.string.composer_send_expiring_message_to_external_recipients_dialog_title,
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = stringResource(
                        id = R.string.composer_send_expiring_message_to_external_recipients_dialog_text
                    ),
                    style = ProtonTheme.typography.defaultWeak
                )
                Spacer(modifier = Modifier.height(ProtonDimens.SmallSpacing))
                Text(
                    text = externalRecipients.joinToString(separator = "\n") { it.address },
                    style = ProtonTheme.typography.defaultWeak
                )
            }
        },
        dismissButton = {
            ProtonAlertDialogButton(
                titleResId = R.string.composer_send_expiring_message_to_external_recipients_dialog_cancel
            ) { onDismissClicked() }
        },
        confirmButton = {
            ProtonAlertDialogButton(
                titleResId = R.string.composer_send_expiring_message_to_external_recipients_dialog_confirm
            ) { onConfirmClicked() }
        },
        onDismissRequest = {}
    )
}
