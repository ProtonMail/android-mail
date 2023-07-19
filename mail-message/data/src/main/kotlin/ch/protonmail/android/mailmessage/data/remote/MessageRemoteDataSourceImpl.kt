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

import arrow.core.Either
import ch.protonmail.android.mailcommon.data.mapper.toEither
import ch.protonmail.android.mailcommon.data.worker.Enqueuer
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailmessage.data.remote.worker.AddLabelMessageWorker
import ch.protonmail.android.mailmessage.data.remote.worker.MarkMessageAsReadWorker
import ch.protonmail.android.mailmessage.data.remote.worker.MarkMessageAsUnreadWorker
import ch.protonmail.android.mailmessage.data.remote.worker.RemoveLabelMessageWorker
import ch.protonmail.android.mailmessage.domain.entity.Message
import ch.protonmail.android.mailmessage.domain.entity.MessageId
import ch.protonmail.android.mailmessage.domain.entity.MessageWithBody
import ch.protonmail.android.mailpagination.domain.model.OrderBy
import ch.protonmail.android.mailpagination.domain.model.OrderDirection
import ch.protonmail.android.mailpagination.domain.model.PageKey
import ch.protonmail.android.mailpagination.domain.model.ReadStatus
import me.proton.core.domain.entity.UserId
import me.proton.core.label.domain.entity.LabelId
import me.proton.core.network.data.ApiProvider
import me.proton.core.util.kotlin.takeIfNotBlank
import javax.inject.Inject

class MessageRemoteDataSourceImpl @Inject constructor(
    private val apiProvider: ApiProvider,
    private val enqueuer: Enqueuer
) : MessageRemoteDataSource {

    override suspend fun getMessages(userId: UserId, pageKey: PageKey): Either<DataError.Remote, List<Message>> =
        apiProvider.get<MessageApi>(userId).invoke {
            require(pageKey.size <= MessageApi.maxPageSize)
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
            ).messages.map { it.toMessage(userId) }
        }.toEither()

    override suspend fun getMessage(userId: UserId, messageId: MessageId): MessageWithBody =
        apiProvider.get<MessageApi>(userId).invoke {
            getMessage(
                messageId = messageId.id
            ).message.toMessageWithBody(userId)
        }.valueOrThrow

    override fun addLabelsToMessages(
        userId: UserId,
        messageIds: List<MessageId>,
        labelIds: List<LabelId>
    ) {
        messageIds.chunked(Enqueuer.MAX_PARAMETER_COUNT).forEach { messages ->
            labelIds.forEach { labelId ->
                enqueuer.enqueue<AddLabelMessageWorker>(AddLabelMessageWorker.params(userId, messages, labelId))
            }
        }
    }

    override fun removeLabelsFromMessages(
        userId: UserId,
        messageIds: List<MessageId>,
        labelIds: List<LabelId>
    ) {
        messageIds.chunked(Enqueuer.MAX_PARAMETER_COUNT).forEach {
            labelIds.forEach { labelIds ->
                enqueuer.enqueue<RemoveLabelMessageWorker>(RemoveLabelMessageWorker.params(userId, it, labelIds))
            }
        }
    }

    override fun markUnread(userId: UserId, messageId: MessageId) {
        enqueuer.enqueue<MarkMessageAsUnreadWorker>(MarkMessageAsUnreadWorker.params(userId, messageId))
    }

    override fun markRead(userId: UserId, messageId: MessageId) {
        enqueuer.enqueue<MarkMessageAsReadWorker>(MarkMessageAsReadWorker.params(userId, messageId))
    }
}
