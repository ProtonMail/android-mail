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
import ch.protonmail.android.mailcommon.domain.coroutines.IODispatcher
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailmessage.data.local.entity.MessageAttachmentMetadataEntity
import ch.protonmail.android.mailmessage.data.local.usecase.DecryptAttachmentByteArray
import ch.protonmail.android.mailmessage.data.local.usecase.PrepareAttachmentForSharing
import ch.protonmail.android.mailmessage.data.mapper.toMessageAttachmentMetadata
import ch.protonmail.android.mailmessage.domain.entity.AttachmentId
import ch.protonmail.android.mailmessage.domain.entity.AttachmentWorkerStatus
import ch.protonmail.android.mailmessage.domain.entity.MessageAttachmentMetadata
import ch.protonmail.android.mailmessage.domain.entity.MessageId
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

    override suspend fun getEmbeddedImage(
        userId: UserId,
        messageId: MessageId,
        attachmentId: AttachmentId
    ): Either<DataError.Local, File> {
        return runCatching {
            attachmentFileStorage.readAttachment(userId, messageId.id, attachmentId.id)
        }.fold(
            onSuccess = { it.right() },
            onFailure = { DataError.Local.NoDataCached.left() }
        )
    }

    override suspend fun getDownloadingAttachmentsForMessages(userId: UserId, messageIds: List<MessageId>) =
        attachmentDao.getAttachmentsForUserMessagesAndStatus(userId, messageIds, AttachmentWorkerStatus.Running)
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
                attachmentDao.insertOrUpdate(
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
                attachmentDao.insertOrUpdate(
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
                uri = null
            )
        )
    }

    override suspend fun deleteAttachments(userId: UserId, messageId: MessageId): Boolean {
        attachmentDao.deleteAttachmentMetadataForMessage(userId, messageId)
        return true
    }

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
