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
import ch.protonmail.android.mailcommon.domain.model.DaoError
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailmessage.domain.model.Message
import ch.protonmail.android.mailmessage.domain.model.MessageAttachment
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailmessage.domain.model.MessageWithBody
import ch.protonmail.android.mailpagination.domain.model.PageKey
import kotlinx.coroutines.flow.Flow
import me.proton.core.domain.entity.UserId
import me.proton.core.label.domain.entity.LabelId

@Suppress("TooManyFunctions", "ComplexInterface")
interface MessageLocalDataSource {

    /**
     * Delete all messages for [userId] except [messageIdsToExclude].
     */
    suspend fun deleteAllMessagesExcept(userId: UserId, messageIdsToExclude: List<MessageId>)

    /**
     * Delete all messages for [userId].
     */
    suspend fun deleteAllMessages(userId: UserId)

    /**
     * Delete all search intervals for [userId]
     */
    suspend fun deleteSearchIntervals(userId: UserId)

    /**
     * Delete Message(s) for [userId], by [ids].
     */
    suspend fun deleteMessages(userId: UserId, ids: List<MessageId>)

    /**
     * Delete messages for [userId], by [messageIds]. Returns [DataError.Local] if the operation fails.
     */
    suspend fun deleteMessagesWithId(userId: UserId, messageIds: List<MessageId>): Either<DataError.Local, Unit>

    /**
     * Delete Message(s) for [userId], by [ids], only messages with the given [contextLabelId] will be deleted.
     */
    suspend fun deleteMessagesInConversations(
        userId: UserId,
        conversationIds: List<ConversationId>,
        contextLabelId: LabelId
    ): Either<DataError.Local, Unit>

    /**
     * Delete Message(s) for [userId], only messages with the given [labelId] will be deleted.
     */
    suspend fun deleteMessagesWithLabel(userId: UserId, labelId: LabelId): Either<DataError.Local, Unit>

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
     * Observe search results by [userId] for this [pageKey].
     */
    fun observeSearchResults(userId: UserId, pageKey: PageKey): Flow<List<Message>>

    /**
     * Observe all [Message] by [userId] for given [messageIds].
     */
    fun observeMessages(userId: UserId, messageIds: List<MessageId>): Flow<List<Message>>

    /**
     * Get all [Message] by [userId] for given [conversationIds].
     */
    fun observeMessagesForConversation(userId: UserId, conversationIds: List<ConversationId>): Flow<List<Message>>

    /**
     * Update or insert [Message].
     */
    suspend fun upsertMessage(message: Message): Either<DaoError.UpsertError, Unit>

    /**
     * Update or insert a list of [Message].
     */
    suspend fun upsertMessages(items: List<Message>): Either<DaoError.UpsertError, Unit>

    /**
     * Update or insert [Message] related to the same [userId] and [pageKey].
     */
    suspend fun upsertMessages(
        userId: UserId,
        pageKey: PageKey,
        items: List<Message>
    ): Either<DaoError.UpsertError, Unit>

    /**
     * Observe [MessageWithBody] by [messageId] for this [userId].
     */
    fun observeMessageWithBody(userId: UserId, messageId: MessageId): Flow<MessageWithBody?>

    /**
     * Observe [MessageAttachment] by [messageId] for this [userId].
     */
    fun observeMessageAttachments(userId: UserId, messageId: MessageId): Flow<List<MessageAttachment>>

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
     * Relabels all affected [messageIds] for the given [userId]
     * The operation will first remove the labels of the affected messages and then add the new ones
     * @param userId the user id of the affected messages
     * @param messageIds the ids of the affected messages
     * @param labelIdsToRemove the ids of the labels to remove
     * @param labelIdsToAdd the ids of the labels to add
     * @return either the list of affected messages or a [DataError.Local]
     */
    suspend fun relabelMessages(
        userId: UserId,
        messageIds: List<MessageId>,
        labelIdsToRemove: Set<LabelId> = emptySet(),
        labelIdsToAdd: Set<LabelId> = emptySet()
    ): Either<DataError.Local, List<Message>>

    /**
     * Relabels all affected messages for the given [conversationIds] and [userId]
     */
    suspend fun relabelMessagesInConversations(
        userId: UserId,
        conversationIds: List<ConversationId>,
        labelIdsToRemove: Set<LabelId> = emptySet(),
        labelIdsToAdd: Set<LabelId> = emptySet()
    ): Either<DataError.Local, List<Message>>

    /**
     * Marks as unread the messages for the given [messageIds] related to the same [userId]
     */
    suspend fun markUnread(userId: UserId, messageIds: List<MessageId>): Either<DataError.Local, List<Message>>

    suspend fun markUnreadLastReadMessageInConversations(
        userId: UserId,
        conversationIds: List<ConversationId>,
        contextLabelId: LabelId
    ): Either<DataError.Local, List<Message>>

    /**
     * Marks as read the messages for the given [messageIds] related to the same [userId]
     */
    suspend fun markRead(userId: UserId, messageIds: List<MessageId>): Either<DataError.Local, List<Message>>

    suspend fun markMessagesInConversationsRead(
        userId: UserId,
        conversationIds: List<ConversationId>
    ): Either<DataError.Local, List<Message>>

    suspend fun isMessageRead(userId: UserId, messageId: MessageId): Either<DataError.Local, Boolean>

    suspend fun updateDraftRemoteIds(
        userId: UserId,
        localDraftId: MessageId,
        apiAssignedId: MessageId,
        conversationId: ConversationId
    )
}
