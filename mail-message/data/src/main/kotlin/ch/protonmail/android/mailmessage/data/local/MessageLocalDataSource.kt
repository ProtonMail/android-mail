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

package ch.protonmail.android.mailmessage.data.local

import arrow.core.Either
import ch.protonmail.android.mailcommon.domain.model.ConversationId
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailmessage.domain.entity.Message
import ch.protonmail.android.mailmessage.domain.entity.MessageId
import ch.protonmail.android.mailmessage.domain.entity.MessageWithBody
import ch.protonmail.android.mailpagination.domain.model.PageKey
import kotlinx.coroutines.flow.Flow
import me.proton.core.domain.entity.UserId
import me.proton.core.label.domain.entity.LabelId

@Suppress("TooManyFunctions", "ComplexInterface")
interface MessageLocalDataSource {

    /**
     * Delete all messages for [userId].
     */
    suspend fun deleteAllMessages(userId: UserId)

    /**
     * Delete Message(s) for [userId], by [ids].
     */
    suspend fun deleteMessages(userId: UserId, ids: List<MessageId>)

    /**
     * Return clipped [PageKey] according already persisted intervals.
     *
     * Note: Usually used to trim unnecessary interval from the [PageKey] before fetching.
     */
    suspend fun getClippedPageKey(userId: UserId, pageKey: PageKey): PageKey?

    /**
     * Get all [Message] by [userId] for this [pageKey].
     */
    suspend fun getMessages(userId: UserId, pageKey: PageKey): List<Message>

    /**
     * Return true if all [Message] are considered locally up-to-date according the given [pageKey].
     */
    suspend fun isLocalPageValid(
        userId: UserId,
        pageKey: PageKey,
        items: List<Message>
    ): Boolean

    /**
     * Mark local data as stale for [userId], by [labelId].
     */
    suspend fun markAsStale(userId: UserId, labelId: LabelId)

    /**
     * Observe [Message] by [UserId] and [MessageId]
     */
    fun observeMessage(userId: UserId, messageId: MessageId): Flow<Message?>

    /**
     * Observe all [Message] by [userId] for given [conversationId].
     */
    fun observeMessages(userId: UserId, conversationId: ConversationId): Flow<List<Message>>

    /**
     * Observe all [Message] by [userId] for this [pageKey].
     */
    fun observeMessages(userId: UserId, pageKey: PageKey): Flow<List<Message>>

    /**
     * Update or insert [Message].
     */
    suspend fun upsertMessage(message: Message)

    /**
     * Update or insert a list of [Message].
     */
    suspend fun upsertMessages(items: List<Message>)

    /**
     * Update or insert [Message] related to the same [userId] and [pageKey].
     */
    suspend fun upsertMessages(
        userId: UserId,
        pageKey: PageKey,
        items: List<Message>
    )

    /**
     * Observe [MessageWithBody] by [messageId] for this [userId].
     */
    fun observeMessageWithBody(userId: UserId, messageId: MessageId): Flow<MessageWithBody?>

    /**
     * Update or insert [MessageWithBody] for this [userId].
     */
    suspend fun upsertMessageWithBody(userId: UserId, messageWithBody: MessageWithBody)

    /**
     * Adds [labelId] to given [messageId] related to the same [userId]
     */
    suspend fun addLabel(
        userId: UserId,
        messageId: MessageId,
        labelId: LabelId
    ): Either<DataError.Local, Message>

    /**
     * Adds all [labelIds] to given [messageId] related to the same [userId]
     */
    suspend fun addLabels(
        userId: UserId,
        messageId: MessageId,
        labelIds: List<LabelId>
    ): Either<DataError.Local, Message>

    /**
     * Removes [labelId] to given [messageId] related to the same [userId]
     */
    suspend fun removeLabel(
        userId: UserId,
        messageId: MessageId,
        labelId: LabelId
    ): Either<DataError.Local, Message>

    /**
     * Removes all [labelIds] to given [messageId] related to the same [userId]
     */
    suspend fun removeLabels(
        userId: UserId,
        messageId: MessageId,
        labelIds: List<LabelId>
    ): Either<DataError.Local, Message>

    /**
     * Marks as unread the message for the given [messageId] related to the same [userId]
     */
    suspend fun markUnread(userId: UserId, messageId: MessageId): Either<DataError.Local, Message>

    /**
     * Marks as read the message for the given [messageId] related to the same [userId]
     */
    suspend fun markRead(userId: UserId, messageId: MessageId): Either<DataError.Local, Message>

    suspend fun isMessageRead(userId: UserId, messageId: MessageId): Either<DataError.Local, Boolean>
}
