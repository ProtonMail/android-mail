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

package ch.protonmail.android.mailmessage.data.repository

import arrow.core.Either
import arrow.core.NonEmptyList
import arrow.core.left
import arrow.core.right
import arrow.core.toNonEmptyListOrNull
import ch.protonmail.android.mailcommon.domain.mapper.mapToEither
import ch.protonmail.android.mailcommon.domain.model.ConversationId
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.maillabel.domain.extension.isSpam
import ch.protonmail.android.maillabel.domain.extension.isTrash
import ch.protonmail.android.maillabel.domain.model.filterUnmodifiableLabels
import ch.protonmail.android.mailmessage.data.local.MessageBodyFileWriteException
import ch.protonmail.android.mailmessage.data.local.MessageLocalDataSource
import ch.protonmail.android.mailmessage.data.remote.MessageApi
import ch.protonmail.android.mailmessage.data.remote.MessageRemoteDataSource
import ch.protonmail.android.mailmessage.data.usecase.ExcludeDraftMessagesAlreadyInOutbox
import ch.protonmail.android.mailmessage.domain.model.DecryptedMessageBody
import ch.protonmail.android.mailmessage.domain.model.Message
import ch.protonmail.android.mailmessage.domain.model.MessageAttachment
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailmessage.domain.model.MessageWithBody
import ch.protonmail.android.mailmessage.domain.model.RefreshedMessageWithBody
import ch.protonmail.android.mailmessage.domain.repository.MessageRepository
import ch.protonmail.android.mailpagination.domain.model.PageKey
import com.dropbox.android.external.store4.Fetcher
import com.dropbox.android.external.store4.SourceOfTruth
import com.dropbox.android.external.store4.StoreBuilder
import com.dropbox.android.external.store4.StoreRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.mapLatest
import me.proton.core.data.arch.ProtonStore
import me.proton.core.data.arch.buildProtonStore
import me.proton.core.data.arch.toDataResult
import me.proton.core.domain.entity.UserId
import me.proton.core.label.domain.entity.LabelId
import me.proton.core.util.kotlin.CoroutineScopeProvider
import okio.IOException
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min

@SuppressWarnings("TooManyFunctions")
@Singleton
class MessageRepositoryImpl @Inject constructor(
    private val remoteDataSource: MessageRemoteDataSource,
    private val localDataSource: MessageLocalDataSource,
    private val excludeDraftMessagesAlreadyInOutbox: ExcludeDraftMessagesAlreadyInOutbox,
    coroutineScopeProvider: CoroutineScopeProvider
) : MessageRepository {

    private data class MessageKey(val userId: UserId, val messageId: MessageId)

    private val messageWithBodyStore: ProtonStore<MessageKey, MessageWithBody> = StoreBuilder.from(
        fetcher = Fetcher.of { key: MessageKey ->
            remoteDataSource.getMessageOrThrow(key.userId, key.messageId)
        },
        sourceOfTruth = SourceOfTruth.of(
            reader = { key: MessageKey ->
                localDataSource.observeMessageWithBody(key.userId, key.messageId)
            },
            writer = { key, messageWithBody ->
                localDataSource.upsertMessageWithBody(key.userId, messageWithBody)
            }
        )
    ).disableCache().buildProtonStore(coroutineScopeProvider)

    override suspend fun getLocalMessages(userId: UserId, pageKey: PageKey) =
        localDataSource.getMessages(userId, pageKey)

    override suspend fun getLocalMessages(userId: UserId, messages: List<MessageId>) =
        localDataSource.observeMessages(userId, messages).first()

    override suspend fun isLocalPageValid(
        userId: UserId,
        pageKey: PageKey,
        items: List<Message>
    ): Boolean = localDataSource.isLocalPageValid(userId, pageKey, items)

    override suspend fun getRemoteMessages(userId: UserId, pageKey: PageKey): Either<DataError.Remote, List<Message>> {
        val adaptedPageKey = localDataSource.getClippedPageKey(
            userId = userId,
            pageKey = pageKey.copy(size = min(MessageApi.maxPageSize, pageKey.size))
        ) ?: return emptyList<Message>().right()

        return remoteDataSource.getMessages(
            userId = userId,
            pageKey = adaptedPageKey
        ).onRight { messages ->
            val filteredMessages = excludeDraftMessagesAlreadyInOutbox(userId, messages)
            upsertMessages(userId, adaptedPageKey, filteredMessages)
        }
    }

    override suspend fun markAsStale(userId: UserId, labelId: LabelId) = localDataSource.markAsStale(userId, labelId)

    override fun observeCachedMessage(userId: UserId, messageId: MessageId): Flow<Either<DataError.Local, Message>> =
        localDataSource.observeMessage(userId, messageId).mapLatest {
            it?.right() ?: DataError.Local.NoDataCached.left()
        }

    override fun observeCachedMessages(
        userId: UserId,
        messageIds: List<MessageId>
    ): Flow<Either<DataError.Local, List<Message>>> =
        localDataSource.observeMessages(userId, messageIds).mapLatest { list ->
            list.toNonEmptyListOrNull()?.right() ?: DataError.Local.NoDataCached.left()
        }

    override fun observeCachedMessages(
        userId: UserId,
        conversationId: ConversationId
    ): Flow<Either<DataError.Local, NonEmptyList<Message>>> =
        localDataSource.observeMessages(userId, conversationId).mapLatest { list ->
            list.toNonEmptyListOrNull()?.right() ?: DataError.Local.NoDataCached.left()
        }

    override fun observeCachedMessagesForConversations(
        userId: UserId,
        conversationIds: List<ConversationId>
    ): Flow<List<Message>> = localDataSource.observeMessagesForConversation(userId, conversationIds)


    override fun observeMessageWithBody(
        userId: UserId,
        messageId: MessageId
    ): Flow<Either<DataError, MessageWithBody>> = messageWithBodyStore.stream(
        StoreRequest.cached(MessageKey(userId, messageId), false)
    ).mapLatest { it.toDataResult() }
        .mapToEither()
        .distinctUntilChanged()

    override fun observeMessageAttachments(userId: UserId, messageId: MessageId): Flow<List<MessageAttachment>> =
        localDataSource.observeMessageAttachments(userId, messageId)

    override suspend fun getMessageWithBody(userId: UserId, messageId: MessageId): Either<DataError, MessageWithBody> =
        observeMessageWithBody(userId, messageId).first()

    override suspend fun getLocalMessageWithBody(userId: UserId, messageId: MessageId): MessageWithBody? =
        localDataSource.observeMessageWithBody(userId, messageId).firstOrNull()

    override suspend fun getRefreshedMessageWithBody(userId: UserId, messageId: MessageId): RefreshedMessageWithBody? =
        remoteDataSource.getMessage(userId, messageId).fold(
            ifRight = {
                upsertMessageWithBody(userId, it)
                RefreshedMessageWithBody(it, isRefreshed = true)
            },
            ifLeft = {
                getLocalMessageWithBody(userId, messageId)?.let {
                    RefreshedMessageWithBody(it, isRefreshed = false)
                }
            }
        )

    override suspend fun upsertMessageWithBody(userId: UserId, messageWithBody: MessageWithBody): Boolean {
        return try {
            localDataSource.upsertMessageWithBody(userId, messageWithBody)
            true
        } catch (e: IOException) {
            Timber.w("Failed to save draft", e)
            false
        } catch (e: MessageBodyFileWriteException) {
            Timber.w("Failed to save draft", e)
            false
        }
    }

    override suspend fun moveTo(
        userId: UserId,
        messageId: MessageId,
        fromLabel: LabelId?,
        toLabel: LabelId
    ): Either<DataError.Local, Message> = moveTo(userId, mapOf(messageId to fromLabel), toLabel).map { it.first() }

    override suspend fun moveTo(
        userId: UserId,
        messageWithExclusiveLabel: Map<MessageId, LabelId?>,
        toLabel: LabelId
    ): Either<DataError.Local, List<Message>> {
        val messageIds = messageWithExclusiveLabel.keys.toList()
        if (toLabel.isTrash() || toLabel.isSpam()) {
            return moveToTrashOrSpam(userId, messageIds, toLabel)
        }

        val messages = localDataSource.observeMessages(userId, messageIds).first()
            .takeIf { it.isNotEmpty() }
            ?: return DataError.Local.NoDataCached.left()

        val updatedMessages = messages.map { message ->
            val labelList = message.labelIds.toMutableList().apply {
                messageWithExclusiveLabel[message.messageId].let { fromLabel ->
                    fromLabel?.let { this.remove(it) }
                    this.add(toLabel)
                }
            }
            message.copy(labelIds = labelList)
        }

        localDataSource.upsertMessages(updatedMessages)
        remoteDataSource.addLabelsToMessages(userId, messageIds, listOf(toLabel))
        return updatedMessages.right()
    }

    override suspend fun markUnread(userId: UserId, messageId: MessageId): Either<DataError.Local, Message> =
        markUnread(userId, listOf(messageId)).map { it.first() }

    override suspend fun markUnread(
        userId: UserId,
        messageIds: List<MessageId>
    ): Either<DataError.Local, List<Message>> = localDataSource.markUnread(userId, messageIds).onRight {
        remoteDataSource.markUnread(userId, messageIds)
    }

    override suspend fun markRead(userId: UserId, messageId: MessageId): Either<DataError.Local, Message> =
        markRead(userId, listOf(messageId)).map { it.first() }

    override suspend fun markRead(userId: UserId, messageIds: List<MessageId>): Either<DataError.Local, List<Message>> =
        localDataSource.markRead(userId, messageIds).onRight {
            remoteDataSource.markRead(userId, messageIds)
        }

    override suspend fun isMessageRead(userId: UserId, messageId: MessageId): Either<DataError.Local, Boolean> =
        localDataSource.isMessageRead(userId, messageId)

    override suspend fun relabel(
        userId: UserId,
        messageId: MessageId,
        labelsToBeRemoved: List<LabelId>,
        labelsToBeAdded: List<LabelId>
    ): Either<DataError.Local, Message> =
        relabel(userId, listOf(messageId), labelsToBeRemoved, labelsToBeAdded).map { it.first() }

    override suspend fun relabel(
        userId: UserId,
        messageIds: List<MessageId>,
        labelsToBeRemoved: List<LabelId>,
        labelsToBeAdded: List<LabelId>
    ): Either<DataError.Local, List<Message>> {
        val filteredLabelIdsToAdd = labelsToBeAdded.filterUnmodifiableLabels()
        val filteredLabelIdsToRemove = labelsToBeRemoved.filterUnmodifiableLabels()

        return localDataSource.relabelMessages(
            userId,
            messageIds,
            filteredLabelIdsToRemove.toSet(),
            filteredLabelIdsToAdd.toSet()
        ).onRight {
            remoteDataSource.removeLabelsFromMessages(userId, messageIds, filteredLabelIdsToRemove)
            remoteDataSource.addLabelsToMessages(userId, messageIds, filteredLabelIdsToAdd)
        }
    }

    override suspend fun updateDraftRemoteIds(
        userId: UserId,
        localDraftId: MessageId,
        apiAssignedId: MessageId,
        conversationId: ConversationId
    ) {
        localDataSource.updateDraftRemoteIds(userId, localDraftId, apiAssignedId, conversationId)
    }

    override suspend fun deleteMessages(
        userId: UserId,
        messageIds: List<MessageId>,
        currentLabelId: LabelId
    ): Either<DataError, Unit> {
        return localDataSource.deleteMessagesWithId(userId, messageIds).onRight {
            remoteDataSource.deleteMessages(userId, messageIds, currentLabelId)
        }
    }

    override suspend fun deleteMessages(userId: UserId, labelId: LabelId) {
        remoteDataSource.clearLabel(userId, labelId)
    }

    override fun observeClearLabelOperation(userId: UserId, labelId: LabelId) =
        remoteDataSource.observeClearWorkerIsEnqueuedOrRunning(userId, labelId)

    override suspend fun reportPhishing(
        userId: UserId,
        decryptedMessageBody: DecryptedMessageBody
    ): Either<DataError, Unit> = remoteDataSource.reportPhishing(userId, decryptedMessageBody)

    private suspend fun moveToTrashOrSpam(
        userId: UserId,
        messageIds: List<MessageId>,
        labelId: LabelId
    ): Either<DataError.Local, List<Message>> {
        require(labelId.isTrash() || labelId.isSpam()) { "Invalid system label id: $labelId" }

        val messages = localDataSource.observeMessages(userId, messageIds).first().takeIf { it.isNotEmpty() }
            ?: return DataError.Local.NoDataCached.left()

        val labelsToBeRemoved = messages.flatMap { it.labelIds }.filterUnmodifiableLabels()

        return localDataSource.relabelMessages(
            userId,
            messageIds,
            labelsToBeRemoved.toSet(),
            setOf(labelId)
        ).onRight {
            remoteDataSource.addLabelsToMessages(userId, messageIds, listOf(labelId))
        }
    }

    private suspend fun upsertMessages(
        userId: UserId,
        pageKey: PageKey,
        messages: List<Message>
    ) = localDataSource.upsertMessages(
        userId = userId,
        pageKey = pageKey,
        items = messages
    )
}
