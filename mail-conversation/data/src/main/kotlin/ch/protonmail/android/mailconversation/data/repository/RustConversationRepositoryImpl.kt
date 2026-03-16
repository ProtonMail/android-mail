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

package ch.protonmail.android.mailconversation.data.repository

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.right
import ch.protonmail.android.mailcommon.data.repository.RustConversationCursorImpl
import ch.protonmail.android.mailcommon.domain.model.ConversationCursorError
import ch.protonmail.android.mailcommon.domain.model.ConversationId
import ch.protonmail.android.mailcommon.domain.model.CursorId
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcommon.domain.repository.ConversationCursor
import ch.protonmail.android.mailcommon.domain.repository.UndoRepository
import ch.protonmail.android.mailconversation.data.local.RustConversationDataSource
import ch.protonmail.android.mailconversation.data.mapper.toConversation
import ch.protonmail.android.mailconversation.data.mapper.toConversationMessagesWithMessageToOpen
import ch.protonmail.android.mailconversation.domain.entity.Conversation
import ch.protonmail.android.mailconversation.domain.entity.ConversationDetailEntryPoint
import ch.protonmail.android.mailconversation.domain.entity.ConversationError
import ch.protonmail.android.mailconversation.domain.entity.ConversationWithMessages
import ch.protonmail.android.mailconversation.domain.model.ConversationScrollerFetchNewStatus
import ch.protonmail.android.mailconversation.domain.repository.ConversationRepository
import ch.protonmail.android.maillabel.data.mapper.toLocalLabelId
import ch.protonmail.android.maillabel.domain.model.LabelId
import ch.protonmail.android.mailmessage.data.mapper.toLocalConversationId
import ch.protonmail.android.mailmessage.domain.model.ConversationMessages
import ch.protonmail.android.mailpagination.domain.model.PageKey
import ch.protonmail.android.mailpagination.domain.model.PaginationError
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import me.proton.core.domain.entity.UserId
import timber.log.Timber
import javax.inject.Inject

class RustConversationRepositoryImpl @Inject constructor(
    private val rustConversationDataSource: RustConversationDataSource,
    private val undoRepository: UndoRepository
) : ConversationRepository {

    override suspend fun updateShowSpamTrashFilter(showSpamTrash: Boolean) =
        rustConversationDataSource.updateShowSpamTrashFilter(showSpamTrash)

    override suspend fun updateUnreadFilter(filterUnread: Boolean) =
        rustConversationDataSource.updateUnreadFilter(filterUnread)

    override suspend fun terminatePaginator(userId: UserId) {
        Timber.d("rust-conversation-repo: terminatePaginator for userId: $userId")
        rustConversationDataSource.terminatePaginator(userId)
    }

    override suspend fun supportsIncludeFilter() = rustConversationDataSource.supportsIncludeFilter()

    override suspend fun getConversations(
        userId: UserId,
        pageKey: PageKey.DefaultPageKey
    ): Either<PaginationError, List<Conversation>> {
        Timber.d("rust-conversation-repo: getConversations, pageKey: $pageKey")
        return rustConversationDataSource.getConversations(userId, pageKey)
            .map { localConversations ->
                localConversations.map {
                    it.toConversation()
                }
            }
    }

    override suspend fun observeConversation(
        userId: UserId,
        id: ConversationId,
        labelId: LabelId,
        entryPoint: ConversationDetailEntryPoint,
        showAllMessages: Boolean
    ): Flow<Either<ConversationError, Conversation>> = rustConversationDataSource.observeConversationWithMessages(
        userId,
        id.toLocalConversationId(),
        labelId.toLocalLabelId(),
        entryPoint,
        showAllMessages
    ).map { either ->
        either.map { it.conversation.toConversation() }
    }

    override suspend fun observeConversationMessages(
        userId: UserId,
        conversationId: ConversationId,
        labelId: LabelId,
        entryPoint: ConversationDetailEntryPoint,
        showAllMessages: Boolean
    ): Flow<Either<ConversationError, ConversationMessages>> =
        rustConversationDataSource.observeConversationWithMessages(
            userId,
            conversationId.toLocalConversationId(),
            labelId.toLocalLabelId(),
            entryPoint,
            showAllMessages
        ).map { either ->
            either.flatMap { it.messages.toConversationMessagesWithMessageToOpen() }
        }

    override suspend fun observeConversationWithMessages(
        userId: UserId,
        conversationId: ConversationId,
        labelId: LabelId,
        entryPoint: ConversationDetailEntryPoint,
        showAllMessages: Boolean
    ): Flow<Either<ConversationError, ConversationWithMessages>> =
        rustConversationDataSource.observeConversationWithMessages(
            userId,
            conversationId.toLocalConversationId(),
            labelId.toLocalLabelId(),
            entryPoint,
            showAllMessages
        ).map { either ->
            either.flatMap { localConversationWithMessages ->
                localConversationWithMessages.messages.toConversationMessagesWithMessageToOpen()
                    .map { conversationMessages ->
                        ConversationWithMessages(
                            conversation = localConversationWithMessages.conversation.toConversation(),
                            messages = conversationMessages
                        )
                    }
            }
        }

    override suspend fun getConversationCursor(
        firstPage: CursorId,
        userId: UserId,
        labelId: LabelId
    ): Either<ConversationCursorError, ConversationCursor> = rustConversationDataSource
        .getConversationCursor(
            userId = userId,
            labelId = labelId,
            firstPage = firstPage.conversationId.toLocalConversationId()
        )
        .map {
            RustConversationCursorImpl(firstPage, it)
        }

    override suspend fun move(
        userId: UserId,
        conversationIds: List<ConversationId>,
        toLabelId: LabelId
    ): Either<DataError, List<Conversation>> {
        rustConversationDataSource.moveConversations(
            userId,
            conversationIds.map {
                it.toLocalConversationId()
            },
            toLabelId.toLocalLabelId()
        ).map {
            undoRepository.setLastOperation(it)
        }

        return emptyList<Conversation>().right()
    }

    override suspend fun markUnread(
        userId: UserId,
        labelId: LabelId,
        conversationIds: List<ConversationId>
    ): Either<DataError, Unit> {
        rustConversationDataSource.markUnread(
            userId,
            labelId.toLocalLabelId(),
            conversationIds.map { it.toLocalConversationId() }
        )

        return Unit.right()
    }

    // It will be implemented later on
    override suspend fun markRead(
        userId: UserId,
        labelId: LabelId,
        conversationIds: List<ConversationId>
    ): Either<DataError, Unit> {
        rustConversationDataSource.markRead(
            userId,
            labelId.toLocalLabelId(),
            conversationIds.map { it.toLocalConversationId() }
        )

        return Unit.right()
    }

    override suspend fun star(
        userId: UserId,
        conversationIds: List<ConversationId>
    ): Either<DataError, List<Conversation>> {
        rustConversationDataSource.starConversations(userId, conversationIds.map { it.toLocalConversationId() })

        return emptyList<Conversation>().right()
    }

    override suspend fun unStar(
        userId: UserId,
        conversationIds: List<ConversationId>
    ): Either<DataError, List<Conversation>> {

        rustConversationDataSource.unStarConversations(userId, conversationIds.map { it.toLocalConversationId() })

        return emptyList<Conversation>().right()
    }

    override suspend fun labelAs(
        userId: UserId,
        conversationIds: List<ConversationId>,
        selectedLabels: List<LabelId>,
        partiallySelectedLabels: List<LabelId>,
        shouldArchive: Boolean
    ): Either<DataError, Unit> = rustConversationDataSource.labelConversations(
        userId,
        conversationIds.map { it.toLocalConversationId() },
        selectedLabels.map { it.toLocalLabelId() },
        partiallySelectedLabels.map { it.toLocalLabelId() },
        shouldArchive
    ).map {
        undoRepository.setLastOperation(it)
    }

    override suspend fun deleteConversations(
        userId: UserId,
        conversationIds: List<ConversationId>
    ): Either<DataError, Unit> = rustConversationDataSource.deleteConversations(
        userId,
        conversationIds.map { it.toLocalConversationId() }
    )

    override fun observeScrollerFetchNewStatus(): Flow<ConversationScrollerFetchNewStatus> =
        rustConversationDataSource.observeScrollerFetchNewStatus()
}
