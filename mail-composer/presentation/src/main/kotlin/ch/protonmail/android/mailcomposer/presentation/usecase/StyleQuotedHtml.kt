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

import ch.protonmail.android.mailcomposer.domain.model.OriginalHtmlQuote
import ch.protonmail.android.mailcomposer.domain.model.StyledHtmlQuote
import ch.protonmail.android.mailmessage.presentation.model.MessageBodyWithType
import ch.protonmail.android.mailmessage.presentation.model.MimeTypeUiModel
import ch.protonmail.android.mailmessage.presentation.usecase.InjectCssIntoDecryptedMessageBody
import ch.protonmail.android.mailmessage.presentation.usecase.SanitizeHtmlOfDecryptedMessageBody
import javax.inject.Inject

class StyleQuotedHtml @Inject constructor(
    private val injectCssIntoDecryptedMessageBody: InjectCssIntoDecryptedMessageBody,
    private val sanitizeHtmlOfDecryptedMessageBody: SanitizeHtmlOfDecryptedMessageBody
) {

    operator fun invoke(originalHtmlQuote: OriginalHtmlQuote): StyledHtmlQuote {
        val originalHtmlMessageQuoteWithType = MessageBodyWithType(originalHtmlQuote.value, MimeTypeUiModel.Html)
        val sanitizedQuoteRaw = sanitizeHtmlOfDecryptedMessageBody(originalHtmlMessageQuoteWithType)

        val sanitizedHtmlMessageWithType = MessageBodyWithType(sanitizedQuoteRaw, MimeTypeUiModel.Html)
        val styledQuoteRaw = injectCssIntoDecryptedMessageBody(sanitizedHtmlMessageWithType)

        return StyledHtmlQuote(styledQuoteRaw)
    }
}
