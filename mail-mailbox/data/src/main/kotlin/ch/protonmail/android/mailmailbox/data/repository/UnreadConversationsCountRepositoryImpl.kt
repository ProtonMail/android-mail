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

import ch.protonmail.android.mailmailbox.data.local.UnreadConversationsCountLocalDataSource
import ch.protonmail.android.mailmailbox.data.remote.UnreadConversationsCountRemoteDataSource
import ch.protonmail.android.mailmailbox.domain.model.UnreadCounter
import ch.protonmail.android.mailmailbox.domain.repository.UnreadConversationsCountRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapLatest
import me.proton.core.domain.entity.UserId
import javax.inject.Inject

class UnreadConversationsCountRepositoryImpl @Inject constructor(
    private val conversationLocalDataSource: UnreadConversationsCountLocalDataSource,
    private val conversationRemoteDataSource: UnreadConversationsCountRemoteDataSource
) : UnreadConversationsCountRepository {

    override fun observeUnreadCounters(userId: UserId): Flow<List<UnreadCounter>> =
        conversationLocalDataSource.observeConversationCounters(userId).mapLatest { conversationCounters ->
            if (conversationCounters.isEmpty()) {
                refreshLocalConversationCounters(userId)
            }
            conversationCounters.map { UnreadCounter(it.labelId, it.unreadCount) }
        }

    private suspend fun refreshLocalConversationCounters(userId: UserId) {
        conversationLocalDataSource.saveConversationCounters(
            conversationRemoteDataSource.getConversationCounters(userId).map {
                it.toUnreadCountConversationsEntity(userId)
            }
        )
    }

}
