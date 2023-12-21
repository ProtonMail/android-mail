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

package ch.protonmail.android.mailconversation.data.local

import ch.protonmail.android.mailconversation.data.local.entity.UnreadConversationsCountEntity
import kotlinx.coroutines.flow.Flow
import me.proton.core.domain.entity.UserId
import me.proton.core.label.domain.entity.LabelId
import javax.inject.Inject

class UnreadConversationsCountLocalDataSourceImpl @Inject constructor(
    database: ConversationDatabase
) : UnreadConversationsCountLocalDataSource {

    private val unreadConversationsCountDao = database.unreadConversationsCountDao()

    override fun observeConversationCounters(userId: UserId): Flow<List<UnreadConversationsCountEntity>> =
        unreadConversationsCountDao.observeConversationsCounts(userId)

    override suspend fun saveConversationCounters(counters: List<UnreadConversationsCountEntity>) {
        unreadConversationsCountDao.insertOrUpdate(*counters.toTypedArray())
    }

    override suspend fun delete(userId: UserId, labelIds: List<LabelId>) {
        unreadConversationsCountDao.delete(userId, labelIds.map { it.id })
    }

    override suspend fun deleteAll(userId: UserId) {
        unreadConversationsCountDao.deleteAll(userId)
    }

    override suspend fun saveConversationCounter(counter: UnreadConversationsCountEntity) {
        unreadConversationsCountDao.insertOrUpdate(counter)
    }
}
