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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import ch.protonmail.android.maillabel.presentation.R
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.captionStrongNorm
import me.proton.core.compose.theme.captionWeak
import me.proton.core.compose.theme.defaultNorm

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
            style = ProtonTheme.typography.captionStrongNorm
        )

        var textFieldValue by rememberSaveable(stateSaver = TextFieldValue.Saver) {
            mutableStateOf(TextFieldValue(initialValue))
        }
        TextField(
            value = textFieldValue,
            onValueChange = {
                if (it.text.length <= maxCharacters) {
                    textFieldValue = it
                    onTextChange(it.text)
                }
            },
            placeholder = {
                Text(
                    text = hint,
                    color = ProtonTheme.colors.textHint,
                    style = ProtonTheme.typography.defaultNorm
                )
            },
            shape = RoundedCornerShape(ProtonDimens.LargeCornerRadius),
            colors = TextFieldDefaults.formTextFieldColors(),
            maxLines = 1,
            textStyle = ProtonTheme.typography.defaultNorm,
            keyboardOptions = KeyboardOptions.Default.copy(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Done
            ),
            modifier = Modifier
                .fillMaxWidth()
        )

        Text(
            text = stringResource(
                id = R.string.input_field_max_characters_count,
                textFieldValue.text.length,
                maxCharacters
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
