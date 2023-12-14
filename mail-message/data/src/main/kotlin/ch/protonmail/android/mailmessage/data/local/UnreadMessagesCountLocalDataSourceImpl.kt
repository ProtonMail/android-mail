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

package ch.protonmail.android.mailmessage.data.local

import ch.protonmail.android.mailmessage.data.local.entity.UnreadMessagesCountEntity
import kotlinx.coroutines.flow.Flow
import me.proton.core.domain.entity.UserId
import me.proton.core.label.domain.entity.LabelId
import javax.inject.Inject

class UnreadMessagesCountLocalDataSourceImpl @Inject constructor(
    database: MessageDatabase
) : UnreadMessagesCountLocalDataSource {

    private val unreadMessagesCountDao = database.unreadMessagesCountDao()

    override fun observeMessageCounters(userId: UserId): Flow<List<UnreadMessagesCountEntity>> =
        unreadMessagesCountDao.observeMessageCounts(userId)

    override suspend fun saveMessageCounters(counters: List<UnreadMessagesCountEntity>) {
        unreadMessagesCountDao.insertOrUpdate(*counters.toTypedArray())
    }

    override suspend fun delete(userId: UserId, labelIds: List<LabelId>) {
        unreadMessagesCountDao.delete(userId, labelIds.map { it.id })
    }

    override suspend fun deleteAll(userId: UserId) {
        unreadMessagesCountDao.deleteAll(userId)
    }

    override suspend fun saveMessageCounter(counter: UnreadMessagesCountEntity) {
        unreadMessagesCountDao.insertOrUpdate(counter)
    }
}
