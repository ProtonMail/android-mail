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

package ch.protonmail.android.composer.data.repository

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.right
import ch.protonmail.android.composer.data.local.AttachmentStateLocalDataSource
import ch.protonmail.android.composer.data.remote.AttachmentRemoteDataSource
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcomposer.domain.repository.AttachmentRepository
import ch.protonmail.android.mailmessage.data.local.AttachmentLocalDataSource
import ch.protonmail.android.mailmessage.domain.model.AttachmentId
import ch.protonmail.android.mailmessage.domain.model.AttachmentState
import ch.protonmail.android.mailmessage.domain.model.AttachmentSyncState
import ch.protonmail.android.mailmessage.domain.model.MessageId
import me.proton.core.domain.entity.UserId
import javax.inject.Inject

class AttachmentRepositoryImpl @Inject constructor(
    private val attachmentStateLocalDataSource: AttachmentStateLocalDataSource,
    private val attachmentRemoteDataSource: AttachmentRemoteDataSource,
    private val attachmentLocalDataSource: AttachmentLocalDataSource
) : AttachmentRepository {

    override suspend fun deleteAttachment(
        userId: UserId,
        messageId: MessageId,
        attachmentId: AttachmentId
    ): Either<DataError, Unit> = either {
        val attachmentState = attachmentStateLocalDataSource.getAttachmentState(userId, messageId, attachmentId).bind()
        when (attachmentState.state) {
            AttachmentSyncState.ExternalUploaded,
            AttachmentSyncState.Uploaded -> attachmentRemoteDataSource.deleteAttachmentFromDraft(userId, attachmentId)

            else -> attachmentRemoteDataSource.cancelAttachmentUpload(attachmentId)
        }
        when (attachmentState.state) {
            AttachmentSyncState.ExternalUploaded,
            AttachmentSyncState.External ->
                attachmentLocalDataSource
                    .deleteAttachment(userId, messageId, attachmentId)
                    .bind()

            else -> attachmentLocalDataSource.deleteAttachmentWithFile(userId, messageId, attachmentId).bind()
        }
    }

    override suspend fun createAttachment(
        userId: UserId,
        messageId: MessageId,
        attachmentId: AttachmentId,
        fileName: String,
        mimeType: String,
        content: ByteArray
    ): Either<DataError, Unit> = either {

        attachmentLocalDataSource.upsertAttachment(
            userId,
            messageId,
            attachmentId,
            fileName,
            mimeType,
            content
        ).bind()

        attachmentStateLocalDataSource.createOrUpdate(
            AttachmentState(
                userId,
                messageId,
                attachmentId,
                AttachmentSyncState.Local
            )
        ).bind()

        return Unit.right()
    }

}
