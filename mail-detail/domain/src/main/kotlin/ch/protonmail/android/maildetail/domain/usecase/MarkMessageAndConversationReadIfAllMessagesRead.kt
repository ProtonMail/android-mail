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
import arrow.core.NonEmptyList
import arrow.core.flatMap
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.ConversationId
import ch.protonmail.android.mailconversation.domain.repository.ConversationRepository
import ch.protonmail.android.mailconversation.domain.usecase.MarkConversationsAsRead
import ch.protonmail.android.maildetail.domain.model.MarkConversationReadError
import ch.protonmail.android.maildetail.domain.model.MarkConversationReadError.DataSourceError
import ch.protonmail.android.mailmessage.domain.model.Message
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailmessage.domain.repository.MessageRepository
import kotlinx.coroutines.flow.first
import me.proton.core.domain.entity.UserId
import javax.inject.Inject

class MarkMessageAndConversationReadIfAllMessagesRead @Inject constructor(
    private val messageRepository: MessageRepository,
    private val markMessageAsRead: MarkMessageAsRead,
    private val conversationRepository: ConversationRepository,
    private val markConversationsAsRead: MarkConversationsAsRead
) {

    suspend operator fun invoke(
        userId: UserId,
        messageId: MessageId,
        conversationId: ConversationId
    ): Either<MarkConversationReadError, Unit> = messageRepository.isMessageRead(userId, messageId)
        .fold(
            ifLeft = { error -> DataSourceError(error).left() },
            ifRight = { isRead ->
                markMessageReadIfUnread(isRead, userId, messageId)
                    .flatMap { markConversationAsReadIfAllRead(userId, conversationId) }
            }
        )

    private suspend fun markMessageReadIfUnread(
        isRead: Boolean,
        userId: UserId,
        messageId: MessageId
    ): Either<DataSourceError, Any> = if (!isRead) {
        markMessageAsRead(userId, messageId)
            .mapLeft { error ->
                DataSourceError(error)
            }
    } else {
        Unit.right()
    }

    private suspend fun markConversationAsReadIfAllRead(
        userId: UserId,
        conversationId: ConversationId
    ): Either<MarkConversationReadError, Unit> {
        return conversationRepository.isCachedConversationRead(userId, conversationId)
            .mapLeft { error -> DataSourceError(error) }
            .flatMap { isRead ->
                if (isRead) {
                    Unit.right()
                } else {
                    getConversationMessages(userId, conversationId)
                        .map { messages -> messages.all { message -> !message.unread } }
                        .flatMap { allRead ->
                            if (allRead) {
                                markConversationsAsRead(userId, listOf(conversationId))
                                    .fold(
                                        ifLeft = { error -> DataSourceError(error).left() },
                                        ifRight = { Unit.right() }
                                    )
                            } else {
                                Unit.right()
                            }
                        }
                }
            }
    }

    private suspend fun getConversationMessages(
        userId: UserId,
        conversationId: ConversationId
    ): Either<DataSourceError, NonEmptyList<Message>> = messageRepository.observeCachedMessages(userId, conversationId)
        .first()
        .mapLeft { error -> DataSourceError(error) }
}
