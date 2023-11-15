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

package ch.protonmail.android.mailmessage.domain.repository

import java.io.File
import android.net.Uri
import arrow.core.Either
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailmessage.domain.model.AttachmentId
import ch.protonmail.android.mailmessage.domain.model.MessageAttachment
import ch.protonmail.android.mailmessage.domain.model.MessageAttachmentMetadata
import ch.protonmail.android.mailmessage.domain.model.MessageId
import kotlinx.coroutines.flow.Flow
import me.proton.core.domain.entity.UserId

interface AttachmentRepository {

    /**
     * Get the attachment for the given [userId], [messageId] and [attachmentId].
     */
    suspend fun getAttachment(
        userId: UserId,
        messageId: MessageId,
        attachmentId: AttachmentId
    ): Either<DataError, MessageAttachmentMetadata>

    suspend fun getAttachmentFromRemote(
        userId: UserId,
        messageId: MessageId,
        attachmentId: AttachmentId
    ): Either<DataError, ByteArray>

    suspend fun saveMimeAttachmentToPublicStorage(
        userId: UserId,
        messageId: MessageId,
        attachmentId: AttachmentId
    ): Either<DataError, Uri>

    /**
     * Get the embedded image for the given [userId], [messageId] and [attachmentId].
     * This method will fetch and store the embedded image if it is not yet stored locally.
     * Otherwise it will return the local file.
     * @return If successful the encrypted bytearray of the embedded image.
     */
    suspend fun getEmbeddedImage(
        userId: UserId,
        messageId: MessageId,
        attachmentId: AttachmentId
    ): Either<DataError, ByteArray>

    suspend fun getDownloadingAttachmentsForMessages(
        userId: UserId,
        messageIds: List<MessageId>
    ): List<MessageAttachmentMetadata>

    suspend fun observeAttachmentMetadata(
        userId: UserId,
        messageId: MessageId,
        attachmentId: AttachmentId
    ): Flow<MessageAttachmentMetadata?>

    suspend fun saveAttachment(
        userId: UserId,
        messageId: MessageId,
        attachmentId: AttachmentId,
        uri: Uri
    ): Either<DataError, Unit>

    suspend fun saveAttachmentToFile(
        userId: UserId,
        messageId: MessageId,
        attachmentId: AttachmentId,
        content: ByteArray
    ): Either<DataError, File>

    suspend fun saveMimeAttachment(
        userId: UserId,
        messageId: MessageId,
        attachmentId: AttachmentId,
        content: ByteArray,
        attachment: MessageAttachment
    ): Either<DataError, Unit>

    suspend fun getFileSizeFromUri(uri: Uri): Either<DataError, Long>

    suspend fun readFileFromStorage(
        userId: UserId,
        messageId: MessageId,
        attachmentId: AttachmentId
    ): Either<DataError, File>

    suspend fun getAttachmentInfo(
        userId: UserId,
        messageId: MessageId,
        attachmentId: AttachmentId
    ): Either<DataError, MessageAttachment>

    suspend fun updateMessageAttachment(
        userId: UserId,
        messageId: MessageId,
        localAttachmentId: AttachmentId,
        attachment: MessageAttachment
    ): Either<DataError, Unit>

    suspend fun copyMimeAttachmentsToMessage(
        userId: UserId,
        sourceMessageId: MessageId,
        targetMessageId: MessageId,
        attachmentIds: List<AttachmentId>
    ): Either<DataError.Local, Unit>
}
