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

package ch.protonmail.android.mailconversation.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import ch.protonmail.android.mailcommon.domain.model.ConversationId
import ch.protonmail.android.mailconversation.data.local.entity.ConversationEntity
import ch.protonmail.android.mailconversation.data.local.relation.ConversationWithLabels
import ch.protonmail.android.mailpagination.domain.model.OrderBy
import ch.protonmail.android.mailpagination.domain.model.OrderDirection
import ch.protonmail.android.mailpagination.domain.model.PageKey
import ch.protonmail.android.mailpagination.domain.model.ReadStatus
import kotlinx.coroutines.flow.Flow
import me.proton.core.data.room.db.BaseDao
import me.proton.core.domain.entity.UserId
import me.proton.core.label.domain.entity.LabelId

@Suppress("MaxLineLength", "LongParameterList")
@Dao
abstract class ConversationDao : BaseDao<ConversationEntity>() {

    fun observeAll(userId: UserId, pageKey: PageKey): Flow<List<ConversationWithLabels>> {
        val labelId = pageKey.filter.labelId
        val keyword = pageKey.filter.keyword
        val (minUnread, maxUnread) = when (pageKey.filter.read) {
            // 0 <= contextNumUnread <= infinite
            ReadStatus.All -> 0 to Int.MAX_VALUE
            // 0 <= contextNumUnread <= 0
            ReadStatus.Read -> 0 to 0
            // 1 <= contextNumUnread <= infinite
            ReadStatus.Unread -> 1 to Int.MAX_VALUE
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
                minUnread = minUnread,
                maxUnread = maxUnread,
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
                minUnread = minUnread,
                maxUnread = maxUnread,
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
        SELECT * FROM ConversationEntity
        JOIN ConversationLabelEntity
        ON ConversationLabelEntity.userId = ConversationEntity.userId
        AND ConversationLabelEntity.conversationId = ConversationEntity.conversationId
        AND ConversationLabelEntity.labelId = :labelId
        WHERE ConversationEntity.userId = :userId
        AND (
            ConversationEntity.subject LIKE '%'||:keyword||'%'
            OR ConversationEntity.senders LIKE '%'||:keyword||'%'
            OR ConversationEntity.recipients LIKE '%'||:keyword||'%'
        )
        AND ConversationLabelEntity.contextNumUnread BETWEEN :minUnread AND :maxUnread
        AND (ConversationLabelEntity.contextTime > :minValue OR (ConversationLabelEntity.contextTime = :minValue AND ConversationEntity.`order` >= :minOrder))
        AND (ConversationLabelEntity.contextTime < :maxValue OR (ConversationLabelEntity.contextTime = :maxValue AND ConversationEntity.`order` <= :maxOrder))
        ORDER BY ConversationLabelEntity.contextTime DESC, ConversationEntity.`order` DESC
        LIMIT :size
        """
    )
    @Transaction
    abstract fun observeAllOrderByTimeDesc(
        userId: UserId,
        labelId: LabelId,
        keyword: String,
        minUnread: Int,
        maxUnread: Int,
        minValue: Long,
        maxValue: Long,
        minOrder: Long,
        maxOrder: Long,
        size: Int
    ): Flow<List<ConversationWithLabels>>

    @Query(
        """
        SELECT * FROM ConversationEntity
        JOIN ConversationLabelEntity
        ON ConversationLabelEntity.userId = ConversationEntity.userId
        AND ConversationLabelEntity.conversationId = ConversationEntity.conversationId
        AND ConversationLabelEntity.labelId = :labelId
        WHERE ConversationEntity.userId = :userId
        AND (
            ConversationEntity.subject LIKE '%'||:keyword||'%'
            OR ConversationEntity.senders LIKE '%'||:keyword||'%'
            OR ConversationEntity.recipients LIKE '%'||:keyword||'%'
        )
        AND ConversationLabelEntity.contextNumUnread BETWEEN :minUnread AND :maxUnread
        AND (ConversationLabelEntity.contextTime > :minValue OR (ConversationLabelEntity.contextTime = :minValue AND ConversationEntity.`order` >= :minOrder))
        AND (ConversationLabelEntity.contextTime < :maxValue OR (ConversationLabelEntity.contextTime = :maxValue AND ConversationEntity.`order` <= :maxOrder))
        ORDER BY ConversationLabelEntity.contextTime ASC, ConversationEntity.`order` ASC
        LIMIT :size
        """
    )
    @Transaction
    abstract fun observeAllOrderByTimeAsc(
        userId: UserId,
        labelId: LabelId,
        keyword: String,
        minUnread: Int,
        maxUnread: Int,
        minValue: Long,
        maxValue: Long,
        minOrder: Long,
        maxOrder: Long,
        size: Int
    ): Flow<List<ConversationWithLabels>>

    @Query("DELETE FROM ConversationEntity WHERE userId = :userId AND conversationId IN (:conversationIds)")
    abstract suspend fun delete(userId: UserId, conversationIds: List<String>)

    @Query("DELETE FROM ConversationEntity WHERE userId = :userId")
    abstract suspend fun deleteAll(userId: UserId)

    @Query(
        """
            DELETE FROM ConversationEntity 
            WHERE userId = :userId 
            AND conversationId IN (
              SELECT conversationId FROM ConversationLabelEntity WHERE userId = :userId AND labelId = :labelId
            )
        """
    )
    abstract suspend fun deleteAllConversationsWithLabel(userId: UserId, labelId: LabelId)

    @Query("SELECT * FROM ConversationEntity WHERE userId = :userId AND conversationId = :conversationId")
    @Transaction
    abstract fun observe(userId: UserId, conversationId: ConversationId): Flow<ConversationWithLabels?>

    @Query("SELECT * FROM ConversationEntity WHERE userId = :userId AND conversationId IN (:conversationIds)")
    @Transaction
    abstract fun observe(userId: UserId, conversationIds: List<ConversationId>): Flow<List<ConversationWithLabels>>

    @Query("SELECT * FROM ConversationEntity WHERE userId = :userId AND conversationId = :conversationId")
    @Transaction
    abstract suspend fun getConversation(userId: UserId, conversationId: ConversationId): ConversationWithLabels?

    @Query("SELECT * FROM ConversationEntity WHERE userId = :userId AND conversationId IN (:conversationIds)")
    abstract suspend fun getConversations(
        userId: UserId,
        conversationIds: List<ConversationId>
    ): List<ConversationWithLabels>
}
