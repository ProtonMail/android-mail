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

package ch.protonmail.android.mailmessage.data.mapper

import ch.protonmail.android.mailmessage.data.local.entity.MessageAttachmentEntity
import ch.protonmail.android.mailmessage.domain.model.MessageAttachment
import ch.protonmail.android.mailmessage.domain.model.MessageId
import me.proton.core.domain.entity.UserId
import javax.inject.Inject

class MessageAttachmentEntityMapper @Inject constructor() {

    fun toMessageAttachmentEntity(
        userId: UserId,
        messageId: MessageId,
        messageAttachment: MessageAttachment
    ) = MessageAttachmentEntity(
        userId = userId,
        messageId = messageId,
        attachmentId = messageAttachment.attachmentId,
        name = messageAttachment.name,
        size = messageAttachment.size,
        mimeType = messageAttachment.mimeType,
        disposition = messageAttachment.disposition,
        keyPackets = messageAttachment.keyPackets,
        signature = messageAttachment.signature,
        encSignature = messageAttachment.encSignature,
        headers = messageAttachment.headers
    )

    fun toMessageAttachment(messageAttachmentEntity: MessageAttachmentEntity) = MessageAttachment(
        attachmentId = messageAttachmentEntity.attachmentId,
        name = messageAttachmentEntity.name,
        size = messageAttachmentEntity.size,
        mimeType = messageAttachmentEntity.mimeType,
        disposition = messageAttachmentEntity.disposition,
        keyPackets = messageAttachmentEntity.keyPackets,
        signature = messageAttachmentEntity.signature,
        encSignature = messageAttachmentEntity.encSignature,
        headers = messageAttachmentEntity.headers
    )

}
