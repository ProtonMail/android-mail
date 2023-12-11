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

package ch.protonmail.android.mailmailbox.data.local

import ch.protonmail.android.mailmailbox.data.entity.UnreadConversationsCountEntity
import ch.protonmail.android.mailmailbox.data.entity.UnreadMessagesCountEntity
import kotlinx.coroutines.flow.Flow
import me.proton.core.domain.entity.UserId
import timber.log.Timber
import javax.inject.Inject

@Suppress("NotImplementedDeclaration")
class UnreadCountLocalDataSourceImpl @Inject constructor(
    database: UnreadCountDatabase
) : UnreadCountLocalDataSource {

    private val unreadMessagesCountDao = database.unreadMessagesCountDao()
    private val unreadConversationsCountDao = database.unreadConversationsCountDao()

    override fun observeMessageCounters(userId: UserId): Flow<List<UnreadMessagesCountEntity>> =
        unreadMessagesCountDao.observeMessageCounts(userId)

    override fun observeConversationCounters(userId: UserId): Flow<List<UnreadConversationsCountEntity>> =
        unreadConversationsCountDao.observeConversationsCounts(userId)

    override suspend fun saveMessageCounters(counters: List<UnreadMessagesCountEntity>) {
        Timber.d("Unread counters: Writing message counters to DB $counters")
        unreadMessagesCountDao.insertOrUpdate(*counters.toTypedArray())
        Timber.d("written")
    }

    override suspend fun saveConversationCounters(counters: List<UnreadConversationsCountEntity>) {
        Timber.d("Unread counters: Writing conversation counters to DB $counters")
        unreadConversationsCountDao.insertOrUpdate(*counters.toTypedArray())
        Timber.d("written")
    }
}
