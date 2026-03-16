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
import ch.protonmail.android.mailcommon.domain.model.ConversationCursorError
import ch.protonmail.android.mailcommon.domain.model.CursorId
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcommon.domain.model.UndoSendError
import ch.protonmail.android.mailcommon.domain.repository.ConversationCursor
import ch.protonmail.android.maillabel.domain.model.LabelId
import ch.protonmail.android.mailmessage.domain.model.Message
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailmessage.domain.model.MessageScrollerFetchNewStatus
import ch.protonmail.android.mailmessage.domain.model.PreviousScheduleSendTime
import ch.protonmail.android.mailmessage.domain.model.RemoteMessageId
import ch.protonmail.android.mailmessage.domain.model.SenderImage
import ch.protonmail.android.mailpagination.domain.model.PageKey
import ch.protonmail.android.mailpagination.domain.model.PaginationError
import kotlinx.coroutines.flow.Flow
import me.proton.core.domain.entity.UserId

@Suppress("TooManyFunctions", "ComplexInterface")
interface MessageRepository {

    suspend fun updateShowSpamTrashFilter(showSpamTrash: Boolean)
    suspend fun updateUnreadFilter(filterUnread: Boolean)

    suspend fun terminatePaginator(userId: UserId)

    suspend fun supportsIncludeFilter(): Boolean

    suspend fun getSenderImage(
        userId: UserId,
        address: String,
        bimi: String?
    ): SenderImage?

    /**
     * Load all [Message] from local cache for [userId] filtered by [PageKey].
     */
    suspend fun getMessages(
        userId: UserId,
        pageKey: PageKey = PageKey.DefaultPageKey()
    ): Either<PaginationError, List<Message>>

    /**
     * Gets a [Message] metadata for [userId] from the local storage
     * @return either the [Message] or a [DataError.Local]
     */
    suspend fun getMessage(userId: UserId, messageId: MessageId): Either<DataError, Message>

    /**
     * Gets a [Message] metadata for [userId] from the local storage
     * @return either the [Message] or a [DataError.Local]
     */
    suspend fun observeMessage(userId: UserId, messageId: MessageId): Flow<Either<DataError, Message>>

    /**
     * Gets a [Message] metadata for [userId] from the local storage from a [RemoteMessageId].
     * @return either the [Message] or a [DataError.Local]
     */
    fun observeMessage(userId: UserId, remoteMessageId: RemoteMessageId): Flow<Either<DataError, Message>>

    /**
     * Used to JIT swipe through conversations/ pages with methods exposed to get next and get previous and move to next
     */
    suspend fun getConversationCursor(
        firstPage: CursorId,
        userId: UserId,
        labelId: LabelId
    ): Either<ConversationCursorError, ConversationCursor>

    /**
     * Moves the given [messageIds] from the optional exclusive label to the [toLabel]
     * @param userId the user id of the affected messages
     * @param messageIds the messages to move with their optional exclusive label
     * @param toLabel the label to move the messages to
     */
    suspend fun moveTo(
        userId: UserId,
        messageIds: List<MessageId>,
        toLabel: LabelId
    ): Either<DataError, Unit>

    /**
     * Mark the messages with the given [messageIds] as Starred
     */
    suspend fun starMessages(userId: UserId, messageIds: List<MessageId>): Either<DataError, Unit>

    /**
     * MArk the messages with the given [messageIds] as UnStarred
     */
    suspend fun unStarMessages(userId: UserId, messageIds: List<MessageId>): Either<DataError, Unit>

    /**
     * Set the messages with the given [messageIds] as unread
     */
    suspend fun markUnread(userId: UserId, messageIds: List<MessageId>): Either<DataError, Unit>

    /**
     * Set the messages with the given [messageIds] as read
     */
    suspend fun markRead(userId: UserId, messageIds: List<MessageId>): Either<DataError, Unit>

    /**
     * Delete the message with the given [MessageId]
     */
    suspend fun deleteMessages(
        userId: UserId,
        messageIds: List<MessageId>,
        currentLabelId: LabelId
    ): Either<DataError, Unit>

    /**
     * Delete all messages from the given [labelId]
     */
    suspend fun deleteAllMessagesInLocation(userId: UserId, labelId: LabelId): Either<DataError, Unit>

    /**
     * Delete all messages from the given [labelId]
     */
    fun observeClearLabelOperation(userId: UserId, labelId: LabelId): Flow<Boolean>

    /**
     * Report a message as phishing
     */
    suspend fun reportPhishing(userId: UserId, messageId: MessageId): Either<DataError, Unit>

    suspend fun labelAs(
        userId: UserId,
        messageIds: List<MessageId>,
        selectedLabels: List<LabelId>,
        partiallySelectedLabels: List<LabelId>,
        shouldArchive: Boolean
    ): Either<DataError, Unit>

    suspend fun markMessageAsLegitimate(userId: UserId, messageId: MessageId): Either<DataError, Unit>

    suspend fun unblockSender(userId: UserId, email: String): Either<DataError, Unit>
    suspend fun blockSender(userId: UserId, email: String): Either<DataError, Unit>
    suspend fun isMessageSenderBlocked(userId: UserId, messageId: MessageId): Either<DataError, Boolean>

    suspend fun cancelScheduleSend(
        userId: UserId,
        messageId: MessageId
    ): Either<UndoSendError, PreviousScheduleSendTime>

    fun observeScrollerFetchNewStatus(): Flow<MessageScrollerFetchNewStatus>

}
