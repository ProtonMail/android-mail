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

import arrow.core.Either
import arrow.core.continuations.either
import arrow.core.getOrElse
import ch.protonmail.android.composer.data.remote.AttachmentRemoteDataSource
import ch.protonmail.android.composer.data.remote.UploadAttachmentModel
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcomposer.domain.Transactor
import ch.protonmail.android.mailcomposer.domain.model.AttachmentSyncState
import ch.protonmail.android.mailcomposer.domain.repository.AttachmentStateRepository
import ch.protonmail.android.mailcomposer.domain.usecase.FindLocalDraft
import ch.protonmail.android.mailcomposer.domain.usecase.ResolveUserAddress
import ch.protonmail.android.mailmessage.domain.model.AttachmentId
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailmessage.domain.repository.AttachmentRepository
import me.proton.core.domain.entity.UserId
import timber.log.Timber
import javax.inject.Inject

class UploadAttachments @Inject constructor(
    private val attachmentRepository: AttachmentRepository,
    private val attachmentStateRepository: AttachmentStateRepository,
    private val attachmentRemoteDataSource: AttachmentRemoteDataSource,
    private val findLocalDraft: FindLocalDraft,
    private val encryptAndSignAttachment: EncryptAndSignAttachment,
    private val resolveUserAddress: ResolveUserAddress,
    private val transactor: Transactor
) {

    suspend operator fun invoke(userId: UserId, messageId: MessageId): Either<DataError, Unit> = either {
        val localDraft = findLocalDraft(userId, messageId)?.message ?: shift(DataError.MessageSending.DraftNotFound)

        val attachments = attachmentStateRepository.getAllAttachmentStatesForMessage(userId, localDraft.messageId)
            .filter { it.state != AttachmentSyncState.Uploaded }

        if (attachments.isEmpty()) return@either

        val senderAddress = resolveUserAddress(userId, localDraft.addressId)
            .mapLeft { DataError.MessageSending.SenderAddressNotFound }
            .bind()

        attachments.forEach attachment@{ attachmentState ->
            val attachment = attachmentRepository
                .readFileFromStorage(userId, localDraft.messageId, attachmentState.attachmentId)
                .getOrElse { return@attachment }

            val attachmentInfo = attachmentRepository
                .getAttachmentInfo(userId, localDraft.messageId, attachmentState.attachmentId)
                .getOrElse { return@attachment }

            val encryptedAttachment = encryptAndSignAttachment(senderAddress, attachment)
                .getOrElse {
                    Timber.e(it.exception, "Failed to encrypt and sign attachment")
                    return@attachment
                }

            val uploadAttachment = UploadAttachmentModel(
                messageId = localDraft.messageId,
                fileName = attachmentInfo.name,
                mimeType = attachmentInfo.mimeType,
                keyPacket = encryptedAttachment.keyPacket,
                attachment = encryptedAttachment.encryptedAttachment,
                signature = encryptedAttachment.signature
            )

            val response = attachmentRemoteDataSource.uploadAttachment(userId, uploadAttachment).getOrElse {
                Timber.e("Failed to upload attachment: $it")
                return@attachment
            }
            transactor.performTransaction {
                attachmentStateRepository.updateApiAttachmentIdAndSetSyncedState(
                    userId = userId,
                    messageId = localDraft.messageId,
                    attachmentId = attachmentState.attachmentId,
                    apiAttachmentId = AttachmentId(response.attachment.id)
                ).bind()
                attachmentRepository.updateMessageAttachment(
                    userId = userId,
                    messageId = localDraft.messageId,
                    localAttachmentId = attachmentState.attachmentId,
                    attachment = response.attachment.toMessageAttachment()
                ).bind()
            }

        }

    }
}
