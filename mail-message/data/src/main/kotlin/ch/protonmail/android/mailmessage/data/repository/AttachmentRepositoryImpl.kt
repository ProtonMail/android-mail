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

package ch.protonmail.android.mailmessage.data.repository

import java.io.File
import android.net.Uri
import arrow.core.Either
import arrow.core.raise.either
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.coroutines.IODispatcher
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailmessage.data.local.AttachmentLocalDataSource
import ch.protonmail.android.mailmessage.data.local.usecase.DecryptAttachmentByteArray
import ch.protonmail.android.mailmessage.data.remote.AttachmentRemoteDataSource
import ch.protonmail.android.mailmessage.domain.model.AttachmentId
import ch.protonmail.android.mailmessage.domain.model.AttachmentWorkerStatus
import ch.protonmail.android.mailmessage.domain.model.MessageAttachment
import ch.protonmail.android.mailmessage.domain.model.MessageAttachmentMetadata
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailmessage.domain.model.finished
import ch.protonmail.android.mailmessage.domain.repository.AttachmentRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import me.proton.core.domain.entity.UserId
import timber.log.Timber
import javax.inject.Inject

class AttachmentRepositoryImpl @Inject constructor(
    private val decryptAttachmentByteArray: DecryptAttachmentByteArray,
    private val remoteDataSource: AttachmentRemoteDataSource,
    private val localDataSource: AttachmentLocalDataSource,
    @IODispatcher private val ioDispatcher: CoroutineDispatcher
) : AttachmentRepository {

    override suspend fun getAttachment(
        userId: UserId,
        messageId: MessageId,
        attachmentId: AttachmentId
    ): Either<DataError, MessageAttachmentMetadata> {
        val attachment = localDataSource.getAttachment(userId, messageId, attachmentId)
        if (attachment.isRight()) return attachment

        // Resets the status to running so that in case of a failure
        // observing is not emitting directly failure when retrying
        localDataSource.updateAttachmentDownloadStatus(userId, messageId, attachmentId, AttachmentWorkerStatus.Running)
        remoteDataSource.enqueueGetAttachmentWorker(userId, messageId, attachmentId)
        return localDataSource.observeAttachmentMetadata(userId, messageId, attachmentId)
            .firstOrNull { it?.status?.finished() == true }
            ?.let {
                Timber.d("Attachment download status: ${it.status}")
                when (it.status) {
                    AttachmentWorkerStatus.Success -> it.right()
                    else -> {
                        if (it.status is AttachmentWorkerStatus.Failed.OutOfMemory) {
                            DataError.Local.OutOfMemory.left()
                        } else {
                            DataError.Remote.Unknown.left()
                        }
                    }

                }
            }
            ?: DataError.Remote.Unknown.left()
    }

    override suspend fun getAttachmentFromRemote(
        userId: UserId,
        messageId: MessageId,
        attachmentId: AttachmentId
    ): Either<DataError, ByteArray> = remoteDataSource.getAttachment(userId, messageId, attachmentId)

    override suspend fun saveMimeAttachmentToPublicStorage(
        userId: UserId,
        messageId: MessageId,
        attachmentId: AttachmentId
    ): Either<DataError, Uri> = localDataSource.saveMimeAttachmentToPublicStorage(userId, messageId, attachmentId)

    override suspend fun getEmbeddedImage(
        userId: UserId,
        messageId: MessageId,
        attachmentId: AttachmentId
    ): Either<DataError, ByteArray> {
        localDataSource.getEmbeddedImage(userId, messageId, attachmentId).getOrNull()?.let {
            return withContext(ioDispatcher) {
                decryptAttachmentByteArray(userId, messageId, attachmentId, it.readBytes())
            }.mapLeft { DataError.Local.DecryptionError }
        }

        return either {
            remoteDataSource.getAttachment(userId, messageId, attachmentId).bind().let {
                localDataSource.storeEmbeddedImage(userId, messageId, attachmentId, it)
                decryptAttachmentByteArray(userId, messageId, attachmentId, it).mapLeft {
                    DataError.Local.DecryptionError
                }.bind()
            }
        }
    }

    override suspend fun getDownloadingAttachmentsForMessages(userId: UserId, messageIds: List<MessageId>) =
        localDataSource.getDownloadingAttachmentsForMessages(userId, messageIds)

    override suspend fun observeAttachmentMetadata(
        userId: UserId,
        messageId: MessageId,
        attachmentId: AttachmentId
    ): Flow<MessageAttachmentMetadata?> = localDataSource.observeAttachmentMetadata(userId, messageId, attachmentId)

    override suspend fun saveAttachment(
        userId: UserId,
        messageId: MessageId,
        attachmentId: AttachmentId,
        uri: Uri
    ): Either<DataError, Unit> = localDataSource.upsertAttachment(userId, messageId, attachmentId, uri)

    override suspend fun saveAttachmentToFile(
        userId: UserId,
        messageId: MessageId,
        attachmentId: AttachmentId,
        content: ByteArray
    ): Either<DataError, File> = localDataSource.saveAttachmentToFile(userId, messageId, attachmentId, content)

    override suspend fun saveMimeAttachment(
        userId: UserId,
        messageId: MessageId,
        attachmentId: AttachmentId,
        content: ByteArray,
        attachment: MessageAttachment
    ): Either<DataError, Unit> =
        localDataSource.upsertMimeAttachment(userId, messageId, attachmentId, content, attachment)

    override suspend fun getFileSizeFromUri(uri: Uri) = localDataSource.getFileSizeFromUri(uri)

    override suspend fun readFileFromStorage(
        userId: UserId,
        messageId: MessageId,
        attachmentId: AttachmentId
    ): Either<DataError, File> = localDataSource.readFileFromStorage(
        userId, messageId, attachmentId
    )

    override suspend fun getAttachmentInfo(
        userId: UserId,
        messageId: MessageId,
        attachmentId: AttachmentId
    ): Either<DataError, MessageAttachment> = localDataSource.getAttachmentInfo(userId, messageId, attachmentId)

    override suspend fun updateMessageAttachment(
        userId: UserId,
        messageId: MessageId,
        localAttachmentId: AttachmentId,
        attachment: MessageAttachment
    ): Either<DataError, Unit> = localDataSource.updateMessageAttachment(
        userId, messageId, localAttachmentId, attachment
    )

    override suspend fun copyMimeAttachmentsToMessage(
        userId: UserId,
        sourceMessageId: MessageId,
        targetMessageId: MessageId,
        attachmentIds: List<AttachmentId>
    ): Either<DataError.Local, Unit> =
        localDataSource.copyMimeAttachmentsToMessage(userId, sourceMessageId, targetMessageId, attachmentIds)
}
