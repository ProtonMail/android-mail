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

package ch.protonmail.android.uicomponents.text

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Constraints
import ch.protonmail.android.design.compose.theme.ProtonTheme

@Composable
fun HighlightedText(
    text: String,
    highlight: String,
    highlightColor: Color = ProtonTheme.colors.textAccent,
    maxLines: Int,
    style: TextStyle,
    overflow: TextOverflow
) {
    val annotatedString = buildAnnotatedString {
        if (highlight.isNotEmpty()) {
            val startIndex = text.lowercase().indexOf(highlight.lowercase())
            if (startIndex >= 0) {
                val endIndex = (startIndex + highlight.length).coerceAtMost(text.length)
                append(text.substring(0, startIndex))

                withStyle(style = SpanStyle(color = highlightColor)) {
                    append(text.substring(startIndex, endIndex))
                }

                append(text.substring(endIndex))
            } else {
                append(text)
            }
        } else {
            append(text)
        }
    }

    BasicText(
        text = annotatedString,
        maxLines = maxLines,
        // BasicText ignores LocalContentColor, so an unspecified style color would draw black
        // Fall back to the content color like Material Text does.
        style = style.copy(color = style.color.takeOrElse { LocalContentColor.current }),
        overflow = overflow
    )
}

@Composable
fun MultiWordHighlightedText(
    modifier: Modifier = Modifier,
    text: String,
    highlight: String,
    highlightTextColor: Color = ProtonTheme.colors.textAccent,
    highlightBackgroundColor: Color = Color.Transparent,
    maxLines: Int,
    style: TextStyle,
    overflow: TextOverflow
) {
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current

    BoxWithConstraints(modifier = modifier) {
        val maxWidthPx = with(density) { maxWidth.roundToPx() }

        val visibleEnd = remember(text, style, maxLines, overflow, maxWidthPx) {
            if (maxWidthPx <= 0 || overflow != TextOverflow.Ellipsis) {
                text.length
            } else {
                val layout = textMeasurer.measure(
                    text = AnnotatedString(text),
                    style = style,
                    maxLines = maxLines,
                    overflow = overflow,
                    constraints = Constraints(maxWidth = maxWidthPx)
                )
                val lastLine = layout.lineCount - 1
                layout.getLineEnd(lastLine, visibleEnd = true)
            }
        }

        val annotatedString = remember(
            text, highlight,
            highlightTextColor, highlightBackgroundColor, visibleEnd
        ) {
            buildMultiWordHighlightedText(
                text = text,
                highlight = highlight,
                highlightTextColor = highlightTextColor,
                highlightBackgroundColor = highlightBackgroundColor,
                visibleEnd = visibleEnd.coerceIn(0, text.length)
            )
        }

        BasicText(
            modifier = modifier,
            text = annotatedString,
            maxLines = maxLines,
            style = style.copy(color = style.color.takeOrElse { LocalContentColor.current }),
            overflow = overflow
        )
    }
}

fun buildMultiWordHighlightedText(
    text: String,
    highlight: String,
    highlightTextColor: Color,
    highlightBackgroundColor: Color,
    visibleEnd: Int
): AnnotatedString {
    val query = highlight.trim()

    return buildAnnotatedString {

        if (query.isEmpty()) {
            // Nothing to highlight
            append(text)
        } else {
            // Split query into words
            val words = query.trim().split(Regex("\\s+"))

            if (words.isEmpty()) {
                append(text)
            } else {
                val pattern = words.joinToString("|") { Regex.escape(it) }
                val regex = Regex(pattern, RegexOption.IGNORE_CASE)

                var currentIndex = 0

                for (match in regex.findAll(text)) {
                    val start = match.range.first
                    val end = match.range.last + 1

                    // Stop highlighting once we reach truncated area
                    if (end > visibleEnd) {
                        break
                    }

                    // Append text before the match
                    if (currentIndex < start) {
                        append(text.substring(currentIndex, start))
                    }

                    // Append highlighted part
                    withStyle(
                        style = SpanStyle(
                            color = highlightTextColor,
                            background = highlightBackgroundColor
                        )
                    ) {
                        append(text.substring(start, end))
                    }

                    currentIndex = end
                }

                // Append remaining text after the last match
                if (currentIndex < text.length) {
                    append(text.substring(currentIndex))
                }
            }
        }
    }
}
