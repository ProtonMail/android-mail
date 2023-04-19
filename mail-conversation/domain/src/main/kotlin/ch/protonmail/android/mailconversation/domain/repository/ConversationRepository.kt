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
import ch.protonmail.android.mailpagination.domain.model.PageKey
import kotlinx.coroutines.flow.Flow
import me.proton.core.domain.entity.UserId
import me.proton.core.label.domain.entity.LabelId

@Suppress("TooManyFunctions")
interface ConversationRepository {

    /**
     * Load all [Conversation] for [userId].
     */
    suspend fun loadConversations(
        userId: UserId,
        pageKey: PageKey = PageKey()
    ): List<ConversationWithContext>

    /**
     * Return true if all [Conversation] are considered locally valid according the given [pageKey].
     */
    suspend fun isLocalPageValid(
        userId: UserId,
        pageKey: PageKey,
        items: List<ConversationWithContext>
    ): Boolean

    /**
     * Fetch all [Conversation] for [userId] filtered by [PageKey].
     */
    suspend fun fetchConversations(
        userId: UserId,
        pageKey: PageKey
    ): Either<DataError.Remote, List<ConversationWithContext>>

    /**
     * Mark local data as stale for [userId], by [labelId].
     */
    suspend fun markAsStale(
        userId: UserId,
        labelId: LabelId
    )

    /**
     * Get a conversation.
     * Returns any conversation data that is available locally right away.
     * Message metadata is fetched and returned as available
     */
    fun observeConversation(
        userId: UserId,
        id: ConversationId,
        refreshData: Boolean
    ): Flow<Either<DataError, Conversation>>

    fun observeConversationCacheUpToDate(
        userId: UserId,
        id: ConversationId
    ): Flow<Either<DataError, Unit>>

    /**
     * Adds the given [labelId] to the message with the given [conversationId]
     */
    suspend fun addLabel(
        userId: UserId,
        conversationId: ConversationId,
        labelId: LabelId
    ): Either<DataError, Conversation>

    suspend fun addLabels(
        userId: UserId,
        conversationId: ConversationId,
        labelIds: List<LabelId>
    ): Either<DataError, Conversation>

    suspend fun removeLabel(
        userId: UserId,
        conversationId: ConversationId,
        labelId: LabelId
    ): Either<DataError, Conversation>

    suspend fun removeLabels(
        userId: UserId,
        conversationId: ConversationId,
        labelIds: List<LabelId>
    ): Either<DataError, Conversation>

    suspend fun move(
        userId: UserId,
        conversationId: ConversationId,
        fromLabelIds: List<LabelId> = emptyList(),
        toLabelId: LabelId
    ): Either<DataError, Conversation>

    suspend fun markUnread(
        userId: UserId,
        conversationId: ConversationId,
        contextLabelId: LabelId
    ): Either<DataError.Local, Conversation>

    suspend fun markRead(
        userId: UserId,
        conversationId: ConversationId,
        contextLabelId: LabelId
    ): Either<DataError.Local, Conversation>

    suspend fun isCachedConversationRead(
        userId: UserId,
        conversationId: ConversationId
    ): Either<DataError, Boolean>

    suspend fun relabel(
        userId: UserId,
        conversationId: ConversationId,
        labelsToBeRemoved: List<LabelId>,
        labelsToBeAdded: List<LabelId>
    ): Either<DataError, Conversation>
}
