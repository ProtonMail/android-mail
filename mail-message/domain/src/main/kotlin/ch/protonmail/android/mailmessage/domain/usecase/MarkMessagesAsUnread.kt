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

package ch.protonmail.android.mailmessage.domain.usecase

import arrow.core.Either
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailmessage.domain.model.Message
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailmessage.domain.repository.MessageRepository
import kotlinx.coroutines.flow.firstOrNull
import me.proton.core.domain.entity.UserId
import javax.inject.Inject

class MarkMessagesAsUnread @Inject constructor(
    private val messageRepository: MessageRepository,
    private val incrementUnreadCount: IncrementUnreadCount
) {

    suspend operator fun invoke(userId: UserId, messageIds: List<MessageId>): Either<DataError.Local, List<Message>> {
        incrementUnreadMessagesCount(userId, messageIds)
        return messageRepository.markUnread(userId, messageIds)
    }

    private suspend fun incrementUnreadMessagesCount(userId: UserId, messageIds: List<MessageId>) {
        messageRepository.observeCachedMessages(userId, messageIds).firstOrNull()?.map { messages ->
            messages.onEach { message ->
                if (message.read) {
                    incrementUnreadCount(userId, message.labelIds)
                }
            }
        }
    }

}
