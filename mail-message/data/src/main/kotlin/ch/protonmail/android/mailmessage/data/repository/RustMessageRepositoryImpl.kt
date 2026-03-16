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

package ch.protonmail.android.mailmessage.data.repository

import java.io.File
import arrow.core.Either
import ch.protonmail.android.mailcommon.data.mapper.LocalConversationId
import ch.protonmail.android.mailcommon.data.repository.RustConversationCursorImpl
import ch.protonmail.android.mailcommon.domain.model.ConversationCursorError
import ch.protonmail.android.mailcommon.domain.model.CursorId
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcommon.domain.model.UndoSendError
import ch.protonmail.android.mailcommon.domain.repository.ConversationCursor
import ch.protonmail.android.mailcommon.domain.repository.UndoRepository
import ch.protonmail.android.maillabel.data.mapper.toLocalLabelId
import ch.protonmail.android.maillabel.domain.model.LabelId
import ch.protonmail.android.mailmessage.data.local.RustMessageDataSource
import ch.protonmail.android.mailmessage.data.mapper.toLocalConversationId
import ch.protonmail.android.mailmessage.data.mapper.toLocalMessageId
import ch.protonmail.android.mailmessage.data.mapper.toMessage
import ch.protonmail.android.mailmessage.data.mapper.toRemoteMessageId
import ch.protonmail.android.mailmessage.domain.model.Message
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailmessage.domain.model.MessageScrollerFetchNewStatus
import ch.protonmail.android.mailmessage.domain.model.PreviousScheduleSendTime
import ch.protonmail.android.mailmessage.domain.model.RemoteMessageId
import ch.protonmail.android.mailmessage.domain.model.SenderImage
import ch.protonmail.android.mailmessage.domain.repository.MessageRepository
import ch.protonmail.android.mailpagination.domain.model.PageKey
import ch.protonmail.android.mailpagination.domain.model.PaginationError
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import me.proton.core.domain.entity.UserId
import timber.log.Timber
import javax.inject.Inject

@Suppress("TooManyFunctions")
class RustMessageRepositoryImpl @Inject constructor(
    private val rustMessageDataSource: RustMessageDataSource,
    private val undoRepository: UndoRepository
) : MessageRepository {

    override suspend fun updateShowSpamTrashFilter(showSpamTrash: Boolean) =
        rustMessageDataSource.updateShowSpamTrashFilter(showSpamTrash)

    override suspend fun updateUnreadFilter(filterUnread: Boolean) =
        rustMessageDataSource.updateUnreadFilter(filterUnread)

    override suspend fun terminatePaginator(userId: UserId) {
        Timber.d("Terminating message paginator for userId: $userId")
        rustMessageDataSource.terminatePaginator(userId)
    }

    override suspend fun supportsIncludeFilter() = rustMessageDataSource.supportsIncludeFilter()

    override suspend fun getSenderImage(
        userId: UserId,
        address: String,
        bimi: String?
    ): SenderImage? {
        return rustMessageDataSource.getSenderImage(userId, address, bimi)?.let { imageString ->
            SenderImage(File(imageString))
        }
    }

    override suspend fun getMessages(userId: UserId, pageKey: PageKey): Either<PaginationError, List<Message>> {
        return rustMessageDataSource.getMessages(userId, pageKey)
            .map { list -> list.map { it.toMessage() } }
    }

    override suspend fun getMessage(userId: UserId, messageId: MessageId): Either<DataError, Message> =
        rustMessageDataSource.getMessage(userId, messageId.toLocalMessageId())
            .map { it.toMessage() }

    override suspend fun observeMessage(userId: UserId, messageId: MessageId): Flow<Either<DataError, Message>> =
        rustMessageDataSource.observeMessage(userId, messageId.toLocalMessageId())
            .map { either -> either.map { it.toMessage() } }

    @Deprecated(
        message = "Observing is faked! This won't reflect changes to the message after the first emission",
        replaceWith = ReplaceWith("getMessage(userId, messageId)")
    )
    override fun observeMessage(userId: UserId, remoteMessageId: RemoteMessageId): Flow<Either<DataError, Message>> =
        flow {
            val message = rustMessageDataSource.getMessage(userId, remoteMessageId.toRemoteMessageId())
                .map { it.toMessage() }

            emit(message)
        }

    override suspend fun getConversationCursor(
        firstPage: CursorId,
        userId: UserId,
        labelId: LabelId
    ): Either<ConversationCursorError, ConversationCursor> = rustMessageDataSource
        .getConversationCursor(
            firstPage = firstPage.messageId?.toLocalConversationId()
                ?: firstPage.conversationId.toLocalConversationId(),
            userId = userId,
            labelId = labelId
        )
        .map {
            RustConversationCursorImpl(firstPage, it)
        }

    override suspend fun moveTo(
        userId: UserId,
        messageIds: List<MessageId>,
        toLabel: LabelId
    ): Either<DataError, Unit> = rustMessageDataSource.moveMessages(
        userId,
        messageIds.map { it.toLocalMessageId() },
        toLabel.toLocalLabelId()
    ).map {
        undoRepository.setLastOperation(it)
    }

    override suspend fun markUnread(userId: UserId, messageIds: List<MessageId>): Either<DataError, Unit> =
        rustMessageDataSource.markUnread(userId, messageIds.map { it.toLocalMessageId() })

    override suspend fun markRead(userId: UserId, messageIds: List<MessageId>): Either<DataError, Unit> =
        rustMessageDataSource.markRead(userId, messageIds.map { it.toLocalMessageId() })

    override suspend fun starMessages(userId: UserId, messageIds: List<MessageId>): Either<DataError, Unit> =
        rustMessageDataSource.starMessages(userId, messageIds.map { it.toLocalMessageId() })

    override suspend fun unStarMessages(userId: UserId, messageIds: List<MessageId>): Either<DataError, Unit> =
        rustMessageDataSource.unStarMessages(userId, messageIds.map { it.toLocalMessageId() })

    override suspend fun labelAs(
        userId: UserId,
        messageIds: List<MessageId>,
        selectedLabels: List<LabelId>,
        partiallySelectedLabels: List<LabelId>,
        shouldArchive: Boolean
    ): Either<DataError, Unit> = rustMessageDataSource.labelMessages(
        userId,
        messageIds.map { it.toLocalMessageId() },
        selectedLabels.map { it.toLocalLabelId() },
        partiallySelectedLabels.map { it.toLocalLabelId() },
        shouldArchive
    ).map {
        undoRepository.setLastOperation(it)
    }

    override suspend fun cancelScheduleSend(
        userId: UserId,
        messageId: MessageId
    ): Either<UndoSendError, PreviousScheduleSendTime> =
        rustMessageDataSource.cancelScheduleSendMessage(userId, messageId)

    override fun observeScrollerFetchNewStatus(): Flow<MessageScrollerFetchNewStatus> =
        rustMessageDataSource.observeScrollerFetchNewStatus()

    override suspend fun deleteMessages(
        userId: UserId,
        messageIds: List<MessageId>,
        currentLabelId: LabelId
    ): Either<DataError, Unit> = rustMessageDataSource.deleteMessages(userId, messageIds.map { it.toLocalMessageId() })

    override suspend fun deleteAllMessagesInLocation(userId: UserId, labelId: LabelId): Either<DataError, Unit> =
        rustMessageDataSource.deleteAllMessagesInLocation(userId, labelId.toLocalLabelId())

    // Mailbox requires this function to be implemented
    override fun observeClearLabelOperation(userId: UserId, labelId: LabelId): Flow<Boolean> = flowOf(false)

    override suspend fun reportPhishing(userId: UserId, messageId: MessageId): Either<DataError, Unit> =
        rustMessageDataSource.reportPhishing(userId, messageId.toLocalMessageId())

    override suspend fun markMessageAsLegitimate(userId: UserId, messageId: MessageId): Either<DataError, Unit> =
        rustMessageDataSource.markMessageAsLegitimate(userId, messageId.toLocalMessageId())

    override suspend fun unblockSender(userId: UserId, email: String): Either<DataError, Unit> =
        rustMessageDataSource.unblockSender(userId, email)

    override suspend fun blockSender(userId: UserId, email: String): Either<DataError, Unit> =
        rustMessageDataSource.blockSender(userId, email)

    private fun String.toLocalConversationId() = LocalConversationId(this.toULong())

    override suspend fun isMessageSenderBlocked(userId: UserId, messageId: MessageId): Either<DataError, Boolean> =
        rustMessageDataSource.isMessageSenderBlocked(userId, messageId.toLocalMessageId())
}
