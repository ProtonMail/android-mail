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

package ch.protonmail.android.mailcomposer.domain.usecase

import androidx.core.text.HtmlCompat
import ch.protonmail.android.mailcomposer.domain.model.DraftBody
import ch.protonmail.android.mailcomposer.domain.model.OriginalHtmlQuote
import ch.protonmail.android.mailmessage.domain.model.DecryptedMessageBody
import ch.protonmail.android.mailmessage.domain.model.MimeType
import org.jsoup.Jsoup
import javax.inject.Inject

class SplitMessageBodyHtmlQuote @Inject constructor() {

    operator fun invoke(decryptedBody: DecryptedMessageBody): Pair<DraftBody, OriginalHtmlQuote?> {
        if (decryptedBody.mimeType == MimeType.PlainText) {
            return Pair(DraftBody(decryptedBody.value), null)
        }

        val htmlBodyDocument = Jsoup.parse(decryptedBody.value)
        var htmlQuote: String? = null

        for (quoteAnchor in QuoteAnchors) {
            val quotedContentElements = htmlBodyDocument.select(quoteAnchor)
            if (quotedContentElements.isNotEmpty()) {
                htmlQuote = quotedContentElements[0].toString()
                // Removes the quoted content from htmlBodyDocument
                quotedContentElements.remove()
            }
        }

        val bodyContent = HtmlCompat.fromHtml(
            htmlBodyDocument.body().html(),
            HtmlCompat.FROM_HTML_SEPARATOR_LINE_BREAK_DIV
        ).toString()

        val draftBody = DraftBody(bodyContent)
        val draftQuote = htmlQuote?.let { OriginalHtmlQuote(it) }
        return Pair(draftBody, draftQuote)
    }

    companion object {

        private val QuoteAnchors = listOf(
            ".protonmail_quote",
            ".gmail_quote",
            ".yahoo_quoted",
            ".gmail_extra",
            ".zmail_extra", // zoho
            ".moz-cite-prefix",
            "#isForwardContent",
            "#isReplyContent",
            "#mailcontent:not(table)",
            "#origbody",
            "#reply139content",
            "#oriMsgHtmlSeperator",
            "blockquote[type=\"cite\"]",
            "[name=\"quote\"]" // gmx
        )
    }
}
