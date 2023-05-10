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

package ch.protonmail.android.mailcommon.presentation.compose

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged

/**
 * Represents a set of focusable fields such that:
 * - the focused field retains focus when the configuration change happens,
 * - the focused field is brought into the view only after the IME is visible.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun <FocusedField> FocusableForm(
    initialFocus: FocusedField,
    content: @Composable FocusableFormScope<FocusedField>.() -> Unit
) {
    var focusedField by rememberSaveable(inputs = emptyArray()) { mutableStateOf(initialFocus) }
    val focusRequester = remember { FocusRequester() }
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val isKeyboardVisible by keyboardVisibilityAsState()
    val onFieldFocused: (FocusedField) -> Unit = { focusedField = it }

    FocusableFormScope(focusedField, focusRequester, bringIntoViewRequester, onFieldFocused).content()

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    // This is a workaround as the keyboard needs to be fully visible before the composable can be brought into
    // the view, otherwise the bringIntoView() call has no effect.
    // See https://kotlinlang.slack.com/archives/CJLTWPH7S/p1683542940483379 for more context.
    LaunchedEffect(isKeyboardVisible) {
        bringIntoViewRequester.bringIntoView()
    }
}

class FocusableFormScope<FocusedField> @OptIn(ExperimentalFoundationApi::class) constructor(
    private val currentFocus: FocusedField,
    private val focusRequester: FocusRequester,
    private val bringIntoViewRequester: BringIntoViewRequester,
    private val onFieldFocused: (focusedField: FocusedField) -> Unit
) {

    @OptIn(ExperimentalFoundationApi::class)
    @Stable
    fun Modifier.retainFieldFocusOnConfigurationChange(fieldType: FocusedField): Modifier =
        if (currentFocus == fieldType) {
            focusRequester(focusRequester)
                .bringIntoViewRequester(bringIntoViewRequester)
        } else {
            this
        }.onFocusChanged {
            if (it.isFocused) onFieldFocused(fieldType)
        }
}
