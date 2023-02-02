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
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailmessage.data.remote.worker.AddLabelMessageWorker
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
    private val addLabelMessageWorker: AddLabelMessageWorker.Enqueuer,
    private val markMessageAsUnreadWorker: MarkMessageAsUnreadWorker.Enqueuer,
    private val removeLabelMessageWorker: RemoveLabelMessageWorker.Enqueuer
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

    override fun addLabel(
        userId: UserId,
        messageId: MessageId,
        labelId: LabelId
    ) = addLabels(userId, messageId, listOf(labelId))

    override fun addLabels(
        userId: UserId,
        messageId: MessageId,
        labelIds: List<LabelId>
    ) {
        labelIds.forEach { labelId -> addLabelMessageWorker.enqueue(userId, messageId, labelId) }
    }

    override fun removeLabel(
        userId: UserId,
        messageId: MessageId,
        labelId: LabelId
    ) = removeLabels(userId, messageId, listOf(labelId))

    override fun removeLabels(
        userId: UserId,
        messageId: MessageId,
        labelIds: List<LabelId>
    ) {
        labelIds.forEach { labelId -> removeLabelMessageWorker.enqueue(userId, messageId, labelId) }
    }

    override fun markUnread(userId: UserId, messageId: MessageId) {
        markMessageAsUnreadWorker.enqueue(userId, messageId)
    }
}
