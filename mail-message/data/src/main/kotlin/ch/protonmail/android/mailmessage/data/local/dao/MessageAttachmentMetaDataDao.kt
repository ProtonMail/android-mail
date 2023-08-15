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

package ch.protonmail.android.mailmessage.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import ch.protonmail.android.mailmessage.data.local.entity.MessageAttachmentMetadataEntity
import ch.protonmail.android.mailmessage.domain.model.AttachmentId
import ch.protonmail.android.mailmessage.domain.model.AttachmentWorkerStatus
import ch.protonmail.android.mailmessage.domain.model.MessageId
import kotlinx.coroutines.flow.Flow
import me.proton.core.data.room.db.BaseDao
import me.proton.core.domain.entity.UserId

@Dao
abstract class MessageAttachmentMetadataDao : BaseDao<MessageAttachmentMetadataEntity>() {

    @Query(
        """
            SELECT * FROM MessageAttachmentMetadataEntity 
            WHERE userId = :userId 
            AND messageId = :messageId 
            AND attachmentId = :attachmentId
        """
    )
    abstract fun observeAttachmentMetadata(
        userId: UserId,
        messageId: MessageId,
        attachmentId: AttachmentId
    ): Flow<MessageAttachmentMetadataEntity?>

    @Query(
        """
            SELECT * FROM MessageAttachmentMetadataEntity 
            WHERE userId = :userId
            AND messageId IN (:messages)
            AND status = :status
        """
    )
    abstract suspend fun getAttachmentsForUserMessagesAndStatus(
        userId: UserId,
        messages: List<MessageId>,
        status: AttachmentWorkerStatus
    ): List<MessageAttachmentMetadataEntity>

    @Query(
        """
            DELETE FROM MessageAttachmentMetadataEntity 
            WHERE userId = :userId 
            AND messageId = :messageId
        """
    )
    abstract suspend fun deleteAttachmentMetadataForMessage(userId: UserId, messageId: MessageId)

}
