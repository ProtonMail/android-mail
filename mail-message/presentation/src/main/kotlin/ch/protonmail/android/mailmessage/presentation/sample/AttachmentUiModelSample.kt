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

package ch.protonmail.android.mailmessage.presentation.sample

import ch.protonmail.android.mailmessage.domain.model.AttachmentWorkerStatus
import ch.protonmail.android.mailmessage.presentation.model.AttachmentUiModel

object AttachmentUiModelSample {

    val invoice = AttachmentUiModel(
        attachmentId = "invoice",
        fileName = "invoice",
        extension = "pdf",
        size = 5678,
        mimeType = "application/pdf"
    )

    val invoiceStatusRunning = AttachmentUiModel(
        attachmentId = "invoice",
        fileName = "invoice",
        extension = "pdf",
        size = 5678,
        mimeType = "application/pdf",
        status = AttachmentWorkerStatus.Running
    )

    val deletableInvoice = invoice.copy(
        deletable = true
    )

    val document = AttachmentUiModel(
        attachmentId = "document",
        fileName = "document",
        extension = "pdf",
        size = 1234,
        mimeType = "application/doc"
    )

    val documentWithMultipleDots = AttachmentUiModel(
        attachmentId = "complicated.document.name",
        fileName = "complicated.document",
        extension = "pdf",
        size = 1234,
        mimeType = "application/doc"
    )

    val documentWithReallyLongFileName = AttachmentUiModel(
        attachmentId = "document",
        fileName = "document-with-really-long-and-unnecessary-file-name-that-should-be-truncated",
        extension = "pdf",
        size = 1234,
        mimeType = "application/doc"
    )

    val image = AttachmentUiModel(
        attachmentId = "image",
        fileName = "image",
        extension = "png",
        size = 1234,
        mimeType = "image/png"
    )

    val calendar = AttachmentUiModel(
        attachmentId = "calendar",
        fileName = "invite",
        extension = "ics",
        size = 1234,
        mimeType = "text/calendar"
    )

    val invoiceWithBinaryContentType = AttachmentUiModel(
        attachmentId = "invoice_binary_content_type",
        fileName = "invoice",
        extension = "pdf",
        size = 1234,
        mimeType = "application/pdf"
    )

}
