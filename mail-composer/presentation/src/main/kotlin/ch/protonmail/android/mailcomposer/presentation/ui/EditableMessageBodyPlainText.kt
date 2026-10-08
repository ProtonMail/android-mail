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

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import ch.protonmail.android.design.compose.theme.ProtonDimens
import ch.protonmail.android.design.compose.theme.ProtonTheme
import ch.protonmail.android.design.compose.theme.bodyMediumNorm
import ch.protonmail.android.mailcommon.presentation.ConsumableLaunchedEffect
import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailcommon.presentation.compose.dpToPxFloat
import ch.protonmail.android.mailcomposer.presentation.R
import ch.protonmail.android.mailcomposer.presentation.model.editor.CursorPosition
import ch.protonmail.android.mailcomposer.presentation.model.editor.EditorViewDrawingState
import ch.protonmail.android.uicomponents.keyboardVisibilityAsState

@Composable
fun EditableMessageBodyPlainText(
    modifier: Modifier = Modifier,
    bodyTextFieldState: TextFieldState,
    shouldRequestFocus: Effect<Unit>,
    focusRequester: FocusRequester,
    actions: EditableMessageBodyPlainText.Actions
) {

    val shouldFocus = remember { mutableStateOf(false) }

    var lastLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }

    val isKeyboardVisible by keyboardVisibilityAsState()

    // Local content version for plain text body
    var bodyContentVersion by remember { mutableIntStateOf(0) }

    // Cursor offset to consider when scrolling to caret due to padding
    val cursorOffsetPx = ProtonDimens.Spacing.Large.dpToPxFloat()

    val textScrollState = rememberScrollState()

    BasicTextField(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(vertical = ProtonDimens.Spacing.Large)
            .onGloballyPositioned { coords ->

                actions.onEditorViewPositioned(coords.boundsInWindow())

                if (shouldFocus.value) {
                    focusRequester.requestFocus()
                    shouldFocus.value = false
                }
            }
            .focusRequester(focusRequester),
        textStyle = ProtonTheme.typography.bodyMediumNorm,
        keyboardOptions = KeyboardOptions.Default.copy(
            KeyboardCapitalization.Sentences,
            keyboardType = KeyboardType.Text,
            autoCorrectEnabled = true
        ),
        cursorBrush = SolidColor(TextFieldDefaults.colors().cursorColor),
        onTextLayout = { getResult ->
            val layoutResult = getResult()
            lastLayoutResult = layoutResult

            layoutResult?.let {
                val drawingState = buildEditorViewDrawingState(
                    layoutResult = it,
                    selection = bodyTextFieldState.selection,
                    bodyContentVersion = bodyContentVersion,
                    isKeyboardVisible = isKeyboardVisible,
                    cursorOffsetPx = cursorOffsetPx
                )
                actions.onEditorViewDrawingStateChanged(drawingState)
            }

        },
        state = bodyTextFieldState,
        decorator = @Composable { innerTextField ->
            if (bodyTextFieldState.text.isEmpty()) {
                PlaceholderText()
            }

            innerTextField()
        },
        scrollState = textScrollState
    )

    ConsumableLaunchedEffect(shouldRequestFocus) {
        shouldFocus.value = true
    }

    LaunchedEffect(Unit) {
        snapshotFlow { bodyTextFieldState.text.toString() }
            .collect { _ ->
                bodyContentVersion++
            }
    }

    // Track caret moves even when layout does not change
    LaunchedEffect(Unit) {
        snapshotFlow { bodyTextFieldState.selection }
            .collect { selection ->
                val layout = lastLayoutResult ?: return@collect
                val drawingState = buildEditorViewDrawingState(
                    layoutResult = layout,
                    selection = selection,
                    bodyContentVersion = bodyContentVersion,
                    isKeyboardVisible = isKeyboardVisible,
                    cursorOffsetPx = cursorOffsetPx
                )
                actions.onEditorViewDrawingStateChanged(drawingState)
            }
    }
}

@Composable
private fun PlaceholderText() {
    Text(
        modifier = Modifier.testTag(ComposerTestTags.MessageBodyPlaceholder),
        text = stringResource(R.string.compose_message_placeholder),
        color = ProtonTheme.colors.textHint,
        style = ProtonTheme.typography.bodyMediumNorm
    )
}

private fun buildEditorViewDrawingState(
    layoutResult: TextLayoutResult,
    selection: TextRange,
    bodyContentVersion: Int,
    isKeyboardVisible: Boolean,
    cursorOffsetPx: Float
): EditorViewDrawingState {
    val textLength = layoutResult.layoutInput.text.length
    val safeOffset = if (textLength == 0) 0 else selection.start.coerceIn(0, textLength)

    val caretRect = layoutResult.getCursorRect(safeOffset)

    val cursorPosition = CursorPosition(
        topPx = caretRect.top + cursorOffsetPx,
        bottomPx = caretRect.bottom + cursorOffsetPx
    )

    // Use caret height as line height approximation
    val lineHeightPx = caretRect.height

    val heightPx = layoutResult.size.height.toFloat()

    return EditorViewDrawingState(
        heightPx = heightPx,
        cursorPosition = cursorPosition,
        lineHeightPx = lineHeightPx,
        bodyContentVersion = bodyContentVersion,
        isKeyboardVisible = isKeyboardVisible
    )
}

object EditableMessageBodyPlainText {
    data class Actions(
        val onEditorViewDrawingStateChanged: (EditorViewDrawingState) -> Unit,
        val onEditorViewPositioned: (Rect) -> Unit
    ) {
        companion object {
            val Empty = Actions(
                onEditorViewDrawingStateChanged = {},
                onEditorViewPositioned = {}
            )
        }
    }
}
