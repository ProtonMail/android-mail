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

package ch.protonmail.android.composer.data.usecase

import java.io.File
import arrow.core.Either
import arrow.core.raise.either
import ch.protonmail.android.mailmessage.domain.repository.DraftStateRepository
import ch.protonmail.android.mailmessage.data.local.usecase.DecryptAttachmentByteArray
import ch.protonmail.android.mailmessage.domain.model.AttachmentId
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailmessage.domain.repository.AttachmentRepository
import kotlinx.coroutines.flow.first
import me.proton.core.domain.entity.UserId
import timber.log.Timber
import javax.inject.Inject

class GetAttachmentFiles @Inject constructor(
    private val attachmentRepository: AttachmentRepository,
    private val decryptAttachmentByteArray: DecryptAttachmentByteArray,
    private val draftStateRepository: DraftStateRepository
) {

    suspend operator fun invoke(
        userId: UserId,
        messageId: MessageId,
        attachmentIds: List<AttachmentId>
    ): Either<Error, Map<AttachmentId, File>> = either {
        val apiMessageId = draftStateRepository.observe(userId, messageId).first().onLeft {
            Timber.e("No draft state found for $messageId when reading attachments from storage")
        }.getOrNull()?.apiMessageId ?: raise(Error.DraftNotFound)

        attachmentIds.associateWith { attachmentId ->
            attachmentRepository.readFileFromStorage(userId, apiMessageId, attachmentId).fold(
                ifRight = { it },
                ifLeft = {
                    val encryptedAttachment = attachmentRepository.getAttachmentFromRemote(
                        userId,
                        apiMessageId,
                        attachmentId
                    ).mapLeft { Error.DownloadingAttachments }
                        .bind()

                    decryptAttachmentByteArray(userId, apiMessageId, attachmentId, encryptedAttachment).fold(
                        ifRight = {
                            attachmentRepository.saveAttachmentToFile(userId, apiMessageId, attachmentId, it)
                                .mapLeft { Error.FailedToStoreFile }
                                .bind()
                        },
                        ifLeft = { raise(Error.DownloadingAttachments) }
                    )
                }
            )
        }
    }

    sealed interface Error {
        object DraftNotFound : Error
        object FailedToStoreFile : Error
        object DownloadingAttachments : Error
    }

}
