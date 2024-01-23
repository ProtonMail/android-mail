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

package ch.protonmail.android.mailmessage.data.remote

import arrow.core.Either
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailmessage.domain.model.DecryptedMessageBody
import ch.protonmail.android.mailmessage.domain.model.Message
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailmessage.domain.model.MessageWithBody
import ch.protonmail.android.mailpagination.domain.model.PageKey
import kotlinx.coroutines.flow.Flow
import me.proton.core.domain.entity.UserId
import me.proton.core.label.domain.entity.LabelId

interface MessageRemoteDataSource {

    /**
     * Get all [Message] for [userId].
     */
    suspend fun getMessages(userId: UserId, pageKey: PageKey): Either<DataError.Remote, List<Message>>

    /**
     * Get a [Message] for [userId], by [messageId].
     */
    suspend fun getMessageOrThrow(userId: UserId, messageId: MessageId): MessageWithBody

    /**
     * Adds [labelIds] to the given [MessageId]
     */
    fun addLabelsToMessages(
        userId: UserId,
        messageIds: List<MessageId>,
        labelIds: List<LabelId>
    )

    /**
     * Removes [labelIds] from the given [MessageId]
     */
    fun removeLabelsFromMessages(
        userId: UserId,
        messageIds: List<MessageId>,
        labelIds: List<LabelId>
    )

    /**
     * Mark messages with the given [messageIds] as unread
     */
    fun markUnread(userId: UserId, messageIds: List<MessageId>)

    /**
     * Mark messages with the given [messageIds] as read
     */
    fun markRead(userId: UserId, messageIds: List<MessageId>)
    suspend fun getMessage(userId: UserId, messageId: MessageId): Either<DataError, MessageWithBody>

    /**
     * Delete messages with the given [messageIds]
     * @param currentLabelId the current label id of the messages (only valid for system folders)
     */
    fun deleteMessages(
        userId: UserId,
        messageIds: List<MessageId>,
        currentLabelId: LabelId
    )

    /**
     * Delete all messages from the given [labelId]
     */
    fun clearLabel(userId: UserId, labelId: LabelId)

    /**
     * Observe if the [ClearLabelWorker] is enqueued or running for the given [userId] and [labelId]
     */
    fun observeClearWorkerIsEnqueuedOrRunning(userId: UserId, labelId: LabelId): Flow<Boolean>

    /**
     * Report a message as phishing
     */
    suspend fun reportPhishing(
        userId: UserId,
        decryptedMessageBody: DecryptedMessageBody
    ): Either<DataError.Remote, Unit>
}
