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

package ch.protonmail.android.mailmessage.data.local

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.ConversationId
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailmessage.data.local.entity.MessageLabelEntity
import ch.protonmail.android.mailmessage.data.mapper.MessageWithBodyEntityMapper
import ch.protonmail.android.mailmessage.domain.entity.Message
import ch.protonmail.android.mailmessage.domain.entity.MessageId
import ch.protonmail.android.mailmessage.domain.entity.MessageWithBody
import ch.protonmail.android.mailpagination.data.local.getClippedPageKey
import ch.protonmail.android.mailpagination.data.local.isLocalPageValid
import ch.protonmail.android.mailpagination.data.local.upsertPageInterval
import ch.protonmail.android.mailpagination.domain.model.PageItemType
import ch.protonmail.android.mailpagination.domain.model.PageKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapLatest
import me.proton.core.domain.entity.UserId
import me.proton.core.label.domain.entity.LabelId
import javax.inject.Inject

class MessageLocalDataSourceImpl @Inject constructor(
    private val db: MessageDatabase,
    private val messageWithBodyEntityMapper: MessageWithBodyEntityMapper
) : MessageLocalDataSource {

    private val messageDao = db.messageDao()
    private val messageLabelDao = db.messageLabelDao()
    private val pageIntervalDao = db.pageIntervalDao()
    private val messageBodyDao = db.messageBodyDao()

    override suspend fun deleteAllMessages(
        userId: UserId
    ) = db.inTransaction {
        messageDao.deleteAll(userId)
        pageIntervalDao.deleteAll(userId, PageItemType.Message)
    }

    override suspend fun deleteMessage(
        userId: UserId,
        ids: List<MessageId>
    ) = messageDao.delete(userId, ids.map { it.id })

    override suspend fun getClippedPageKey(
        userId: UserId,
        pageKey: PageKey
    ): PageKey = pageIntervalDao.getClippedPageKey(userId, PageItemType.Message, pageKey)

    override suspend fun getMessages(
        userId: UserId,
        pageKey: PageKey
    ): List<Message> = observeMessages(userId, pageKey).first()

    override suspend fun isLocalPageValid(
        userId: UserId,
        pageKey: PageKey,
        items: List<Message>
    ): Boolean = pageIntervalDao.isLocalPageValid(userId, PageItemType.Message, pageKey, items)

    override suspend fun markAsStale(
        userId: UserId,
        labelId: LabelId
    ) = pageIntervalDao.deleteAll(userId, PageItemType.Message, labelId)

    override fun observeMessage(
        userId: UserId,
        messageId: MessageId
    ): Flow<Message?> = messageDao.observe(userId, messageId).mapLatest { it?.toMessage() }

    override fun observeMessages(
        userId: UserId,
        conversationId: ConversationId
    ): Flow<List<Message>> = messageDao.observeAllOrderByTimeAsc(
        userId = userId,
        conversationId = conversationId
    ).mapLatest { list -> list.map { messageWithLabelIds -> messageWithLabelIds.toMessage() } }

    override fun observeMessages(
        userId: UserId,
        pageKey: PageKey
    ): Flow<List<Message>> = messageDao
        .observeAll(userId, pageKey)
        .mapLatest { list -> list.map { it.toMessage() } }


    override suspend fun upsertMessage(
        message: Message
    ) = db.inTransaction {
        upsertMessages(listOf(message))
    }

    override suspend fun upsertMessages(
        items: List<Message>
    ) = db.inTransaction {
        messageDao.insertOrUpdate(*items.map { it.toEntity() }.toTypedArray())
        updateLabels(items)
    }

    override suspend fun upsertMessages(
        userId: UserId,
        pageKey: PageKey,
        items: List<Message>
    ) = db.inTransaction {
        upsertMessages(items)
        upsertPageInterval(userId, pageKey, items)
    }

    override fun observeMessageWithBody(userId: UserId, messageId: MessageId): Flow<MessageWithBody?> {
        return messageBodyDao.observeMessageWithBodyEntity(userId, messageId).mapLatest { messageWithBodyEntity ->
            if (messageWithBodyEntity != null) {
                messageWithBodyEntityMapper.toMessageWithBody(messageWithBodyEntity)
            } else null
        }
    }

    override suspend fun upsertMessageWithBody(userId: UserId, messageWithBody: MessageWithBody) = db.inTransaction {
        upsertMessage(messageWithBody.message)
        messageBodyDao.insertOrUpdate(messageWithBodyEntityMapper.toMessageBodyEntity(messageWithBody.messageBody))
    }

    override suspend fun addLabel(
        userId: UserId,
        messageId: MessageId,
        labelId: LabelId
    ): Either<DataError.Local, Message> {
        val message = observeMessage(userId, messageId).first()
            ?: return DataError.Local.NoDataCached.left()
        val updatedMessage = message.copy(
            labelIds = message.labelIds.toMutableSet().apply { add(labelId) }.toList()
        )
        upsertMessage(updatedMessage)
        return updatedMessage.right()
    }

    override suspend fun removeLabel(
        userId: UserId,
        messageId: MessageId,
        labelId: LabelId
    ): Either<DataError.Local, Message> {
        val message = observeMessage(userId, messageId).first()
            ?: return DataError.Local.NoDataCached.left()
        val updatedMessage = message.copy(
            labelIds = message.labelIds - labelId
        )
        upsertMessage(updatedMessage)
        return updatedMessage.right()
    }

    override suspend fun markUnread(userId: UserId, messageId: MessageId): Either<DataError.Local, Message> =
        DataError.Local.NoDataCached.left()

    private suspend fun updateLabels(
        messages: List<Message>
    ) = with(groupByUserId(messages)) {
        deleteLabels()
        insertLabels()
    }

    private suspend fun upsertPageInterval(
        userId: UserId,
        pageKey: PageKey,
        messages: List<Message>
    ) = pageIntervalDao.upsertPageInterval(userId, PageItemType.Message, pageKey, messages)

    private fun groupByUserId(messages: List<Message>) = messages.fold(
        mutableMapOf<UserId, MutableList<Message>>()
    ) { acc, message ->
        acc.apply { getOrPut(message.userId) { mutableListOf() }.add(message) }
    }.toMap()

    private suspend fun Map<UserId, MutableList<Message>>.insertLabels() {
        entries.forEach { (userId, messages) ->
            messages.forEach { message ->
                message.labelIds.forEach { labelId ->
                    messageLabelDao.insertOrUpdate(
                        MessageLabelEntity(userId, labelId, message.messageId)
                    )
                }
            }
        }
    }

    private suspend fun Map<UserId, MutableList<Message>>.deleteLabels() {
        entries.forEach { (userId, messages) ->
            messageLabelDao.deleteAll(userId, messages.map { it.messageId })
        }
    }
}
