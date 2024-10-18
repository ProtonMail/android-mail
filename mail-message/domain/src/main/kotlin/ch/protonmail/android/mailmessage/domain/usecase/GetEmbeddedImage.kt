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
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailmessage.domain.model.attachments.header.HeaderValue
import ch.protonmail.android.mailmessage.domain.repository.AttachmentRepository
import ch.protonmail.android.mailmessage.domain.repository.MessageRepository
import me.proton.core.domain.entity.UserId
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
        val messageWithBody =
            messageRepository.getLocalMessageWithBody(userId, messageId) ?: raise(DataError.Local.NoDataCached)
        val attachment = messageWithBody.messageBody.attachments
            .filter { it.hasAllowedEmbeddedImageMimeType() }
            .firstOrNull {
                when (val headerValue = it.headers["content-id"]) {
                    is HeaderValue.StringValue -> headerValue.value == contentId
                    else -> false
                }
            }
            ?: raise(DataError.Local.NoDataCached)

        val decryptedEmbeddedImage =
            attachmentRepository.getEmbeddedImage(userId, messageId, attachment.attachmentId).bind()

        GetEmbeddedImageResult(
            data = decryptedEmbeddedImage,
            mimeType = attachment.mimeType
        )
    }
}

data class GetEmbeddedImageResult(
    val data: ByteArray,
    val mimeType: String
)
