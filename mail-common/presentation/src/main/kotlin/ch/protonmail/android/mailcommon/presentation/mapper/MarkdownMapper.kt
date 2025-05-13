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

object MarkdownMapper {

    private val boldMatchingRegex = Regex("\\*\\*(.*?)\\*\\*")

    fun parseBoldToAnnotatedString(
        mdString: String,
        boldColor: Color,
        normalColor: Color
    ): AnnotatedString = buildAnnotatedString {
        var lastEnd = 0
        for (match in boldMatchingRegex.findAll(mdString)) {
            if (match.range.first > lastEnd) {
                withStyle(SpanStyle(color = normalColor)) {
                    append(mdString.substring(lastEnd, match.range.first))
                }
            }
            withStyle(SpanStyle(color = boldColor, fontWeight = FontWeight.Bold)) {
                append(match.groupValues[1])
            }
            lastEnd = match.range.last + 1
        }
        if (lastEnd < mdString.length) {
            withStyle(SpanStyle(color = normalColor)) {
                append(mdString.substring(lastEnd))
            }
        }
    }
}
