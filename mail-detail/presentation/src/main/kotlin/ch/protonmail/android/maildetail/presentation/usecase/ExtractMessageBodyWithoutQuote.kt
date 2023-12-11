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

package ch.protonmail.android.maildetail.presentation.usecase

import ch.protonmail.android.maildetail.presentation.model.MessageBodyWithoutQuote
import org.jsoup.Jsoup
import javax.inject.Inject

class ExtractMessageBodyWithoutQuote @Inject constructor() {

    private val quoteDescriptors = listOf(
        ".protonmail_quote", // Proton Mail
        ".gmail_quote", // Gmail
        ".yahoo_quoted", // Yahoo Mail
        ".gmail_extra", // Gmail
        ".zmail_extra", // zoho
        ".moz-cite-prefix",
        ".tutanota_quote", // Tutanota Mail
        ".skiff_quote", // Skiff Mail
        "#replySplit",
        "#isForwardContent",
        "#isReplyContent",
        "#mailcontent:not(table)",
        "#origbody",
        "#reply139content",
        "#oriMsgHtmlSeperator",
        "#divRplyFwdMsg", // Outlook Mail
        "blockquote[type=\"cite\"]",
        "blockquote.iosymail", // Yahoo iOS Mail
        "blockquote[data-skiff-mail]", // Skiff Mail
        "[name=\"quote\"]" // gmx
    )

    operator fun invoke(messageBody: String): MessageBodyWithoutQuote {
        val htmlDocumentWithoutQuote = Jsoup.parse(messageBody)
        var hasQuote = false
        for (quoteElement in quoteDescriptors) {
            val quotedContentElements = htmlDocumentWithoutQuote.select(quoteElement)
            if (quotedContentElements.isNotEmpty()) {
                quotedContentElements.remove()
                hasQuote = true
            }
        }

        return MessageBodyWithoutQuote(htmlDocumentWithoutQuote.toString(), hasQuote)
    }

}
