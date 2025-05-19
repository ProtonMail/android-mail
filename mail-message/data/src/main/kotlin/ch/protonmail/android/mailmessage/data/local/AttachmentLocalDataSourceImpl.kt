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

import java.io.File
import java.io.FileNotFoundException
import android.content.Context
import android.net.Uri
import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.data.file.UriHelper
import ch.protonmail.android.mailcommon.domain.coroutines.IODispatcher
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailmessage.data.local.entity.MessageAttachmentEntity
import ch.protonmail.android.mailmessage.data.local.entity.MessageAttachmentMetadataEntity
import ch.protonmail.android.mailmessage.data.local.usecase.DecryptAttachmentByteArray
import ch.protonmail.android.mailmessage.data.local.usecase.PrepareAttachmentForSharing
import ch.protonmail.android.mailmessage.data.mapper.MessageAttachmentEntityMapper
import ch.protonmail.android.mailmessage.data.mapper.toMessageAttachmentMetadata
import ch.protonmail.android.mailmessage.domain.model.AttachmentId
import ch.protonmail.android.mailmessage.domain.model.AttachmentWorkerStatus
import ch.protonmail.android.mailmessage.domain.model.MessageAttachment
import ch.protonmail.android.mailmessage.domain.model.MessageAttachmentMetadata
import ch.protonmail.android.mailmessage.domain.model.MessageId
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.withContext
import me.proton.core.domain.entity.UserId
import timber.log.Timber
import javax.inject.Inject

class AttachmentLocalDataSourceImpl @Inject constructor(
    db: MessageDatabase,
    private val attachmentFileStorage: AttachmentFileStorage,
    @ApplicationContext private val context: Context,
    private val decryptAttachmentByteArray: DecryptAttachmentByteArray,
    private val prepareAttachmentForSharing: PrepareAttachmentForSharing,
    private val messageAttachmentEntityMapper: MessageAttachmentEntityMapper,
    @IODispatcher private val ioDispatcher: CoroutineDispatcher,
    private val uriHelper: UriHelper
) : AttachmentLocalDataSource {

    private val attachmentMetadataDao by lazy { db.messageAttachmentMetadataDao() }
    private val attachmentDao by lazy { db.messageAttachmentDao() }

    override suspend fun observeAttachmentMetadata(
        userId: UserId,
        messageId: MessageId,
        attachmentId: AttachmentId
    ) = attachmentMetadataDao.observeAttachmentMetadata(userId, messageId, attachmentId)
        .mapLatest { it?.toMessageAttachmentMetadata() }


    override suspend fun getAttachment(
        userId: UserId,
        messageId: MessageId,
        attachmentId: AttachmentId
    ): Either<DataError.Local, MessageAttachmentMetadata> {
        return withContext(ioDispatcher) {
            Timber.d("Get local attachment for AttachmentId: $attachmentId")
            try {
                observeAttachmentMetadata(userId, messageId, attachmentId).firstOrNull()
                    ?.takeIf { metadata -> metadata.uri?.let { isAttachmentFileAvailable(it) } ?: false }
                    ?.right()
                    ?: DataError.Local.NoDataCached.left()
            } catch (e: AttachmentFileReadException) {
                Timber.w(e, "Failed to read attachment from file storage")
                DataError.Local.NoDataCached.left()
            }
        }
    }

    override suspend fun getAttachmentInfo(
        userId: UserId,
        messageId: MessageId,
        attachmentId: AttachmentId
    ) = attachmentDao.getMessageAttachment(userId, messageId, attachmentId)
        ?.let { messageAttachmentEntityMapper.toMessageAttachment(it) }
        ?.right()
        ?: DataError.Local.NoDataCached.left()


    override suspend fun getEmbeddedImage(
        userId: UserId,
        messageId: MessageId,
        attachmentId: AttachmentId
    ): Either<DataError.Local, File> {
        return runCatching {
            attachmentFileStorage.readCachedAttachment(userId, messageId, attachmentId)
        }.fold(
            onSuccess = { it.right() },
            onFailure = { DataError.Local.NoDataCached.left() }
        )
    }

    override suspend fun getDownloadingAttachmentsForMessages(userId: UserId, messageIds: List<MessageId>) =
        attachmentMetadataDao.getAttachmentsForUserMessagesAndStatus(userId, messageIds, AttachmentWorkerStatus.Running)
            .map { it.toMessageAttachmentMetadata() }

    override suspend fun upsertAttachment(
        userId: UserId,
        messageId: MessageId,
        attachmentId: AttachmentId,
        encryptedAttachment: ByteArray,
        status: AttachmentWorkerStatus
    ) {

        decryptAttachmentByteArray(userId, messageId, attachmentId, encryptedAttachment).fold(
            ifLeft = {
                Timber.e("Failed to decrypt attachment: $it")
                attachmentMetadataDao.insertOrUpdate(
                    MessageAttachmentMetadataEntity(
                        userId = userId,
                        messageId = messageId,
                        attachmentId = attachmentId,
                        uri = null,
                        status = AttachmentWorkerStatus.Failed.Generic
                    )
                )
            },
            ifRight = { decryptedByteArray ->
                val uri = prepareAttachmentForSharing(userId, messageId, attachmentId, decryptedByteArray).getOrElse {
                    Timber.e("Failed to prepare attachment for sharing: $it")
                    null
                }
                attachmentMetadataDao.insertOrUpdate(
                    MessageAttachmentMetadataEntity(
                        userId = userId,
                        messageId = messageId,
                        attachmentId = attachmentId,
                        uri = uri,
                        status = if (uri != null) {
                            AttachmentWorkerStatus.Success
                        } else {
                            AttachmentWorkerStatus.Failed.Generic
                        }
                    )
                )
            }
        )
    }

    override suspend fun upsertAttachment(
        userId: UserId,
        messageId: MessageId,
        attachmentId: AttachmentId,
        uri: Uri
    ): Either<DataError.Local, Unit> {
        val fileInformation = attachmentFileStorage.saveAttachment(userId, messageId, attachmentId, uri)
            ?: return DataError.Local.FailedToStoreFile.left()
        val messageAttachmentEntity = MessageAttachmentEntity(
            userId = userId,
            messageId = messageId,
            attachmentId = attachmentId,
            name = fileInformation.name,
            size = fileInformation.size,
            mimeType = fileInformation.mimeType,
            disposition = "attachment",
            keyPackets = null,
            signature = null,
            encSignature = null,
            headers = emptyMap()
        )
        val result = runCatching { attachmentDao.insertOrUpdate(messageAttachmentEntity) }
        return when (result.isSuccess) {
            true -> Unit.right()
            false -> DataError.Local.FailedToStoreFile.left()
        }
    }

    override suspend fun upsertAttachment(
        userId: UserId,
        messageId: MessageId,
        attachmentId: AttachmentId,
        fileName: String,
        mimeType: String,
        content: ByteArray
    ): Either<DataError.Local, Unit> {
        attachmentFileStorage.saveAttachment(userId, messageId, attachmentId, content)
            ?: return DataError.Local.FailedToStoreFile.left()
        val messageAttachmentEntity = MessageAttachmentEntity(
            userId = userId,
            messageId = messageId,
            attachmentId = attachmentId,
            name = fileName,
            size = content.size.toLong(),
            mimeType = mimeType,
            disposition = "attachment",
            keyPackets = null,
            signature = null,
            encSignature = null,
            headers = emptyMap()
        )
        val result = runCatching { attachmentDao.insertOrUpdate(messageAttachmentEntity) }
        return when (result.isSuccess) {
            true -> Unit.right()
            false -> DataError.Local.FailedToStoreFile.left()
        }
    }

    override suspend fun saveAttachmentToFile(
        userId: UserId,
        messageId: MessageId,
        attachmentId: AttachmentId,
        content: ByteArray
    ): Either<DataError.Local, File> {
        val file = attachmentFileStorage.saveAttachment(userId, messageId, attachmentId, content)
            ?: return DataError.Local.FailedToStoreFile.left()
        return file.right()
    }

    override suspend fun upsertMimeAttachment(
        userId: UserId,
        messageId: MessageId,
        attachmentId: AttachmentId,
        content: ByteArray,
        attachment: MessageAttachment
    ): Either<DataError.Local, Unit> {
        attachmentFileStorage.saveAttachmentCached(userId, messageId, attachmentId, content)
            ?: return DataError.Local.FailedToStoreFile.left()
        attachmentDao.insertOrUpdate(
            messageAttachmentEntityMapper.toMessageAttachmentEntity(userId, messageId, attachment)
        )
        return Unit.right()
    }

    override suspend fun updateAttachmentDownloadStatus(
        userId: UserId,
        messageId: MessageId,
        attachmentId: AttachmentId,
        status: AttachmentWorkerStatus
    ) {
        attachmentMetadataDao.insertOrUpdate(
            MessageAttachmentMetadataEntity(
                userId = userId,
                messageId = messageId,
                attachmentId = attachmentId,
                status = status,
                uri = null
            )
        )
    }

    override suspend fun storeEmbeddedImage(
        userId: UserId,
        messageId: MessageId,
        attachmentId: AttachmentId,
        encryptedAttachment: ByteArray
    ) {
        runCatching {
            attachmentFileStorage.saveAttachmentCached(
                userId = userId,
                messageId = messageId,
                attachmentId = attachmentId,
                content = encryptedAttachment
            )
        }
            .onFailure { Timber.d("Failed to store attachment: $attachmentId") }
    }

    override suspend fun deleteAttachments(userId: UserId, messageId: MessageId): Boolean {
        attachmentMetadataDao.deleteAttachmentMetadataForMessage(userId, messageId)
        return true
    }

    override suspend fun deleteAttachments(userId: UserId): Boolean {
        return runCatching {
            attachmentDao.deleteAttachments(userId)
        }.isSuccess && attachmentFileStorage.deleteAttachmentsForUser(userId)
    }

    override suspend fun readFileFromStorage(
        userId: UserId,
        messageId: MessageId,
        attachmentId: AttachmentId
    ): Either<DataError.Local, File> =
        runCatching { attachmentFileStorage.readAttachment(userId, messageId, attachmentId) }.fold(
            onSuccess = { it.right() },
            onFailure = {
                Timber.e(it, "Failed to read attachment from file storage")
                DataError.Local.NoDataCached.left()
            }
        )

    override suspend fun updateMessageAttachment(
        userId: UserId,
        messageId: MessageId,
        localAttachmentId: AttachmentId,
        attachment: MessageAttachment
    ): Either<DataError.Local, Unit> {
        return runCatching {
            attachmentDao.updateAttachmentIdAndKeyPackets(
                userId,
                messageId,
                localAttachmentId,
                attachment.attachmentId,
                attachment.keyPackets
            )
            attachmentFileStorage.updateFileNameForAttachment(
                userId,
                messageId,
                localAttachmentId,
                attachment.attachmentId
            )
        }.fold(
            onSuccess = { Unit.right() },
            onFailure = {
                Timber.e(it, "Failed to update message attachment")
                DataError.Local.Unknown.left()
            }
        )
    }

    override suspend fun deleteAttachmentWithFile(
        userId: UserId,
        messageId: MessageId,
        attachmentId: AttachmentId
    ): Either<DataError.Local, Unit> {
        return runCatching {
            attachmentDao.deleteMessageAttachment(userId, messageId, attachmentId)
            attachmentFileStorage.deleteAttachment(userId, messageId.id, attachmentId.id)
        }.fold(
            onSuccess = {
                when {
                    it -> Unit.right()
                    else -> {
                        Timber.e("Failed to delete attachment")
                        DataError.Local.FailedToDeleteFile.left()
                    }
                }
            },
            onFailure = {
                Timber.e(it, "Failed to delete message attachment")
                DataError.Local.Unknown.left()
            }
        )
    }

    override suspend fun deleteAttachment(
        userId: UserId,
        messageId: MessageId,
        attachmentId: AttachmentId
    ): Either<DataError.Local, Unit> = Either.catch {
        attachmentDao.deleteMessageAttachment(userId, messageId, attachmentId)
    }.mapLeft {
        Timber.e(it, "Failed to delete message attachment")
        DataError.Local.Unknown
    }

    override suspend fun copyMimeAttachmentsToMessage(
        userId: UserId,
        sourceMessageId: MessageId,
        targetMessageId: MessageId,
        attachmentIds: List<AttachmentId>
    ): Either<DataError.Local, Unit> {
        attachmentIds.forEach {
            attachmentFileStorage.copyCachedAttachmentToMessage(
                userId,
                sourceMessageId.id,
                targetMessageId.id,
                it.id
            ) ?: return DataError.Local.FailedToStoreFile.left()
        }
        return Unit.right()
    }

    override suspend fun saveMimeAttachmentToPublicStorage(
        userId: UserId,
        messageId: MessageId,
        attachmentId: AttachmentId
    ): Either<DataError.Local, Uri> {
        val attachment = runCatching {
            attachmentFileStorage.readCachedAttachment(userId, messageId, attachmentId)
        }.getOrElse { return DataError.Local.NoDataCached.left() }
        return prepareAttachmentForSharing(userId, messageId, attachmentId, attachment.readBytes()).getOrElse {
            return DataError.Local.FailedToStoreFile.left()
        }.right()
    }

    override suspend fun getFileSizeFromUri(uri: Uri) =
        uriHelper.getFileInformationFromUri(uri)?.size?.right() ?: DataError.Local.NoDataCached.left()

    private fun isAttachmentFileAvailable(uri: Uri): Boolean {
        val doesFileExist = try {
            context.contentResolver.openInputStream(uri)?.use {
                it.close()
                true
            } ?: false
        } catch (fileException: FileNotFoundException) {
            Timber.v(fileException, "Uri not found")
            false
        }
        Timber.d("doesFileExist: $doesFileExist")
        return doesFileExist
    }
}
