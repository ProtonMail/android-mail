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
import ch.protonmail.android.mailmessage.data.local.entity.UnreadMessagesCountEntity
import kotlinx.coroutines.flow.Flow
import me.proton.core.data.room.db.BaseDao
import me.proton.core.domain.entity.UserId

@Dao
abstract class UnreadMessagesCountDao : BaseDao<UnreadMessagesCountEntity>() {

    @Query("DELETE FROM UnreadMessagesCountEntity WHERE userId = :userId AND labelId IN (:labelIds)")
    abstract suspend fun delete(userId: UserId, labelIds: List<String>)

    @Query("DELETE FROM UnreadMessagesCountEntity WHERE userId = :userId")
    abstract suspend fun deleteAll(userId: UserId)

    @Query("SELECT * FROM UnreadMessagesCountEntity WHERE userId = :userId")
    abstract fun observeMessageCounts(userId: UserId): Flow<List<UnreadMessagesCountEntity>>

}
