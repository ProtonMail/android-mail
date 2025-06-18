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

package ch.protonmail.android.maillabel.presentation.ui

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import ch.protonmail.android.maillabel.presentation.R
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.captionWeak
import me.proton.core.compose.theme.defaultNorm
import me.proton.core.compose.theme.defaultSmallStrongNorm
import kotlin.math.min

@Composable
fun FormInputField(
    initialValue: String = "",
    title: String,
    hint: String,
    maxCharacters: Int = 100,
    onTextChange: (String) -> Unit
) {
    Column(
        Modifier.padding(
            horizontal = ProtonDimens.DefaultSpacing,
            vertical = ProtonDimens.MediumSpacing
        )
    ) {
        Text(
            text = title,
            modifier = Modifier.padding(bottom = ProtonDimens.SmallSpacing),
            style = ProtonTheme.typography.defaultSmallStrongNorm
        )

        var textFieldValue by rememberSaveable(stateSaver = TextFieldValue.Saver) {
            mutableStateOf(TextFieldValue(initialValue))
        }
        OutlinedTextField(
            modifier = Modifier
                .fillMaxWidth(),
            value = textFieldValue,
            onValueChange = {
                textFieldValue = it.copy(text = it.text.substring(0, min(it.text.length, maxCharacters)))
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
            singleLine = true,
            textStyle = ProtonTheme.typography.defaultNorm,
            keyboardOptions = KeyboardOptions.Default.copy(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Done
            ),
            trailingIcon = {
                if (textFieldValue.text.isNotBlank()) {
                    IconButton(
                        modifier = Modifier.size(ProtonDimens.DefaultIconSize),
                        content = {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_proton_cross),
                                contentDescription = stringResource(R.string.input_field_clear_content_description),
                                tint = ProtonTheme.colors.iconNorm
                            )
                        }, onClick = {
                            textFieldValue = TextFieldValue("")
                            onTextChange("")
                        }
                    )
                }
            }
        )

        Text(
            text = pluralStringResource(
                id = R.plurals.input_field_max_characters_count,
                count = textFieldValue.text.length,
                textFieldValue.text.length, maxCharacters
            ),
            modifier = Modifier.padding(
                top = ProtonDimens.ExtraSmallSpacing
            ),
            style = ProtonTheme.typography.captionWeak
        )
    }
}

@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
private fun FormInputFieldPreview() {
    FormInputField(
        initialValue = "Input field value",
        title = "Name",
        hint = "Input field hint",
        onTextChange = {}
    )
}

@Composable
@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO, showBackground = true)
private fun EmptyFormInputFieldPreview() {
    FormInputField(
        title = "Name",
        hint = "Input field hint",
        onTextChange = {}
    )
}
