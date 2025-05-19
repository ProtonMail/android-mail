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
import arrow.core.raise.either
import arrow.core.right
import ch.protonmail.android.mailcommon.data.db.dao.upsertOrError
import ch.protonmail.android.mailcommon.data.file.shouldBeStoredAsFile
import ch.protonmail.android.mailcommon.domain.model.ConversationId
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailmessage.data.local.entity.MessageLabelEntity
import ch.protonmail.android.mailmessage.data.local.relation.MessageWithBodyEntity
import ch.protonmail.android.mailmessage.data.mapper.MessageAttachmentEntityMapper
import ch.protonmail.android.mailmessage.data.mapper.MessageWithBodyEntityMapper
import ch.protonmail.android.mailmessage.domain.model.Message
import ch.protonmail.android.mailmessage.domain.model.MessageAttachment
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailmessage.domain.model.MessageWithBody
import ch.protonmail.android.mailpagination.data.local.getClippedPageKey
import ch.protonmail.android.mailpagination.data.local.isLocalPageValid
import ch.protonmail.android.mailpagination.data.local.upsertPageInterval
import ch.protonmail.android.mailpagination.domain.model.PageItemType
import ch.protonmail.android.mailpagination.domain.model.PageKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import me.proton.core.domain.entity.UserId
import me.proton.core.label.domain.entity.LabelId
import timber.log.Timber
import javax.inject.Inject

@Suppress("TooManyFunctions")
class MessageLocalDataSourceImpl @Inject constructor(
    private val db: MessageDatabase,
    private val messageBodyFileStorage: MessageBodyFileStorage,
    private val messageWithBodyEntityMapper: MessageWithBodyEntityMapper,
    private val messageAttachmentEntityMapper: MessageAttachmentEntityMapper,
    private val attachmentFileStorage: AttachmentFileStorage,
    private val searchResultsLocalDataSource: SearchResultsLocalDataSource
) : MessageLocalDataSource {

    private val messageDao = db.messageDao()
    private val messageLabelDao = db.messageLabelDao()
    private val pageIntervalDao = db.pageIntervalDao()
    private val messageBodyDao = db.messageBodyDao()
    private val messageAttachmentDao = db.messageAttachmentDao()

    override suspend fun deleteAllMessages(userId: UserId) = db.inTransaction {
        messageBodyFileStorage.deleteAllMessageBodies(userId)
        messageDao.deleteAll(userId)
        pageIntervalDao.deleteAll(userId, PageItemType.Message)
    }

    override suspend fun deleteSearchIntervals(userId: UserId) {
        pageIntervalDao.deleteSearchedIntervals()
    }

    override suspend fun deleteAllMessagesExcept(userId: UserId, messageIdsToExclude: List<MessageId>) =
        db.inTransaction {
            messageBodyFileStorage.deleteAllMessageBodies(userId)
            messageDao.deleteAllExcept(userId, messageIdsToExclude)
            pageIntervalDao.deleteAll(userId, PageItemType.Message)
        }

    override suspend fun deleteMessages(userId: UserId, ids: List<MessageId>) {
        ids.chunked(SQL_CHUNK_SIZE).forEach { chunkedIds ->
            messageDao.delete(userId, chunkedIds.map { it.id })
        }
        ids.forEach { messageBodyFileStorage.deleteMessageBody(userId, it) }
    }

    override suspend fun deleteMessagesWithId(
        userId: UserId,
        messageIds: List<MessageId>
    ): Either<DataError.Local, Unit> {
        runCatching {
            deleteMessages(userId, messageIds)
        }.getOrElse {
            Timber.e(it, "Failed to delete messages")
            return DataError.Local.DeletingFailed.left()
        }
        return Unit.right()
    }

    override suspend fun deleteMessagesInConversations(
        userId: UserId,
        conversationIds: List<ConversationId>,
        contextLabelId: LabelId
    ): Either<DataError.Local, Unit> {
        runCatching {
            messageDao.getMessageWithLabelsInConversations(userId, conversationIds)
                .filter { it.labelIds.contains(contextLabelId) }
                .let { messageWithLabelIds -> deleteMessages(userId, messageWithLabelIds.map { it.message.messageId }) }
        }.getOrElse {
            Timber.e(it, "Failed to delete messages")
            return DataError.Local.DeletingFailed.left()
        }
        return Unit.right()
    }

    override suspend fun deleteMessagesWithLabel(userId: UserId, labelId: LabelId): Either<DataError.Local, Unit> {
        runCatching {
            messageDao.observeMessages(userId, labelId).first().let { messageWithLabelIds ->
                deleteMessages(userId, messageWithLabelIds.map { it.message.messageId })
            }
        }.getOrElse {
            Timber.e(it, "Failed to delete messages")
            return DataError.Local.DeletingFailed.left()
        }
        return Unit.right()
    }

    override suspend fun getClippedPageKey(userId: UserId, pageKey: PageKey): PageKey? =
        pageIntervalDao.getClippedPageKey(userId, PageItemType.Message, pageKey)

    override suspend fun getMessages(userId: UserId, pageKey: PageKey): List<Message> {
        return if (pageKey.filter.keyword.isEmpty()) {
            observeMessages(userId, pageKey).first()
        } else {
            observeSearchResults(userId, pageKey).first()
        }
    }

    override suspend fun isLocalPageValid(
        userId: UserId,
        pageKey: PageKey,
        items: List<Message>
    ): Boolean = pageIntervalDao.isLocalPageValid(userId, PageItemType.Message, pageKey, items)

    override suspend fun markAsStale(userId: UserId, labelId: LabelId) =
        pageIntervalDao.deleteAll(userId, PageItemType.Message, labelId)

    override fun observeMessage(userId: UserId, messageId: MessageId): Flow<Message?> =
        messageDao.observe(userId, messageId).mapLatest { it?.toMessage() }

    override fun observeMessages(userId: UserId, conversationId: ConversationId): Flow<List<Message>> =
        messageDao.observeAllOrderByTimeAsc(
            userId = userId,
            conversationId = conversationId
        ).mapLatest { list -> list.map { messageWithLabelIds -> messageWithLabelIds.toMessage() } }

    override fun observeMessages(userId: UserId, pageKey: PageKey): Flow<List<Message>> = messageDao
        .observeAll(userId, pageKey)
        .mapLatest { list -> list.map { it.toMessage() } }

    override fun observeSearchResults(userId: UserId, pageKey: PageKey): Flow<List<Message>> = messageDao
        .observeSearchResults(userId, pageKey)
        .mapLatest { list -> list.map { it.toMessage() } }

    override fun observeMessages(userId: UserId, messageIds: List<MessageId>): Flow<List<Message>> =
        messageDao.observeMessages(
            userId = userId,
            messages = messageIds
        ).mapLatest { list -> list.mapNotNull { it?.toMessage() } }

    override fun observeMessagesForConversation(
        userId: UserId,
        conversationIds: List<ConversationId>
    ): Flow<List<Message>> = messageDao.observeMessageWithLabelsInConversations(
        userId = userId,
        conversationIds = conversationIds
    ).mapLatest { it.map { messageWithLabelIds -> messageWithLabelIds.toMessage() } }

    override suspend fun upsertMessage(message: Message) = db.inTransaction {
        upsertMessages(listOf(message))
    }

    override suspend fun upsertMessages(items: List<Message>) = db.inTransaction {
        messageDao.upsertOrError(*items.map { it.toEntity() }.toTypedArray()).onRight {
            updateLabels(items)
        }
    }

    override suspend fun upsertMessages(
        userId: UserId,
        pageKey: PageKey,
        items: List<Message>
    ) = db.inTransaction {
        upsertMessages(items).onRight {
            upsertPageInterval(userId, pageKey, items)
            if (pageKey.filter.keyword.isNotEmpty()) {
                upsertSearchResults(userId, pageKey.filter.keyword, items)
            }
        }
    }

    override fun observeMessageWithBody(userId: UserId, messageId: MessageId): Flow<MessageWithBody?> {
        return combine(
            messageBodyDao.observeMessageWithBodyEntity(userId, messageId),
            messageAttachmentDao.observeMessageAttachmentEntities(userId, messageId)
        ) { messageWithBodyEntity, messageAttachments ->
            if (messageWithBodyEntity != null) {
                messageWithBodyEntityMapper.toMessageWithBody(
                    messageWithBodyEntity.withBodyFromFileIfNeeded(userId),
                    messageAttachments.map { messageAttachmentEntityMapper.toMessageAttachment(it) }
                )
            } else {
                null
            }
        }
    }

    override fun observeMessageAttachments(userId: UserId, messageId: MessageId): Flow<List<MessageAttachment>> =
        messageAttachmentDao.observeMessageAttachmentEntities(userId, messageId).map { messageAttachmentEntities ->
            messageAttachmentEntities.map { messageAttachmentEntityMapper.toMessageAttachment(it) }
        }

    override suspend fun upsertMessageWithBody(userId: UserId, messageWithBody: MessageWithBody) = db.inTransaction {
        upsertMessage(messageWithBody.message)
        val messageBodyEntity = messageWithBodyEntityMapper.toMessageBodyEntity(messageWithBody.messageBody)
        if (messageWithBody.messageBody.body.shouldBeStoredAsFile()) {
            messageBodyFileStorage.saveMessageBody(userId, messageWithBody.messageBody)
            messageBodyDao.upsertOrError(messageBodyEntity.copy(body = null))
        } else {
            messageBodyDao.upsertOrError(messageBodyEntity)
        }
        if (messageWithBody.messageBody.attachments.isNotEmpty()) {
            val attachmentEntities = messageWithBody.messageBody.attachments.map {
                messageAttachmentEntityMapper.toMessageAttachmentEntity(userId, messageWithBody.message.messageId, it)
            }.toTypedArray()
            messageAttachmentDao.upsertOrError(*attachmentEntities)
        }
    }

    override suspend fun addLabel(
        userId: UserId,
        messageId: MessageId,
        labelId: LabelId
    ): Either<DataError.Local, Message> = addLabels(userId, messageId, listOf(labelId))

    override suspend fun addLabels(
        userId: UserId,
        messageId: MessageId,
        labelIds: List<LabelId>
    ): Either<DataError.Local, Message> {
        val message = observeMessage(userId, messageId).first()
            ?: return DataError.Local.NoDataCached.left()
        val updatedMessage = message.copy(
            labelIds = message.labelIds.toMutableSet().apply { addAll(labelIds) }.toList()
        )
        upsertMessage(updatedMessage)
        return updatedMessage.right()
    }

    override suspend fun removeLabel(
        userId: UserId,
        messageId: MessageId,
        labelId: LabelId
    ): Either<DataError.Local, Message> = removeLabels(userId, messageId, listOf(labelId))

    override suspend fun removeLabels(
        userId: UserId,
        messageId: MessageId,
        labelIds: List<LabelId>
    ): Either<DataError.Local, Message> {
        val message = observeMessage(userId, messageId).first()
            ?: return DataError.Local.NoDataCached.left()
        val updatedMessage = message.copy(
            labelIds = message.labelIds - labelIds
        )
        upsertMessage(updatedMessage)
        return updatedMessage.right()
    }

    override suspend fun relabelMessages(
        userId: UserId,
        messageIds: List<MessageId>,
        labelIdsToRemove: Set<LabelId>,
        labelIdsToAdd: Set<LabelId>
    ): Either<DataError.Local, List<Message>> {
        val messages = observeMessages(userId, messageIds).first().takeIf { it.isNotEmpty() }
            ?: return DataError.Local.NoDataCached.left()
        val updatedMessages = messages.map { message ->
            message.copy(labelIds = (message.labelIds - labelIdsToRemove).union(labelIdsToAdd).toList())
        }
        upsertMessages(updatedMessages)
        return updatedMessages.right()
    }

    override suspend fun relabelMessagesInConversations(
        userId: UserId,
        conversationIds: List<ConversationId>,
        labelIdsToRemove: Set<LabelId>,
        labelIdsToAdd: Set<LabelId>
    ): Either<DataError.Local, List<Message>> = db.inTransaction {
        val messageIds = messageDao.getMessageIdsInConversations(userId, conversationIds)
        relabelMessages(userId, messageIds, labelIdsToRemove, labelIdsToAdd)
    }

    override suspend fun markUnread(
        userId: UserId,
        messageIds: List<MessageId>
    ): Either<DataError.Local, List<Message>> {
        val messages = observeMessages(userId, messageIds).first()
            .takeIf { it.isNotEmpty() }
            ?: return DataError.Local.NoDataCached.left()
        val updatedMessages = messages.map {
            it.copy(
                unread = true
            )
        }
        upsertMessages(updatedMessages)
        return updatedMessages.right()
    }

    override suspend fun markUnreadLastReadMessageInConversations(
        userId: UserId,
        conversationIds: List<ConversationId>,
        contextLabelId: LabelId
    ): Either<DataError.Local, List<Message>> = db.inTransaction {
        either {
            conversationIds.map { conversationId ->
                observeMessages(userId, conversationId).first()
                    .filter { message -> message.read && message.labelIds.contains(contextLabelId) }
                    .maxByOrNull { message -> message.time }
                    .let { message ->
                        markUnread(userId, message?.let { listOf(it.messageId) } ?: emptyList())
                    }
                    .map { it.first() }
                    .bind()
            }
        }
    }

    override suspend fun markRead(userId: UserId, messageIds: List<MessageId>): Either<DataError.Local, List<Message>> {
        val messages = observeMessages(userId, messageIds).first()
            .takeIf { it.isNotEmpty() }
            ?: return DataError.Local.NoDataCached.left()
        val updatedMessages = messages.map { it.copy(unread = false) }
        upsertMessages(updatedMessages)
        return updatedMessages.right()
    }

    override suspend fun markMessagesInConversationsRead(
        userId: UserId,
        conversationIds: List<ConversationId>
    ): Either<DataError.Local, List<Message>> = db.inTransaction {
        val messageIds = messageDao.getMessageIdsInConversations(userId, conversationIds)
        markRead(userId, messageIds)
    }

    override suspend fun isMessageRead(userId: UserId, messageId: MessageId): Either<DataError.Local, Boolean> {
        val message = observeMessage(userId, messageId).first()
            ?: return DataError.Local.NoDataCached.left()
        return message.unread.not().right()
    }

    override suspend fun updateDraftRemoteIds(
        userId: UserId,
        localDraftId: MessageId,
        apiAssignedId: MessageId,
        conversationId: ConversationId
    ) {
        messageDao.updateDraftRemoteIds(userId, localDraftId, apiAssignedId, conversationId)
        messageBodyFileStorage.updateFileNameForMessageBody(userId, localDraftId, apiAssignedId)
        attachmentFileStorage.updateParentFolderForAttachments(userId, localDraftId, apiAssignedId)
    }

    private suspend fun updateLabels(messages: List<Message>) = with(groupByUserId(messages)) {
        deleteLabels()
        insertLabels()
    }

    private suspend fun upsertPageInterval(
        userId: UserId,
        pageKey: PageKey,
        messages: List<Message>
    ) = pageIntervalDao.upsertPageInterval(userId, PageItemType.Message, pageKey, messages)

    private suspend fun upsertSearchResults(
        userId: UserId,
        keyword: String,
        messages: List<Message>
    ) = searchResultsLocalDataSource.upsertResults(userId, keyword, messages)

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

    private suspend fun MessageWithBodyEntity.withBodyFromFileIfNeeded(userId: UserId) = if (messageBody.body == null) {
        copy(
            messageBody = messageBody.copy(body = messageBodyFileStorage.readMessageBody(userId, message.messageId))
        )
    } else {
        this
    }

    companion object {
        const val SQL_CHUNK_SIZE = 100
    }
}
