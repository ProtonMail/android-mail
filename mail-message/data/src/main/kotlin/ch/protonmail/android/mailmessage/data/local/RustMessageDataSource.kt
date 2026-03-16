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
import ch.protonmail.android.mailcommon.data.mapper.LocalConversationId
import ch.protonmail.android.mailcommon.data.mapper.LocalLabelAsAction
import ch.protonmail.android.mailcommon.data.mapper.LocalLabelId
import ch.protonmail.android.mailcommon.data.mapper.LocalMessageId
import ch.protonmail.android.mailcommon.data.mapper.LocalMessageMetadata
import ch.protonmail.android.mailcommon.data.mapper.RemoteMessageId
import ch.protonmail.android.mailcommon.data.wrapper.ConversationCursor
import ch.protonmail.android.mailcommon.domain.model.ConversationCursorError
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcommon.domain.model.UndoSendError
import ch.protonmail.android.mailcommon.domain.model.UndoableOperation
import ch.protonmail.android.maillabel.domain.model.LabelId
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailmessage.domain.model.MessageScrollerFetchNewStatus
import ch.protonmail.android.mailmessage.domain.model.PreviousScheduleSendTime
import ch.protonmail.android.mailpagination.domain.model.PageKey
import ch.protonmail.android.mailpagination.domain.model.PaginationError
import kotlinx.coroutines.flow.Flow
import me.proton.core.domain.entity.UserId
import uniffi.mail_uniffi.AllListActions
import uniffi.mail_uniffi.AllMessageActions
import uniffi.mail_uniffi.Message
import uniffi.mail_uniffi.MessageActionSheet
import uniffi.mail_uniffi.MoveAction
import uniffi.mail_uniffi.ThemeOpts

@Suppress("ComplexInterface", "TooManyFunctions")
interface RustMessageDataSource {

    suspend fun updateShowSpamTrashFilter(showSpamTrash: Boolean)

    suspend fun updateUnreadFilter(filterUnread: Boolean)

    suspend fun terminatePaginator(userId: UserId)

    suspend fun supportsIncludeFilter(): Boolean

    suspend fun getMessage(userId: UserId, messageId: LocalMessageId): Either<DataError, LocalMessageMetadata>
    suspend fun getMessage(userId: UserId, messageId: RemoteMessageId): Either<DataError, LocalMessageMetadata>

    suspend fun getMessages(userId: UserId, pageKey: PageKey): Either<PaginationError, List<Message>>

    suspend fun observeMessage(userId: UserId, messageId: LocalMessageId): Flow<Either<DataError, LocalMessageMetadata>>

    suspend fun getConversationCursor(
        firstPage: LocalConversationId,
        userId: UserId,
        labelId: LabelId
    ): Either<ConversationCursorError, ConversationCursor>

    suspend fun getSenderImage(
        userId: UserId,
        address: String,
        bimi: String?
    ): String?

    suspend fun markRead(userId: UserId, messages: List<LocalMessageId>): Either<DataError, Unit>
    suspend fun markUnread(userId: UserId, messages: List<LocalMessageId>): Either<DataError, Unit>

    suspend fun starMessages(userId: UserId, messages: List<LocalMessageId>): Either<DataError, Unit>
    suspend fun unStarMessages(userId: UserId, messages: List<LocalMessageId>): Either<DataError, Unit>

    suspend fun moveMessages(
        userId: UserId,
        messageIds: List<LocalMessageId>,
        toLabelId: LocalLabelId
    ): Either<DataError, UndoableOperation>

    suspend fun getAvailableActions(
        userId: UserId,
        labelId: LocalLabelId,
        messageId: LocalMessageId,
        themeOpts: ThemeOpts
    ): Either<DataError, MessageActionSheet>

    suspend fun getAvailableSystemMoveToActions(
        userId: UserId,
        labelId: LocalLabelId,
        messageIds: List<LocalMessageId>
    ): Either<DataError, List<MoveAction.SystemFolder>>

    suspend fun getAvailableLabelAsActions(
        userId: UserId,
        labelId: LocalLabelId,
        messageIds: List<LocalMessageId>
    ): Either<DataError, List<LocalLabelAsAction>>

    suspend fun getAllAvailableListBottomBarActions(
        userId: UserId,
        labelId: LocalLabelId,
        messageIds: List<LocalMessageId>
    ): Either<DataError, AllListActions>

    suspend fun getAllAvailableBottomBarActions(
        userId: UserId,
        labelId: LocalLabelId,
        messageId: LocalMessageId,
        themeOpts: ThemeOpts
    ): Either<DataError, AllMessageActions>

    suspend fun deleteMessages(userId: UserId, messageIds: List<LocalMessageId>): Either<DataError, Unit>

    suspend fun labelMessages(
        userId: UserId,
        messageIds: List<LocalMessageId>,
        selectedLabelIds: List<LocalLabelId>,
        partiallySelectedLabelIds: List<LocalLabelId>,
        shouldArchive: Boolean
    ): Either<DataError, UndoableOperation>

    suspend fun markMessageAsLegitimate(userId: UserId, messageId: LocalMessageId): Either<DataError, Unit>

    suspend fun blockSender(userId: UserId, email: String): Either<DataError, Unit>

    suspend fun unblockSender(userId: UserId, email: String): Either<DataError, Unit>

    suspend fun isMessageSenderBlocked(userId: UserId, messageId: LocalMessageId): Either<DataError, Boolean>

    suspend fun reportPhishing(userId: UserId, messageId: LocalMessageId): Either<DataError, Unit>

    suspend fun deleteAllMessagesInLocation(userId: UserId, labelId: LocalLabelId): Either<DataError, Unit>

    suspend fun cancelScheduleSendMessage(
        userId: UserId,
        messageId: MessageId
    ): Either<UndoSendError, PreviousScheduleSendTime>

    fun observeScrollerFetchNewStatus(): Flow<MessageScrollerFetchNewStatus>

}
