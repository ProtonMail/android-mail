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
import ch.protonmail.android.maillabel.domain.SelectedMailLabelId
import kotlinx.coroutines.flow.firstOrNull
import me.proton.core.domain.entity.UserId
import me.proton.core.label.domain.entity.LabelId
import javax.inject.Inject

class MarkConversationsAsUnread @Inject constructor(
    private val conversationRepository: ConversationRepository,
    private val selectedMailLabelId: SelectedMailLabelId,
    private val incrementUnreadCount: IncrementUnreadCount
) {

    suspend operator fun invoke(
        userId: UserId,
        conversationIds: List<ConversationId>
    ): Either<DataError, List<Conversation>> {
        val contextLabelId = selectedMailLabelId.flow.value.labelId
        incrementUnreadConversationsCount(userId, conversationIds, contextLabelId)
        return conversationRepository.markUnread(userId, conversationIds, contextLabelId)
    }

    private suspend fun incrementUnreadConversationsCount(
        userId: UserId,
        conversationIds: List<ConversationId>,
        contextLabelId: LabelId
    ) {
        conversationRepository.observeCachedConversations(userId, conversationIds)
            .firstOrNull()
            ?.onEach { conversation ->
                if (conversation.allMessagesInLabelAreRead(contextLabelId)) {
                    incrementUnreadCount(userId, listOf(contextLabelId))
                }
            }
    }

    private fun Conversation.allMessagesInLabelAreRead(contextLabelId: LabelId) =
        this.labels.find { it.labelId == contextLabelId }.allMessagesAreRead()

    private fun ConversationLabel?.allMessagesAreRead() = this?.contextNumUnread == 0
}
