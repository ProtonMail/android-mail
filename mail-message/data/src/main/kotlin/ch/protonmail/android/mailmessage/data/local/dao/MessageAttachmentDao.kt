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
import ch.protonmail.android.mailmessage.data.local.entity.MessageAttachmentEntity
import ch.protonmail.android.mailmessage.domain.model.AttachmentId
import ch.protonmail.android.mailmessage.domain.model.MessageId
import kotlinx.coroutines.flow.Flow
import me.proton.core.data.room.db.BaseDao
import me.proton.core.domain.entity.UserId

@Dao
abstract class MessageAttachmentDao : BaseDao<MessageAttachmentEntity>() {

    @Query(
        """
            SELECT * FROM MessageAttachmentEntity
            WHERE userId = :userId
            AND messageId = :messageId
        """
    )
    abstract fun observeMessageAttachmentEntities(
        userId: UserId,
        messageId: MessageId
    ): Flow<List<MessageAttachmentEntity>>

    @Query(
        """
            SELECT * FROM MessageAttachmentEntity
            WHERE userId = :userId
            AND messageId = :messageId
            AND attachmentId = :attachmentId            
        """
    )
    abstract suspend fun getMessageAttachment(
        userId: UserId,
        messageId: MessageId,
        attachmentId: AttachmentId
    ): MessageAttachmentEntity?

    @Query(
        """
            UPDATE MessageAttachmentEntity
            SET attachmentId = :apiAssignedId, keyPackets = :keyPackets
            WHERE userId = :userId AND messageId = :messageId AND attachmentId = :localAttachmentId
        """
    )
    abstract suspend fun updateAttachmentIdAndKeyPackets(
        userId: UserId,
        messageId: MessageId,
        localAttachmentId: AttachmentId,
        apiAssignedId: AttachmentId,
        keyPackets: String?
    )

    @Query(
        """
            DELETE FROM MessageAttachmentEntity
            WHERE userId = :userId
            AND messageId = :messageId
            AND attachmentId = :attachmentId
        """
    )
    abstract suspend fun deleteMessageAttachment(
        userId: UserId,
        messageId: MessageId,
        attachmentId: AttachmentId
    )

    @Query(
        """
            DELETE FROM MessageAttachmentEntity
            WHERE userId = :userId
        """
    )
    abstract suspend fun deleteAttachments(userId: UserId)
}
