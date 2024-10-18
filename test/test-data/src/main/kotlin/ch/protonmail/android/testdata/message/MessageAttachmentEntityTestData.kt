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

package ch.protonmail.android.testdata.message

import ch.protonmail.android.mailcommon.domain.sample.UserIdSample
import ch.protonmail.android.mailmessage.data.local.entity.MessageAttachmentEntity
import ch.protonmail.android.mailmessage.domain.model.AttachmentId
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailmessage.domain.model.attachments.header.HeaderValue
import ch.protonmail.android.mailmessage.domain.sample.MessageIdSample
import me.proton.core.domain.entity.UserId

object MessageAttachmentEntityTestData {


    fun invoice(userId: UserId = UserIdSample.Primary, messageId: MessageId = MessageIdSample.Invoice) =
        MessageAttachmentEntity(
            userId = userId,
            messageId = messageId,
            attachmentId = AttachmentId("invoice"),
            name = "invoice.pdf",
            size = 5678,
            mimeType = "application/pdf",
            disposition = null,
            keyPackets = "keyPackets",
            signature = null,
            encSignature = null,
            headers = emptyMap()
        )

    fun document(userId: UserId = UserIdSample.Primary, messageId: MessageId = MessageIdSample.AlphaAppInfoRequest) =
        MessageAttachmentEntity(
            userId = userId,
            messageId = messageId,
            attachmentId = AttachmentId("document"),
            name = "document.pdf",
            size = 1234,
            mimeType = "application/doc",
            disposition = null,
            keyPackets = null,
            signature = null,
            encSignature = null,
            headers = emptyMap()
        )

    fun build(
        userId: UserId = UserIdSample.Primary,
        messageId: MessageId = MessageIdSample.Invoice,
        attachmentId: AttachmentId = AttachmentId("attachmentId"),
        name: String = "name.jpg",
        size: Long = 123,
        mimeType: String = "image/jpeg",
        disposition: String? = null,
        keyPackets: String? = null,
        signature: String? = null,
        encSignature: String? = null,
        headers: Map<String, HeaderValue> = emptyMap()
    ) = MessageAttachmentEntity(
        userId = userId,
        messageId = messageId,
        attachmentId = attachmentId,
        name = name,
        size = size,
        mimeType = mimeType,
        disposition = disposition,
        keyPackets = keyPackets,
        signature = signature,
        encSignature = encSignature,
        headers = headers
    )

    fun createHeaderMap(vararg pairs: Pair<String, Any>): Map<String, HeaderValue> =
        pairs.toMap().mapValues { (_, value) ->
            when (value) {
                is String -> HeaderValue.StringValue(value)
                is List<*> -> HeaderValue.ListValue(value.filterIsInstance<String>())
                else -> throw IllegalArgumentException("Unsupported header value type: ${value::class.java}")
            }
        }
}
