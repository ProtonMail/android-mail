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
import ch.protonmail.android.mailcommon.domain.model.ConversationCursorError
import ch.protonmail.android.mailcommon.domain.model.ConversationId
import ch.protonmail.android.mailcommon.domain.model.CursorId
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcommon.domain.repository.ConversationCursor
import ch.protonmail.android.mailconversation.domain.entity.Conversation
import ch.protonmail.android.mailconversation.domain.entity.ConversationDetailEntryPoint
import ch.protonmail.android.mailconversation.domain.entity.ConversationError
import ch.protonmail.android.mailconversation.domain.entity.ConversationWithMessages
import ch.protonmail.android.mailconversation.domain.model.ConversationScrollerFetchNewStatus
import ch.protonmail.android.maillabel.domain.model.LabelId
import ch.protonmail.android.mailmessage.domain.model.ConversationMessages
import ch.protonmail.android.mailpagination.domain.model.PageKey
import ch.protonmail.android.mailpagination.domain.model.PaginationError
import kotlinx.coroutines.flow.Flow
import me.proton.core.domain.entity.UserId

@Suppress("TooManyFunctions", "ComplexInterface")
interface ConversationRepository {

    suspend fun updateShowSpamTrashFilter(showSpamTrash: Boolean)

    suspend fun updateUnreadFilter(filterUnread: Boolean)

    suspend fun terminatePaginator(userId: UserId)

    suspend fun supportsIncludeFilter(): Boolean

    /**
     * Load all [Conversation] from local cache for [userId].
     */
    suspend fun getConversations(
        userId: UserId,
        pageKey: PageKey.DefaultPageKey = PageKey.DefaultPageKey()
    ): Either<PaginationError, List<Conversation>>

    suspend fun observeConversation(
        userId: UserId,
        id: ConversationId,
        labelId: LabelId,
        entryPoint: ConversationDetailEntryPoint,
        showAllMessages: Boolean
    ): Flow<Either<ConversationError, Conversation>>

    suspend fun observeConversationMessages(
        userId: UserId,
        conversationId: ConversationId,
        labelId: LabelId,
        entryPoint: ConversationDetailEntryPoint,
        showAllMessages: Boolean
    ): Flow<Either<ConversationError, ConversationMessages>>

    suspend fun observeConversationWithMessages(
        userId: UserId,
        conversationId: ConversationId,
        labelId: LabelId,
        entryPoint: ConversationDetailEntryPoint,
        showAllMessages: Boolean
    ): Flow<Either<ConversationError, ConversationWithMessages>>

    /**
     * Used to JIT swipe through conversations/ pages with methods exposed to get next and get previous and move to next
     */
    suspend fun getConversationCursor(
        firstPage: CursorId,
        userId: UserId,
        labelId: LabelId
    ): Either<ConversationCursorError, ConversationCursor>

    suspend fun move(
        userId: UserId,
        conversationIds: List<ConversationId>,
        toLabelId: LabelId
    ): Either<DataError, List<Conversation>>

    suspend fun markUnread(
        userId: UserId,
        labelId: LabelId,
        conversationIds: List<ConversationId>
    ): Either<DataError, Unit>

    suspend fun markRead(
        userId: UserId,
        labelId: LabelId,
        conversationIds: List<ConversationId>
    ): Either<DataError, Unit>

    suspend fun star(userId: UserId, conversationIds: List<ConversationId>): Either<DataError, List<Conversation>>

    suspend fun unStar(userId: UserId, conversationIds: List<ConversationId>): Either<DataError, List<Conversation>>

    suspend fun labelAs(
        userId: UserId,
        conversationIds: List<ConversationId>,
        selectedLabels: List<LabelId>,
        partiallySelectedLabels: List<LabelId>,
        shouldArchive: Boolean
    ): Either<DataError, Unit>

    suspend fun deleteConversations(userId: UserId, conversationIds: List<ConversationId>): Either<DataError, Unit>

    fun observeScrollerFetchNewStatus(): Flow<ConversationScrollerFetchNewStatus>

}
