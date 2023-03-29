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
import arrow.core.Nel
import arrow.core.flatMap
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.ConversationId
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailconversation.domain.repository.ConversationRepository
import ch.protonmail.android.maildetail.domain.model.MarkConversationReadError
import ch.protonmail.android.maildetail.domain.model.MarkConversationReadError.DataSourceError
import ch.protonmail.android.maillabel.domain.SelectedMailLabelId
import ch.protonmail.android.mailmessage.domain.entity.Message
import ch.protonmail.android.mailmessage.domain.entity.MessageId
import ch.protonmail.android.mailmessage.domain.repository.MessageRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEmpty
import me.proton.core.domain.entity.UserId
import me.proton.core.label.domain.entity.LabelId
import javax.inject.Inject

class MarkMessageAndConversationReadIfAllMessagesRead @Inject constructor(
    private val messageRepository: MessageRepository,
    private val markMessageAsRead: MarkMessageAsRead,
    private val conversationRepository: ConversationRepository,
    private val selectedMailLabelId: SelectedMailLabelId
) {

    suspend operator fun invoke(
        userId: UserId,
        messageId: MessageId,
        conversationId: ConversationId
    ): Either<MarkConversationReadError, Unit> {
        val contextLabelId = selectedMailLabelId.flow.value.labelId
        return conversationRepository.observeConversationCacheUpToDate(userId, conversationId)
            .onEmpty { emit(DataError.Local.Unknown.left()) }
            .map {
                it.fold(
                    ifLeft = { error -> DataSourceError(error).left() },
                    ifRight = { markMessageAndConversationAsRead(userId, messageId, conversationId, contextLabelId) }
                )
            }
            .first()
    }

    private suspend fun markMessageAndConversationAsRead(
        userId: UserId,
        messageId: MessageId,
        conversationId: ConversationId,
        contextLabelId: LabelId
    ) = markMessageAsRead(userId, messageId)
        .mapLeft { error -> DataSourceError(error) }
        .flatMap { getConversationMessages(userId, conversationId) }
        .flatMap { messages -> markConversationAsReadIfAllRead(messages, userId, conversationId, contextLabelId) }

    private suspend fun markConversationAsReadIfAllRead(
        messages: Nel<Message>,
        userId: UserId,
        conversationId: ConversationId,
        contextLabelId: LabelId
    ): Either<MarkConversationReadError, Unit> {
        val allRead = messages.all { message -> !message.unread }
        return if (allRead) {
            conversationRepository.markRead(userId, conversationId, contextLabelId)
                .fold(
                    ifLeft = { error -> DataSourceError(error).left() },
                    ifRight = { Unit.right() }
                )
        } else {
            MarkConversationReadError.ConversationHasUnreadMessages.left()
        }
    }

    private suspend fun getConversationMessages(
        userId: UserId,
        conversationId: ConversationId
    ) = messageRepository.observeCachedMessages(userId, conversationId)
        .first()
        .mapLeft { error -> DataSourceError(error) }
}
