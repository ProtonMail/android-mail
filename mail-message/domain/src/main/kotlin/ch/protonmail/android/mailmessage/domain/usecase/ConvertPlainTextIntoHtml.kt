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

package ch.protonmail.android.mailmessage.domain.usecase

import android.text.util.Linkify
import androidx.core.text.HtmlCompat
import androidx.core.text.toSpannable
import javax.inject.Inject

class ConvertPlainTextIntoHtml @Inject constructor() {

    /**
     * Converts a message body to HTML.
     *
     * @param messageBody the message body.
     * @param autoTransformLinks detect and transform links to become clickable.
     * Note that the current mask for links detection only applies to web URLs and email addresses.
     *
     * @return the HTML [String] representation of the original `messageBody`.
     */
    operator fun invoke(messageBody: String, autoTransformLinks: Boolean = false): String {
        val spannable = messageBody.toSpannable()

        if (autoTransformLinks) {
            Linkify.addLinks(spannable, Linkify.WEB_URLS or Linkify.EMAIL_ADDRESSES)
        }

        return HtmlCompat.toHtml(spannable, HtmlCompat.TO_HTML_PARAGRAPH_LINES_CONSECUTIVE)
    }
}

