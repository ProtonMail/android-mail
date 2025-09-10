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
import ch.protonmail.android.mailcommon.domain.model.ConversationId
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcommon.domain.repository.UndoRepository
import ch.protonmail.android.mailconversation.data.local.RustConversationDataSource
import ch.protonmail.android.mailconversation.data.mapper.toConversation
import ch.protonmail.android.mailconversation.data.mapper.toConversationMessagesWithMessageToOpen
import ch.protonmail.android.mailconversation.domain.entity.Conversation
import ch.protonmail.android.mailconversation.domain.entity.ConversationError
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
        labelId: LabelId
    ): Flow<Either<ConversationError, Conversation>> = rustConversationDataSource
        .observeConversation(userId, id.toLocalConversationId(), labelId.toLocalLabelId())
        .map { eitherFlow -> eitherFlow.map { it.toConversation() } }


    override suspend fun observeConversationMessages(
        userId: UserId,
        conversationId: ConversationId,
        labelId: LabelId
    ): Flow<Either<ConversationError, ConversationMessages>> = rustConversationDataSource.observeConversationMessages(
        userId, conversationId.toLocalConversationId(), labelId.toLocalLabelId()
    ).map { eitherConversationMessages ->
        eitherConversationMessages.flatMap { it.toConversationMessagesWithMessageToOpen() }
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
}
