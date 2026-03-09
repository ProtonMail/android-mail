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

package ch.protonmail.android.mailconversation.data.local

import arrow.core.Either
import ch.protonmail.android.mailcommon.data.mapper.LocalConversation
import ch.protonmail.android.mailcommon.data.mapper.LocalConversationId
import ch.protonmail.android.mailcommon.data.mapper.LocalLabelId
import ch.protonmail.android.mailcommon.data.wrapper.ConversationCursor
import ch.protonmail.android.mailcommon.domain.model.ConversationCursorError
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcommon.domain.model.UndoableOperation
import ch.protonmail.android.mailconversation.domain.entity.ConversationDetailEntryPoint
import ch.protonmail.android.mailconversation.domain.entity.ConversationError
import ch.protonmail.android.mailconversation.domain.model.ConversationScrollerFetchNewStatus
import ch.protonmail.android.mailmessage.data.model.LocalConversationWithMessages
import ch.protonmail.android.mailpagination.domain.model.PageKey
import ch.protonmail.android.mailpagination.domain.model.PaginationError
import kotlinx.coroutines.flow.Flow
import me.proton.core.domain.entity.UserId
import uniffi.mail_uniffi.AllConversationActions
import uniffi.mail_uniffi.AllListActions
import uniffi.mail_uniffi.ConversationActionSheet
import uniffi.mail_uniffi.LabelAsAction
import uniffi.mail_uniffi.MoveAction

@SuppressWarnings("ComplexInterface", "TooManyFunctions")
interface RustConversationDataSource {

    suspend fun updateShowSpamTrashFilter(showSpamTrash: Boolean)

    suspend fun updateUnreadFilter(filterUnread: Boolean)

    suspend fun terminatePaginator(userId: UserId)

    suspend fun supportsIncludeFilter(): Boolean

    suspend fun observeConversationWithMessages(
        userId: UserId,
        conversationId: LocalConversationId,
        labelId: LocalLabelId,
        entryPoint: ConversationDetailEntryPoint,
        showAllMessages: Boolean
    ): Flow<Either<ConversationError, LocalConversationWithMessages>>

    suspend fun getConversations(
        userId: UserId,
        pageKey: PageKey.DefaultPageKey
    ): Either<PaginationError, List<LocalConversation>>

    suspend fun deleteConversations(
        userId: UserId,
        conversations: List<LocalConversationId>
    ): Either<DataError.Local, Unit>

    suspend fun markRead(
        userId: UserId,
        labelId: LocalLabelId,
        conversations: List<LocalConversationId>
    )

    suspend fun markUnread(
        userId: UserId,
        labelId: LocalLabelId,
        conversations: List<LocalConversationId>
    )

    suspend fun starConversations(userId: UserId, conversations: List<LocalConversationId>)
    suspend fun unStarConversations(userId: UserId, conversations: List<LocalConversationId>)

    suspend fun moveConversations(
        userId: UserId,
        conversationIds: List<LocalConversationId>,
        toLabelId: LocalLabelId
    ): Either<DataError, UndoableOperation>

    fun getSenderImage(address: String, bimi: String?): ByteArray?

    suspend fun getAvailableBottomSheetActions(
        userId: UserId,
        labelId: LocalLabelId,
        conversationId: LocalConversationId
    ): Either<DataError, ConversationActionSheet>

    suspend fun getAvailableSystemMoveToActions(
        userId: UserId,
        labelId: LocalLabelId,
        conversationIds: List<LocalConversationId>
    ): Either<DataError, List<MoveAction.SystemFolder>>

    suspend fun getAvailableLabelAsActions(
        userId: UserId,
        labelId: LocalLabelId,
        conversationIds: List<LocalConversationId>
    ): Either<DataError, List<LabelAsAction>>

    suspend fun getAllAvailableListBottomBarActions(
        userId: UserId,
        labelId: LocalLabelId,
        conversationIds: List<LocalConversationId>
    ): Either<DataError, AllListActions>

    suspend fun getAllAvailableBottomBarActions(
        userId: UserId,
        labelId: LocalLabelId,
        conversationId: LocalConversationId
    ): Either<DataError, AllConversationActions>

    suspend fun labelConversations(
        userId: UserId,
        conversationIds: List<LocalConversationId>,
        selectedLabelIds: List<LocalLabelId>,
        partiallySelectedLabelIds: List<LocalLabelId>,
        shouldArchive: Boolean
    ): Either<DataError, UndoableOperation>

    suspend fun getConversationCursor(
        userId: UserId,
        firstPage: LocalConversationId
    ): Either<ConversationCursorError, ConversationCursor>

    fun observeScrollerFetchNewStatus(): Flow<ConversationScrollerFetchNewStatus>
}
