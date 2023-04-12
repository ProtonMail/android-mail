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

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.SemanticsNodeInteraction
import ch.protonmail.android.uitest.util.extensions.getKeyValueByName
import org.junit.Assert.assertEquals
import kotlin.test.assertNull

fun SemanticsNodeInteraction.assertTintColor(tintColor: Color) = apply {
    val tintColorProperty = getKeyValueByName(CustomSemanticsPropertyKeyNames.TintColorKey)
        ?: throw AssertionError("Expected TintColorKey property was not found on this node.")

    assertEquals(tintColor, tintColorProperty.value)
}

fun SemanticsNodeInteraction.assertNoTintColor() = apply {
    val tintColorProperty = getKeyValueByName(CustomSemanticsPropertyKeyNames.TintColorKey)
        ?: throw AssertionError("Expected TintColorKey property was not found on this node.")

    assertNull(tintColorProperty.value)
}
