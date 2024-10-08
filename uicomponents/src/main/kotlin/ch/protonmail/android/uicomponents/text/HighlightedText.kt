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

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import me.proton.core.compose.theme.ProtonTheme

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
        style = style,
        overflow = overflow
    )
}
