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

package ch.protonmail.android.mailcommon.presentation.mapper

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import org.junit.Assert.assertEquals
import org.junit.Test

class MarkdownMapperTest {

    @Test
    fun `should parse bold regions correctly`() {
        testCases.forEach { (markdown, expected) ->
            val actual = MarkdownMapper.parseBoldToAnnotatedString(
                markdown,
                boldColor = boldColor,
                normalColor = normalColor
            )
            assertEquals(
                "Failed on input: \"$markdown\"",
                expected,
                actual
            )
        }
    }
}

private val boldColor = Color.Red
private val normalColor = Color.Blue

private data class TestInput(
    val markdown: String,
    val expected: AnnotatedString
)

private sealed interface Segment {

    val text: String
}

private data class Bold(override val text: String) : Segment
private data class Regular(override val text: String) : Segment

private fun buildMdString(vararg segments: Segment): AnnotatedString = buildAnnotatedString {
    segments.forEach { segment ->
        when (segment) {
            is Bold -> withStyle(SpanStyle(color = boldColor, fontWeight = FontWeight.Bold)) {
                append(segment.text)
            }

            is Regular -> withStyle(SpanStyle(color = normalColor)) {
                append(segment.text)
            }
        }
    }
}

private val testCases = listOf(
    TestInput(
        markdown = "just plain text",
        expected = buildMdString(Regular("just plain text"))
    ),
    TestInput(
        markdown = "**bold**",
        expected = buildMdString(Bold("bold"))
    ),
    TestInput(
        markdown = "start **bold** end",
        expected = buildMdString(
            Regular("start "),
            Bold("bold"),
            Regular(" end")
        )
    ),
    TestInput(
        markdown = "first **one** then **two**! and **last one**",
        expected = buildMdString(
            Regular("first "),
            Bold("one"),
            Regular(" then "),
            Bold("two"),
            Regular("! and "),
            Bold("last one")
        )
    ),
    TestInput(
        markdown = "first **one** then **two**! rem",
        expected = buildMdString(
            Regular("first "),
            Bold("one"),
            Regular(" then "),
            Bold("two"),
            Regular("! rem")
        )
    ),
    TestInput(
        markdown = "single stars *are not bold* here",
        expected = buildMdString(Regular("single stars *are not bold* here"))
    ),
    TestInput(
        markdown = "unmatched **bold is not matched",
        expected = buildMdString(Regular("unmatched **bold is not matched"))
    ),
    TestInput(
        markdown = "empty **** markers",
        expected = buildMdString(
            Regular("empty "),
            Bold(""),
            Regular(" markers")
        )
    ),
    TestInput(
        markdown = "****",
        expected = buildMdString(Bold(""))
    ),
    TestInput(
        markdown = "**A****B**",
        expected = buildMdString(
            Bold("A"),
            Bold("B")
        )
    ),
    TestInput(
        markdown = "",
        expected = buildMdString()
    )
)
