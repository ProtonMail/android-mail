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

package ch.protonmail.android.mailconversation.domain.usecase

import arrow.core.Either
import ch.protonmail.android.mailcommon.domain.model.ConversationId
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailconversation.domain.entity.Conversation
import ch.protonmail.android.mailconversation.domain.entity.ConversationLabel
import ch.protonmail.android.mailconversation.domain.repository.ConversationRepository
import kotlinx.coroutines.flow.firstOrNull
import me.proton.core.domain.entity.UserId
import javax.inject.Inject

class MarkConversationsAsRead @Inject constructor(
    private val conversationRepository: ConversationRepository,
    private val decrementUnreadCount: DecrementUnreadCount
) {

    suspend operator fun invoke(
        userId: UserId,
        conversationIds: List<ConversationId>
    ): Either<DataError, List<Conversation>> {
        decrementUnreadConversationsCount(userId, conversationIds)
        return conversationRepository.markRead(userId, conversationIds)
    }

    private suspend fun decrementUnreadConversationsCount(userId: UserId, conversationIds: List<ConversationId>) {
        conversationRepository.observeCachedConversations(userId, conversationIds)
            .firstOrNull()
            ?.onEach { conversation ->
                val labelsWithUnreadMessages = conversation.labels.mapNotNull { it.takeIf { it.hasUnreads() } }
                decrementUnreadCount(userId, labelsWithUnreadMessages.map { it.labelId })
            }
    }

    private fun ConversationLabel.hasUnreads() = this.contextNumUnread > 0
}
