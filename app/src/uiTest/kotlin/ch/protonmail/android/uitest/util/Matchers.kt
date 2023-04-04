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

package ch.protonmail.android.uitest.util

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasText
import androidx.compose.ui.text.TextLayoutResult

fun hasText(
    @StringRes textRes: Int,
    substring: Boolean = false,
    ignoreCase: Boolean = false
): SemanticsMatcher = hasText(getString(textRes), substring, ignoreCase)

// Not as straightforward, some bits are taken from the compose-ui source code which can be found here:
// https://github.com/androidx/androidx/blob/3606267939d1cb78310a1e40e76f673920f277b8/compose/ui/ui/src/androidMain/kotlin/androidx/compose/ui/platform/AndroidComposeViewAccessibilityDelegateCompat.android.kt#L1840-L1849
fun hasTextColor(color: Color): SemanticsMatcher {
    val semanticsAction = SemanticsActions.GetTextLayoutResult

    return SemanticsMatcher(
        description = "${SemanticsProperties.Text.name} color matches '$color'"
    ) {
        val textLayoutElements = mutableListOf<TextLayoutResult>().apply {
            it.config[semanticsAction].action?.invoke(this)
        } as List<TextLayoutResult>

        if (textLayoutElements.isNotEmpty()) {
            textLayoutElements[0].layoutInput.style.color == color
        } else {
            false
        }
    }
}
