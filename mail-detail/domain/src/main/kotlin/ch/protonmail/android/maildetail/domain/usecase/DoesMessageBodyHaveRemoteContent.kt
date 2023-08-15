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

package ch.protonmail.android.maildetail.domain.usecase

import java.util.regex.Pattern
import ch.protonmail.android.mailmessage.domain.model.DecryptedMessageBody
import ch.protonmail.android.mailmessage.domain.model.MimeType
import org.jsoup.Jsoup
import javax.inject.Inject

class DoesMessageBodyHaveRemoteContent @Inject constructor() {

    @Suppress("ReturnCount")
    operator fun invoke(messageBody: DecryptedMessageBody): Boolean {
        if (messageBody.mimeType == MimeType.PlainText) {
            return false
        }

        val messageBodyDocument = Jsoup.parse(messageBody.value)
        val imgElements = messageBodyDocument.select("img")
        val styleElements = messageBodyDocument.select("style")
        val elementsWithStyleAttribute = messageBodyDocument.select("[style]")

        imgElements.forEach { element ->
            if (Pattern.compile(REGEX_REMOTE_URL).matcher(element.attr("src")).matches())
                return true
        }

        styleElements.forEach { element ->
            if (Pattern.compile(REGEX_STYLE_URL).matcher(element.data().replace(NEW_LINE, EMPTY_SPACE)).matches())
                return true
        }

        elementsWithStyleAttribute.forEach { element ->
            if (Pattern.compile(REGEX_STYLE_URL).matcher(element.attr("style")).matches())
                return true
        }

        return false
    }

    companion object {
        private const val REGEX_REMOTE_URL = "^(https?:)?//.*?"
        private const val REGEX_STYLE_URL = ".*?url\\((.*?)\\).*?"
        private const val NEW_LINE = '\n'
        private const val EMPTY_SPACE = ' '
    }
}
