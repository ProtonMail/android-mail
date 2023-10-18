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

package ch.protonmail.android.mailcomposer.domain.usecase

import arrow.core.Either
import arrow.core.raise.either
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcomposer.domain.model.SenderEmail
import ch.protonmail.android.mailcomposer.domain.repository.AttachmentRepository
import ch.protonmail.android.mailmessage.domain.model.AttachmentId
import ch.protonmail.android.mailmessage.domain.model.MessageId
import me.proton.core.domain.entity.UserId
import javax.inject.Inject

class DeleteAttachment @Inject constructor(
    private val localDraft: GetLocalDraft,
    private val attachmentRepository: AttachmentRepository
) {

    suspend operator fun invoke(
        userId: UserId,
        senderEmail: SenderEmail,
        messageId: MessageId,
        attachmentId: AttachmentId
    ): Either<AttachmentDeleteError, Unit> = either {
        val localDraft = localDraft(userId, messageId, senderEmail)
            .mapLeft { AttachmentDeleteError.DraftNotFound }
            .bind()

        attachmentRepository.deleteAttachment(userId, localDraft.message.messageId, attachmentId)
            .mapLeft {
                when (it) {
                    DataError.Local.FailedToDeleteFile -> AttachmentDeleteError.FailedToDeleteFile
                    else -> AttachmentDeleteError.Unknown
                }
            }
            .bind()
    }
}

sealed interface AttachmentDeleteError {
    object DraftNotFound : AttachmentDeleteError
    object FailedToDeleteFile : AttachmentDeleteError
    object Unknown : AttachmentDeleteError
}
