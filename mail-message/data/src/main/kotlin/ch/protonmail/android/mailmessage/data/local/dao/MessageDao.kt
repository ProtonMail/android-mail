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
import ch.protonmail.android.mailmessage.data.local.entity.MessageEntity
import ch.protonmail.android.mailmessage.data.local.relation.MessageWithLabelIds
import ch.protonmail.android.mailmessage.domain.entity.MessageId
import ch.protonmail.android.mailpagination.domain.model.OrderBy
import ch.protonmail.android.mailpagination.domain.model.OrderDirection
import ch.protonmail.android.mailpagination.domain.model.PageKey
import ch.protonmail.android.mailpagination.domain.model.ReadStatus
import kotlinx.coroutines.flow.Flow
import me.proton.core.data.room.db.BaseDao
import me.proton.core.domain.entity.UserId
import me.proton.core.label.domain.entity.LabelId

@Suppress("LongParameterList", "ComplexMethod")
@Dao
abstract class MessageDao : BaseDao<MessageEntity>() {

    fun observeAll(userId: UserId, pageKey: PageKey): Flow<List<MessageWithLabelIds>> {
        val labelId = pageKey.filter.labelId
        val keyword = pageKey.filter.keyword
        val unread = when (pageKey.filter.read) {
            ReadStatus.All -> listOf(false, true)
            ReadStatus.Read -> listOf(false)
            ReadStatus.Unread -> listOf(true)
        }
        val (minValue, maxValue) = when (pageKey.orderBy) {
            OrderBy.Time -> pageKey.filter.minTime to pageKey.filter.maxTime
        }
        val (minOrder, maxOrder) = when (pageKey.orderBy) {
            OrderBy.Time -> pageKey.filter.minOrder to pageKey.filter.maxOrder
        }
        val size = pageKey.size
        return when (pageKey.orderDirection) {
            OrderDirection.Ascending -> observeAllOrderByTimeAsc(
                userId = userId,
                labelId = labelId,
                keyword = keyword,
                unread = unread,
                minValue = minValue,
                maxValue = maxValue,
                minOrder = minOrder,
                maxOrder = maxOrder,
                size = size
            )
            OrderDirection.Descending -> observeAllOrderByTimeDesc(
                userId = userId,
                labelId = labelId,
                keyword = keyword,
                unread = unread,
                minValue = minValue,
                maxValue = maxValue,
                minOrder = minOrder,
                maxOrder = maxOrder,
                size = size
            )
        }
    }

    @Query(
        """
        SELECT * FROM MessageEntity
        JOIN MessageLabelEntity
        ON MessageLabelEntity.userId = MessageEntity.userId
        AND MessageLabelEntity.messageId = MessageEntity.messageId
        AND MessageLabelEntity.labelId = :labelId
        WHERE MessageEntity.userId = :userId
        AND (
            MessageEntity.subject LIKE '%'||:keyword||'%'
            OR MessageEntity.sender_name LIKE '%'||:keyword||'%'
            OR MessageEntity.sender_address LIKE '%'||:keyword||'%'
            OR MessageEntity.toList LIKE '%'||:keyword||'%'
            OR MessageEntity.ccList LIKE '%'||:keyword||'%'
            OR MessageEntity.bccList LIKE '%'||:keyword||'%'
        )
        AND MessageEntity.unread IN (:unread)
        AND (MessageEntity.time > :minValue OR (MessageEntity.time = :minValue AND MessageEntity.`order` >= :minOrder))
        AND (MessageEntity.time < :maxValue OR (MessageEntity.time = :maxValue AND MessageEntity.`order` <= :maxOrder))
        ORDER BY MessageEntity.time DESC, MessageEntity.`order` DESC
        LIMIT :size
        """
    )
    abstract fun observeAllOrderByTimeDesc(
        userId: UserId,
        labelId: LabelId,
        keyword: String,
        unread: List<Boolean>,
        minValue: Long,
        maxValue: Long,
        minOrder: Long,
        maxOrder: Long,
        size: Int
    ): Flow<List<MessageWithLabelIds>>

    @Query(
        """
        SELECT * FROM MessageEntity
        JOIN MessageLabelEntity
        ON MessageLabelEntity.userId = MessageEntity.userId
        AND MessageLabelEntity.messageId = MessageEntity.messageId
        AND MessageLabelEntity.labelId = :labelId
        WHERE MessageEntity.userId = :userId
        AND (
            MessageEntity.subject LIKE '%'||:keyword||'%'
            OR MessageEntity.sender_name LIKE '%'||:keyword||'%'
            OR MessageEntity.sender_address LIKE '%'||:keyword||'%'
            OR MessageEntity.toList LIKE '%'||:keyword||'%'
            OR MessageEntity.ccList LIKE '%'||:keyword||'%'
            OR MessageEntity.bccList LIKE '%'||:keyword||'%'
        )
        AND MessageEntity.unread IN (:unread)
        AND (MessageEntity.time > :minValue OR (MessageEntity.time = :minValue AND MessageEntity.`order` >= :minOrder))
        AND (MessageEntity.time < :maxValue OR (MessageEntity.time = :maxValue AND MessageEntity.`order` <= :maxOrder))
        ORDER BY MessageEntity.time ASC, MessageEntity.`order` ASC
        LIMIT :size
        """
    )
    abstract fun observeAllOrderByTimeAsc(
        userId: UserId,
        labelId: LabelId,
        keyword: String,
        unread: List<Boolean>,
        minValue: Long,
        maxValue: Long,
        minOrder: Long,
        maxOrder: Long,
        size: Int
    ): Flow<List<MessageWithLabelIds>>

    @Query("DELETE FROM MessageEntity WHERE userId = :userId AND messageId IN (:messageIds)")
    abstract suspend fun delete(userId: UserId, messageIds: List<String>)

    @Query("DELETE FROM MessageEntity WHERE userId = :userId")
    abstract suspend fun deleteAll(userId: UserId)

    @Query("SELECT * FROM MessageEntity WHERE userId = :userId AND messageId = :messageId")
    abstract fun observe(userId: UserId, messageId: MessageId): Flow<MessageWithLabelIds?>
}
