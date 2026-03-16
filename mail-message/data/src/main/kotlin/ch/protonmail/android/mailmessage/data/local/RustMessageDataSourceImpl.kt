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
import arrow.core.left
import ch.protonmail.android.mailcommon.data.mapper.LocalConversationId
import ch.protonmail.android.mailcommon.data.mapper.LocalLabelAsAction
import ch.protonmail.android.mailcommon.data.mapper.LocalLabelId
import ch.protonmail.android.mailcommon.data.mapper.LocalMessageId
import ch.protonmail.android.mailcommon.data.mapper.LocalMessageMetadata
import ch.protonmail.android.mailcommon.data.mapper.RemoteMessageId
import ch.protonmail.android.mailcommon.data.wrapper.ConversationCursor
import ch.protonmail.android.mailcommon.domain.annotation.MissingRustApi
import ch.protonmail.android.mailcommon.domain.coroutines.IODispatcher
import ch.protonmail.android.mailcommon.domain.model.ConversationCursorError
import ch.protonmail.android.mailcommon.domain.model.ConversationCursorError.InvalidState
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcommon.domain.model.UndoSendError
import ch.protonmail.android.mailcommon.domain.model.UndoableOperation
import ch.protonmail.android.maillabel.data.local.RustMailboxFactory
import ch.protonmail.android.maillabel.domain.model.LabelId
import ch.protonmail.android.mailmessage.data.mapper.toLocalMessageId
import ch.protonmail.android.mailmessage.data.mapper.toMessageScrollerFetchNewStatus
import ch.protonmail.android.mailmessage.data.mapper.toPreviousScheduleSendTime
import ch.protonmail.android.mailmessage.data.usecase.CreateRustMessageAccessor
import ch.protonmail.android.mailmessage.data.usecase.GetRustAllMessageBottomBarActions
import ch.protonmail.android.mailmessage.data.usecase.GetRustAllMessageListBottomBarActions
import ch.protonmail.android.mailmessage.data.usecase.GetRustAvailableMessageActions
import ch.protonmail.android.mailmessage.data.usecase.GetRustMessageLabelAsActions
import ch.protonmail.android.mailmessage.data.usecase.GetRustMessageMoveToActions
import ch.protonmail.android.mailmessage.data.usecase.GetRustSenderImage
import ch.protonmail.android.mailmessage.data.usecase.RustBlockAddress
import ch.protonmail.android.mailmessage.data.usecase.RustDeleteAllMessagesInLabel
import ch.protonmail.android.mailmessage.data.usecase.RustDeleteMessages
import ch.protonmail.android.mailmessage.data.usecase.RustIsMessageSenderBlocked
import ch.protonmail.android.mailmessage.data.usecase.RustLabelMessages
import ch.protonmail.android.mailmessage.data.usecase.RustMarkMessageAsLegitimate
import ch.protonmail.android.mailmessage.data.usecase.RustMarkMessagesRead
import ch.protonmail.android.mailmessage.data.usecase.RustMarkMessagesUnread
import ch.protonmail.android.mailmessage.data.usecase.RustMoveMessages
import ch.protonmail.android.mailmessage.data.usecase.RustReportPhishing
import ch.protonmail.android.mailmessage.data.usecase.RustStarMessages
import ch.protonmail.android.mailmessage.data.usecase.RustUnblockAddress
import ch.protonmail.android.mailmessage.data.usecase.RustUnstarMessages
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailmessage.domain.model.MessageScrollerFetchNewStatus
import ch.protonmail.android.mailmessage.domain.model.PreviousScheduleSendTime
import ch.protonmail.android.mailmessage.domain.model.toConversationCursorError
import ch.protonmail.android.mailpagination.domain.model.PageKey
import ch.protonmail.android.mailpagination.domain.model.PaginationError
import ch.protonmail.android.mailsession.data.usecase.ExecuteWithUserSession
import ch.protonmail.android.mailsession.domain.repository.UserSessionRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import me.proton.core.domain.entity.UserId
import timber.log.Timber
import uniffi.proton_mail_uniffi.AllListActions
import uniffi.proton_mail_uniffi.AllMessageActions
import uniffi.proton_mail_uniffi.MessageActionSheet
import uniffi.proton_mail_uniffi.MoveAction
import uniffi.proton_mail_uniffi.ThemeOpts
import javax.inject.Inject

@SuppressWarnings("LongParameterList", "TooManyFunctions")
class RustMessageDataSourceImpl @Inject constructor(
    private val userSessionRepository: UserSessionRepository,
    private val rustMailboxFactory: RustMailboxFactory,
    private val rustMessageListQuery: RustMessageListQuery,
    private val rustMessageQuery: RustMessageQuery,
    private val createRustMessageAccessor: CreateRustMessageAccessor,
    private val getRustSenderImage: GetRustSenderImage,
    private val rustMarkMessagesRead: RustMarkMessagesRead,
    private val rustMarkMessagesUnread: RustMarkMessagesUnread,
    private val rustStarMessages: RustStarMessages,
    private val rustUnstarMessages: RustUnstarMessages,
    private val getRustAllMessageListBottomBarActions: GetRustAllMessageListBottomBarActions,
    private val getRustAllMessageBottomBarActions: GetRustAllMessageBottomBarActions,
    private val rustDeleteMessages: RustDeleteMessages,
    private val rustMoveMessages: RustMoveMessages,
    private val rustLabelMessages: RustLabelMessages,
    private val getRustAvailableMessageActions: GetRustAvailableMessageActions,
    private val getRustMessageMoveToActions: GetRustMessageMoveToActions,
    private val getRustMessageLabelAsActions: GetRustMessageLabelAsActions,
    private val rustMarkMessageAsLegitimate: RustMarkMessageAsLegitimate,
    private val rustUnblockAddress: RustUnblockAddress,
    private val rustBlockAddress: RustBlockAddress,
    private val rustIsMessageSenderBlocked: RustIsMessageSenderBlocked,
    private val rustReportPhishing: RustReportPhishing,
    private val rustDeleteAllMessagesInLabel: RustDeleteAllMessagesInLabel,
    private val cancelScheduleSendMessage: RustCancelScheduleSendMessage,
    private val executeWithUserSession: ExecuteWithUserSession,
    @IODispatcher private val ioDispatcher: CoroutineDispatcher
) : RustMessageDataSource {

    override suspend fun updateShowSpamTrashFilter(showSpamTrash: Boolean) =
        rustMessageListQuery.updateShowSpamTrashFilter(showSpamTrash)

    override suspend fun updateUnreadFilter(filterUnread: Boolean) =
        rustMessageListQuery.updateUnreadFilter(filterUnread)

    override suspend fun terminatePaginator(userId: UserId) = rustMessageListQuery.terminatePaginator(userId)

    override suspend fun supportsIncludeFilter() = rustMessageListQuery.supportsIncludeFilter()

    override suspend fun getMessage(
        userId: UserId,
        messageId: LocalMessageId
    ): Either<DataError, LocalMessageMetadata> = withContext(ioDispatcher) {
        val session = userSessionRepository.getUserSession(userId)
        if (session == null) {
            Timber.e("rust-message: trying to load message with a null session")
            return@withContext DataError.Local.NoUserSession.left()
        }

        return@withContext createRustMessageAccessor(session, messageId)
            .onLeft { Timber.e("rust-message: Failed to get message $it") }
    }

    override suspend fun getMessage(
        userId: UserId,
        messageId: RemoteMessageId
    ): Either<DataError, LocalMessageMetadata> = withContext(ioDispatcher) {
        val session = userSessionRepository.getUserSession(userId)
        if (session == null) {
            Timber.e("rust-message: trying to fetch remote message with a null session")
            return@withContext DataError.Local.NoUserSession.left()
        }

        return@withContext createRustMessageAccessor(session, messageId)
            .onLeft { Timber.e("rust-message: Failed to get remote message $it") }
    }

    override suspend fun getMessages(
        userId: UserId,
        pageKey: PageKey
    ): Either<PaginationError, List<LocalMessageMetadata>> = rustMessageListQuery.getMessages(userId, pageKey)

    override suspend fun observeMessage(
        userId: UserId,
        messageId: LocalMessageId
    ): Flow<Either<DataError, LocalMessageMetadata>> {
        val session = userSessionRepository.getUserSession(userId)
        if (session == null) {
            Timber.e("rust-message: trying to fetch remote message with a null session")
            return flowOf(DataError.Local.NoUserSession.left())
        }

        return rustMessageQuery.observeMessage(session, messageId).flowOn(ioDispatcher)
    }

    override suspend fun getConversationCursor(
        firstPage: LocalConversationId,
        userId: UserId,
        labelId: LabelId
    ): Either<ConversationCursorError, ConversationCursor> = withContext(ioDispatcher) {
        // we are relying on the exiting pager being open already
        val result = rustMessageListQuery.getCursor(userId = userId, conversationId = firstPage, labelId = labelId)
            ?: initializeCursor(userId, labelId, firstPage).apply {
                Timber.d("rust-message cursor unable to get cursor, retrieving conversations and retrying")
            }

        return@withContext when {
            result == null -> InvalidState.left()
            else -> {
                result.mapLeft {
                    it.toConversationCursorError()
                }
            }
        }
    }

    private suspend fun initializeCursor(
        userId: UserId,
        labelId: LabelId,
        firstPage: LocalConversationId
    ) = rustMessageListQuery.getMessages(userId, PageKey.DefaultPageKey())
        .onLeft {
            Timber.e("rust-message cursor unable to recover and get conversations")
        }
        .fold(
            ifLeft = { null },
            ifRight = { rustMessageListQuery.getCursor(userId, labelId, firstPage) }
        )


    override suspend fun getSenderImage(
        userId: UserId,
        address: String,
        bimi: String?
    ): String? = withContext(ioDispatcher) {
        val session = userSessionRepository.getUserSession(userId)
        if (session == null) {
            Timber.e("rust-message: trying to get sender image with a null session")
            return@withContext null
        }

        return@withContext getRustSenderImage(session, address, bimi)
            .onLeft { Timber.d("rust-message: Failed to get sender image $it") }
            .getOrNull()
    }

    @MissingRustApi
    override suspend fun markRead(userId: UserId, messages: List<LocalMessageId>): Either<DataError, Unit> =
        withContext(ioDispatcher) {
            // Hardcoded rust mailbox to "AllMail" to avoid this method having labelId as param;
            // the current labelId is not needed to mark as read and is planned to be dropped on this API
            val mailbox = rustMailboxFactory.createAllMail(userId).getOrNull()
            if (mailbox == null) {
                Timber.e("rust-message: trying to mark message read with a null mailbox")
                return@withContext DataError.Local.IllegalStateError.left()
            }

            Timber.d("rust-message: marking message as read...")
            return@withContext rustMarkMessagesRead(mailbox, messages)
                .onLeft { Timber.e("rust-message: Failed to mark message read $it") }
        }

    override suspend fun markUnread(userId: UserId, messages: List<LocalMessageId>): Either<DataError, Unit> =
        withContext(ioDispatcher) {
            // Hardcoded rust mailbox to "AllMail" to avoid this method having labelId as param;
            // the current labelId is not needed to mark as unread and is planned to be dropped on this API
            val mailbox = rustMailboxFactory.createAllMail(userId).getOrNull()
            if (mailbox == null) {
                Timber.e("rust-message: trying to mark unread with null Mailbox! failing")
                return@withContext DataError.Local.IllegalStateError.left()
            }

            return@withContext rustMarkMessagesUnread(mailbox, messages)
                .onLeft { Timber.e("rust-message: Failed to mark message unread $it") }
        }

    override suspend fun starMessages(userId: UserId, messages: List<LocalMessageId>): Either<DataError, Unit> =
        withContext(ioDispatcher) {
            val session = userSessionRepository.getUserSession(userId)
                ?: return@withContext DataError.Local.NoUserSession.left()

            return@withContext rustStarMessages(session, messages)
                .onLeft { Timber.e("rust-message: Failed to mark message as starred $it") }
        }

    override suspend fun unStarMessages(userId: UserId, messages: List<LocalMessageId>): Either<DataError, Unit> =
        withContext(ioDispatcher) {
            val session = userSessionRepository.getUserSession(userId)
                ?: return@withContext DataError.Local.NoUserSession.left()

            return@withContext rustUnstarMessages(session, messages)
                .onLeft { Timber.e("rust-message: Failed to mark message unStarred $it") }
        }

    override suspend fun moveMessages(
        userId: UserId,
        messageIds: List<LocalMessageId>,
        toLabelId: LocalLabelId
    ): Either<DataError, UndoableOperation> = withContext(ioDispatcher) {
        val mailbox = rustMailboxFactory.create(userId).getOrNull()
        if (mailbox == null) {
            Timber.e("rust-message: trying to move messages with null Mailbox! failing")
            return@withContext DataError.Local.IllegalStateError.left()
        }
        return@withContext rustMoveMessages(mailbox, toLabelId, messageIds)
            .onLeft { Timber.e("rust-message: Failed to move messages $it") }
            .map { undo ->
                UndoableOperation {
                    executeWithUserSession(userId) {
                        withContext(ioDispatcher) {
                            undo?.undo(it.getRustUserSession())
                        }
                    }
                }
            }
    }

    override suspend fun getAvailableActions(
        userId: UserId,
        labelId: LocalLabelId,
        messageId: LocalMessageId,
        themeOpts: ThemeOpts
    ): Either<DataError, MessageActionSheet> = withContext(ioDispatcher) {
        val mailbox = rustMailboxFactory.create(userId, labelId).getOrNull()
        if (mailbox == null) {
            Timber.e("rust-message: trying to get available actions for null Mailbox! failing")
            return@withContext DataError.Local.IllegalStateError.left()
        }
        return@withContext getRustAvailableMessageActions(mailbox, messageId, themeOpts)
    }

    override suspend fun getAllAvailableListBottomBarActions(
        userId: UserId,
        labelId: LocalLabelId,
        messageIds: List<LocalMessageId>
    ): Either<DataError, AllListActions> = withContext(ioDispatcher) {
        val mailbox = rustMailboxFactory.create(userId, labelId).getOrNull()
        if (mailbox == null) {
            Timber.e("rust-message: trying to get all available actions for null Mailbox! failing")
            return@withContext DataError.Local.IllegalStateError.left()
        }

        return@withContext getRustAllMessageListBottomBarActions(mailbox, messageIds)
    }

    override suspend fun getAllAvailableBottomBarActions(
        userId: UserId,
        labelId: LocalLabelId,
        messageId: LocalMessageId,
        themeOpts: ThemeOpts
    ): Either<DataError, AllMessageActions> = withContext(ioDispatcher) {
        val mailbox = rustMailboxFactory.create(userId, labelId).getOrNull()
        if (mailbox == null) {
            Timber.e("rust-message: trying to get all available message actions for null Mailbox! failing")
            return@withContext DataError.Local.IllegalStateError.left()
        }

        return@withContext getRustAllMessageBottomBarActions(mailbox, themeOpts, messageId)
    }

    override suspend fun getAvailableSystemMoveToActions(
        userId: UserId,
        labelId: LocalLabelId,
        messageIds: List<LocalMessageId>
    ): Either<DataError, List<MoveAction.SystemFolder>> = withContext(ioDispatcher) {
        val mailbox = rustMailboxFactory.create(userId, labelId).getOrNull()
        if (mailbox == null) {
            Timber.e("rust-message: trying to get available actions for null Mailbox! failing")
            return@withContext DataError.Local.IllegalStateError.left()
        }
        val moveActions = getRustMessageMoveToActions(mailbox, messageIds)
        return@withContext moveActions.map { it.filterIsInstance<MoveAction.SystemFolder>() }
    }

    override suspend fun getAvailableLabelAsActions(
        userId: UserId,
        labelId: LocalLabelId,
        messageIds: List<LocalMessageId>
    ): Either<DataError, List<LocalLabelAsAction>> = withContext(ioDispatcher) {
        val mailbox = rustMailboxFactory.create(userId, labelId).getOrNull()
        if (mailbox == null) {
            Timber.e("rust-message: trying to get available label actions for null Mailbox! failing")
            return@withContext DataError.Local.IllegalStateError.left()
        }
        return@withContext getRustMessageLabelAsActions(mailbox, messageIds)
    }

    override suspend fun deleteMessages(userId: UserId, messageIds: List<LocalMessageId>): Either<DataError, Unit> =
        withContext(ioDispatcher) {
            // Hardcoded rust mailbox to "AllMail" to avoid this method having labelId as param;
            // the current labelId is not needed to delete messages and is planned to be dropped on this API
            val mailbox = rustMailboxFactory.createAllMail(userId).getOrNull()
            if (mailbox == null) {
                Timber.e("rust-message: trying to delete messages with null Mailbox! failing")
                return@withContext DataError.Local.IllegalStateError.left()
            }

            return@withContext rustDeleteMessages(mailbox, messageIds)
                .onLeft { Timber.e("rust-message: Failure deleting message on rust lib $it") }
        }

    override suspend fun labelMessages(
        userId: UserId,
        messageIds: List<LocalMessageId>,
        selectedLabelIds: List<LocalLabelId>,
        partiallySelectedLabelIds: List<LocalLabelId>,
        shouldArchive: Boolean
    ): Either<DataError, UndoableOperation> = withContext(ioDispatcher) {
        Timber.d("rust-message: executing labels messages for $messageIds")
        val mailbox = rustMailboxFactory.create(userId).getOrNull()
        if (mailbox == null) {
            Timber.e("rust-message: trying to label messages with null Mailbox! failing")
            return@withContext DataError.Local.IllegalStateError.left()
        }

        return@withContext rustLabelMessages(
            mailbox = mailbox,
            messageIds = messageIds,
            selectedLabelIds = selectedLabelIds,
            partiallySelectedLabelIds = partiallySelectedLabelIds,
            shouldArchive = shouldArchive
        ).map { labelAsOutput ->
            UndoableOperation {
                executeWithUserSession(userId) {
                    withContext(ioDispatcher) {
                        labelAsOutput.undo?.undo(it.getRustUserSession())
                    }
                }
            }
        }
    }

    override suspend fun markMessageAsLegitimate(userId: UserId, messageId: LocalMessageId): Either<DataError, Unit> =
        withContext(ioDispatcher) {
            val mailbox = rustMailboxFactory.create(userId).getOrNull()
            if (mailbox == null) {
                Timber.e("rust-message: trying to mark message as legitimate with null Mailbox! failing")
                return@withContext DataError.Local.IllegalStateError.left()
            }

            return@withContext rustMarkMessageAsLegitimate(mailbox, messageId)
        }

    override suspend fun blockSender(userId: UserId, email: String): Either<DataError, Unit> {
        val session = userSessionRepository.getUserSession(userId)
        if (session == null) {
            Timber.e("rust-message: trying to block sender with a null session")
            return DataError.Local.NoUserSession.left()
        }

        return rustBlockAddress(session, email)
    }

    override suspend fun unblockSender(userId: UserId, email: String): Either<DataError, Unit> {
        val mailbox = rustMailboxFactory.create(userId).getOrNull()
        if (mailbox == null) {
            Timber.e("rust-message: trying to unblock sender with null Mailbox! failing")
            return DataError.Local.NoDataCached.left()
        }

        return rustUnblockAddress(mailbox, email)
    }

    override suspend fun isMessageSenderBlocked(userId: UserId, messageId: LocalMessageId): Either<DataError, Boolean> {
        val mailbox = rustMailboxFactory.create(userId).getOrNull()
        if (mailbox == null) {
            Timber.e("rust-message: trying to check if sender is blocked with null Mailbox! failing")
            return DataError.Local.NoDataCached.left()
        }

        return rustIsMessageSenderBlocked(mailbox, messageId)
    }


    override suspend fun reportPhishing(userId: UserId, messageId: LocalMessageId): Either<DataError, Unit> =
        withContext(ioDispatcher) {
            val mailbox = rustMailboxFactory.create(userId).getOrNull()
            if (mailbox == null) {
                Timber.e("rust-message: trying to report phishing with null Mailbox! failing")
                return@withContext DataError.Local.IllegalStateError.left()
            }

            return@withContext rustReportPhishing(mailbox, messageId)
        }

    override suspend fun deleteAllMessagesInLocation(userId: UserId, labelId: LocalLabelId): Either<DataError, Unit> =
        withContext(ioDispatcher) {
            val session = userSessionRepository.getUserSession(userId)
            if (session == null) {
                Timber.e("rust-message: trying to delete all messages in location with a null session")
                return@withContext DataError.Local.NoUserSession.left()
            }

            rustDeleteAllMessagesInLabel(session, labelId)
        }

    override suspend fun cancelScheduleSendMessage(
        userId: UserId,
        messageId: MessageId
    ): Either<UndoSendError, PreviousScheduleSendTime> {
        Timber.d("rust-message: Cancels schedule send raft...")
        val session = userSessionRepository.getUserSession(userId)
        if (session == null) {
            Timber.e("rust-message: Trying to cancel schedule send with null session; Failing.")
            return UndoSendError.Other(DataError.Local.NoUserSession).left()
        }

        return cancelScheduleSendMessage(session, messageId.toLocalMessageId()).map {
            it.toPreviousScheduleSendTime()
        }
    }

    override fun observeScrollerFetchNewStatus(): Flow<MessageScrollerFetchNewStatus> =
        rustMessageListQuery.observeScrollerFetchNewStatus().map {
            it.toMessageScrollerFetchNewStatus()
        }

}
