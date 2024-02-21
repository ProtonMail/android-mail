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

package ch.protonmail.android.mailcontact.presentation.ui

import android.content.res.Configuration
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import ch.protonmail.android.maillabel.presentation.R
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.defaultNorm

@Composable
fun FormInputField(
    modifier: Modifier = Modifier,
    initialValue: String = "",
    hint: String,
    singleLine: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    maxCharacters: Int? = null,
    showClearTextIcon: Boolean = false,
    onTextChange: (String) -> Unit
) {
    var textFieldValue by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(initialValue))
    }
    OutlinedTextField(
        modifier = modifier,
        value = textFieldValue,
        onValueChange = {
            textFieldValue = it.copy(
                text = maxCharacters?.let { maxCharacters ->
                    it.text.take(maxCharacters)
                } ?: it.text
            )
            onTextChange(textFieldValue.text)
        },
        placeholder = {
            Text(
                text = hint,
                color = ProtonTheme.colors.textHint,
                style = ProtonTheme.typography.defaultNorm
            )
        },
        shape = RoundedCornerShape(ProtonDimens.LargeCornerRadius),
        colors = formTextFieldColors(),
        singleLine = singleLine,
        textStyle = ProtonTheme.typography.defaultNorm,
        keyboardOptions = keyboardOptions,
        trailingIcon = {
            if (showClearTextIcon && textFieldValue.text.isNotBlank()) {
                IconButton(
                    modifier = Modifier.size(ProtonDimens.DefaultIconSize),
                    content = {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_proton_cross),
                            contentDescription = stringResource(R.string.input_field_clear_content_description),
                            tint = ProtonTheme.colors.iconNorm
                        )
                    },
                    onClick = {
                        textFieldValue = TextFieldValue("")
                        onTextChange("")
                    }
                )
            }
        }
    )
}

@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
private fun FormInputFieldPreview() {
    FormInputField(
        initialValue = "Input field value",
        hint = "Input field hint",
        onTextChange = {}
    )
}

@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
private fun EmptyFormInputFieldPreview() {
    FormInputField(
        hint = "Input field hint",
        onTextChange = {}
    )
}
