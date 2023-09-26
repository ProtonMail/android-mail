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
import java.io.InputStream
import android.net.Uri
import ch.protonmail.android.mailcommon.data.file.FileInformation
import ch.protonmail.android.mailcommon.data.file.InternalFileStorage
import ch.protonmail.android.mailcommon.data.file.UriHelper
import me.proton.core.domain.entity.UserId
import javax.inject.Inject

class AttachmentFileStorage @Inject constructor(
    private val uriHelper: UriHelper,
    private val internalFileStorage: InternalFileStorage
) {

    suspend fun saveAttachment(
        userId: UserId,
        messageId: String,
        attachmentId: String,
        content: ByteArray
    ): File? {
        return internalFileStorage.writeFile(
            userId,
            InternalFileStorage.Folder.MessageAttachments(messageId),
            InternalFileStorage.FileIdentifier(attachmentId),
            content
        )
    }

    suspend fun saveAttachment(
        userId: UserId,
        messageId: String,
        attachmentId: String,
        uri: Uri
    ): FileInformation? {
        return uriHelper.readFromUri(uri)?.let {
            saveAttachmentAsStream(userId, messageId, attachmentId, it)?.let {
                uriHelper.getFileInformationFromUri(uri)
            }
        }
    }

    suspend fun saveAttachmentCached(
        userId: UserId,
        messageId: String,
        attachmentId: String,
        content: ByteArray
    ): File? {
        return internalFileStorage.writeCachedFile(
            userId,
            InternalFileStorage.Folder.MessageAttachments(messageId),
            InternalFileStorage.FileIdentifier(attachmentId),
            content
        )
    }

    suspend fun saveAttachmentAsStream(
        userId: UserId,
        messageId: String,
        attachmentId: String,
        inputStream: InputStream
    ): File? {
        return internalFileStorage.writeFileAsStream(
            userId,
            InternalFileStorage.Folder.MessageAttachments(messageId),
            InternalFileStorage.FileIdentifier(attachmentId),
            inputStream
        )
    }

    suspend fun updateParentFolderForAttachments(
        userId: UserId,
        oldMessageId: String,
        updatedMessageId: String
    ) {
        internalFileStorage.renameFolder(
            userId = userId,
            oldFolder = InternalFileStorage.Folder.MessageAttachments(oldMessageId),
            newFolder = InternalFileStorage.Folder.MessageAttachments(updatedMessageId)
        )
    }

    suspend fun updateFileNameForAttachment(
        userId: UserId,
        messageId: String,
        oldAttachmentId: String,
        newAttachmentId: String
    ) {
        internalFileStorage.renameFile(
            userId,
            InternalFileStorage.Folder.MessageAttachments(messageId),
            InternalFileStorage.FileIdentifier(oldAttachmentId),
            InternalFileStorage.FileIdentifier(newAttachmentId)
        )
    }

    @Throws(AttachmentFileReadException::class)
    suspend fun readAttachment(
        userId: UserId,
        messageId: String,
        attachmentId: String
    ): File {
        return internalFileStorage.getFile(
            userId,
            InternalFileStorage.Folder.MessageAttachments(messageId),
            InternalFileStorage.FileIdentifier(attachmentId)
        ) ?: throw AttachmentFileReadException
    }

    @Throws(AttachmentFileReadException::class)
    suspend fun readCachedAttachment(
        userId: UserId,
        messageId: String,
        attachmentId: String
    ): File {
        return internalFileStorage.getCachedFile(
            userId,
            InternalFileStorage.Folder.MessageAttachments(messageId),
            InternalFileStorage.FileIdentifier(attachmentId)
        ) ?: throw AttachmentFileReadException
    }

    suspend fun deleteAttachment(
        userId: UserId,
        messageId: String,
        attachmentId: String
    ): Boolean = internalFileStorage.deleteFile(
        userId,
        InternalFileStorage.Folder.MessageAttachments(messageId),
        InternalFileStorage.FileIdentifier(attachmentId)
    )

    suspend fun deleteAttachmentsOfMessage(userId: UserId, messageId: String): Boolean =
        internalFileStorage.deleteFolder(userId, InternalFileStorage.Folder.MessageAttachments(messageId))

    suspend fun deleteCachedAttachmentsOfMessage(userId: UserId, messageId: String): Boolean =
        internalFileStorage.deleteCachedFolder(userId, InternalFileStorage.Folder.MessageAttachments(messageId))
}

object AttachmentFileReadException : RuntimeException()
