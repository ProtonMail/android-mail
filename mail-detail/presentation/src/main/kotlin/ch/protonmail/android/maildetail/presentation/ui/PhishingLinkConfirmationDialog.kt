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

package ch.protonmail.android.maildetail.presentation.ui

import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import ch.protonmail.android.maildetail.presentation.R
import me.proton.core.compose.component.ProtonAlertDialog
import me.proton.core.compose.component.ProtonAlertDialogButton
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.defaultNorm
import me.proton.core.compose.theme.defaultWeak

@Composable
fun PhishingLinkConfirmationDialog(
    onCancelClicked: () -> Unit,
    onContinueClicked: () -> Unit,
    modifier: Modifier = Modifier,
    linkUri: Uri? = null
) {
    ProtonAlertDialog(
        modifier = modifier,
        titleResId = R.string.phishing_link_confirmation_dialog_title,
        text = {
            Column {
                Text(
                    text = stringResource(id = R.string.phishing_link_confirmation_dialog_content_part_1),
                    style = ProtonTheme.typography.defaultNorm
                )
                Spacer(modifier = Modifier.height(ProtonDimens.DefaultSpacing))
                Text(
                    text = encodeToPunycode(linkUri.toString()),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    style = ProtonTheme.typography.defaultWeak
                )
                Spacer(modifier = Modifier.height(ProtonDimens.DefaultSpacing))
                Text(
                    text = stringResource(id = R.string.phishing_link_confirmation_dialog_content_part_2),
                    style = ProtonTheme.typography.defaultNorm
                )
            }
        },
        dismissButton = {
            ProtonAlertDialogButton(R.string.phishing_link_confirmation_dialog_button_go_back) {
                onCancelClicked()
            }
        },
        confirmButton = {
            ProtonAlertDialogButton(R.string.phishing_link_confirmation_dialog_button_continue) {
                onContinueClicked()
            }
        },
        onDismissRequest = { onCancelClicked() }
    )
}
