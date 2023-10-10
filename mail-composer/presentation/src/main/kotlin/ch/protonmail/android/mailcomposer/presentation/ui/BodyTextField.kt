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

import android.content.res.Configuration
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import ch.protonmail.android.mailcomposer.presentation.R
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.defaultNorm

@Composable
internal fun BodyTextField(
    initialValue: String,
    hasQuotedBody: Boolean,
    onBodyChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val focusRequester = remember { FocusRequester() }
    var text by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(initialValue))
    }
    val screenOrientation = LocalConfiguration.current.orientation
    val bodyMinLines =
        if (screenOrientation == Configuration.ORIENTATION_PORTRAIT) Composer.MessageBodyPortraitMinLines else 1

    TextField(
        value = text,
        onValueChange = {
            text = it
            onBodyChange(it.text)
        },
        modifier = modifier.fillMaxSize().focusRequester(focusRequester),
        textStyle = ProtonTheme.typography.defaultNorm,
        minLines = bodyMinLines,
        colors = TextFieldDefaults.composerTextFieldColors(),
        keyboardOptions = KeyboardOptions.Default.copy(capitalization = KeyboardCapitalization.Sentences),
        placeholder = {
            Text(
                modifier = Modifier.testTag(ComposerTestTags.MessageBodyPlaceholder),
                text = stringResource(R.string.compose_message_placeholder),
                color = ProtonTheme.colors.textHint,
                style = ProtonTheme.typography.defaultNorm
            )
        }
    )

    LaunchedEffect(initialValue) {
        val shouldGetFocus = initialValue.isNotEmpty() || hasQuotedBody
        if (shouldGetFocus) { focusRequester.requestFocus() }
    }
}
