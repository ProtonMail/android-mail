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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import ch.protonmail.android.mailcommon.presentation.ConsumableLaunchedEffect
import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailcomposer.presentation.R
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.defaultNorm

@Composable
internal fun BodyTextField2(
    textFieldState: TextFieldState,
    shouldRequestFocus: Effect<Unit>,
    modifier: Modifier = Modifier
) {
    val focusRequester = remember { FocusRequester() }
    val orientation = LocalConfiguration.current.orientation

    var shouldFocus by remember { mutableStateOf(false) }
    var isFocused by remember { mutableStateOf(false) }

    val bodyMinLines = if (orientation == Configuration.ORIENTATION_PORTRAIT) {
        Composer.MessageBodyPortraitMinLines
    } else {
        1
    }

    BasicTextField(
        state = textFieldState,
        modifier = modifier
            .fillMaxSize()
            .padding(ProtonDimens.DefaultSpacing)
            .focusRequester(focusRequester)
            .onFocusChanged { isFocused = it.isFocused }
            .onGloballyPositioned {
                if (shouldFocus) {
                    focusRequester.requestFocus()
                    shouldFocus = false
                }
            },
        keyboardOptions = KeyboardOptions.Default.copy(
            capitalization = KeyboardCapitalization.Sentences,
            keyboardType = KeyboardType.Text
        ),
        textStyle = ProtonTheme.typography.defaultNorm,
        cursorBrush = SolidColor(TextFieldDefaults.colors().cursorColor),
        lineLimits = TextFieldLineLimits.MultiLine(minHeightInLines = bodyMinLines),
        decorator = @Composable { innerTextField ->
            if (textFieldState.text.isEmpty()) {
                PlaceholderText()
            }

            innerTextField()
        }
    )

    ConsumableLaunchedEffect(shouldRequestFocus) {
        shouldFocus = true
    }
}

@Composable
private fun PlaceholderText() {
    Text(
        modifier = Modifier.testTag(ComposerTestTags.MessageBodyPlaceholder),
        text = stringResource(R.string.compose_message_placeholder),
        color = ProtonTheme.colors.textHint,
        style = ProtonTheme.typography.defaultNorm
    )
}
