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

package ch.protonmail.android.mailmessage.data.local

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.coroutines.IODispatcher
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailmessage.data.local.entity.MessageAttachmentMetadataEntity
import ch.protonmail.android.mailmessage.data.mapper.toMessageAttachmentMetadata
import ch.protonmail.android.mailmessage.domain.entity.AttachmentId
import ch.protonmail.android.mailmessage.domain.entity.AttachmentWorkerStatus
import ch.protonmail.android.mailmessage.domain.entity.MessageAttachmentMetadata
import ch.protonmail.android.mailmessage.domain.entity.MessageId
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.withContext
import me.proton.core.domain.entity.UserId
import me.proton.core.util.kotlin.sha256
import timber.log.Timber
import javax.inject.Inject

class AttachmentLocalDataSourceImpl @Inject constructor(
    db: MessageDatabase,
    private val attachmentFileStorage: AttachmentFileStorage,
    @IODispatcher private val ioDispatcher: CoroutineDispatcher
) : AttachmentLocalDataSource {

    private val attachmentDao by lazy { db.messageAttachmentMetadataDao() }

    override suspend fun observeAttachmentMetadata(
        userId: UserId,
        messageId: MessageId,
        attachmentId: AttachmentId
    ) = attachmentDao.observeAttachmentMetadata(userId, messageId, attachmentId)
        .mapLatest { it?.toMessageAttachmentMetadata() }


    override suspend fun getAttachment(
        userId: UserId,
        messageId: MessageId,
        attachmentId: AttachmentId
    ): Either<DataError.Local, MessageAttachmentMetadata> {
        return withContext(ioDispatcher) {
            Timber.d("Get local attachment for AttachmentId: $attachmentId")
            try {
                val storedAttachmentFile = attachmentFileStorage.readAttachment(userId, messageId.id, attachmentId.id)
                observeAttachmentMetadata(userId, messageId, attachmentId).firstOrNull()
                    ?.takeIf { it.hash == storedAttachmentFile.sha256() }
                    ?.right()
                    ?: DataError.Local.NoDataCached.left()
            } catch (e: AttachmentFileReadException) {
                Timber.w(e, "Failed to read attachment from file storage")
                DataError.Local.NoDataCached.left()
            }
        }
    }

    override suspend fun getAttachmentMetadataByHash(
        attachmentHash: String
    ): Either<DataError, MessageAttachmentMetadata> {
        return attachmentDao.getMessageAttachmentMetadataByHash(attachmentHash)
            ?.toMessageAttachmentMetadata()
            ?.right()
            ?: DataError.Local.NoDataCached.left()
    }

    override suspend fun getDownloadingAttachmentsForUser(userId: UserId, messageIds: List<MessageId>) =
        attachmentDao.getAllAttachmentsForUserAndStatus(userId, messageIds, AttachmentWorkerStatus.Running)
            .map { it.toMessageAttachmentMetadata() }

    override suspend fun upsertAttachment(
        userId: UserId,
        messageId: MessageId,
        attachmentId: AttachmentId,
        attachment: ByteArray,
        status: AttachmentWorkerStatus
    ) {
        withContext(ioDispatcher) {
            attachmentFileStorage.saveAttachment(
                userId = userId,
                messageId = messageId.id,
                attachmentId = attachmentId.id,
                content = attachment
            )?.also { savedFile ->
                attachmentDao.insertOrUpdate(
                    MessageAttachmentMetadataEntity(
                        userId = userId,
                        messageId = messageId,
                        attachmentId = attachmentId,
                        hash = savedFile.sha256(),
                        path = savedFile.path,
                        status = status
                    )
                )
            }
        }
    }

    override suspend fun updateAttachmentDownloadStatus(
        userId: UserId,
        messageId: MessageId,
        attachmentId: AttachmentId,
        status: AttachmentWorkerStatus
    ) {
        attachmentDao.insertOrUpdate(
            MessageAttachmentMetadataEntity(
                userId = userId,
                messageId = messageId,
                attachmentId = attachmentId,
                status = status,
                hash = null,
                path = null
            )
        )
    }

    override suspend fun deleteAttachments(userId: UserId, messageId: MessageId): Boolean {
        attachmentDao.deleteAttachmentMetadataForMessage(userId, messageId)
        return attachmentFileStorage.deleteAttachmentsOfMessage(userId, messageId.id)
    }
}
