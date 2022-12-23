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

package ch.protonmail.android.mailconversation.domain.repository

import arrow.core.Either
import ch.protonmail.android.mailcommon.domain.model.ConversationId
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailconversation.domain.entity.Conversation
import ch.protonmail.android.mailconversation.domain.entity.ConversationWithContext
import ch.protonmail.android.mailconversation.domain.entity.ConversationWithMessages
import ch.protonmail.android.mailmessage.domain.entity.MessageId
import ch.protonmail.android.mailpagination.domain.model.PageKey
import me.proton.core.domain.entity.UserId
import me.proton.core.label.domain.entity.LabelId

interface ConversationRemoteDataSource {

    /**
     * Get all [Conversation] for [userId].
     */
    suspend fun getConversations(
        userId: UserId,
        pageKey: PageKey
    ): Either<DataError.Remote, List<ConversationWithContext>>

    suspend fun getConversationWithMessages(
        userId: UserId,
        conversationId: ConversationId
    ): ConversationWithMessages

    /**
     * Add [labelId] to the given [conversationId]
     */
    fun addLabel(
        userId: UserId,
        conversationId: ConversationId,
        labelId: LabelId,
        messageIds: List<MessageId>
    )

    /**
     * Remove [labelId] from the given [conversationId]
     */
    fun removeLabel(
        userId: UserId,
        conversationId: ConversationId,
        labelId: LabelId,
        messageIds: List<MessageId>
    )
}
