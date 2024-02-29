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

import arrow.core.Either
import arrow.core.Nel
import ch.protonmail.android.mailcommon.domain.model.ConversationId
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailmessage.domain.model.DecryptedMessageBody
import ch.protonmail.android.mailmessage.domain.model.Message
import ch.protonmail.android.mailmessage.domain.model.MessageAttachment
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailmessage.domain.model.MessageWithBody
import ch.protonmail.android.mailmessage.domain.model.RefreshedMessageWithBody
import ch.protonmail.android.mailpagination.domain.model.PageKey
import kotlinx.coroutines.flow.Flow
import me.proton.core.domain.entity.UserId
import me.proton.core.label.domain.entity.LabelId

@Suppress("TooManyFunctions", "ComplexInterface")
interface MessageRepository {

    /**
     * Load all [Message] from local cache for [userId] filtered by [PageKey].
     */
    suspend fun getLocalMessages(userId: UserId, pageKey: PageKey = PageKey()): List<Message>

    suspend fun getLocalMessages(userId: UserId, messages: List<MessageId>): List<Message>

    /**
     * Return true if all [Message] are considered locally valid according the given [pageKey].
     */
    suspend fun isLocalPageValid(
        userId: UserId,
        pageKey: PageKey,
        items: List<Message>
    ): Boolean

    /**
     * Fetch all [Message] from network for [userId] filtered by [PageKey].
     */
    suspend fun getRemoteMessages(userId: UserId, pageKey: PageKey): Either<DataError.Remote, List<Message>>

    /**
     * Mark local data as stale for [userId], by [labelId].
     */
    suspend fun markAsStale(userId: UserId, labelId: LabelId)

    /**
     * Gets a [Message] metadata for [userId] from the local storage
     * @return either the [Message] or a [DataError.Local]
     */
    fun observeCachedMessage(userId: UserId, messageId: MessageId): Flow<Either<DataError.Local, Message>>

    /**
     * Gets a list of [Message] metadata for [userId] from the local storage
     * @return either the [Message] or a [DataError.Local]
     */
    fun observeCachedMessages(userId: UserId, messageIds: List<MessageId>): Flow<Either<DataError.Local, List<Message>>>

    /**
     * Gets a list of [Message] metadata for [userId] and [conversationIds] from the local storage
     * @return either the [Message] or a [DataError.Local]
     */
    fun observeCachedMessagesForConversations(
        userId: UserId,
        conversationIds: List<ConversationId>
    ): Flow<List<Message>>

    /**
     * Get all the [Message]s metadata for a given [ConversationId], for [userId] from the local storage
     */
    fun observeCachedMessages(
        userId: UserId,
        conversationId: ConversationId
    ): Flow<Either<DataError.Local, Nel<Message>>>

    /**
     * Observe the [MessageWithBody] for a given [MessageId], for [userId]
     */
    fun observeMessageWithBody(userId: UserId, messageId: MessageId): Flow<Either<DataError, MessageWithBody>>

    /**
     * Observe the list of [MessageAttachment] for a given [MessageId], for [userId]
     */
    fun observeMessageAttachments(userId: UserId, messageId: MessageId): Flow<List<MessageAttachment>>

    /**
     * Get the [MessageWithBody] for a given [MessageId], for [userId]
     */
    suspend fun getMessageWithBody(userId: UserId, messageId: MessageId): Either<DataError, MessageWithBody>

    /**
     * Get the [MessageWithBody] for a given [MessageId] and [userId] from the local storage.
     */
    suspend fun getLocalMessageWithBody(userId: UserId, messageId: MessageId): MessageWithBody?

    /**
     * Get the [MessageWithBody] for a given [MessageId] and [userId] from the remote storage
     * and stores it locally. When getting from remote fails, returns any existing local one
     */
    suspend fun getRefreshedMessageWithBody(userId: UserId, messageId: MessageId): RefreshedMessageWithBody?

    suspend fun upsertMessageWithBody(userId: UserId, messageWithBody: MessageWithBody): Boolean

    /**
     * Moves the given [messageId] from the optional exclusive label to the [toLabel]
     * @param userId the user id of the affected messages
     * @param messageId the message to be moved
     * @param fromLabel the message's optional exclusive label
     * @param toLabel the label to move the messages to
     */
    suspend fun moveTo(
        userId: UserId,
        messageId: MessageId,
        fromLabel: LabelId?,
        toLabel: LabelId
    ): Either<DataError.Local, Message>

    /**
     * Moves the given [messageIds] from the optional exclusive label to the [toLabel]
     * @param userId the user id of the affected messages
     * @param messageWithExclusiveLabel the messages to move with their optional exclusive label
     * @param toLabel the label to move the messages to
     */
    suspend fun moveTo(
        userId: UserId,
        messageWithExclusiveLabel: Map<MessageId, LabelId?>,
        toLabel: LabelId
    ): Either<DataError.Local, List<Message>>

    /**
     * Set the message with the given [messageId] as unread
     */
    suspend fun markUnread(userId: UserId, messageId: MessageId): Either<DataError.Local, Message>

    /**
     * Set the messages with the given [messageIds] as unread
     */
    suspend fun markUnread(userId: UserId, messageIds: List<MessageId>): Either<DataError.Local, List<Message>>

    /**
     * Set the message with the given [messageId] as read
     */
    suspend fun markRead(userId: UserId, messageId: MessageId): Either<DataError.Local, Message>

    /**
     * Set the messages with the given [messageIds] as read
     */
    suspend fun markRead(userId: UserId, messageIds: List<MessageId>): Either<DataError.Local, List<Message>>

    suspend fun isMessageRead(userId: UserId, messageId: MessageId): Either<DataError.Local, Boolean>

    /**
     * Removes [labelsToBeRemoved] and adds [labelsToBeAdded] from the message with the given [messageId]
     */
    suspend fun relabel(
        userId: UserId,
        messageId: MessageId,
        labelsToBeRemoved: List<LabelId> = emptyList(),
        labelsToBeAdded: List<LabelId> = emptyList()
    ): Either<DataError.Local, Message>

    /**
     * Removes [labelsToBeRemoved] and adds [labelsToBeAdded] from the messages with the given [messageIds]
     */
    suspend fun relabel(
        userId: UserId,
        messageIds: List<MessageId>,
        labelsToBeRemoved: List<LabelId> = emptyList(),
        labelsToBeAdded: List<LabelId> = emptyList()
    ): Either<DataError.Local, List<Message>>

    suspend fun updateDraftRemoteIds(
        userId: UserId,
        localDraftId: MessageId,
        apiAssignedId: MessageId,
        conversationId: ConversationId
    )

    /**
     * Delete the message with the given [messageId]
     */
    suspend fun deleteMessages(
        userId: UserId,
        messageIds: List<MessageId>,
        currentLabelId: LabelId
    ): Either<DataError, Unit>

    /**
     * Delete all messages from the given [labelId]
     */
    suspend fun deleteMessages(userId: UserId, labelId: LabelId)

    /**
     * Delete all messages from the given [labelId]
     */
    fun observeClearLabelOperation(userId: UserId, labelId: LabelId): Flow<Boolean>

    /**
     * Report a message as phishing
     */
    suspend fun reportPhishing(userId: UserId, decryptedMessageBody: DecryptedMessageBody): Either<DataError, Unit>
}
