/*
 * Copyright (c) 2026 Proton Technologies AG
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

package ch.protonmail.android.uicomponents.text

import kotlin.test.Test
import kotlin.test.assertEquals

internal class ContainsHighlightTest {

    @Test
    fun `should match any highlight word ignoring case`() {
        // When
        val actual = listOf("Q3_INVOICE-final.pdf", "roadmap.docx").map {
            containsHighlight(it, highlight = "invoce invoice")
        }

        // Then
        assertEquals(listOf(true, false), actual)
    }

    @Test
    fun `should match nothing for a blank highlight`() {
        // When
        val actual = containsHighlight("invoice.pdf", highlight = "  ")

        // Then
        assertEquals(false, actual)
    }

    @Test
    fun `should match highlight words literally`() {
        // When
        val actual = listOf("a+b.pdf", "aab.pdf").map { containsHighlight(it, highlight = "a+b") }

        // Then
        assertEquals(listOf(true, false), actual)
    }
}
