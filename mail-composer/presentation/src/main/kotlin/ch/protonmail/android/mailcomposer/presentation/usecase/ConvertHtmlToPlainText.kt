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
        val headlessHtml = removeHead(html)
        return HtmlCompat.fromHtml(headlessHtml, HtmlCompat.FROM_HTML_MODE_COMPACT).toString()
    }

    private fun removeHead(html: String): String {
        if (html.hasNoHeadTag()) {
            return html
        }
        val beforeHead = html.substringBefore(HEAD_START_TAG)
        val afterHead = html.substringAfter(HEAD_END_TAG)
        return beforeHead + afterHead
    }

    private fun String.hasNoHeadTag() = this.contains(HEAD_START_TAG).not() || this.contains(HEAD_END_TAG).not()

    private companion object {

        const val HEAD_START_TAG = "<head>"
        const val HEAD_END_TAG = "</head>"
    }
}
