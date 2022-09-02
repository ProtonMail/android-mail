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

package ch.protonmail.android.mailmessage.domain.repository

import ch.protonmail.android.mailmessage.domain.entity.Message
import ch.protonmail.android.mailmessage.domain.entity.MessageId
import ch.protonmail.android.mailpagination.domain.entity.PageKey
import kotlinx.coroutines.flow.Flow
import me.proton.core.domain.entity.UserId
import me.proton.core.label.domain.entity.LabelId

interface MessageLocalDataSource {

    /**
     * Observe all [Message] by [userId] for this [pageKey].
     */
    fun observeMessages(
        userId: UserId,
        pageKey: PageKey
    ): Flow<List<Message>>

    /**
     * Get all [Message] by [userId] for this [pageKey].
     */
    suspend fun getMessages(
        userId: UserId,
        pageKey: PageKey
    ): List<Message>

    /**
     * Update or insert [Message] related to the same [userId] and [pageKey].
     */
    suspend fun upsertMessages(
        userId: UserId,
        pageKey: PageKey,
        items: List<Message>
    )

    /**
     * Update or insert [Message].
     */
    suspend fun upsertMessages(
        items: List<Message>
    )

    /**
     * Delete Message(s) for [userId], by [ids].
     */
    suspend fun deleteMessage(
        userId: UserId,
        ids: List<MessageId>
    )

    /**
     * Delete all messages for [userId].
     */
    suspend fun deleteAllMessages(
        userId: UserId
    )

    /**
     * Mark local data as stale for [userId], by [labelId].
     */
    suspend fun markAsStale(
        userId: UserId,
        labelId: LabelId
    )

    /**
     * Return true if all [Message] are considered locally up-to-date according the given [pageKey].
     */
    suspend fun isLocalPageValid(
        userId: UserId,
        pageKey: PageKey,
        items: List<Message>
    ): Boolean

    /**
     * Return clipped [PageKey] according already persisted intervals.
     *
     * Note: Usually used to trim unnecessary interval from the [PageKey] before fetching.
     */
    suspend fun getClippedPageKey(
        userId: UserId,
        pageKey: PageKey
    ): PageKey

    /**
     * Observe [Message] by [UserId] and [MessageId]
     */
    fun observeMessage(
        userId: UserId,
        messageId: MessageId
    ): Flow<Message?>
}
