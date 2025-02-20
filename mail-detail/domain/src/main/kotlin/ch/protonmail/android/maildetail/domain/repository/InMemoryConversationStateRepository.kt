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

package ch.protonmail.android.maildetail.domain.repository

import ch.protonmail.android.mailmessage.domain.model.DecryptedMessageBody
import ch.protonmail.android.mailmessage.domain.model.MessageId
import kotlinx.coroutines.flow.Flow

interface InMemoryConversationStateRepository {

    val conversationState: Flow<MessagesState>

    suspend fun expandMessage(
        messageId: MessageId,
        decryptedBody: DecryptedMessageBody,
        postExpandEffect: PostExpandEffect?
    )

    suspend fun consumeEffect(messageId: MessageId)

    suspend fun expandingMessage(messageId: MessageId)

    suspend fun collapseMessage(messageId: MessageId)

    suspend fun switchTrashedMessagesFilter()

    data class MessagesState(
        val messagesState: Map<MessageId, MessageState>,
        val shouldHideMessagesBasedOnTrashFilter: Boolean
    )

    sealed class MessageState {
        data object Collapsed : MessageState()
        data object Expanding : MessageState()
        data class Expanded(val decryptedBody: DecryptedMessageBody, val effect: PostExpandEffect?) : MessageState()
    }

    sealed interface PostExpandEffect {
        data object PrintRequested : PostExpandEffect
        data object ReplyRequested : PostExpandEffect
        data object ReplyAllRequested : PostExpandEffect
        data object ForwardRequested : PostExpandEffect
    }
}
