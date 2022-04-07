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
import ch.protonmail.android.mailconversation.data.local.entity.ConversationLabelEntity
import ch.protonmail.android.mailconversation.domain.entity.ConversationId
import me.proton.core.data.room.db.BaseDao
import me.proton.core.domain.entity.UserId

@Dao
abstract class ConversationLabelDao : BaseDao<ConversationLabelEntity>() {

    @Query("DELETE FROM ConversationLabelEntity WHERE userId = :userId AND conversationId in (:conversationIds)")
    abstract suspend fun deleteAll(userId: UserId, conversationIds: List<ConversationId>)
}
