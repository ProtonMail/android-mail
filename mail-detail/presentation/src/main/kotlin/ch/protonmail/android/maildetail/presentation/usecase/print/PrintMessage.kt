/*
 * Copyright (c) 2025 Proton Technologies AG
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

package ch.protonmail.android.maildetail.presentation.usecase.print

import android.content.Context
import ch.protonmail.android.maildetail.presentation.model.MessageDetailHeaderUiModel
import ch.protonmail.android.mailmessage.domain.model.MessageBodyImage
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailmessage.presentation.model.MessageBodyUiModel
import javax.inject.Inject

class PrintMessage @Inject constructor(
    private val documentBuilder: PrintMessageDocumentBuilder,
    private val webViewHandler: PrintWebViewHandler
) {

    suspend operator fun invoke(
        context: Context,
        subject: String,
        messageHeader: MessageDetailHeaderUiModel,
        messageBody: MessageBodyUiModel,
        loadEmbeddedImage: (MessageId, String) -> MessageBodyImage?,
        printConfiguration: PrintConfiguration = PrintConfiguration()
    ) {
        val htmlDocument = documentBuilder.buildDocument(
            context = context,
            subject = subject,
            messageHeader = messageHeader,
            messageBody = messageBody
        )

        val resourceConfig = PrintWebViewHandler.ResourceLoadingConfig(
            messageId = messageBody.messageId,
            loadEmbeddedImage = loadEmbeddedImage,
            showRemoteContent = printConfiguration.showRemoteContent,
            showEmbeddedImages = printConfiguration.showEmbeddedImages
        )

        val webViewConfig = PrintWebViewHandler.PrintWebViewConfig(
            context = context,
            htmlContent = htmlDocument,
            subject = subject,
            resourceConfig = resourceConfig
        )

        webViewHandler.createPrintWebView(webViewConfig)
    }
}

data class PrintConfiguration(
    val showRemoteContent: Boolean = false,
    val showEmbeddedImages: Boolean = true,
    val includeQuotedText: Boolean = true
)
