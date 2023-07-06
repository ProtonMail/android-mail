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

package ch.protonmail.android.maildetail.domain.usecase

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.maildetail.domain.model.OpenAttachmentIntentValues
import ch.protonmail.android.mailmessage.domain.entity.AttachmentId
import ch.protonmail.android.mailmessage.domain.entity.MessageId
import ch.protonmail.android.mailmessage.domain.repository.AttachmentRepository
import ch.protonmail.android.mailmessage.domain.repository.MessageRepository
import me.proton.core.domain.entity.UserId
import javax.inject.Inject

class GetAttachmentIntentValues @Inject constructor(
    private val attachmentRepository: AttachmentRepository,
    private val messageRepository: MessageRepository
) {

    suspend operator fun invoke(
        userId: UserId,
        messageId: MessageId,
        attachmentId: AttachmentId
    ): Either<DataError, OpenAttachmentIntentValues> {
        val attachmentHash =
            attachmentRepository.getAttachment(userId, messageId, attachmentId).getOrNull()
        val attachment = messageRepository.getMessageWithBody(userId, messageId).getOrNull()
            ?.messageBody
            ?.attachments
            ?.firstOrNull { it.attachmentId == attachmentId }

        if (attachmentHash == null || attachment == null) {
            return DataError.Local.NoDataCached.left()
        }

        val uri = attachmentHash.uri!!

        return OpenAttachmentIntentValues(
            mimeType = attachment.mimeType,
            uri = uri
        ).right()
    }
}
