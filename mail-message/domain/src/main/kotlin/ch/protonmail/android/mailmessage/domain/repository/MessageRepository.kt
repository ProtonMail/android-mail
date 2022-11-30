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
import ch.protonmail.android.mailmessage.domain.entity.Message
import ch.protonmail.android.mailmessage.domain.entity.MessageId
import ch.protonmail.android.mailpagination.domain.model.PageKey
import kotlinx.coroutines.flow.Flow
import me.proton.core.domain.entity.UserId
import me.proton.core.label.domain.entity.LabelId

interface MessageRepository {

    /**
     * Get all [Message] for [userId].
     */
    suspend fun getMessages(
        userId: UserId,
        pageKey: PageKey = PageKey()
    ): List<Message>

    /**
     * Mark local data as stale for [userId], by [labelId].
     */
    suspend fun markAsStale(
        userId: UserId,
        labelId: LabelId
    )

    /**
     * Gets a [Message] metadata for [userId] from the local storage
     * @return either the [Message] or a [DataError.Local]
     */
    fun observeCachedMessage(
        userId: UserId,
        messageId: MessageId
    ): Flow<Either<DataError.Local, Message>>

    /**
     * Get all the [Message]s metadata for a given [ConversationId], for [userId] from the local storage
     */
    fun observeCachedMessages(
        userId: UserId,
        conversationId: ConversationId
    ): Flow<Either<DataError.Local, Nel<Message>>>

    /**
     * Adds the given [labelId] to the message with the given [messageId]
     */
    suspend fun addLabel(
        userId: UserId,
        messageId: MessageId,
        labelId: LabelId
    ): Either<DataError.Local, Message>

    /**
     * Removes the given [labelId] to the message with the given [messageId]
     */
    suspend fun removeLabel(
        userId: UserId,
        messageId: MessageId,
        labelId: LabelId
    ): Either<DataError.Local, Message>

    /**
     * Moves the message for the given [messageId] to trash
     */
    suspend fun moveToTrash(
        userId: UserId,
        messageId: MessageId
    ): Either<DataError.Local, Message>
}
