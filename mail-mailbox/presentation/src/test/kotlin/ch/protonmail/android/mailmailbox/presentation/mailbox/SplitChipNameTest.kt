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

package ch.protonmail.android.mailmailbox.presentation.mailbox

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SplitChipNameTest {

    private val highlight = SpanStyle(color = Color.Red, background = Color.Yellow)

    @Test
    fun `keeps a highlight on the whole name across both parts`() {
        // Given
        val name = buildAnnotatedString { withStyle(highlight) { append("IMG_1234.jpg") } }

        // When
        val parts = splitChipName(name)

        // Then
        assertEquals("IMG_1234", parts.baseName.text)
        assertEquals(listOf(AnnotatedString.Range(highlight, 0, 8)), parts.baseName.spanStyles)
        assertEquals(".jpg", parts.extension?.text)
        assertEquals(listOf(AnnotatedString.Range(highlight, 0, 4)), parts.extension?.spanStyles)
        assertEquals(Color.Yellow, parts.joinBackground)
    }

    @Test
    fun `gives no join background when the highlight stops before the dot`() {
        // Given
        val name = buildAnnotatedString {
            withStyle(highlight) { append("IMG") }
            append("_1234.jpg")
        }

        // When
        val parts = splitChipName(name)

        // Then
        assertNull(parts.joinBackground)
    }

    @Test
    fun `gives no join background when the highlight starts at the dot`() {
        // Given
        val name = buildAnnotatedString {
            append("report")
            withStyle(highlight) { append(".pdf") }
        }

        // When
        val parts = splitChipName(name)

        // Then
        assertNull(parts.joinBackground)
    }

    @Test
    fun `splits at the last dot`() {
        // When
        val parts = splitChipName(AnnotatedString("report.final.pdf"))

        // Then
        assertEquals("report.final", parts.baseName.text)
        assertEquals(".pdf", parts.extension?.text)
    }

    @Test
    fun `gives no extension when the name has no dot`() {
        // When
        val parts = splitChipName(AnnotatedString("README"))

        // Then
        assertEquals("README", parts.baseName.text)
        assertNull(parts.extension)
    }

    @Test
    fun `drops a trailing dot and gives no extension`() {
        // When
        val parts = splitChipName(AnnotatedString("notes."))

        // Then
        assertEquals("notes", parts.baseName.text)
        assertNull(parts.extension)
    }
}
