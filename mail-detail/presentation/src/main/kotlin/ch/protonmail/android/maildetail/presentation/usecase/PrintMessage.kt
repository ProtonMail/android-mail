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

import java.io.ByteArrayInputStream
import android.content.Context
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.maildetail.presentation.R
import ch.protonmail.android.maildetail.presentation.model.MessageDetailHeaderUiModel
import ch.protonmail.android.maildetail.presentation.model.ParticipantUiModel
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailmessage.domain.usecase.GetEmbeddedImageResult
import ch.protonmail.android.mailmessage.presentation.extension.getTotalAttachmentByteSizeReadable
import ch.protonmail.android.mailmessage.presentation.extension.isEmbeddedImage
import ch.protonmail.android.mailmessage.presentation.extension.isRemoteContent
import ch.protonmail.android.mailmessage.presentation.extension.isRemoteUnsecuredContent
import ch.protonmail.android.mailmessage.presentation.extension.getSecuredWebResourceResponse
import ch.protonmail.android.mailmessage.presentation.model.MessageBodyExpandCollapseMode
import ch.protonmail.android.mailmessage.presentation.model.MessageBodyUiModel
import org.jsoup.Jsoup
import timber.log.Timber
import javax.inject.Inject

class PrintMessage @Inject constructor() {

    @Suppress("LongParameterList")
    operator fun invoke(
        context: Context,
        subject: String,
        messageHeaderUiModel: MessageDetailHeaderUiModel,
        messageBodyUiModel: MessageBodyUiModel,
        messageBodyExpandCollapseMode: MessageBodyExpandCollapseMode,
        loadEmbeddedImage: (MessageId, String) -> GetEmbeddedImageResult?
    ) {
        val webView = WebView(context)
        webView.webViewClient = PrintWebViewClient(
            context,
            subject,
            messageBodyUiModel,
            loadEmbeddedImage
        )
        val documentHeader = buildDocumentHeader(context, subject, messageHeaderUiModel, messageBodyUiModel)
        val messageBody = Jsoup.parse(
            if (messageBodyExpandCollapseMode == MessageBodyExpandCollapseMode.Expanded) {
                messageBodyUiModel.messageBody
            } else {
                messageBodyUiModel.messageBodyWithoutQuote
            }
        )
        messageBody.body().prepend(documentHeader)
        webView.loadDataWithBaseURL(null, messageBody.toString(), "text/HTML", "UTF-8", null)
    }

    private fun buildDocumentHeader(
        context: Context,
        subject: String,
        messageHeaderUiModel: MessageDetailHeaderUiModel,
        messageBodyUiModel: MessageBodyUiModel
    ): String {
        val documentHeader = StringBuilder()
        documentHeader.append(
            """
                <hr>
                ${context.resources.getString(R.string.subject)} $subject
                <br/>
                ${context.resources.getString(R.string.from)} ${messageHeaderUiModel.sender.print()}
                <br/>
            """.trimIndent()
        )
        if (messageHeaderUiModel.toRecipients.isNotEmpty()) {
            val toLabel = context.resources.getString(R.string.to)
            documentHeader.append(
                "$toLabel ${messageHeaderUiModel.toRecipients.joinToString { it.print() }}<br/>"
            )
        }
        if (messageHeaderUiModel.ccRecipients.isNotEmpty()) {
            val ccLabel = context.resources.getString(R.string.cc)
            documentHeader.append(
                "$ccLabel ${messageHeaderUiModel.ccRecipients.joinToString { it.print() }}<br/>"
            )
        }
        if (messageHeaderUiModel.bccRecipients.isNotEmpty()) {
            val bccLabel = context.resources.getString(R.string.bcc)
            documentHeader.append(
                "$bccLabel ${messageHeaderUiModel.bccRecipients.joinToString { it.print() }}<br/>"
            )
        }
        val dateLabel = context.resources.getString(R.string.date)
        documentHeader.append(
            "$dateLabel ${(messageHeaderUiModel.extendedTime as TextUiModel.Text).value}<br/>"
        )
        messageBodyUiModel.attachments?.let { attachmentGroupUiModel ->
            val attachmentQuantity = context.resources.getQuantityString(
                R.plurals.attachment,
                attachmentGroupUiModel.attachments.size,
                attachmentGroupUiModel.attachments.size
            )
            val attachmentSize = attachmentGroupUiModel.attachments.getTotalAttachmentByteSizeReadable(context)
            documentHeader.append(
                "$attachmentQuantity ($attachmentSize)"
            )
        }
        documentHeader.append("<hr>")
        return documentHeader.toString()
    }

    private fun ParticipantUiModel.print() = "$participantName $participantAddress"

    private class PrintWebViewClient(
        private val context: Context,
        private val subject: String,
        private val messageBodyUiModel: MessageBodyUiModel,
        private val loadEmbeddedImage: (MessageId, String) -> GetEmbeddedImageResult?
    ) : WebViewClient() {

        @Suppress("TooGenericExceptionCaught")
        override fun onPageFinished(webView: WebView, url: String) {
            val printAdapter = webView.createPrintDocumentAdapter(subject)
            val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
            try {
                printManager.print(subject, printAdapter, PrintAttributes.Builder().build())
            } catch (exception: Exception) {
                Timber.e(exception, "Error printing message")
                Toast.makeText(
                    context, context.resources.getString(R.string.error_print_failed), Toast.LENGTH_LONG
                ).show()
            }
        }

        override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
            return if (!messageBodyUiModel.shouldShowRemoteContent && request?.isRemoteContent() == true) {
                WebResourceResponse("", "", null)
            } else if (messageBodyUiModel.shouldShowEmbeddedImages && request?.isEmbeddedImage() == true) {
                loadEmbeddedImage(messageBodyUiModel.messageId, "<${request.url.schemeSpecificPart}>")?.let {
                    WebResourceResponse(it.mimeType, "", ByteArrayInputStream(it.data))
                }
            } else if (request?.isRemoteUnsecuredContent() == true) {
                request.getSecuredWebResourceResponse()
            } else {
                super.shouldInterceptRequest(view, request)
            }
        }
    }
}
