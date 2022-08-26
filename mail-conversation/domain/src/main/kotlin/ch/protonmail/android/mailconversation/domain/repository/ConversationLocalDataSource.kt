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

import ch.protonmail.android.mailcommon.domain.model.ConversationId
import ch.protonmail.android.mailconversation.domain.entity.Conversation
import ch.protonmail.android.mailpagination.domain.entity.PageKey
import kotlinx.coroutines.flow.Flow
import me.proton.core.domain.entity.UserId
import me.proton.core.label.domain.entity.LabelId

interface ConversationLocalDataSource {

    /**
     * Observe all [Conversation] by [userId] for this [pageKey].
     */
    fun observeConversations(
        userId: UserId,
        pageKey: PageKey,
    ): Flow<List<Conversation>>

    /**
     * Get all [Conversation] by [userId] for this [pageKey].
     */
    suspend fun getConversations(
        userId: UserId,
        pageKey: PageKey,
    ): List<Conversation>

    /**
     * Update or insert [Conversation] related to the same [userId] and [pageKey].
     */
    suspend fun upsertConversations(
        userId: UserId,
        pageKey: PageKey,
        items: List<Conversation>,
    )

    /**
     * Update or insert [Conversation].
     */
    suspend fun upsertConversations(
        items: List<Conversation>,
    )

    /**
     * Delete Conversation(s) for [userId], by [ids].
     */
    suspend fun deleteConversation(
        userId: UserId,
        ids: List<ConversationId>,
    )

    /**
     * Delete all conversations for [userId].
     */
    suspend fun deleteAllConversations(
        userId: UserId,
    )

    /**
     * Mark local data as stale for [userId], by [labelId].
     */
    suspend fun markAsStale(
        userId: UserId,
        labelId: LabelId
    )

    /**
     * Return true if all [Conversation] are considered locally up-to-date according the given [pageKey].
     */
    suspend fun isLocalPageValid(
        userId: UserId,
        pageKey: PageKey,
        items: List<Conversation>,
    ): Boolean

    /**
     * Return clipped [PageKey] according already persisted intervals.
     *
     * Note: Usually used to trim unnecessary interval from the [PageKey] before fetching.
     */
    suspend fun getClippedPageKey(
        userId: UserId,
        pageKey: PageKey,
    ): PageKey
}
