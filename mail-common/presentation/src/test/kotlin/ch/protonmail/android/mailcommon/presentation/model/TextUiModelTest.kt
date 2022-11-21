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

package ch.protonmail.android.mailcommon.presentation.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

internal class TextUiModelTest {

    @Test
    fun `equals on text`() {
        assertEquals(TextUiModel("a"), TextUiModel("a"))
        assertNotEquals(TextUiModel("a"), TextUiModel("b"))
        assertNotEquals(TextUiModel("a"), TextUiModel("A"))
    }

    @Test
    fun `equals on text res`() {
        assertEquals(TextUiModel(1), TextUiModel(1))
        assertNotEquals(TextUiModel(1), TextUiModel(2))
    }

    @Test
    fun `equals on text res with args`() {
        data class Test(val int: Int)
        assertEquals(TextUiModel(1, "a"), TextUiModel(1, "a"))
        assertEquals(TextUiModel(1, 1), TextUiModel(1, 1))
        assertEquals(TextUiModel(1, Test(1)), TextUiModel(1, Test(1)))
        assertNotEquals(TextUiModel(1, "a"), TextUiModel(1, "b"))
        assertNotEquals(TextUiModel(1, "a"), TextUiModel(2, "a"))
        assertNotEquals(TextUiModel(1, 1), TextUiModel(1, 2))
        assertNotEquals(TextUiModel(1, 1), TextUiModel(1, "a"))
        assertNotEquals(TextUiModel(1, Test(1)), TextUiModel(1, Test(2)))
    }

    @Test
    fun `hash code on text`() {
        assertEquals(TextUiModel("a").hashCode(), TextUiModel("a").hashCode())
        assertNotEquals(TextUiModel("a").hashCode(), TextUiModel("b").hashCode())
        assertNotEquals(TextUiModel("a").hashCode(), TextUiModel("A").hashCode())
    }

    @Test
    fun `hash code on text res`() {
        assertEquals(TextUiModel(1).hashCode(), TextUiModel(1).hashCode())
        assertNotEquals(TextUiModel(1).hashCode(), TextUiModel(2).hashCode())
    }

    @Test
    fun `hash code on text res with args`() {
        data class Test(val int: Int)
        assertEquals(TextUiModel(1, "a").hashCode(), TextUiModel(1, "a").hashCode())
        assertEquals(TextUiModel(1, 1).hashCode(), TextUiModel(1, 1).hashCode())
        assertEquals(TextUiModel(1, Test(1)).hashCode(), TextUiModel(1, Test(1)).hashCode())
        assertNotEquals(TextUiModel(1, "a").hashCode(), TextUiModel(1, "b").hashCode())
        assertNotEquals(TextUiModel(1, "a").hashCode(), TextUiModel(2, "a").hashCode())
        assertNotEquals(TextUiModel(1, 1).hashCode(), TextUiModel(1, 2).hashCode())
        assertNotEquals(TextUiModel(1, 1).hashCode(), TextUiModel(1, "a").hashCode())
        assertNotEquals(TextUiModel(1, Test(1)).hashCode(), TextUiModel(1, Test(2)).hashCode())
    }
}
