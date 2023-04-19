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
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.mapper.mapToEither
import ch.protonmail.android.mailcommon.domain.model.ConversationId
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcommon.domain.model.isOfflineError
import ch.protonmail.android.mailconversation.data.remote.ConversationApi
import ch.protonmail.android.mailconversation.domain.entity.Conversation
import ch.protonmail.android.mailconversation.domain.entity.ConversationWithContext
import ch.protonmail.android.mailconversation.domain.repository.ConversationLocalDataSource
import ch.protonmail.android.mailconversation.domain.repository.ConversationRemoteDataSource
import ch.protonmail.android.mailconversation.domain.repository.ConversationRepository
import ch.protonmail.android.maillabel.domain.extension.isSpam
import ch.protonmail.android.maillabel.domain.extension.isTrash
import ch.protonmail.android.maillabel.domain.model.SystemLabelId
import ch.protonmail.android.mailmessage.data.local.MessageLocalDataSource
import ch.protonmail.android.mailpagination.domain.model.PageKey
import com.dropbox.android.external.store4.Fetcher
import com.dropbox.android.external.store4.SourceOfTruth
import com.dropbox.android.external.store4.StoreBuilder
import com.dropbox.android.external.store4.StoreRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import me.proton.core.data.arch.ProtonStore
import me.proton.core.data.arch.buildProtonStore
import me.proton.core.data.arch.toDataResult
import me.proton.core.domain.arch.DataResult
import me.proton.core.domain.arch.ResponseSource
import me.proton.core.domain.entity.UserId
import me.proton.core.label.domain.entity.LabelId
import me.proton.core.util.kotlin.CoroutineScopeProvider
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min

@Singleton
@Suppress("TooManyFunctions")
class ConversationRepositoryImpl @Inject constructor(
    private val conversationLocalDataSource: ConversationLocalDataSource,
    private val conversationRemoteDataSource: ConversationRemoteDataSource,
    coroutineScopeProvider: CoroutineScopeProvider,
    private val messageLocalDataSource: MessageLocalDataSource
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
                messageLocalDataSource.upsertMessages(messages)
            }
        )
    ).buildProtonStore(coroutineScopeProvider)

    override suspend fun getConversations(
        userId: UserId,
        pageKey: PageKey
    ): Either<DataError.Remote, List<ConversationWithContext>> = conversationLocalDataSource.getConversations(
        userId = userId,
        pageKey = pageKey
    ).let { cachedConversations ->
        val isLocalPageValid = conversationLocalDataSource.isLocalPageValid(userId, pageKey, cachedConversations)
        if (isLocalPageValid) cachedConversations.right()
        else fetchConversations(userId, pageKey).fold(
            ifLeft = { dataError ->
                Timber.w("Failed to fetch conversations from remote, returning cached conversations. $dataError")
                cachedConversations.right()
            },
            ifRight = { it.right() }
        )
    }

    override suspend fun loadConversations(
        userId: UserId,
        pageKey: PageKey
    ): Either<DataError, List<ConversationWithContext>> =
        conversationLocalDataSource.getConversations(userId, pageKey).right()

    override suspend fun fetchConversations(
        userId: UserId,
        pageKey: PageKey
    ): Either<DataError.Remote, List<ConversationWithContext>> = conversationLocalDataSource.getClippedPageKey(
        userId = userId,
        pageKey = pageKey.copy(size = min(ConversationApi.maxPageSize, pageKey.size))
    )?.let { adaptedPageKey ->
        conversationRemoteDataSource.getConversations(userId = userId, pageKey = adaptedPageKey)
            .onRight { conversations -> insertConversations(userId, adaptedPageKey, conversations) }
    } ?: emptyList<ConversationWithContext>().right()

    override suspend fun markAsStale(userId: UserId, labelId: LabelId) =
        conversationLocalDataSource.markAsStale(userId, labelId)

    override fun observeConversation(
        userId: UserId,
        id: ConversationId,
        refreshData: Boolean
    ): Flow<Either<DataError, Conversation>> = buildStoreStream(userId, id, refreshData)
        .mapToEither()

    override fun observeConversationCacheUpToDate(userId: UserId, id: ConversationId): Flow<Either<DataError, Unit>> {
        return buildStoreStream(userId, id, true)
            .filter { it.source == ResponseSource.Remote }
            .distinctUntilChanged()
            .mapToEither()
            .map {
                it.fold(
                    ifLeft = { dataError ->
                        if (dataError.isOfflineError()) {
                            Unit.right()
                        } else {
                            dataError.left()
                        }
                    },
                    ifRight = { Unit.right() }
                )
            }
    }

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
    ): Either<DataError, Conversation> = addLabels(userId, conversationId, listOf(labelId))

    override suspend fun addLabels(
        userId: UserId,
        conversationId: ConversationId,
        labelIds: List<LabelId>
    ): Either<DataError, Conversation> {
        val conversationEither = conversationLocalDataSource.addLabels(userId, conversationId, labelIds)
        return conversationEither.tap {
            val affectedMessages = messageLocalDataSource.observeMessages(userId, conversationId).first()
                .filterNot { it.labelIds.containsAll(labelIds) }
            val updatedMessages = affectedMessages.map {
                it.copy(labelIds = it.labelIds.union(labelIds).toList())
            }

            if (updatedMessages.isNotEmpty()) {
                messageLocalDataSource.upsertMessages(updatedMessages)
            }

            conversationRemoteDataSource.addLabels(
                userId,
                conversationId,
                labelIds,
                affectedMessages.map { it.messageId }
            )
        }
    }

    override suspend fun removeLabel(
        userId: UserId,
        conversationId: ConversationId,
        labelId: LabelId
    ): Either<DataError, Conversation> = removeLabels(userId, conversationId, listOf(labelId))

    override suspend fun removeLabels(
        userId: UserId,
        conversationId: ConversationId,
        labelIds: List<LabelId>
    ): Either<DataError, Conversation> {
        return conversationLocalDataSource.removeLabels(userId, conversationId, labelIds).tap {
            val affectedMessages = messageLocalDataSource.observeMessages(userId, conversationId).first()
                .filter { it.labelIds.intersect(labelIds).isNotEmpty() }
            val updatedMessages = affectedMessages.map {
                it.copy(labelIds = it.labelIds - labelIds)
            }
            if (updatedMessages.isNotEmpty()) {
                messageLocalDataSource.upsertMessages(updatedMessages)
            }

            conversationRemoteDataSource.removeLabels(
                userId,
                conversationId,
                labelIds,
                affectedMessages.map { it.messageId }
            )
        }
    }

    override suspend fun move(
        userId: UserId,
        conversationId: ConversationId,
        fromLabelIds: List<LabelId>,
        toLabelId: LabelId
    ): Either<DataError, Conversation> {
        if (toLabelId.isTrash() || toLabelId.isSpam()) {
            return moveToTrashOrSpam(userId, conversationId, toLabelId)
        }

        conversationLocalDataSource.observeConversation(userId, conversationId).first()
            ?: return DataError.Local.NoDataCached.left()

        if (fromLabelIds.isNotEmpty()) {
            conversationLocalDataSource.removeLabels(userId, conversationId, fromLabelIds)
        }

        messageLocalDataSource.observeMessages(userId, conversationId).first()
            .map { message ->
                messageLocalDataSource.removeLabels(userId, message.messageId, fromLabelIds)
                    .onLeft { Timber.d("Failed to remove label") }
            }

        return addLabel(userId, conversationId, toLabelId)
    }

    override suspend fun markUnread(
        userId: UserId,
        conversationId: ConversationId,
        contextLabelId: LabelId
    ): Either<DataError.Local, Conversation> {
        messageLocalDataSource.observeMessages(userId, conversationId).first()
            .filter { message -> message.read && message.labelIds.contains(contextLabelId) }
            .maxByOrNull { message -> message.time }
            ?.let { message -> messageLocalDataSource.markUnread(userId, message.messageId) }

        conversationRemoteDataSource.markUnread(userId, conversationId, contextLabelId)
        return conversationLocalDataSource.markUnread(userId, conversationId, contextLabelId)
    }

    override suspend fun markRead(
        userId: UserId,
        conversationId: ConversationId,
        contextLabelId: LabelId
    ): Either<DataError.Local, Conversation> {
        conversationRemoteDataSource.markRead(userId, conversationId, contextLabelId)
        return conversationLocalDataSource.markRead(userId, conversationId, contextLabelId)
    }

    override suspend fun isCachedConversationRead(
        userId: UserId,
        conversationId: ConversationId
    ): Either<DataError, Boolean> =
        conversationLocalDataSource.isConversationRead(userId, conversationId)

    override suspend fun relabel(
        userId: UserId,
        conversationId: ConversationId,
        labelsToBeRemoved: List<LabelId>,
        labelsToBeAdded: List<LabelId>
    ): Either<DataError, Conversation> {
        val removeOperation = removeLabels(userId, conversationId, labelsToBeRemoved)
        if (removeOperation.isLeft()) return removeOperation

        return addLabels(userId, conversationId, labelsToBeAdded)
    }

    private suspend fun moveToTrashOrSpam(
        userId: UserId,
        conversationId: ConversationId,
        labelId: LabelId
    ): Either<DataError, Conversation> {
        require(labelId.isTrash() || labelId.isSpam()) { "Invalid system label id: $labelId" }

        val persistentLabels = listOf(
            SystemLabelId.AllDrafts.labelId,
            SystemLabelId.AllMail.labelId,
            SystemLabelId.AllSent.labelId
        )

        val conversation = conversationLocalDataSource.observeConversation(userId, conversationId).first()
            ?: return DataError.Local.NoDataCached.left()
        val messages = messageLocalDataSource.observeMessages(userId, conversationId).first()

        val updatedConversation = conversation.copy(
            labels = conversation.labels.filter { conversationLabel ->
                conversationLabel.labelId in persistentLabels
            }
        )
        val updatedMessages = messages.map { message ->
            message.copy(
                labelIds = message.labelIds.filter { labelId ->
                    labelId in persistentLabels
                }
            )
        }

        conversationLocalDataSource.upsertConversation(userId, updatedConversation)
        messageLocalDataSource.upsertMessages(updatedMessages)
        return addLabel(userId, conversationId, labelId)
    }

    private suspend fun insertConversations(
        userId: UserId,
        pageKey: PageKey,
        conversations: List<ConversationWithContext>
    ) = conversationLocalDataSource.upsertConversations(
        userId = userId,
        pageKey = pageKey,
        items = conversations
    )
}
