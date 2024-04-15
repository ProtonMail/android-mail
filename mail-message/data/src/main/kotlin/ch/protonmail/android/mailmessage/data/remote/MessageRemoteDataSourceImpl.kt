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

package ch.protonmail.android.mailmessage.data.remote

import androidx.work.ExistingWorkPolicy
import arrow.core.Either
import ch.protonmail.android.mailcommon.data.mapper.toEither
import ch.protonmail.android.mailcommon.data.worker.Enqueuer
import ch.protonmail.android.mailcommon.domain.benchmark.BenchmarkTracer
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailmessage.data.remote.resource.MessagePhishingReportBody
import ch.protonmail.android.mailmessage.data.remote.worker.AddLabelMessageWorker
import ch.protonmail.android.mailmessage.data.remote.worker.ClearMessageLabelWorker
import ch.protonmail.android.mailmessage.data.remote.worker.DeleteMessagesWorker
import ch.protonmail.android.mailmessage.data.remote.worker.MarkMessageAsReadWorker
import ch.protonmail.android.mailmessage.data.remote.worker.MarkMessageAsUnreadWorker
import ch.protonmail.android.mailmessage.data.remote.worker.RemoveLabelMessageWorker
import ch.protonmail.android.mailmessage.domain.model.DecryptedMessageBody
import ch.protonmail.android.mailmessage.domain.model.Message
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailmessage.domain.model.MessageWithBody
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

class MessageRemoteDataSourceImpl @Inject constructor(
    private val apiProvider: ApiProvider,
    private val enqueuer: Enqueuer,
    private val benchmarkTracer: BenchmarkTracer
) : MessageRemoteDataSource {

    override suspend fun getMessages(userId: UserId, pageKey: PageKey): Either<DataError.Remote, List<Message>> =
        apiProvider.get<MessageApi>(userId).invoke {
            require(pageKey.size <= MessageApi.maxPageSize)
            benchmarkTracer.begin("proton-api-get-messages")

            getMessages(
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
            ).messages.map { it.toMessage(userId) }.also {
                benchmarkTracer.end()
            }
        }.toEither()

    /**
     * Needed by ProtonStore (DropboxStore) implementation, which expects an exception when fetching fails.
     * Such exception is later used by our own Mappers to extract the error (Store -> DataError -> Either (Mappers))
     */
    override suspend fun getMessageOrThrow(userId: UserId, messageId: MessageId): MessageWithBody =
        fetchMessage(userId, messageId).valueOrThrow

    override suspend fun getMessage(userId: UserId, messageId: MessageId): Either<DataError, MessageWithBody> =
        fetchMessage(userId, messageId).toEither()

    override fun addLabelsToMessages(
        userId: UserId,
        messageIds: List<MessageId>,
        labelIds: List<LabelId>
    ) {
        messageIds.chunked(MAX_ACTION_WORKER_PARAMETER_COUNT).forEach { messages ->
            labelIds.forEach { labelId ->
                enqueuer.enqueueUniqueWork<AddLabelMessageWorker>(
                    userId = userId,
                    workerId = AddLabelMessageWorker.id(userId),
                    params = AddLabelMessageWorker.params(userId, messages, labelId),
                    existingWorkPolicy = ExistingWorkPolicy.APPEND_OR_REPLACE
                )
            }
        }
    }

    override fun removeLabelsFromMessages(
        userId: UserId,
        messageIds: List<MessageId>,
        labelIds: List<LabelId>
    ) {
        messageIds.chunked(MAX_ACTION_WORKER_PARAMETER_COUNT).forEach {
            labelIds.forEach { labelIds ->
                enqueuer.enqueue<RemoveLabelMessageWorker>(
                    userId, RemoveLabelMessageWorker.params(userId, it, labelIds)
                )
            }
        }
    }

    override fun markUnread(userId: UserId, messageIds: List<MessageId>) {
        messageIds
            .chunked(MAX_ACTION_WORKER_PARAMETER_COUNT)
            .forEach {
                enqueuer.enqueue<MarkMessageAsUnreadWorker>(userId, MarkMessageAsUnreadWorker.params(userId, it))
            }
    }

    override fun markRead(userId: UserId, messageIds: List<MessageId>) {
        messageIds.chunked(MAX_ACTION_WORKER_PARAMETER_COUNT)
            .forEach { enqueuer.enqueue<MarkMessageAsReadWorker>(userId, MarkMessageAsReadWorker.params(userId, it)) }
    }

    private suspend fun fetchMessage(userId: UserId, messageId: MessageId) =
        apiProvider.get<MessageApi>(userId).invoke {
            getMessage(messageId = messageId.id).message.toMessageWithBody(userId)
        }

    override fun deleteMessages(
        userId: UserId,
        messageIds: List<MessageId>,
        currentLabelId: LabelId
    ) {
        messageIds.chunked(MAX_ACTION_WORKER_PARAMETER_COUNT)
            .forEach {
                enqueuer.enqueue<DeleteMessagesWorker>(userId, DeleteMessagesWorker.params(userId, it, currentLabelId))
            }
    }

    override fun clearLabel(userId: UserId, labelId: LabelId) {
        enqueuer.enqueueUniqueWork<ClearMessageLabelWorker>(
            userId = userId,
            workerId = ClearMessageLabelWorker.id(userId, labelId),
            params = ClearMessageLabelWorker.params(userId, labelId)
        )
    }

    override fun observeClearWorkerIsEnqueuedOrRunning(userId: UserId, labelId: LabelId): Flow<Boolean> =
        enqueuer.observeWorkStatusIsEnqueuedOrRunning(ClearMessageLabelWorker.id(userId, labelId))

    override suspend fun reportPhishing(
        userId: UserId,
        decryptedMessageBody: DecryptedMessageBody
    ): Either<DataError.Remote, Unit> = apiProvider.get<MessageApi>(userId).invoke {
        reportPhishing(
            MessagePhishingReportBody(
                messageId = decryptedMessageBody.messageId.id,
                mimeType = decryptedMessageBody.mimeType.value,
                body = decryptedMessageBody.value
            )
        )
    }.toEither().map { }

    companion object {

        const val MAX_ACTION_WORKER_PARAMETER_COUNT = 100
    }
}
