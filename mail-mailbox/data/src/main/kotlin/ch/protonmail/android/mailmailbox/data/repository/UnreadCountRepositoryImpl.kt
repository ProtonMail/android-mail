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

package ch.protonmail.android.mailmailbox.data.repository

import ch.protonmail.android.mailmailbox.data.local.UnreadCountLocalDataSource
import ch.protonmail.android.mailmailbox.data.remote.UnreadCountRemoteDataSource
import ch.protonmail.android.mailmailbox.domain.model.UnreadCounter
import ch.protonmail.android.mailmailbox.domain.model.UnreadCounters
import ch.protonmail.android.mailmailbox.domain.repository.UnreadCountRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import me.proton.core.domain.entity.UserId
import javax.inject.Inject

class UnreadCountRepositoryImpl @Inject constructor(
    private val localDataSource: UnreadCountLocalDataSource,
    private val remoteDataSource: UnreadCountRemoteDataSource
) : UnreadCountRepository {

    override fun observeUnreadCount(userId: UserId): Flow<UnreadCounters> = combine(
        localDataSource.observeMessageCounters(userId),
        localDataSource.observeConversationCounters(userId)
    ) { messageCounters, conversationCounters ->
        if (messageCounters.isEmpty()) {
            refreshLocalMessageCounters(userId)
        }
        if (conversationCounters.isEmpty()) {
            refreshLocalConversationCounters(userId)
        }
        return@combine UnreadCounters(
            conversationCounters.map { UnreadCounter(it.labelId, it.unreadCount) },
            messageCounters.map { UnreadCounter(it.labelId, it.unreadCount) }
        )
    }

    private suspend fun refreshLocalConversationCounters(userId: UserId) {
        localDataSource.saveConversationCounters(
            remoteDataSource.getConversationCounters(userId).map { it.toUnreadCountConversationsEntity(userId) }
        )
    }

    private suspend fun refreshLocalMessageCounters(userId: UserId) {
        localDataSource.saveMessageCounters(
            remoteDataSource.getMessageCounters(userId).map { it.toUnreadCountMessagesEntity(userId) }
        )
    }

}
