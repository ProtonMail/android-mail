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
import ch.protonmail.android.mailcommon.domain.mapper.mapToEither
import ch.protonmail.android.mailcommon.domain.model.ConversationId
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailconversation.data.remote.ConversationApi
import ch.protonmail.android.mailconversation.domain.entity.Conversation
import ch.protonmail.android.mailconversation.domain.entity.ConversationWithContext
import ch.protonmail.android.mailconversation.domain.repository.ConversationLocalDataSource
import ch.protonmail.android.mailconversation.domain.repository.ConversationRemoteDataSource
import ch.protonmail.android.mailconversation.domain.repository.ConversationRepository
import ch.protonmail.android.mailmessage.data.local.MessageLocalDataSource
import ch.protonmail.android.mailpagination.domain.model.PageKey
import com.dropbox.android.external.store4.Fetcher
import com.dropbox.android.external.store4.SourceOfTruth
import com.dropbox.android.external.store4.StoreBuilder
import com.dropbox.android.external.store4.StoreRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapLatest
import me.proton.core.data.arch.ProtonStore
import me.proton.core.data.arch.buildProtonStore
import me.proton.core.data.arch.toDataResult
import me.proton.core.domain.entity.UserId
import me.proton.core.label.domain.entity.LabelId
import me.proton.core.util.kotlin.CoroutineScopeProvider
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min

@Singleton
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
    ).scope(coroutineScopeProvider.GlobalIOSupervisedScope).buildProtonStore()

    override suspend fun getConversations(
        userId: UserId,
        pageKey: PageKey
    ): List<ConversationWithContext> = conversationLocalDataSource.getConversations(
        userId = userId,
        pageKey = pageKey
    ).let { conversations ->
        if (conversationLocalDataSource.isLocalPageValid(userId, pageKey, conversations)) conversations
        else runCatching { fetchConversations(userId, pageKey) }.getOrElse { conversations }
    }

    override suspend fun markAsStale(
        userId: UserId,
        labelId: LabelId
    ) = conversationLocalDataSource.markAsStale(userId, labelId)

    override fun observeConversation(
        userId: UserId,
        id: ConversationId
    ): Flow<Either<DataError, Conversation>> = conversationStore.stream(
        StoreRequest.cached(ConversationKey(userId, id), true)
    ).mapLatest { it.toDataResult() }
        .mapToEither()
        .distinctUntilChanged()

    override suspend fun addLabel(
        userId: UserId,
        conversationId: ConversationId,
        labelId: LabelId
    ): Either<DataError, Conversation> {
        val conversationEither = conversationLocalDataSource.addLabel(userId, conversationId, labelId)
        return conversationEither.tap {
            val effectedMessages = messageLocalDataSource.observeMessages(userId, conversationId).first()
                .filterNot { it.labelIds.contains(labelId) }
            effectedMessages.map {
                it.copy(
                    labelIds = it.labelIds + labelId
                )
            }.let {
                messageLocalDataSource.upsertMessages(it)
            }

            conversationRemoteDataSource.addLabel(
                userId,
                conversationId,
                labelId,
                effectedMessages.map { it.messageId }
            )
        }
    }

    override suspend fun removeLabel(
        userId: UserId,
        conversationId: ConversationId,
        labelId: LabelId
    ): Either<DataError, Conversation> {
        return conversationLocalDataSource.removeLabel(userId, conversationId, labelId).tap {
            val effectedMessages = messageLocalDataSource.observeMessages(userId, conversationId).first()
                .filter { it.labelIds.contains(labelId) }
            effectedMessages.map {
                it.copy(
                    labelIds = it.labelIds - labelId
                )
            }.let {
                messageLocalDataSource.upsertMessages(it)
            }

            conversationRemoteDataSource.removeLabel(
                userId,
                conversationId,
                labelId,
                effectedMessages.map { it.messageId })
        }
    }

    private suspend fun fetchConversations(
        userId: UserId,
        pageKey: PageKey
    ) = conversationLocalDataSource.getClippedPageKey(
        userId = userId,
        pageKey = pageKey.copy(size = min(ConversationApi.maxPageSize, pageKey.size))
    ).let { adaptedPageKey ->
        conversationRemoteDataSource.getConversations(
            userId = userId,
            pageKey = adaptedPageKey
        ).also { conversations -> insertConversations(userId, adaptedPageKey, conversations) }
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
