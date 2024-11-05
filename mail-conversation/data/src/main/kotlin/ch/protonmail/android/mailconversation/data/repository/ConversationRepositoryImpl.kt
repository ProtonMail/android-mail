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
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.mapper.mapToEither
import ch.protonmail.android.mailcommon.domain.model.ConversationId
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailconversation.data.remote.ConversationApi
import ch.protonmail.android.mailconversation.domain.ConversationLabelPropagationOptions
import ch.protonmail.android.mailconversation.domain.entity.Conversation
import ch.protonmail.android.mailconversation.domain.entity.ConversationWithContext
import ch.protonmail.android.mailconversation.domain.repository.ConversationLocalDataSource
import ch.protonmail.android.mailconversation.domain.repository.ConversationRemoteDataSource
import ch.protonmail.android.mailconversation.domain.repository.ConversationRepository
import ch.protonmail.android.maillabel.domain.extension.isSpam
import ch.protonmail.android.maillabel.domain.extension.isTrash
import ch.protonmail.android.maillabel.domain.model.filterUnmodifiableLabels
import ch.protonmail.android.mailmessage.data.local.MessageLocalDataSource
import ch.protonmail.android.mailmessage.data.usecase.ExcludeDraftMessagesAlreadyInOutbox
import ch.protonmail.android.mailpagination.domain.model.PageKey
import com.dropbox.android.external.store4.Fetcher
import com.dropbox.android.external.store4.SourceOfTruth
import com.dropbox.android.external.store4.StoreBuilder
import com.dropbox.android.external.store4.StoreRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import me.proton.core.data.arch.ProtonStore
import me.proton.core.data.arch.buildProtonStore
import me.proton.core.data.arch.toDataResult
import me.proton.core.domain.arch.DataResult
import me.proton.core.domain.entity.UserId
import me.proton.core.label.domain.entity.LabelId
import me.proton.core.util.kotlin.CoroutineScopeProvider
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min

@Singleton
@Suppress("TooManyFunctions")
class ConversationRepositoryImpl @Inject constructor(
    private val conversationLocalDataSource: ConversationLocalDataSource,
    private val conversationRemoteDataSource: ConversationRemoteDataSource,
    coroutineScopeProvider: CoroutineScopeProvider,
    private val messageLocalDataSource: MessageLocalDataSource,
    private val excludeDraftMessagesAlreadyInOutbox: ExcludeDraftMessagesAlreadyInOutbox
) : ConversationRepository {

    private data class ConversationKey(val userId: UserId, val conversationId: ConversationId)

    private val conversationStore: ProtonStore<ConversationKey, Conversation> = StoreBuilder.from(
        fetcher = Fetcher.of { key: ConversationKey ->
            conversationRemoteDataSource.getConversationWithMessages(key.userId, key.conversationId)
        },
        sourceOfTruth = SourceOfTruth.of(
            reader = { key: ConversationKey ->
                conversationLocalDataSource.observeConversation(key.userId, key.conversationId)
            },
            writer = { key, (conversation, messages) ->
                conversationLocalDataSource.upsertConversation(key.userId, conversation)

                // Message can be in the outbox for sending, we should not overwrite them
                val filteredMessages = excludeDraftMessagesAlreadyInOutbox(key.userId, messages)
                messageLocalDataSource.upsertMessages(filteredMessages)

            }
        )
    ).buildProtonStore(coroutineScopeProvider)

    override suspend fun getLocalConversations(userId: UserId, pageKey: PageKey) =
        conversationLocalDataSource.getConversations(userId, pageKey)

    override suspend fun isLocalPageValid(
        userId: UserId,
        pageKey: PageKey,
        items: List<ConversationWithContext>
    ): Boolean = conversationLocalDataSource.isLocalPageValid(userId, pageKey, items)

    override suspend fun getRemoteConversations(
        userId: UserId,
        pageKey: PageKey
    ): Either<DataError.Remote, List<ConversationWithContext>> {
        val adaptedPageKey = conversationLocalDataSource.getClippedPageKey(
            userId = userId,
            pageKey = pageKey.copy(size = min(ConversationApi.maxPageSize, pageKey.size))
        ) ?: return emptyList<ConversationWithContext>().right()

        return conversationRemoteDataSource.getConversations(userId = userId, pageKey = adaptedPageKey)
            .onRight { conversations -> upsertConversations(userId, adaptedPageKey, conversations) }
    }

    override suspend fun markAsStale(userId: UserId, labelId: LabelId) =
        conversationLocalDataSource.markAsStale(userId, labelId)

    override fun observeConversation(
        userId: UserId,
        id: ConversationId,
        refreshData: Boolean
    ): Flow<Either<DataError, Conversation>> = buildStoreStream(userId, id, refreshData)
        .mapToEither()

    override fun observeCachedConversations(userId: UserId, ids: List<ConversationId>) =
        conversationLocalDataSource.observeCachedConversations(userId, ids)

    private fun buildStoreStream(
        userId: UserId,
        id: ConversationId,
        refreshData: Boolean
    ): Flow<DataResult<Conversation>> =
        conversationStore.stream(StoreRequest.cached(ConversationKey(userId, id), refreshData))
            .map { it.toDataResult() }
            .distinctUntilChanged()

    override suspend fun addLabel(
        userId: UserId,
        conversationId: ConversationId,
        labelId: LabelId
    ): Either<DataError, Conversation> = addLabels(userId, listOf(conversationId), listOf(labelId))
        .map { it.first() }

    override suspend fun addLabel(
        userId: UserId,
        conversationIds: List<ConversationId>,
        labelId: LabelId
    ): Either<DataError, List<Conversation>> = addLabels(userId, conversationIds, listOf(labelId))

    override suspend fun addLabels(
        userId: UserId,
        conversationIds: List<ConversationId>,
        labelIds: List<LabelId>,
        option: ConversationLabelPropagationOptions
    ): Either<DataError, List<Conversation>> {
        val filteredLabelIdsToAdd = labelIds.filterUnmodifiableLabels()
        return conversationLocalDataSource.addLabels(userId, conversationIds, filteredLabelIdsToAdd).onRight {
            if (option.propagateToMessages) {
                messageLocalDataSource.relabelMessagesInConversations(
                    userId = userId,
                    conversationIds = conversationIds,
                    labelIdsToAdd = filteredLabelIdsToAdd.toSet()
                )
            }
            if (option.propagateRemotely) {
                conversationRemoteDataSource.addLabels(userId, conversationIds, filteredLabelIdsToAdd)
            }
        }
    }

    override suspend fun removeLabel(
        userId: UserId,
        conversationId: ConversationId,
        labelId: LabelId
    ): Either<DataError, Conversation> = removeLabel(userId, listOf(conversationId), labelId).map { it.first() }

    override suspend fun removeLabel(
        userId: UserId,
        conversationIds: List<ConversationId>,
        labelId: LabelId
    ): Either<DataError, List<Conversation>> = removeLabels(userId, conversationIds, listOf(labelId))

    override suspend fun removeLabels(
        userId: UserId,
        conversationIds: List<ConversationId>,
        labelIds: List<LabelId>,
        options: ConversationLabelPropagationOptions
    ): Either<DataError, List<Conversation>> {
        val filteredLabelIdsToRemove = labelIds.filterUnmodifiableLabels()
        return conversationLocalDataSource.removeLabels(userId, conversationIds, filteredLabelIdsToRemove).onRight {

            if (options.propagateToMessages) {
                messageLocalDataSource.relabelMessagesInConversations(
                    userId = userId,
                    conversationIds = conversationIds,
                    labelIdsToRemove = filteredLabelIdsToRemove.toSet()
                )
            }

            if (options.propagateRemotely) {
                conversationRemoteDataSource.removeLabels(userId, conversationIds, filteredLabelIdsToRemove)
            }
        }
    }

    override suspend fun move(
        userId: UserId,
        conversationIds: List<ConversationId>,
        allLabelIds: List<LabelId>,
        fromLabelIds: List<LabelId>,
        toLabelId: LabelId
    ): Either<DataError, List<Conversation>> {

        val labelsToRemove = if (toLabelId.isTrash() || toLabelId.isSpam()) {
            allLabelIds.filterUnmodifiableLabels()
        } else {
            fromLabelIds
        }

        return conversationLocalDataSource.relabel(
            userId = userId,
            conversationIds = conversationIds,
            labelIdsToAdd = listOf(toLabelId),
            labelIdsToRemove = labelsToRemove
        ).onRight {
            messageLocalDataSource.relabelMessagesInConversations(
                userId = userId,
                conversationIds = conversationIds,
                labelIdsToAdd = setOf(toLabelId),
                labelIdsToRemove = labelsToRemove.toSet()
            )

            conversationRemoteDataSource.addLabel(userId, conversationIds, toLabelId)
        }
    }

    override suspend fun markUnread(
        userId: UserId,
        conversationId: ConversationId,
        contextLabelId: LabelId
    ): Either<DataError, Conversation> = markUnread(userId, listOf(conversationId), contextLabelId)
        .map { it.first() }

    override suspend fun markUnread(
        userId: UserId,
        conversationIds: List<ConversationId>,
        contextLabelId: LabelId
    ): Either<DataError, List<Conversation>> {
        return conversationLocalDataSource.markUnread(userId, conversationIds, contextLabelId).onRight {
            messageLocalDataSource.markUnreadLastReadMessageInConversations(userId, conversationIds, contextLabelId)
            conversationRemoteDataSource.markUnread(userId, conversationIds, contextLabelId)
        }
    }

    override suspend fun markRead(userId: UserId, conversationId: ConversationId): Either<DataError, Conversation> =
        markRead(userId, listOf(conversationId)).map { it.first() }

    override suspend fun markRead(
        userId: UserId,
        conversationIds: List<ConversationId>
    ): Either<DataError, List<Conversation>> {
        return conversationLocalDataSource.markRead(userId, conversationIds).onRight {
            messageLocalDataSource.markMessagesInConversationsRead(userId, conversationIds)
            conversationRemoteDataSource.markRead(userId, conversationIds)
        }
    }

    override suspend fun isCachedConversationRead(
        userId: UserId,
        conversationId: ConversationId
    ): Either<DataError, Boolean> = conversationLocalDataSource.isConversationRead(userId, conversationId)

    override suspend fun relabel(
        userId: UserId,
        conversationId: ConversationId,
        labelsToBeRemoved: List<LabelId>,
        labelsToBeAdded: List<LabelId>
    ): Either<DataError, Conversation> =
        relabel(userId, listOf(conversationId), labelsToBeRemoved, labelsToBeAdded).map { it.first() }

    override suspend fun relabel(
        userId: UserId,
        conversationIds: List<ConversationId>,
        labelsToBeRemoved: List<LabelId>,
        labelsToBeAdded: List<LabelId>
    ): Either<DataError, List<Conversation>> {
        val removeOperation = removeLabels(userId, conversationIds, labelsToBeRemoved)
        if (removeOperation.isLeft()) return removeOperation

        return addLabels(userId, conversationIds, labelsToBeAdded)
    }

    @Suppress("TooGenericExceptionCaught")
    override suspend fun deleteConversations(
        userId: UserId,
        conversationIds: List<ConversationId>,
        contextLabelId: LabelId
    ): Either<DataError, Unit> {
        return conversationLocalDataSource.deleteConversations(userId, conversationIds).onRight {
            messageLocalDataSource.deleteMessagesInConversations(userId, conversationIds, contextLabelId)
            conversationRemoteDataSource.deleteConversations(userId, conversationIds, contextLabelId)
        }
    }

    override suspend fun deleteConversations(userId: UserId, labelId: LabelId) {
        conversationRemoteDataSource.clearLabel(userId, labelId)
    }

    override fun observeClearLabelOperation(userId: UserId, labelId: LabelId): Flow<Boolean> =
        conversationRemoteDataSource.observeClearWorkerIsEnqueuedOrRunning(userId, labelId)

    private suspend fun upsertConversations(
        userId: UserId,
        pageKey: PageKey,
        conversations: List<ConversationWithContext>
    ) = conversationLocalDataSource.upsertConversations(
        userId = userId,
        pageKey = pageKey,
        items = conversations
    )
}
