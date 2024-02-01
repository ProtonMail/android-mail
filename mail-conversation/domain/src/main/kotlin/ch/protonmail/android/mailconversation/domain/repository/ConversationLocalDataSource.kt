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
import ch.protonmail.android.mailcommon.domain.model.DaoError
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailconversation.domain.entity.Conversation
import ch.protonmail.android.mailconversation.domain.entity.ConversationWithContext
import ch.protonmail.android.mailpagination.domain.model.PageKey
import kotlinx.coroutines.flow.Flow
import me.proton.core.domain.entity.UserId
import me.proton.core.label.domain.entity.LabelId

@Suppress("TooManyFunctions", "ComplexInterface")
interface ConversationLocalDataSource {

    /**
     * Observe all [Conversation] by [userId] for this [pageKey].
     */
    fun observeConversations(userId: UserId, pageKey: PageKey): Flow<List<ConversationWithContext>>

    /**
     * Get all [Conversation] by [userId] for this [pageKey].
     */
    suspend fun getConversations(userId: UserId, pageKey: PageKey): List<ConversationWithContext>

    /**
     * Observe all [Conversation] by [userId] and [ids].
     */
    fun observeCachedConversations(userId: UserId, ids: List<ConversationId>): Flow<List<Conversation>>

    /**
     * Update or insert [Conversation] related to the same [userId] and [pageKey].
     */
    suspend fun upsertConversations(
        userId: UserId,
        pageKey: PageKey,
        items: List<ConversationWithContext>
    ): Either<DaoError.UpsertError, Unit>

    /**
     * Update or insert [Conversation].
     */
    suspend fun upsertConversations(items: List<Conversation>): Either<DaoError.UpsertError, Unit>

    /**
     * Delete Conversation(s) for [userId], by [ids].
     */
    suspend fun deleteConversation(userId: UserId, ids: List<ConversationId>)

    /**
     * Delete Conversation(s) for [userId], by [ids].
     */
    suspend fun deleteConversations(userId: UserId, ids: List<ConversationId>): Either<DataError, Unit>

    /**
     * Delete all conversations for [userId].
     */
    suspend fun deleteAllConversations(userId: UserId)

    /**
     * Delete Conversation(s) for [userId], only conversations with the given [labelId] will be deleted.
     */
    suspend fun deleteConversationsWithLabel(userId: UserId, labelId: LabelId): Either<DataError, Unit>

    /**
     * Mark local data as stale for [userId], by [labelId].
     */
    suspend fun markAsStale(userId: UserId, labelId: LabelId)

    /**
     * Return true if all [Conversation] are considered locally up-to-date according the given [pageKey].
     */
    suspend fun isLocalPageValid(
        userId: UserId,
        pageKey: PageKey,
        items: List<ConversationWithContext>
    ): Boolean

    /**
     * Return clipped [PageKey] according already persisted intervals.
     *
     * Note: Usually used to trim unnecessary interval from the [PageKey] before fetching.
     */
    suspend fun getClippedPageKey(userId: UserId, pageKey: PageKey): PageKey?

    fun observeConversation(userId: UserId, conversationId: ConversationId): Flow<Conversation?>

    suspend fun upsertConversation(userId: UserId, conversation: Conversation): Either<DaoError.UpsertError, Unit>

    /**
     * Adds [labelId] to given [conversationIds] related to the same [userId]
     */
    suspend fun addLabel(
        userId: UserId,
        conversationIds: List<ConversationId>,
        labelId: LabelId
    ): Either<DataError.Local, List<Conversation>>

    /**
     * Adds [labelIds] to given [conversationId] related to the same [userId]
     */
    suspend fun addLabels(
        userId: UserId,
        conversationId: ConversationId,
        labelIds: List<LabelId>
    ): Either<DataError.Local, Conversation>

    /**
     * Adds provided [labelIds] to given [conversationIds] related to the same [userId]
     */
    suspend fun addLabels(
        userId: UserId,
        conversationIds: List<ConversationId>,
        labelIds: List<LabelId>
    ): Either<DataError.Local, List<Conversation>>

    /**
     * Removes [labelId] from given [conversationIds] related to the same [userId]
     */
    suspend fun removeLabel(
        userId: UserId,
        conversationIds: List<ConversationId>,
        labelId: LabelId
    ): Either<DataError.Local, List<Conversation>>

    /**
     * Removes provided [labelIds] from given [conversationId] related to the same [userId]
     */
    suspend fun removeLabels(
        userId: UserId,
        conversationId: ConversationId,
        labelIds: List<LabelId>
    ): Either<DataError.Local, Conversation>

    /**
     * Removes provided [labelIds] from given [conversationIds] related to the same [userId]
     */
    suspend fun removeLabels(
        userId: UserId,
        conversationIds: List<ConversationId>,
        labelIds: List<LabelId>
    ): Either<DataError.Local, List<Conversation>>

    suspend fun relabel(
        userId: UserId,
        conversationIds: List<ConversationId>,
        labelIdsToAdd: List<LabelId> = emptyList(),
        labelIdsToRemove: List<LabelId> = emptyList()
    ): Either<DataError.Local, List<Conversation>>

    suspend fun markUnread(
        userId: UserId,
        conversationIds: List<ConversationId>,
        contextLabelId: LabelId
    ): Either<DataError.Local, List<Conversation>>

    suspend fun markRead(
        userId: UserId,
        conversationIds: List<ConversationId>
    ): Either<DataError.Local, List<Conversation>>

    suspend fun isConversationRead(userId: UserId, conversationId: ConversationId): Either<DataError.Local, Boolean>

    suspend fun getConversation(userId: UserId, conversationId: ConversationId): Either<DataError.Local, Conversation>

    suspend fun getConversations(
        userId: UserId,
        conversationIds: List<ConversationId>
    ): Either<DataError.Local, List<Conversation>>
}
