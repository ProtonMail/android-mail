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

package ch.protonmail.android.mailmessage.domain.usecase

import arrow.core.Either
import arrow.core.raise.either
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailmessage.domain.extension.hasAllowedEmbeddedImageMimeType
import ch.protonmail.android.mailmessage.domain.model.MessageAttachment
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailmessage.domain.repository.AttachmentRepository
import ch.protonmail.android.mailmessage.domain.repository.MessageRepository
import kotlinx.serialization.json.JsonPrimitive
import me.proton.core.domain.entity.UserId
import me.proton.core.util.kotlin.serialize
import javax.inject.Inject

class GetEmbeddedImage @Inject constructor(
    private val attachmentRepository: AttachmentRepository,
    private val messageRepository: MessageRepository
) {

    suspend operator fun invoke(
        userId: UserId,
        messageId: MessageId,
        contentId: String
    ): Either<DataError, GetEmbeddedImageResult> = either {
        val messageWithBody = messageRepository.getLocalMessageWithBody(userId, messageId)
            ?: raise(DataError.Local.NoDataCached)

        val attachment = messageWithBody.messageBody.attachments.findEmbeddedContentById(contentId).bind()

        val decryptedEmbeddedImage =
            attachmentRepository.getEmbeddedImage(userId, messageId, attachment.attachmentId).bind()

        GetEmbeddedImageResult(
            data = decryptedEmbeddedImage,
            mimeType = attachment.mimeType
        )
    }

    private fun List<MessageAttachment>.findEmbeddedContentById(contentId: String) = either {
        filter {
            it.hasAllowedEmbeddedImageMimeType()
        }.firstOrNull {
            val headerContentId = it.headers[ContentIdKeyIdentifier]

            // The OR condition below is for backwards compatibility, as the serialisation strategy
            // has changed with MAILANDR-1531 to support generic JsonElements rather than just Strings.
            headerContentId == JsonPrimitive(contentId.serialize()) || headerContentId == JsonPrimitive(contentId)
        } ?: raise(DataError.Local.NoDataCached)
    }

    private companion object {

        const val ContentIdKeyIdentifier = "content-id"
    }
}

data class GetEmbeddedImageResult(
    val data: ByteArray,
    val mimeType: String
)
