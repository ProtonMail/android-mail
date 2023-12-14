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

import ch.protonmail.android.mailcommon.domain.model.ConversationId
import ch.protonmail.android.mailconversation.domain.entity.ConversationLabel
import ch.protonmail.android.mailconversation.domain.repository.ConversationRepository
import kotlinx.coroutines.flow.firstOrNull
import me.proton.core.domain.entity.UserId
import me.proton.core.label.domain.entity.LabelId
import javax.inject.Inject

class DeleteConversations @Inject constructor(
    private val conversationRepository: ConversationRepository,
    private val decrementUnreadCount: DecrementUnreadCount
) {

    suspend operator fun invoke(
        userId: UserId,
        conversationIds: List<ConversationId>,
        currentLabelId: LabelId
    ) {
        decrementUnreadConversationsCount(userId, conversationIds)
        conversationRepository.deleteConversations(userId, conversationIds, currentLabelId)
    }

    suspend operator fun invoke(userId: UserId, labelId: LabelId) {
        conversationRepository.deleteConversations(userId, labelId)
    }

    private suspend fun decrementUnreadConversationsCount(userId: UserId, conversationIds: List<ConversationId>) {
        conversationRepository.observeCachedConversations(userId, conversationIds)
            .firstOrNull()
            ?.onEach { conversation ->
                val labelIdsWithUnreadMessages = conversation.labels.filter { it.hasUnreadMessages() }
                decrementUnreadCount(userId, labelIdsWithUnreadMessages.map { it.labelId })
            }
    }

    private fun ConversationLabel?.hasUnreadMessages() = this?.let { it.contextNumUnread > 0 } ?: false
}
