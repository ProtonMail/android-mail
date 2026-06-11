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
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.maildetail.presentation.R
import ch.protonmail.android.maildetail.presentation.model.MessageDetailHeaderUiModel
import ch.protonmail.android.maildetail.presentation.model.ParticipantUiModel
import ch.protonmail.android.mailmessage.presentation.extension.getTotalAttachmentByteSizeReadable
import ch.protonmail.android.mailmessage.presentation.model.attachment.AttachmentGroupUiModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class PrintMessageHeaderBuilder @Inject constructor(
    @ApplicationContext private val context: Context,
    private val getPrintHeaderCss: GetPrintHeaderStyle
) {

    @Suppress("LongMethod")
    fun buildHeader(
        subject: String,
        messageHeader: MessageDetailHeaderUiModel,
        attachments: AttachmentGroupUiModel?
    ): String {
        return buildString {
            // Try to add the header custom CSS to prettify it
            getPrintHeaderCss().getOrNull()?.let { append("<style>$it</style>") }
            append("<div class='print-header'>")

            // Subject as title
            append("<div class='print-header-title'>${subject.escapeHtml()}</div>")

            // From
            append("<div class='print-header-row'>")
            append("<span class='print-header-label'>${context.getString(R.string.from)} </span>")
            append("<span class='print-header-value'>${messageHeader.sender.format()}</span>")
            append("</div>")

            // To recipients
            if (messageHeader.toRecipients.isNotEmpty()) {
                append("<div class='print-header-row'>")
                append("<span class='print-header-label'>${context.getString(R.string.to)} </span>")
                append("<span class='print-header-value'>")
                messageHeader.toRecipients.forEachIndexed { index, recipient ->
                    append("<span class='print-header-recipient'>${recipient.format()}</span>")
                    if (index < messageHeader.toRecipients.size - 1) append(", ")
                }
                append("</span>")
                append("</div>")
            }

            // CC recipients
            if (messageHeader.ccRecipients.isNotEmpty()) {
                append("<div class='print-header-row'>")
                append("<span class='print-header-label'>${context.getString(R.string.cc)} </span>")
                append("<span class='print-header-value'>")
                messageHeader.ccRecipients.forEachIndexed { index, recipient ->
                    append("<span class='print-header-recipient'>${recipient.format()}</span>")
                    if (index < messageHeader.ccRecipients.size - 1) append(", ")
                }
                append("</span>")
                append("</div>")
            }

            // BCC recipients
            if (messageHeader.bccRecipients.isNotEmpty()) {
                append("<div class='print-header-row'>")
                append("<span class='print-header-label'>${context.getString(R.string.bcc)} </span>")
                append("<span class='print-header-value'>")
                messageHeader.bccRecipients.forEachIndexed { index, recipient ->
                    append("<span class='print-header-recipient'>${recipient.format()}</span>")
                    if (index < messageHeader.bccRecipients.size - 1) append(", ")
                }
                append("</span>")
                append("</div>")
            }

            // Date
            val dateText = (messageHeader.extendedTime as? TextUiModel.Text)?.value ?: ""
            append("<div class='print-header-row'>")
            append("<span class='print-header-label'>${context.getString(R.string.date)} </span>")
            append("<span class='print-header-value'>$dateText</span>")
            append("</div>")

            // Attachments
            attachments?.let {
                val attachmentCount = it.attachments.size
                val attachmentText = context.resources.getQuantityString(
                    R.plurals.attachment,
                    attachmentCount,
                    attachmentCount
                )
                val attachmentSize = it.attachments.getTotalAttachmentByteSizeReadable(context)
                append("<div class='print-header-attachment'>$attachmentText ($attachmentSize)</div>")
            }

            append("</div>")
        }
    }

    private fun ParticipantUiModel.format(): String {
        val name = participantName.escapeHtml()
        val address = participantAddress.escapeHtml()
        return if (participantName.isNotBlank() && participantName != participantAddress) {
            "$name &lt;$address&gt;"
        } else {
            address
        }
    }

    private fun String.escapeHtml(): String = buildString(length) {
        this@escapeHtml.forEach { char ->
            when (char) {
                '&' -> append("&amp;")
                '<' -> append("&lt;")
                '>' -> append("&gt;")
                '"' -> append("&quot;")
                '\'' -> append("&#39;")
                else -> append(char)
            }
        }
    }
}
