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

import android.content.Context
import ch.protonmail.android.mailcommon.data.file.FileHelper
import ch.protonmail.android.mailmessage.domain.entity.MessageBody
import ch.protonmail.android.mailmessage.domain.entity.MessageId
import dagger.hilt.android.qualifiers.ApplicationContext
import me.proton.core.domain.entity.UserId
import javax.inject.Inject

class MessageBodyFileStorage @Inject constructor(
    @ApplicationContext
    private val applicationContext: Context,
    private val fileHelper: FileHelper
) {
    suspend fun readMessageBody(userId: UserId, messageId: MessageId) = fileHelper.readFromFile(
        folder = userId.asSanitisedFolderName(),
        filename = messageId.asSanitisedFilename()
    )

    suspend fun saveMessageBody(userId: UserId, messageBody: MessageBody) = fileHelper.writeToFile(
        folder = userId.asSanitisedFolderName(),
        filename = messageBody.messageId.asSanitisedFilename(),
        content = messageBody.body
    )

    suspend fun deleteMessageBody(userId: UserId, messageId: MessageId) = fileHelper.deleteFile(
        folder = userId.asSanitisedFolderName(),
        filename = messageId.asSanitisedFilename()
    )

    suspend fun deleteAllMessageBodies(userId: UserId) = fileHelper.deleteFolder(userId.asSanitisedFolderName())

    private fun UserId.asSanitisedFolderName() =
        FileHelper.Folder(applicationContext.filesDir.toString() + "/message_bodies/" + id.asSanitisedPath() + "/")

    private fun MessageId.asSanitisedFilename() = FileHelper.Filename(value = id.asSanitisedPath())

    private fun String.asSanitisedPath() = replace(" ", "_").replace("/", ":")
}
