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

package ch.protonmail.android.maildetail.domain.usecase

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.left
import ch.protonmail.android.mailcommon.domain.model.ConversationId
import ch.protonmail.android.mailconversation.domain.entity.Conversation
import ch.protonmail.android.mailconversation.domain.repository.ConversationRepository
import ch.protonmail.android.mailconversation.domain.usecase.ObserveConversationCacheUpdates
import ch.protonmail.android.maildetail.domain.model.MarkConversationReadError
import ch.protonmail.android.maillabel.domain.SelectedMailLabelId
import ch.protonmail.android.mailmessage.domain.entity.MessageId
import ch.protonmail.android.mailmessage.domain.repository.MessageRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.mapLatest
import me.proton.core.domain.entity.UserId
import javax.inject.Inject

class MarkMessageAndConversationReadIfAllMessagesRead @Inject constructor(
    private val messageRepository: MessageRepository,
    private val markMessageAsRead: MarkMessageAsRead,
    private val conversationRepository: ConversationRepository,
    private val selectedMailLabelId: SelectedMailLabelId,
    private val observeConversationCacheUpdates: ObserveConversationCacheUpdates
) {

    suspend operator fun invoke(
        userId: UserId,
        messageId: MessageId,
        conversationId: ConversationId
    ): Either<MarkConversationReadError, Conversation>? {
        val contextLabelId = selectedMailLabelId.flow.value.labelId
        return observeConversationCacheUpdates(userId, conversationId)
            .mapLatest {
                markMessageAsRead(userId, messageId).mapLeft { MarkConversationReadError.Data(it) }
                    .flatMap {
                        messageRepository.observeCachedMessages(userId, conversationId)
                            .first()
                            .mapLeft { MarkConversationReadError.Data(it) }
                    }
                    .flatMap { messages ->
                        val allRead = messages.all { !it.unread }
                        if (allRead) {
                            conversationRepository.markRead(userId, conversationId, contextLabelId)
                                .mapLeft { MarkConversationReadError.Data(it) }
                        } else {
                            MarkConversationReadError.ConversationHasUnreadMessages.left()
                        }
                    }
            }.firstOrNull()
    }
}
