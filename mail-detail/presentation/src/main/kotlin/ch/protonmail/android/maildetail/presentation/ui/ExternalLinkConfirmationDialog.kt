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

import java.net.IDN
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.Checkbox
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import ch.protonmail.android.maildetail.presentation.R
import me.proton.core.compose.component.ProtonAlertDialog
import me.proton.core.compose.component.ProtonAlertDialogButton
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.defaultWeak
import timber.log.Timber

@Composable
fun ExternalLinkConfirmationDialog(
    onCancelClicked: () -> Unit,
    onContinueClicked: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    linkUri: Uri? = null
) {
    val checkedDoNotShowAgain = remember { mutableStateOf(false) }

    ProtonAlertDialog(
        modifier = modifier,
        titleResId = R.string.external_link_confirmation_dialog_text,
        text = {
            Column {
                Text(
                    text = encodeToPunycode(linkUri.toString()),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    style = ProtonTheme.typography.defaultWeak,
                    modifier = modifier
                )
                Spacer(modifier = Modifier.height(ProtonDimens.ExtraSmallSpacing))
                Row(
                    modifier = modifier,
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = checkedDoNotShowAgain.value,
                        onCheckedChange = {
                            checkedDoNotShowAgain.value = it
                        }
                    )
                    Spacer(modifier = Modifier.width(ProtonDimens.ExtraSmallSpacing))
                    Text(
                        text = stringResource(id = R.string.external_link_confirmation_dialog_do_not_ask_again),
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        style = ProtonTheme.typography.defaultWeak,
                        modifier = modifier
                    )
                }
            }
        },
        dismissButton = {
            ProtonAlertDialogButton(R.string.external_link_confirmation_dialog_cancel_button) {
                onCancelClicked()
            }
        },
        confirmButton = {
            ProtonAlertDialogButton(R.string.external_link_confirmation_dialog_continue_button) {
                onContinueClicked(checkedDoNotShowAgain.value)
            }
        },
        onDismissRequest = { onCancelClicked() }
    )
}

// Function to encode Unicode string to ASCII using Punycode (IDN)
fun encodeToPunycode(unicodeString: String): String {
    return try {
        IDN.toASCII(unicodeString.toString())
    } catch (e: IllegalArgumentException) {
        Timber.d("Error encoding to Punycode: $e")
        unicodeString // Return the original string in case of an error
    }
}
