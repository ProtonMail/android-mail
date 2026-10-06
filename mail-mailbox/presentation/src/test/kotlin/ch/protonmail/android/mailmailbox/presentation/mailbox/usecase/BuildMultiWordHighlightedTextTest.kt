/*
 * Copyright (c) 2025 Proton Technologies AG
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

package ch.protonmail.android.mailmailbox.presentation.mailbox.usecase

import androidx.compose.ui.graphics.Color
import ch.protonmail.android.uicomponents.text.buildMultiWordHighlightedText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BuildMultiWordHighlightedTextTest {

    private val highlightTextColor = Color.Red
    private val highlightBackgroundColor = Color.Yellow

    @Test
    fun `returns plain text and no spans when highlight is empty`() {
        // Given
        val text = "John Smith, London"
        val highlight = ""

        // When
        val result = buildMultiWordHighlightedText(
            text = text,
            highlight = highlight,
            highlightTextColor = highlightTextColor,
            highlightBackgroundColor = highlightBackgroundColor,
            visibleEnd = text.length
        )

        // Then
        assertEquals(text, result.text)
        assertTrue(result.spanStyles.isEmpty())
    }

    @Test
    fun `returns plain text and no spans when highlight is only spaces`() {
        // Given
        val text = "John Smith, London"
        val highlight = "   "

        // When
        val result = buildMultiWordHighlightedText(
            text = text,
            highlight = highlight,
            highlightTextColor = highlightTextColor,
            highlightBackgroundColor = highlightBackgroundColor,
            visibleEnd = text.length
        )

        // Then
        assertEquals(text, result.text)
        assertTrue(result.spanStyles.isEmpty())
    }

    @Test
    fun `single word query highlights the word`() {
        // Given
        val text = "John Smith"
        val highlight = "John"
        val visibleEnd = text.length

        // When
        val result = buildMultiWordHighlightedText(
            text = text,
            highlight = highlight,
            highlightTextColor = highlightTextColor,
            highlightBackgroundColor = highlightBackgroundColor,
            visibleEnd = visibleEnd
        )

        // Then
        assertEquals(text, result.text)
        assertEquals(1, result.spanStyles.size)

        val span = result.spanStyles[0]
        assertEquals(0, span.start)
        assertEquals(4, span.end)
        assertEquals(highlightTextColor, span.item.color)
        assertEquals(highlightBackgroundColor, span.item.background)
    }

    @Test
    fun `single word query with no match produces no spans`() {
        val text = "John Smith, London"
        val highlight = "Paris"

        val result = buildMultiWordHighlightedText(
            text = text,
            highlight = highlight,
            highlightTextColor = highlightTextColor,
            highlightBackgroundColor = highlightBackgroundColor,
            visibleEnd = text.length
        )

        assertEquals(text, result.text)
        assertTrue(result.spanStyles.isEmpty())
    }

    @Test
    fun `multi word query highlights all occurrences of each word`() {
        // Given
        val text = "John Smith, London Office - John in London"
        val highlight = "john london"
        val visibleEnd = text.length

        // When
        val result = buildMultiWordHighlightedText(
            text = text,
            highlight = highlight,
            highlightTextColor = highlightTextColor,
            highlightBackgroundColor = highlightBackgroundColor,
            visibleEnd = visibleEnd
        )

        // Then
        assertEquals(text, result.text)
        assertEquals(4, result.spanStyles.size)

        fun assertSpanCoversSubstring(spanIndex: Int, substring: String) {
            val span = result.spanStyles[spanIndex]
            val actual = result.text.substring(span.start, span.end)
            assertEquals(substring, actual)
            assertEquals(highlightTextColor, span.item.color)
            assertEquals(highlightBackgroundColor, span.item.background)
        }

        assertSpanCoversSubstring(0, "John")
        assertSpanCoversSubstring(1, "London")
        assertSpanCoversSubstring(2, "John")
        assertSpanCoversSubstring(3, "London")
    }

    @Test
    fun `multi word query ignores extra spaces between words`() {
        // Given
        val text = "John Smith, London"
        val highlight = "  john    london  "

        // When
        val result = buildMultiWordHighlightedText(
            text = text,
            highlight = highlight,
            highlightTextColor = highlightTextColor,
            highlightBackgroundColor = highlightBackgroundColor,
            visibleEnd = text.length
        )

        // Then
        assertEquals(text, result.text)
        assertEquals(2, result.spanStyles.size)

        val firstSpan = result.spanStyles[0]
        val secondSpan = result.spanStyles[1]

        assertEquals("John", text.substring(firstSpan.start, firstSpan.end))
        assertEquals("London", text.substring(secondSpan.start, secondSpan.end))
    }

    @Test
    fun `matching is case insensitive for all query words`() {
        // Given
        val text = "john SMITH, lOnDoN"
        val highlight = "JoHn LoNdOn"

        // When
        val result = buildMultiWordHighlightedText(
            text = text,
            highlight = highlight,
            highlightTextColor = highlightTextColor,
            highlightBackgroundColor = highlightBackgroundColor,
            visibleEnd = text.length
        )

        // Then
        assertEquals(text, result.text)
        assertEquals(2, result.spanStyles.size)

        val johnSpan = result.spanStyles[0]
        val londonSpan = result.spanStyles[1]

        assertEquals("john", text.substring(johnSpan.start, johnSpan.end))
        assertEquals("lOnDoN", text.substring(londonSpan.start, londonSpan.end))
    }

    @Test
    fun `does not highlight matches that start after truncation point`() {
        // Given
        val text = "AAA John BBB"
        val highlight = "john"
        val visibleEnd = 4

        // When
        val result = buildMultiWordHighlightedText(
            text = text,
            highlight = highlight,
            highlightTextColor = highlightTextColor,
            highlightBackgroundColor = highlightBackgroundColor,
            visibleEnd = visibleEnd
        )

        // Then
        assertEquals(text, result.text)
        assertTrue(result.spanStyles.isEmpty())
    }

    @Test
    fun `does not highlight a match that is partially visible`() {
        // Given
        val text = "AAA John BBB"
        val highlight = "john"

        // John is [4,8) but we cut at 6 -> partial visibility
        val visibleEnd = 6

        // When
        val result = buildMultiWordHighlightedText(
            text = text,
            highlight = highlight,
            highlightTextColor = highlightTextColor,
            highlightBackgroundColor = highlightBackgroundColor,
            visibleEnd = visibleEnd
        )

        // Then
        assertEquals(text, result.text)
        assertTrue(result.spanStyles.isEmpty())
    }

    @Test
    fun `prefers the longest word when one word is a prefix of another`() {
        // Given
        val text = "New government policy"
        val highlight = "gov government"

        // When
        val result = buildMultiWordHighlightedText(
            text = text,
            highlight = highlight,
            highlightTextColor = highlightTextColor,
            highlightBackgroundColor = highlightBackgroundColor,
            visibleEnd = text.length
        )

        // Then
        val highlighted = result.spanStyles.map { text.substring(it.start, it.end) }
        assertEquals(listOf("government"), highlighted)
    }
}
