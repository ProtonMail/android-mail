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

package ch.protonmail.android.mailsettings.presentation.accountsettings.identity.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import ch.protonmail.android.mailcommon.presentation.compose.MailDimens
import me.proton.core.compose.component.protonOutlineTextFieldColors
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.defaultNorm

@Composable
fun AddressIdentityTextField(
    modifier: Modifier = Modifier,
    text: String,
    enabled: Boolean = true,
    placeholder: String,
    onValueChanged: (String) -> Unit
) {
    OutlinedTextField(
        modifier = modifier
            .focusRequester(FocusRequester())
            .fillMaxWidth()
            .size(MailDimens.TextFieldMultiLineSize)
            .padding(
                top = ProtonDimens.SmallSpacing,
                start = ProtonDimens.DefaultSpacing,
                end = ProtonDimens.DefaultSpacing,
                bottom = ProtonDimens.DefaultSpacing
            ),
        enabled = enabled,
        value = text,
        trailingIcon = {
            if (enabled && text.isNotBlank()) {
                IconButton(
                    modifier = Modifier.size(ProtonDimens.DefaultIconSize),
                    content = {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = null,
                            tint = ProtonTheme.colors.iconNorm
                        )
                    }, onClick = { onValueChanged("") }
                )
            }
        },
        onValueChange = { onValueChanged(it) },
        textStyle = ProtonTheme.typography.defaultNorm,
        colors = TextFieldDefaults.protonOutlineTextFieldColors(),
        placeholder = { Text(text = placeholder) },
        maxLines = 3
    )
}
