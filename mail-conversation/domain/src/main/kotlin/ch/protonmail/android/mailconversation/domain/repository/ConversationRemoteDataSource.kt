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
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailconversation.domain.entity.Conversation
import ch.protonmail.android.mailconversation.domain.entity.ConversationWithContext
import ch.protonmail.android.mailconversation.domain.entity.ConversationWithMessages
import ch.protonmail.android.mailpagination.domain.model.PageKey
import kotlinx.coroutines.flow.Flow
import me.proton.core.domain.entity.UserId
import me.proton.core.label.domain.entity.LabelId

interface ConversationRemoteDataSource {

    /**
     * Get all [Conversation] for [userId].
     */
    suspend fun getConversations(
        userId: UserId,
        pageKey: PageKey
    ): Either<DataError.Remote, List<ConversationWithContext>>

    suspend fun getConversationWithMessages(userId: UserId, conversationId: ConversationId): ConversationWithMessages

    /**
     * Add [labelId] to the given [conversationIds]
     */
    fun addLabel(
        userId: UserId,
        conversationIds: List<ConversationId>,
        labelId: LabelId
    )

    /**
     * Add the provided [labelIds] to the given [conversationIds]
     */
    fun addLabels(
        userId: UserId,
        conversationIds: List<ConversationId>,
        labelIds: List<LabelId>
    )

    /**
     * Remove [labelId] from the given [conversationIds]
     */
    fun removeLabel(
        userId: UserId,
        conversationIds: List<ConversationId>,
        labelId: LabelId
    )

    /**
     * Remove the provided [labelIds] from the given [conversationIds]
     */
    fun removeLabels(
        userId: UserId,
        conversationIds: List<ConversationId>,
        labelIds: List<LabelId>
    )

    suspend fun markUnread(
        userId: UserId,
        conversationIds: List<ConversationId>,
        contextLabelId: LabelId
    )

    suspend fun markRead(userId: UserId, conversationIds: List<ConversationId>)

    /**
     * Delete conversations with the given [conversationIds]
     * @param currentLabelId the current label id of the conversations (only valid for system folders)
     */
    fun deleteConversations(
        userId: UserId,
        conversationIds: List<ConversationId>,
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
}
