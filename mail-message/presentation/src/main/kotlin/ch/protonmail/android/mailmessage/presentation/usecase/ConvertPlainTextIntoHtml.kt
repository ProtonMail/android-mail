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

package ch.protonmail.android.mailmessage.presentation.usecase

import android.text.SpannableStringBuilder
import androidx.core.text.HtmlCompat
import ch.protonmail.android.mailmessage.presentation.model.MessageBodyWithType
import ch.protonmail.android.mailmessage.presentation.model.MimeTypeUiModel
import javax.inject.Inject

/**
 * Converts a [MessageBodyWithType] content text into HTML.
 * Returns the original content if the mime type is not PlainText.
 */
class ConvertPlainTextIntoHtml @Inject constructor() {


    operator fun invoke(messageBodyWithType: MessageBodyWithType): String {
        return if (messageBodyWithType.mimeType != MimeTypeUiModel.PlainText) {
            messageBodyWithType.messageBody
        } else {
            val builder = SpannableStringBuilder()
            messageBodyWithType.messageBody.lines().forEachIndexed { index, line ->
                builder.append(line)
                if (index < messageBodyWithType.messageBody.lines().size - 1) {
                    builder.append("\n")
                }
            }

            return HtmlCompat.toHtml(
                builder,
                HtmlCompat.TO_HTML_PARAGRAPH_LINES_INDIVIDUAL
            ).withWordWrap()
        }
    }

    // If the plaintext message contains very long words
    // we should probably break them to avoid scrolling horizontally.
    private fun String.withWordWrap() = "<body style=\"word-wrap: break-word;\">\n$this</body>"
}
