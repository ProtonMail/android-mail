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

package ch.protonmail.android.composer.data.sample

import ch.protonmail.android.mailmessage.data.remote.resource.AttachmentCountsResource
import ch.protonmail.android.mailmessage.data.remote.resource.AttachmentResource
import ch.protonmail.android.mailmessage.data.remote.resource.AttachmentsInfoResource
import ch.protonmail.android.mailmessage.data.remote.resource.MessageWithBodyResource
import ch.protonmail.android.mailmessage.data.remote.resource.RecipientResource
import ch.protonmail.android.mailmessage.domain.model.MessageAttachment
import ch.protonmail.android.mailmessage.domain.model.MessageWithBody
import ch.protonmail.android.mailmessage.domain.sample.MessageWithBodySample
import me.proton.core.util.kotlin.toInt

object MessageWithBodyResourceSample {

    val NewDraftWithSubject = MessageWithBodySample.NewDraftWithSubject.asResource()

    val NewDraftWithAttachments = MessageWithBodySample.MessageWithInvoiceAttachment.asResource()

    val NewDraftWithSubjectAndBody = MessageWithBodySample.NewDraftWithSubjectAndBody.asResource()

    val RemoteDraft = MessageWithBodySample.RemoteDraft.asResource()

    private fun MessageWithBody.asResource() = with(this) {
        MessageWithBodyResource(
            id = message.id,
            order = message.order,
            conversationId = message.conversationId.id,
            subject = message.subject,
            unread = message.unread.toInt(),
            sender = with(message.sender) { RecipientResource(address, name) },
            toList = message.toList.map { RecipientResource(it.address, it.name) },
            ccList = message.ccList.map { RecipientResource(it.address, it.name) },
            bccList = message.bccList.map { RecipientResource(it.address, it.name) },
            time = message.time,
            size = message.size,
            expirationTime = message.expirationTime,
            isReplied = message.isReplied.toInt(),
            isRepliedAll = message.isRepliedAll.toInt(),
            isForwarded = message.isForwarded.toInt(),
            addressId = message.addressId.id,
            labelIds = message.labelIds.map { it.id },
            externalId = message.externalId,
            numAttachments = message.numAttachments,
            flags = message.flags,
            body = messageBody.body,
            header = messageBody.header,
            parsedHeaders = emptyMap(),
            attachments = messageBody.attachments.asResource(),
            mimeType = messageBody.mimeType.value,
            spamScore = messageBody.spamScore,
            replyTo = with(messageBody.replyTo) { RecipientResource(address, name) },
            replyTos = messageBody.replyTos.map { RecipientResource(it.address, it.name) },
            unsubscribeMethods = null,
            attachmentsInfo = AttachmentsInfoResource(
                AttachmentCountsResource(message.attachmentCount.calendar),
                AttachmentCountsResource()
            )
        )
    }

    private fun List<MessageAttachment>.asResource() = this.map {
        AttachmentResource(
            id = it.attachmentId.id,
            name = it.name,
            size = it.size,
            mimeType = it.mimeType, headers = emptyMap()
        )
    }
}
