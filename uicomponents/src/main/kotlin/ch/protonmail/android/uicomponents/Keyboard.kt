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

package ch.protonmail.android.uicomponents

import android.content.Context
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.SoftwareKeyboardController

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun keyboardVisibilityAsState(): State<Boolean> = rememberUpdatedState(WindowInsets.isImeVisible)

/**
 * Dismisses the IME without a visual glitch.
 * See https://issuetracker.google.com/issues/278739418 for more details.
 */
@OptIn(ExperimentalComposeUiApi::class)
fun dismissKeyboard(
    context: Context,
    view: View,
    keyboardController: SoftwareKeyboardController?
) {
    context.getSystemService(InputMethodManager::class.java)
        ?.hideSoftInputFromWindow(view.windowToken, 0)
    keyboardController?.hide()
}
