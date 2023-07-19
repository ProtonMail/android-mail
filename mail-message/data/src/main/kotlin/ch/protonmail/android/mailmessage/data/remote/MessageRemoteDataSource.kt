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
import ch.protonmail.android.mailmessage.domain.entity.Message
import ch.protonmail.android.mailmessage.domain.entity.MessageId
import ch.protonmail.android.mailmessage.domain.entity.MessageWithBody
import ch.protonmail.android.mailpagination.domain.model.PageKey
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
    suspend fun getMessage(userId: UserId, messageId: MessageId): MessageWithBody

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
     * Mark message with the given [messageId] as unread
     */
    fun markUnread(userId: UserId, messageId: MessageId)

    /**
     * Mark message with the given [messageId] as read
     */
    fun markRead(userId: UserId, messageId: MessageId)
}
