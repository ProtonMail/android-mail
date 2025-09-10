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
import ch.protonmail.android.mailconversation.domain.entity.ConversationError
import ch.protonmail.android.maillabel.domain.model.LabelId
import ch.protonmail.android.mailmessage.domain.model.ConversationMessages
import ch.protonmail.android.mailmessage.domain.model.Message
import ch.protonmail.android.mailpagination.domain.model.PageKey
import ch.protonmail.android.mailpagination.domain.model.PaginationError
import kotlinx.coroutines.flow.Flow
import me.proton.core.domain.entity.UserId

@Suppress("TooManyFunctions", "ComplexInterface")
interface ConversationRepository {

    /**
     * Load all [Conversation] from local cache for [userId].
     */
    suspend fun getConversations(
        userId: UserId,
        pageKey: PageKey.DefaultPageKey = PageKey.DefaultPageKey()
    ): Either<PaginationError, List<Conversation>>

    /**
     * Get a conversation.
     * Returns any conversation data that is available locally right away.
     * Message metadata is fetched and returned as available
     */
    suspend fun observeConversation(
        userId: UserId,
        id: ConversationId,
        labelId: LabelId
    ): Flow<Either<ConversationError, Conversation>>

    /**
     * Get all the [Message]s metadata for a given [ConversationId], for [userId] from the local storage
     */
    suspend fun observeConversationMessages(
        userId: UserId,
        conversationId: ConversationId,
        labelId: LabelId
    ): Flow<Either<ConversationError, ConversationMessages>>

    suspend fun move(
        userId: UserId,
        conversationIds: List<ConversationId>,
        toLabelId: LabelId
    ): Either<DataError, List<Conversation>>

    suspend fun markUnread(
        userId: UserId,
        labelId: LabelId,
        conversationIds: List<ConversationId>
    ): Either<DataError, Unit>

    suspend fun markRead(
        userId: UserId,
        labelId: LabelId,
        conversationIds: List<ConversationId>
    ): Either<DataError, Unit>

    suspend fun star(userId: UserId, conversationIds: List<ConversationId>): Either<DataError, List<Conversation>>

    suspend fun unStar(userId: UserId, conversationIds: List<ConversationId>): Either<DataError, List<Conversation>>

    suspend fun labelAs(
        userId: UserId,
        conversationIds: List<ConversationId>,
        selectedLabels: List<LabelId>,
        partiallySelectedLabels: List<LabelId>,
        shouldArchive: Boolean
    ): Either<DataError, Unit>

    suspend fun deleteConversations(userId: UserId, conversationIds: List<ConversationId>): Either<DataError, Unit>
}
