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

import ch.protonmail.android.mailcommon.data.file.InternalFileStorage
import ch.protonmail.android.mailmessage.domain.model.MessageBody
import ch.protonmail.android.mailmessage.domain.model.MessageId
import me.proton.core.domain.entity.UserId
import javax.inject.Inject

class MessageBodyFileStorage @Inject constructor(
    private val internalFileStorage: InternalFileStorage
) {

    @Throws(MessageBodyFileReadException::class)
    suspend fun readMessageBody(userId: UserId, messageId: MessageId): String = internalFileStorage.readFromFile(
        userId = userId,
        folder = InternalFileStorage.Folder.MessageBodies,
        fileIdentifier = InternalFileStorage.FileIdentifier(messageId.id)
    ) ?: throw MessageBodyFileReadException

    @Throws(MessageBodyFileWriteException::class)
    suspend fun saveMessageBody(userId: UserId, messageBody: MessageBody) {
        val messageBodySaved = internalFileStorage.writeToFile(
            userId = userId,
            folder = InternalFileStorage.Folder.MessageBodies,
            fileIdentifier = InternalFileStorage.FileIdentifier(messageBody.messageId.id),
            content = messageBody.body
        )
        if (!messageBodySaved) throw MessageBodyFileWriteException
    }

    suspend fun deleteMessageBody(userId: UserId, messageId: MessageId): Boolean = internalFileStorage.deleteFile(
        userId = userId,
        folder = InternalFileStorage.Folder.MessageBodies,
        fileIdentifier = InternalFileStorage.FileIdentifier(messageId.id)
    )

    suspend fun deleteAllMessageBodies(userId: UserId): Boolean =
        internalFileStorage.deleteFolder(userId, InternalFileStorage.Folder.MessageBodies)

    suspend fun updateFileNameForMessageBody(
        userId: UserId,
        oldMessageId: MessageId,
        newMessageId: MessageId
    ) {
        internalFileStorage.renameFile(
            userId,
            InternalFileStorage.Folder.MessageBodies,
            InternalFileStorage.FileIdentifier(oldMessageId.id),
            InternalFileStorage.FileIdentifier(newMessageId.id)
        )
    }
}

object MessageBodyFileReadException : RuntimeException()
object MessageBodyFileWriteException : RuntimeException()
