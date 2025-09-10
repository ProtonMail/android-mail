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
import arrow.core.left
import ch.protonmail.android.mailcommon.data.mapper.LocalConversation
import ch.protonmail.android.mailcommon.data.mapper.LocalConversationId
import ch.protonmail.android.mailcommon.data.mapper.LocalLabelId
import ch.protonmail.android.mailcommon.domain.annotation.MissingRustApi
import ch.protonmail.android.mailcommon.domain.coroutines.IODispatcher
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcommon.domain.model.UndoableOperation
import ch.protonmail.android.mailconversation.data.usecase.GetRustConversationBottomBarActions
import ch.protonmail.android.mailconversation.data.usecase.GetRustConversationBottomSheetActions
import ch.protonmail.android.mailconversation.data.usecase.GetRustConversationLabelAsActions
import ch.protonmail.android.mailconversation.data.usecase.GetRustConversationListBottomBarActions
import ch.protonmail.android.mailconversation.data.usecase.GetRustConversationMoveToActions
import ch.protonmail.android.mailconversation.domain.entity.ConversationError
import ch.protonmail.android.maillabel.data.local.RustMailboxFactory
import ch.protonmail.android.maillabel.data.wrapper.MailboxWrapper
import ch.protonmail.android.mailmessage.data.model.LocalConversationMessages
import ch.protonmail.android.mailpagination.domain.model.PageKey
import ch.protonmail.android.mailpagination.domain.model.PaginationError
import ch.protonmail.android.mailsession.data.usecase.ExecuteWithUserSession
import ch.protonmail.android.mailsession.domain.wrapper.MailUserSessionWrapper
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import me.proton.core.domain.entity.UserId
import timber.log.Timber
import uniffi.proton_mail_uniffi.AllConversationActions
import uniffi.proton_mail_uniffi.AllListActions
import uniffi.proton_mail_uniffi.ConversationActionSheet
import uniffi.proton_mail_uniffi.LabelAsAction
import uniffi.proton_mail_uniffi.MoveAction
import javax.inject.Inject
import uniffi.proton_mail_uniffi.starConversations as rustStarConversation
import uniffi.proton_mail_uniffi.unstarConversations as rustUnstarConversation

class RustConversationDataSourceImpl @Inject constructor(
    private val rustMailboxFactory: RustMailboxFactory,
    private val rustMoveConversations: RustMoveConversations,
    private val rustLabelConversations: RustLabelConversations,
    private val rustConversationDetailQuery: RustConversationDetailQuery,
    private val rustConversationsQuery: RustConversationsQuery,
    private val getRustConversationListBottomBarActions: GetRustConversationListBottomBarActions,
    private val getRustConversationBottomBarActions: GetRustConversationBottomBarActions,
    private val getRustConversationBottomSheetActions: GetRustConversationBottomSheetActions,
    private val getRustConversationMoveToActions: GetRustConversationMoveToActions,
    private val getRustConversationLabelAsActions: GetRustConversationLabelAsActions,
    private val rustDeleteConversations: RustDeleteConversations,
    private val rustMarkConversationsAsRead: RustMarkConversationsAsRead,
    private val rustMarkConversationsAsUnread: RustMarkConversationsAsUnread,
    private val executeWithUserSession: ExecuteWithUserSession,
    @IODispatcher private val ioDispatcher: CoroutineDispatcher
) : RustConversationDataSource {

    /**
     * Gets the first x conversations for this labelId.
     * Adds in an Invalidation Observer on the label that will be fired when any conversation
     * in the label changes
     */
    override suspend fun getConversations(
        userId: UserId,
        pageKey: PageKey.DefaultPageKey
    ): Either<PaginationError, List<LocalConversation>> = rustConversationsQuery.getConversations(userId, pageKey)

    override suspend fun observeConversation(
        userId: UserId,
        conversationId: LocalConversationId,
        labelId: LocalLabelId
    ): Flow<Either<ConversationError, LocalConversation>> =
        rustConversationDetailQuery.observeConversation(userId, conversationId, labelId)
            .flowOn(ioDispatcher)

    override suspend fun observeConversationMessages(
        userId: UserId,
        conversationId: LocalConversationId,
        labelId: LocalLabelId
    ): Flow<Either<ConversationError, LocalConversationMessages>> = rustConversationDetailQuery
        .observeConversationMessages(userId, conversationId, labelId)
        .flowOn(ioDispatcher)

    override suspend fun deleteConversations(userId: UserId, conversations: List<LocalConversationId>) =
        withContext(ioDispatcher) {
            executeMailboxAction(
                userId = userId,
                action = { rustDeleteConversations(it, conversations) },
                actionName = "delete conversations"
            )
        }

    override suspend fun markRead(
        userId: UserId,
        labelId: LocalLabelId,
        conversations: List<LocalConversationId>
    ) {
        withContext(ioDispatcher) {
            executeMailboxAction(
                userId = userId,
                labelId = labelId,
                action = { rustMarkConversationsAsRead(it, conversations) },
                actionName = "mark as read"
            )
        }
    }

    override suspend fun markUnread(
        userId: UserId,
        labelId: LocalLabelId,
        conversations: List<LocalConversationId>
    ) {
        withContext(ioDispatcher) {
            executeMailboxAction(
                userId = userId,
                labelId = labelId,
                action = { mailbox -> rustMarkConversationsAsUnread(mailbox, conversations) },
                actionName = "mark as unread"
            )
        }
    }

    override suspend fun starConversations(userId: UserId, conversations: List<LocalConversationId>) =
        withContext(ioDispatcher) {
            executeUserSessionAction(
                userId = userId,
                action = { userSession -> rustStarConversation(userSession.getRustUserSession(), conversations) },
                actionName = "star conversations"
            )
        }

    override suspend fun unStarConversations(userId: UserId, conversations: List<LocalConversationId>) =
        withContext(ioDispatcher) {
            executeUserSessionAction(
                userId = userId,
                action = { userSession -> rustUnstarConversation(userSession.getRustUserSession(), conversations) },
                actionName = "unstar conversations"
            )
        }

    @MissingRustApi
    // ET - Missing Implementation. This function requires Rust settings integration
    override fun getSenderImage(address: String, bimi: String?): ByteArray? = null

    override suspend fun getAvailableBottomSheetActions(
        userId: UserId,
        labelId: LocalLabelId,
        conversationId: LocalConversationId
    ): Either<DataError, ConversationActionSheet> = withContext(ioDispatcher) {
        val mailbox = rustMailboxFactory.create(userId, labelId).getOrNull()
        if (mailbox == null) {
            Timber.e("rust-conversation: trying to get available actions for null Mailbox! failing")
            return@withContext DataError.Local.NoDataCached.left()
        }

        return@withContext getRustConversationBottomSheetActions(mailbox, conversationId)
    }

    override suspend fun getAllAvailableListBottomBarActions(
        userId: UserId,
        labelId: LocalLabelId,
        conversationIds: List<LocalConversationId>
    ): Either<DataError, AllListActions> = withContext(ioDispatcher) {
        val mailbox = rustMailboxFactory.create(userId, labelId).getOrNull()
        if (mailbox == null) {
            Timber.e("rust-conversation: trying to get available actions for null Mailbox! failing")
            return@withContext DataError.Local.NoDataCached.left()
        }

        return@withContext getRustConversationListBottomBarActions(mailbox, conversationIds)
    }

    override suspend fun getAllAvailableBottomBarActions(
        userId: UserId,
        labelId: LocalLabelId,
        conversationId: LocalConversationId
    ): Either<DataError, AllConversationActions> = withContext(ioDispatcher) {
        val mailbox = rustMailboxFactory.create(userId, labelId).getOrNull()
        if (mailbox == null) {
            Timber.e("rust-conversation: trying to get available actions for null Mailbox! failing")
            return@withContext DataError.Local.NoDataCached.left()
        }

        return@withContext getRustConversationBottomBarActions(mailbox, conversationId)
    }

    override suspend fun labelConversations(
        userId: UserId,
        conversationIds: List<LocalConversationId>,
        selectedLabelIds: List<LocalLabelId>,
        partiallySelectedLabelIds: List<LocalLabelId>,
        shouldArchive: Boolean
    ): Either<DataError, UndoableOperation> = withContext(ioDispatcher) {
        Timber.d("rust-conversation: executing label conversations for $conversationIds")
        val mailbox = rustMailboxFactory.create(userId).getOrNull()
        if (mailbox == null) {
            Timber.e("rust-conversation: trying to label conversations with null Mailbox! failing")
            return@withContext DataError.Local.NoDataCached.left()
        }

        return@withContext rustLabelConversations(
            mailbox = mailbox,
            conversationIds = conversationIds,
            selectedLabelIds = selectedLabelIds,
            partiallySelectedLabelIds = partiallySelectedLabelIds,
            shouldArchive = shouldArchive
        ).onLeft {
            Timber.e("rust-conversation: Failure deleting conversation on rust lib $it")
        }.map { labelAsOutput ->
            UndoableOperation {
                executeWithUserSession(userId) {
                    withContext(ioDispatcher) {
                        labelAsOutput.undo?.undo(it.getRustUserSession())
                    }
                }
            }
        }
    }


    override suspend fun getAvailableSystemMoveToActions(
        userId: UserId,
        labelId: LocalLabelId,
        conversationIds: List<LocalConversationId>
    ): Either<DataError, List<MoveAction.SystemFolder>> = withContext(ioDispatcher) {
        val mailbox = rustMailboxFactory.create(userId, labelId).getOrNull()
        if (mailbox == null) {
            Timber.e("rust-conversation: trying to get available actions for null Mailbox! failing")
            return@withContext DataError.Local.NoDataCached.left()
        }

        return@withContext getRustConversationMoveToActions(mailbox, conversationIds)
            .map { it.filterIsInstance<MoveAction.SystemFolder>() }
    }

    override suspend fun moveConversations(
        userId: UserId,
        conversationIds: List<LocalConversationId>,
        toLabelId: LocalLabelId
    ): Either<DataError, UndoableOperation> = withContext(ioDispatcher) {
        Timber.d("rust-conversation: move conversations to $toLabelId executing for: $conversationIds")

        val mailbox = rustMailboxFactory.create(userId).getOrNull()
        if (mailbox == null) {
            Timber.e("rust-conversation: trying to label conversations with null Mailbox! failing")
            return@withContext DataError.Local.NoDataCached.left()
        }

        return@withContext rustMoveConversations(mailbox, toLabelId, conversationIds)
            .onLeft {
                return@withContext it.left()
            }
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

    override suspend fun getAvailableLabelAsActions(
        userId: UserId,
        labelId: LocalLabelId,
        conversationIds: List<LocalConversationId>
    ): Either<DataError, List<LabelAsAction>> = withContext(ioDispatcher) {
        val mailbox = rustMailboxFactory.create(userId, labelId).getOrNull()
        if (mailbox == null) {
            Timber.e("rust-conversation: trying to get available label actions for null Mailbox! failing")
            return@withContext DataError.Local.NoDataCached.left()
        }
        return@withContext getRustConversationLabelAsActions(mailbox, conversationIds)
    }

    private suspend fun executeUserSessionAction(
        userId: UserId,
        action: suspend (MailUserSessionWrapper) -> Unit,
        actionName: String
    ) {
        executeWithUserSession(userId, action)
            .onLeft { Timber.e("rust-conversation: Failed to perform $actionName due to $it") }
    }

    private suspend fun executeMailboxAction(
        userId: UserId,
        labelId: LocalLabelId? = null,
        action: suspend (MailboxWrapper) -> Unit,
        actionName: String
    ): Either<DataError.Local, Unit> {
        Timber.d("rust-conversation: executing action $actionName")
        val mailbox = if (labelId != null) {
            rustMailboxFactory.create(userId, labelId)
        } else {
            rustMailboxFactory.create(userId)
        }.getOrNull()
        if (mailbox == null) {
            Timber.e("rust-conversation: Failed to perform $actionName, null mailbox")
            return DataError.Local.NoDataCached.left()
        }

        return Either.catch {
            action(mailbox)
        }.mapLeft {
            Timber.e("rust-conversation: $actionName failed in rust: $it")
            DataError.Local.Unknown
        }
    }
}
