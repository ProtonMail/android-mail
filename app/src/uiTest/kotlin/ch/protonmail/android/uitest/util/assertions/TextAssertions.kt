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

package ch.protonmail.android.uitest.util.assertions

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertTextEquals
import ch.protonmail.android.uitest.util.extensions.getKeyValueByName
import ch.protonmail.android.uitest.util.getString
import ch.protonmail.android.uitest.util.hasTextColor
import kotlin.test.assertEquals

fun SemanticsNodeInteraction.assertTextColor(color: Long): SemanticsNodeInteraction = assertTextColor(Color(color))

fun SemanticsNodeInteraction.assertTextColor(color: Color): SemanticsNodeInteraction = assert(hasTextColor(color))

fun SemanticsNodeInteraction.assertEmptyText() = assertTextEquals("")

fun SemanticsNodeInteraction.assertTextContains(
    @StringRes valueRes: Int,
    substring: Boolean = false,
    ignoreCase: Boolean = false
): SemanticsNodeInteraction = assertTextContains(getString(valueRes), substring, ignoreCase)

/**
 * Performs an assertion against the content of the receiving node
 * excluding the content of the `EditableText` [SemanticsPropertyKey] .
 *
 * This is useful when performing checks against nodes located with the merged tree strategy.
 *
 * @param value the expected [String] value.
 */
fun SemanticsNodeInteraction.assertNotEditableTextEquals(value: String) {
    assertTextEquals(value, includeEditableText = false)
}

/**
 * Performs an assertion against the content of the `EditableText` [SemanticsPropertyKey] of the receiving node.
 *
 * This is useful when performing checks against nodes located with the merged tree strategy.
 *
 * @param value the expected [String] value.
 */
fun SemanticsNodeInteraction.assertEditableTextEquals(value: String) {
    val editableText = requireNotNull(getKeyValueByName(SemanticsProperties.EditableText.name)) {
        "Expected EditableText property was not found on this node."
    }

    assertEquals(value, editableText.value.toString())
}
