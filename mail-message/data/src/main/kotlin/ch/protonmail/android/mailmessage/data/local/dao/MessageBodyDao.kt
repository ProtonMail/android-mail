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
import androidx.room.Transaction
import ch.protonmail.android.mailmessage.data.local.entity.MessageBodyEntity
import ch.protonmail.android.mailmessage.data.local.relation.MessageWithBodyEntity
import ch.protonmail.android.mailmessage.domain.model.MessageId
import kotlinx.coroutines.flow.Flow
import me.proton.core.data.room.db.BaseDao
import me.proton.core.domain.entity.UserId

@Dao
abstract class MessageBodyDao : BaseDao<MessageBodyEntity>() {

    @Query(
        """
            SELECT * FROM MessageEntity
            JOIN MessageBodyEntity
            ON MessageBodyEntity.userId = MessageEntity.userId
            AND MessageBodyEntity.messageId = MessageEntity.messageId
            JOIN MessageLabelEntity
            ON MessageLabelEntity.userId = MessageEntity.userId
            AND MessageLabelEntity.messageId = MessageEntity.messageId
            WHERE MessageEntity.userId = :userId
            AND MessageEntity.messageId = :messageId
        """
    )
    @Transaction
    abstract fun observeMessageWithBodyEntity(userId: UserId, messageId: MessageId): Flow<MessageWithBodyEntity?>
}
