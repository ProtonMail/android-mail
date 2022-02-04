/*
 * Copyright (c) 2021 Proton Technologies AG
 * This file is part of Proton Technologies AG and ProtonMail.
 *
 * ProtonMail is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ProtonMail is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with ProtonMail.  If not, see <https://www.gnu.org/licenses/>.
 */

package ch.protonmail.android.mailconversation.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import ch.protonmail.android.mailconversation.data.local.entity.ConversationEntity
import kotlinx.coroutines.flow.Flow
import me.proton.core.data.room.db.BaseDao
import me.proton.core.domain.entity.UserId

@Dao
abstract class ConversationDao : BaseDao<ConversationEntity>() {

    @Query("SELECT * FROM ConversationEntity WHERE userId = :userId")
    abstract fun observeAll(userId: UserId): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM ConversationEntity WHERE userId = :userId ")
    abstract suspend fun getAll(userId: UserId): List<ConversationEntity>

    @Query("DELETE FROM ConversationEntity WHERE userId = :userId")
    abstract suspend fun deleteAll(userId: UserId)
}
