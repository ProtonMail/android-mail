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
import ch.protonmail.android.mailcommon.domain.model.ConversationId
import ch.protonmail.android.mailmessage.data.local.entity.MessageEntity
import ch.protonmail.android.mailmessage.data.local.relation.MessageWithLabelIds
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailpagination.domain.model.OrderBy
import ch.protonmail.android.mailpagination.domain.model.OrderDirection
import ch.protonmail.android.mailpagination.domain.model.PageKey
import ch.protonmail.android.mailpagination.domain.model.ReadStatus
import kotlinx.coroutines.flow.Flow
import me.proton.core.data.room.db.BaseDao
import me.proton.core.domain.entity.UserId
import me.proton.core.label.domain.entity.LabelId

@Dao
@Suppress("LongParameterList")
abstract class MessageDao : BaseDao<MessageEntity>() {

    @Query("DELETE FROM MessageEntity WHERE userId = :userId AND messageId IN (:messageIds)")
    abstract suspend fun delete(userId: UserId, messageIds: List<String>)

    @Query("DELETE FROM MessageEntity WHERE userId = :userId")
    abstract suspend fun deleteAll(userId: UserId)

    @Query("SELECT * FROM MessageEntity WHERE userId = :userId AND messageId = :messageId")
    @Transaction
    abstract fun observe(userId: UserId, messageId: MessageId): Flow<MessageWithLabelIds?>

    @Query("SELECT * FROM MessageEntity WHERE userId = :userId AND messageId IN (:messages) ")
    @Transaction
    abstract fun observeMessages(userId: UserId, messages: List<MessageId>): Flow<List<MessageWithLabelIds?>>

    @Query(
        """
       SELECT * FROM MessageEntity
        JOIN MessageLabelEntity
        ON MessageLabelEntity.userId = MessageEntity.userId
        AND MessageLabelEntity.messageId = MessageEntity.messageId
        AND MessageLabelEntity.labelId = :labelId
        WHERE MessageEntity.userId = :userId
        AND MessageEntity.messageId IN (
          SELECT messageId FROM MessageLabelEntity WHERE labelId = :labelId)
        """
    )
    @Transaction
    abstract fun observeMessages(userId: UserId, labelId: LabelId): Flow<List<MessageWithLabelIds>>

    @Query(
        """
            /*
            * Calculates the size of the message bodies for messages that have been opened at least once by the user
            * by subtracting the attachment sizes from the "whole" message `size` present in the `MessageEntity` table.
            * Attachments belonging to drafts are excluded since draft entities are not stored in `MessageEntity`
            * but rather in `DraftStateEntity`, not used at all in this query. 
            */
            SELECT IFNULL(SUM(size), 0) - (
              SELECT IFNULL(SUM(size), 0) 
              FROM MessageAttachmentEntity WHERE messageId NOT IN (
                SELECT messageId 
                FROM MessageLabelEntity
                WHERE labelId IN (1, 8) -- 1 and 8 are labelId for "All drafts" and "Drafts"
              ) 
            )
            FROM MessageEntity me 
            JOIN MessageBodyEntity mbe
            ON me.userId = mbe.userId AND me.messageId == mbe.messageId
        """
    )
    abstract fun observeCachedMessagesTotalSize(): Flow<Long>

    fun observeAll(userId: UserId, pageKey: PageKey): Flow<List<MessageWithLabelIds>> {
        val labelId = pageKey.filter.labelId
        val unread = when (pageKey.filter.read) {
            ReadStatus.All -> ReadAndUnread
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
                conversationId = null,
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
        AND (
            MessageLabelEntity.labelId = :labelId
            OR :labelId IS NULL
        )
        AND (
            MessageEntity.conversationId = :conversationId
            OR :conversationId IS NULL
        )
        WHERE MessageEntity.userId = :userId
        AND MessageEntity.unread IN (:unread)
        AND (MessageEntity.time > :minValue OR (MessageEntity.time = :minValue AND MessageEntity.`order` >= :minOrder))
        AND (MessageEntity.time < :maxValue OR (MessageEntity.time = :maxValue AND MessageEntity.`order` <= :maxOrder))
        GROUP BY MessageEntity.messageId
        ORDER BY MessageEntity.time ASC, MessageEntity.`order` ASC
        LIMIT :size
        """
    )
    @Transaction
    abstract fun observeAllOrderByTimeAsc(
        userId: UserId,
        labelId: LabelId? = null,
        conversationId: ConversationId? = null,
        unread: List<Boolean> = ReadAndUnread,
        minValue: Long = Long.MIN_VALUE,
        maxValue: Long = Long.MAX_VALUE,
        minOrder: Long = Long.MIN_VALUE,
        maxOrder: Long = Long.MAX_VALUE,
        size: Int = Int.MAX_VALUE
    ): Flow<List<MessageWithLabelIds>>

    @Query(
        """
        SELECT * FROM MessageEntity
        JOIN MessageLabelEntity
        ON MessageLabelEntity.userId = MessageEntity.userId
        AND MessageLabelEntity.messageId = MessageEntity.messageId
        AND (
            MessageLabelEntity.labelId = :labelId
            OR :labelId IS NULL
        )
        AND (
            MessageEntity.conversationId = :conversationId
            OR :conversationId IS NULL
        )
        WHERE MessageEntity.userId = :userId
        AND MessageEntity.unread IN (:unread)
        AND (MessageEntity.time > :minValue OR (MessageEntity.time = :minValue AND MessageEntity.`order` >= :minOrder))
        AND (MessageEntity.time < :maxValue OR (MessageEntity.time = :maxValue AND MessageEntity.`order` <= :maxOrder))
        GROUP BY MessageEntity.messageId
        ORDER BY MessageEntity.time DESC, MessageEntity.`order` DESC
        LIMIT :size
        """
    )
    @Transaction
    abstract fun observeAllOrderByTimeDesc(
        userId: UserId,
        labelId: LabelId? = null,
        conversationId: ConversationId? = null,
        unread: List<Boolean> = ReadAndUnread,
        minValue: Long = Long.MIN_VALUE,
        maxValue: Long = Long.MAX_VALUE,
        minOrder: Long = Long.MIN_VALUE,
        maxOrder: Long = Long.MAX_VALUE,
        size: Int = Int.MAX_VALUE
    ): Flow<List<MessageWithLabelIds>>

    @Query("SELECT messageId FROM MessageEntity WHERE userId = :userId AND conversationId IN (:conversationIds)")
    abstract suspend fun getMessageIdsInConversations(
        userId: UserId,
        conversationIds: List<ConversationId>
    ): List<MessageId>

    @Query("SELECT * FROM MessageEntity WHERE userId = :userId AND conversationId IN (:conversationIds)")
    @Transaction
    abstract suspend fun getMessageWithLabelsInConversations(
        userId: UserId,
        conversationIds: List<ConversationId>
    ): List<MessageWithLabelIds>

    @Query("SELECT * FROM MessageEntity WHERE userId = :userId AND conversationId IN (:conversationIds)")
    @Transaction
    abstract fun observeMessageWithLabelsInConversations(
        userId: UserId,
        conversationIds: List<ConversationId>
    ): Flow<List<MessageWithLabelIds>>

    @Query("UPDATE MessageEntity SET messageId = :apiAssignedId WHERE userId = :userId AND messageId = :localDraftId")
    abstract suspend fun updateDraftMessageId(
        userId: UserId,
        localDraftId: MessageId,
        apiAssignedId: MessageId
    )

    private companion object {

        private val ReadAndUnread = listOf(false, true)
    }
}
