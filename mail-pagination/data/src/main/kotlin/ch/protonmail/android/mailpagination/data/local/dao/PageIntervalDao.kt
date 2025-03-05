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

package ch.protonmail.android.mailpagination.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import ch.protonmail.android.mailpagination.data.local.entity.PageIntervalEntity
import ch.protonmail.android.mailpagination.data.local.merge
import ch.protonmail.android.mailpagination.domain.model.OrderBy
import ch.protonmail.android.mailpagination.domain.model.PageItemType
import ch.protonmail.android.mailpagination.domain.model.ReadStatus
import me.proton.core.data.room.db.BaseDao
import me.proton.core.domain.entity.UserId
import me.proton.core.label.domain.entity.LabelId

@Suppress("LongParameterList")
@Dao
abstract class PageIntervalDao : BaseDao<PageIntervalEntity>() {

    @Query(
        """
        SELECT * FROM PageIntervalEntity 
        WHERE PageIntervalEntity.userId = :userId
        AND PageIntervalEntity.type = :type
        AND PageIntervalEntity.labelId = :labelId
        AND PageIntervalEntity.orderBy = :orderBy
        AND PageIntervalEntity.keyword = :keyword
        AND PageIntervalEntity.read = :read
        ORDER BY PageIntervalEntity.minValue ASC, PageIntervalEntity.minOrder ASC
        """
    )
    abstract suspend fun getAll(
        userId: UserId,
        type: PageItemType,
        orderBy: OrderBy,
        labelId: LabelId,
        keyword: String,
        read: ReadStatus
    ): List<PageIntervalEntity>

    @Transaction
    open suspend fun insertOrMerge(entity: PageIntervalEntity) {
        val currentList = getAll(
            userId = entity.userId,
            type = entity.type,
            orderBy = entity.orderBy,
            labelId = entity.labelId,
            keyword = entity.keyword,
            read = entity.read
        )
        delete(*currentList.toTypedArray())
        val merged = (currentList + entity).merge()
        insertOrUpdate(*merged.toTypedArray())
    }

    @Query(
        """
        DELETE FROM PageIntervalEntity 
        WHERE PageIntervalEntity.userId = :userId
        AND PageIntervalEntity.type = :type 
        AND PageIntervalEntity.labelId = :labelId
        """
    )
    abstract suspend fun deleteAll(
        userId: UserId,
        type: PageItemType,
        labelId: LabelId
    )

    @Query(
        """
        DELETE FROM PageIntervalEntity 
        WHERE PageIntervalEntity.userId = :userId
        AND PageIntervalEntity.type = :type 
        """
    )
    abstract suspend fun deleteAll(userId: UserId, type: PageItemType)

    @Query(
        """
        DELETE FROM PageIntervalEntity WHERE keyword <> ''
        """
    )
    abstract suspend fun deleteSearchedIntervals()
}
