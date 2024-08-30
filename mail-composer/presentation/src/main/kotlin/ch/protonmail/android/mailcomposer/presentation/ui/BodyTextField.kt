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
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import ch.protonmail.android.mailcommon.presentation.ConsumableLaunchedEffect
import ch.protonmail.android.mailcommon.presentation.ConsumableTextEffect
import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailcomposer.presentation.R
import kotlinx.coroutines.launch
import me.proton.core.compose.theme.ProtonDimens
import me.proton.core.compose.theme.ProtonTheme
import me.proton.core.compose.theme.defaultNorm

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun BodyTextField(
    initialValue: String,
    replaceDraftBody: Effect<TextUiModel>,
    shouldRequestFocus: Effect<Unit>,
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

    var userUpdated by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(key1 = initialValue) {
        if (!userUpdated) {
            text = TextFieldValue(initialValue)
        }
    }

    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val coroutineScope = rememberCoroutineScope()
    var cursorRect by remember { mutableStateOf(Rect.Zero) }
    var isFocused by remember { mutableStateOf(false) }

    // If the cursorRect is already displayed completely, the requester does nothing
    // and the following call becomes a no-op, no need to put any guards.
    // See ContentInViewNode#bringChildIntoView#L120 for reference.
    fun bringRectIntoView(rect: Rect) = coroutineScope.launch { bringIntoViewRequester.bringIntoView(rect) }

    LaunchedEffect(cursorRect, isFocused) {
        if (isFocused && cursorRect != Rect.Zero) {
            bringRectIntoView(cursorRect)
        }
    }

    BasicTextField(
        value = text,
        modifier = modifier
            .fillMaxSize()
            .padding(ProtonDimens.DefaultSpacing)
            .focusRequester(focusRequester)
            .bringIntoViewRequester(bringIntoViewRequester)
            .onFocusChanged { isFocused = it.isFocused },
        onTextLayout = {
            cursorRect = it.getCursorRect(text.selection.end)
        },
        onValueChange = {
            text = it
            onBodyChange(it.text)
            userUpdated = true
        },
        minLines = bodyMinLines,
        keyboardOptions = KeyboardOptions.Default.copy(capitalization = KeyboardCapitalization.Sentences),
        textStyle = ProtonTheme.typography.defaultNorm,
        cursorBrush = SolidColor(TextFieldDefaults.colors().cursorColor),
        decorationBox = @Composable { innerTextField ->
            if (text.text.isEmpty()) {
                Text(
                    modifier = Modifier.testTag(ComposerTestTags.MessageBodyPlaceholder),
                    text = stringResource(R.string.compose_message_placeholder),
                    color = ProtonTheme.colors.textHint,
                    style = ProtonTheme.typography.defaultNorm
                )
            }
            innerTextField()
        }
    )

    ConsumableLaunchedEffect(shouldRequestFocus) {
        focusRequester.requestFocus()
    }

    ConsumableTextEffect(effect = replaceDraftBody) {
        text = TextFieldValue(it)
        onBodyChange(it)
    }
}
