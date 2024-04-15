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

package ch.protonmail.android.mailconversation.data.remote

import androidx.work.ExistingWorkPolicy
import arrow.core.Either
import ch.protonmail.android.mailcommon.data.mapper.toEither
import ch.protonmail.android.mailcommon.data.worker.Enqueuer
import ch.protonmail.android.mailcommon.domain.benchmark.BenchmarkTracer
import ch.protonmail.android.mailcommon.domain.model.ConversationId
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailconversation.data.remote.worker.AddLabelConversationWorker
import ch.protonmail.android.mailconversation.data.remote.worker.ClearConversationLabelWorker
import ch.protonmail.android.mailconversation.data.remote.worker.DeleteConversationsWorker
import ch.protonmail.android.mailconversation.data.remote.worker.MarkConversationAsReadWorker
import ch.protonmail.android.mailconversation.data.remote.worker.MarkConversationAsUnreadWorker
import ch.protonmail.android.mailconversation.data.remote.worker.RemoveLabelConversationWorker
import ch.protonmail.android.mailconversation.domain.entity.ConversationWithContext
import ch.protonmail.android.mailconversation.domain.entity.ConversationWithMessages
import ch.protonmail.android.mailconversation.domain.repository.ConversationRemoteDataSource
import ch.protonmail.android.mailpagination.domain.model.OrderBy
import ch.protonmail.android.mailpagination.domain.model.OrderDirection
import ch.protonmail.android.mailpagination.domain.model.PageKey
import ch.protonmail.android.mailpagination.domain.model.ReadStatus
import kotlinx.coroutines.flow.Flow
import me.proton.core.domain.entity.UserId
import me.proton.core.label.domain.entity.LabelId
import me.proton.core.network.data.ApiProvider
import me.proton.core.util.kotlin.takeIfNotBlank
import javax.inject.Inject

class ConversationRemoteDataSourceImpl @Inject constructor(
    private val apiProvider: ApiProvider,
    private val enqueuer: Enqueuer,
    private val benchmarkTracer: BenchmarkTracer
) : ConversationRemoteDataSource {

    override suspend fun getConversations(
        userId: UserId,
        pageKey: PageKey
    ): Either<DataError.Remote, List<ConversationWithContext>> = apiProvider.get<ConversationApi>(userId).invoke {
        require(pageKey.size <= ConversationApi.maxPageSize)
        benchmarkTracer.begin("proton-api-get-conversations")

        getConversations(
            labelIds = listOf(pageKey.filter.labelId).map { it.id },
            beginTime = pageKey.filter.minTime.takeIf { it != Long.MIN_VALUE },
            endTime = pageKey.filter.maxTime.takeIf { it != Long.MAX_VALUE },
            beginId = pageKey.filter.minId,
            endId = pageKey.filter.maxId,
            keyword = pageKey.filter.keyword.takeIfNotBlank(),
            unread = when (pageKey.filter.read) {
                ReadStatus.All -> null
                ReadStatus.Read -> 0
                ReadStatus.Unread -> 1
            },
            sort = when (pageKey.orderBy) {
                OrderBy.Time -> "Time"
            },
            desc = when (pageKey.orderDirection) {
                OrderDirection.Ascending -> 0
                OrderDirection.Descending -> 1
            },
            pageSize = pageKey.size
        ).conversations.map { it.toConversationWithContext(userId, pageKey.filter.labelId) }
            .also { benchmarkTracer.end() }
    }.toEither()

    override suspend fun getConversationWithMessages(
        userId: UserId,
        conversationId: ConversationId
    ): ConversationWithMessages = apiProvider.get<ConversationApi>(userId).invoke {
        val response = getConversation(conversationId = conversationId.id)
        ConversationWithMessages(
            response.conversation.toConversation(userId),
            messages = response.messages.map { it.toMessage(userId) }
        )
    }.valueOrThrow

    override fun addLabel(
        userId: UserId,
        conversationIds: List<ConversationId>,
        labelId: LabelId
    ) {
        addLabels(userId, conversationIds, listOf(labelId))
    }

    override fun addLabels(
        userId: UserId,
        conversationIds: List<ConversationId>,
        labelIds: List<LabelId>
    ) {
        conversationIds.chunked(MAX_CONVERSATION_IDS_API_LIMIT).forEach { conversationIdsChunk ->
            labelIds.forEach { labelId ->
                enqueuer.enqueueUniqueWork<AddLabelConversationWorker>(
                    userId = userId,
                    workerId = AddLabelConversationWorker.id(userId),
                    params = AddLabelConversationWorker.params(
                        userId = userId,
                        conversationIds = conversationIdsChunk,
                        labelId = labelId
                    ),
                    existingWorkPolicy = ExistingWorkPolicy.APPEND_OR_REPLACE
                )
            }
        }
    }

    override fun removeLabel(
        userId: UserId,
        conversationIds: List<ConversationId>,
        labelId: LabelId
    ) {
        removeLabels(userId, conversationIds, listOf(labelId))
    }

    override fun removeLabels(
        userId: UserId,
        conversationIds: List<ConversationId>,
        labelIds: List<LabelId>
    ) {
        conversationIds.chunked(MAX_CONVERSATION_IDS_API_LIMIT).forEach { conversationIdsChunk ->
            labelIds.forEach { labelId ->
                enqueuer.enqueue<RemoveLabelConversationWorker>(
                    userId,
                    RemoveLabelConversationWorker.params(
                        userId = userId,
                        conversationIds = conversationIdsChunk,
                        labelId = labelId
                    )
                )
            }
        }
    }

    override suspend fun markUnread(
        userId: UserId,
        conversationIds: List<ConversationId>,
        contextLabelId: LabelId
    ) {
        conversationIds.chunked(MAX_CONVERSATION_IDS_API_LIMIT).forEach { conversationIdsChunk ->
            enqueuer.enqueue<MarkConversationAsUnreadWorker>(
                userId,
                MarkConversationAsUnreadWorker.params(
                    userId,
                    conversationIdsChunk,
                    contextLabelId
                )
            )
        }
    }

    override suspend fun markRead(userId: UserId, conversationIds: List<ConversationId>) {
        conversationIds.chunked(MAX_CONVERSATION_IDS_API_LIMIT).forEach { conversationIdsChunk ->
            enqueuer.enqueue<MarkConversationAsReadWorker>(
                userId,
                MarkConversationAsReadWorker.params(
                    userId,
                    conversationIdsChunk
                )
            )
        }
    }

    override fun deleteConversations(
        userId: UserId,
        conversationIds: List<ConversationId>,
        currentLabelId: LabelId
    ) {
        conversationIds.chunked(MAX_CONVERSATION_IDS_API_LIMIT).forEach {
            enqueuer.enqueue<DeleteConversationsWorker>(
                userId = userId,
                params = DeleteConversationsWorker.params(userId, it, currentLabelId)
            )
        }
    }

    override fun clearLabel(userId: UserId, labelId: LabelId) {
        enqueuer.enqueueUniqueWork<ClearConversationLabelWorker>(
            userId = userId,
            workerId = ClearConversationLabelWorker.id(userId, labelId),
            params = ClearConversationLabelWorker.params(userId, labelId)
        )
    }

    override fun observeClearWorkerIsEnqueuedOrRunning(userId: UserId, labelId: LabelId): Flow<Boolean> =
        enqueuer.observeWorkStatusIsEnqueuedOrRunning(ClearConversationLabelWorker.id(userId, labelId))

    companion object {

        const val MAX_CONVERSATION_IDS_API_LIMIT = 50
    }
}
