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

package ch.protonmail.android.mailcomposer.presentation.usecase

import androidx.core.text.HtmlCompat
import javax.inject.Inject

class ConvertHtmlToPlainText @Inject constructor() {

    operator fun invoke(html: String): String {
        val htmlToConvert = html
            .removeHead()
            .removeStyle()
        return HtmlCompat.fromHtml(htmlToConvert, HtmlCompat.FROM_HTML_MODE_COMPACT).toString()
    }

    private fun String.removeStyle(): String {
        if (this.hasNoStyleTag()) {
            return this
        }
        val styleTag = if (this.contains(STYLE_START_TAG)) STYLE_START_TAG else STYLE_OPEN_START_TAG
        val beforeStyle = this.substringBefore(styleTag)
        val afterStyle = this.substringAfter(STYLE_END_TAG)
        return beforeStyle + afterStyle
    }

    private fun String.removeHead(): String {
        if (this.hasNoHeadTag()) {
            return this
        }
        val beforeHead = this.substringBefore(HEAD_START_TAG)
        val afterHead = this.substringAfter(HEAD_END_TAG)
        return beforeHead + afterHead
    }

    private fun String.hasNoHeadTag() = this.contains(HEAD_START_TAG).not() || this.contains(HEAD_END_TAG).not()

    private fun String.hasNoStyleTag() =
        this.contains(STYLE_START_TAG).not() && this.contains(STYLE_OPEN_START_TAG).not() ||
            this.contains(STYLE_END_TAG).not()

    private companion object {

        const val HEAD_START_TAG = "<head>"
        const val HEAD_END_TAG = "</head>"
        const val STYLE_START_TAG = "<style>"
        const val STYLE_OPEN_START_TAG = "<style "
        const val STYLE_END_TAG = "</style>"
    }
}
