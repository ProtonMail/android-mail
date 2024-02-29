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

package ch.protonmail.android.mailconversation.data.local

import arrow.core.Either
import arrow.core.left
import arrow.core.raise.either
import arrow.core.right
import ch.protonmail.android.mailcommon.data.db.dao.upsertOrError
import ch.protonmail.android.mailcommon.domain.model.ConversationId
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailconversation.data.local.entity.ConversationLabelEntity
import ch.protonmail.android.mailconversation.domain.entity.Conversation
import ch.protonmail.android.mailconversation.domain.entity.ConversationLabel
import ch.protonmail.android.mailconversation.domain.entity.ConversationWithContext
import ch.protonmail.android.mailconversation.domain.repository.ConversationLocalDataSource
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
import timber.log.Timber
import javax.inject.Inject

@Suppress("TooManyFunctions")
class ConversationLocalDataSourceImpl @Inject constructor(
    private val db: ConversationDatabase
) : ConversationLocalDataSource {

    private val conversationDao = db.conversationDao()
    private val conversationLabelDao = db.conversationLabelDao()
    private val pageIntervalDao = db.pageIntervalDao()

    override fun observeConversations(userId: UserId, pageKey: PageKey): Flow<List<ConversationWithContext>> =
        conversationDao
            .observeAll(userId, pageKey)
            .mapLatest { list -> list.map { it.toConversationWithContext(pageKey.filter.labelId) } }

    override fun observeCachedConversations(userId: UserId, ids: List<ConversationId>) =
        conversationDao.observe(userId, ids)
            .mapLatest { list -> list.map { it.toConversation() } }


    override suspend fun getConversations(userId: UserId, pageKey: PageKey): List<ConversationWithContext> =
        observeConversations(userId, pageKey).first()

    override suspend fun upsertConversations(
        userId: UserId,
        pageKey: PageKey,
        items: List<ConversationWithContext>
    ) = db.inTransaction {
        upsertConversations(items.map { it.conversation }).onRight {
            upsertPageInterval(userId, pageKey, items)
        }
    }

    override suspend fun upsertConversations(items: List<Conversation>) = db.inTransaction {
        conversationDao.upsertOrError(*items.map { it.toEntity() }.toTypedArray()).onRight {
            updateLabels(items)
        }
    }

    override suspend fun deleteConversation(userId: UserId, ids: List<ConversationId>) =
        conversationDao.delete(userId, ids.map { it.id })

    override suspend fun deleteConversations(userId: UserId, ids: List<ConversationId>): Either<DataError, Unit> {
        runCatching {
            conversationDao.delete(userId, ids.map { it.id })
        }.getOrElse {
            Timber.e(it, "Failed to delete conversations")
            return DataError.Local.DeletingFailed.left()
        }
        return Unit.right()
    }

    override suspend fun deleteAllConversations(userId: UserId) = db.inTransaction {
        conversationDao.deleteAll(userId)
        pageIntervalDao.deleteAll(userId, PageItemType.Conversation)
    }

    override suspend fun deleteConversationsWithLabel(userId: UserId, labelId: LabelId): Either<DataError, Unit> {
        runCatching {
            conversationDao.deleteAllConversationsWithLabel(userId, labelId)
        }.getOrElse {
            Timber.e(it, "Failed to delete conversations with label")
            return DataError.Local.DeletingFailed.left()
        }
        return Unit.right()
    }

    override suspend fun markAsStale(userId: UserId, labelId: LabelId) =
        pageIntervalDao.deleteAll(userId, PageItemType.Conversation, labelId)

    override suspend fun isLocalPageValid(
        userId: UserId,
        pageKey: PageKey,
        items: List<ConversationWithContext>
    ): Boolean = pageIntervalDao.isLocalPageValid(userId, PageItemType.Conversation, pageKey, items)

    override suspend fun getClippedPageKey(userId: UserId, pageKey: PageKey): PageKey? =
        pageIntervalDao.getClippedPageKey(userId, PageItemType.Conversation, pageKey)

    override fun observeConversation(userId: UserId, conversationId: ConversationId): Flow<Conversation?> =
        conversationDao
            .observe(userId, conversationId)
            .mapLatest { it?.toConversation() }

    override suspend fun upsertConversation(userId: UserId, conversation: Conversation) = db.inTransaction {
        conversationDao.upsertOrError(conversation.toEntity()).onRight {
            updateLabels(listOf(conversation))
        }
    }

    override suspend fun addLabel(
        userId: UserId,
        conversationIds: List<ConversationId>,
        labelId: LabelId
    ): Either<DataError.Local, List<Conversation>> = addLabels(userId, conversationIds, listOf(labelId))

    override suspend fun addLabels(
        userId: UserId,
        conversationId: ConversationId,
        labelIds: List<LabelId>
    ): Either<DataError.Local, Conversation> = addLabels(userId, listOf(conversationId), labelIds).map { it.first() }

    override suspend fun addLabels(
        userId: UserId,
        conversationIds: List<ConversationId>,
        labelIds: List<LabelId>
    ): Either<DataError.Local, List<Conversation>> = db.inTransaction {
        getConversations(userId, conversationIds).map { conversations ->
            val updatedConversations = conversations.map { conversation ->
                val conversationLabels = labelIds.map { labelId ->
                    ConversationLabel(
                        conversationId = conversation.conversationId,
                        labelId = labelId,
                        contextTime = conversation.labels.maxOf { it.contextTime },
                        contextSize = 0L,
                        contextNumMessages = conversation.numMessages,
                        contextNumUnread = conversation.numUnread,
                        contextNumAttachments = conversation.numAttachments
                    )
                }
                conversation.copy(
                    labels = conversation.labels + conversationLabels
                )
            }
            upsertConversations(updatedConversations)
            return@map updatedConversations
        }
    }

    override suspend fun removeLabel(
        userId: UserId,
        conversationIds: List<ConversationId>,
        labelId: LabelId
    ): Either<DataError.Local, List<Conversation>> = removeLabels(userId, conversationIds, listOf(labelId))

    override suspend fun removeLabels(
        userId: UserId,
        conversationId: ConversationId,
        labelIds: List<LabelId>
    ): Either<DataError.Local, Conversation> = removeLabels(userId, listOf(conversationId), labelIds).map { it.first() }

    override suspend fun removeLabels(
        userId: UserId,
        conversationIds: List<ConversationId>,
        labelIds: List<LabelId>
    ): Either<DataError.Local, List<Conversation>> = db.inTransaction {
        getConversations(userId, conversationIds).map { conversations ->
            val updatedConversations = conversations.map { conversation ->
                conversation.copy(
                    labels = conversation.labels.filterNot { labelIds.contains(it.labelId) }
                )
            }
            upsertConversations(updatedConversations)
            return@map updatedConversations
        }
    }

    override suspend fun relabel(
        userId: UserId,
        conversationIds: List<ConversationId>,
        labelIdsToAdd: List<LabelId>,
        labelIdsToRemove: List<LabelId>
    ): Either<DataError.Local, List<Conversation>> = db.inTransaction {
        either {
            removeLabels(userId, conversationIds, labelIdsToRemove).bind()
            addLabels(userId, conversationIds, labelIdsToAdd).bind()
        }
    }

    override suspend fun markUnread(
        userId: UserId,
        conversationIds: List<ConversationId>,
        contextLabelId: LabelId
    ): Either<DataError.Local, List<Conversation>> = db.inTransaction {
        getConversations(userId, conversationIds).map { conversations ->
            val updatedConversations = conversations.map { conversation ->
                val updatedLabels = conversation.labels.mapOnly(contextLabelId) { label ->
                    label.copy(contextNumUnread = label.contextNumUnread.incrementUpTo(label.contextNumMessages))
                }
                conversation.copy(
                    numUnread = conversation.numUnread.incrementUpTo(conversation.numMessages),
                    labels = updatedLabels
                )
            }
            upsertConversations(updatedConversations)
            return@map updatedConversations
        }
    }

    override suspend fun markRead(
        userId: UserId,
        conversationIds: List<ConversationId>
    ): Either<DataError.Local, List<Conversation>> = db.inTransaction {
        getConversations(userId, conversationIds).map { conversations ->
            val updatedConversations = conversations.map { conversation ->
                val updatedLabels = conversation.labels.map { label ->
                    label.copy(contextNumUnread = 0)
                }
                conversation.copy(
                    numUnread = conversation.numUnread.decrementCoercingZero(),
                    labels = updatedLabels
                )
            }
            upsertConversations(updatedConversations)
            return@map updatedConversations
        }
    }

    override suspend fun isConversationRead(
        userId: UserId,
        conversationId: ConversationId
    ): Either<DataError.Local, Boolean> {
        val conversation = conversationDao.getConversation(userId, conversationId)
            ?.toConversation()
            ?: return DataError.Local.NoDataCached.left()
        return (conversation.numUnread == 0).right()
    }

    private suspend fun upsertPageInterval(
        userId: UserId,
        pageKey: PageKey,
        items: List<ConversationWithContext>
    ) = pageIntervalDao.upsertPageInterval(userId, PageItemType.Conversation, pageKey, items)

    private suspend fun updateLabels(items: List<Conversation>) = with(groupByUserId(items)) {
        deleteLabels()
        insertLabels()
    }

    private fun groupByUserId(items: List<Conversation>) = items.fold(
        mutableMapOf<UserId, MutableList<Conversation>>()
    ) { acc, conversation ->
        acc.apply { getOrPut(conversation.userId) { mutableListOf() }.add(conversation) }
    }.toMap()

    override suspend fun getConversation(
        userId: UserId,
        conversationId: ConversationId
    ): Either<DataError.Local, Conversation> = conversationDao.getConversation(userId, conversationId)
        ?.toConversation()
        ?.right()
        ?: DataError.Local.NoDataCached.left()

    override suspend fun getConversations(
        userId: UserId,
        conversationIds: List<ConversationId>
    ): Either<DataError.Local, List<Conversation>> {
        val conversations = conversationDao.getConversations(userId, conversationIds)
            .takeIf { it.isNotEmpty() }
            ?: return DataError.Local.NoDataCached.left()

        return conversations.map { it.toConversation() }.right()
    }

    private suspend fun Map<UserId, MutableList<Conversation>>.insertLabels() {
        entries.forEach { (userId, conversations) ->
            conversations.forEach { conversation ->
                conversation.labels.forEach { label ->
                    conversationLabelDao.insertOrUpdate(
                        ConversationLabelEntity(
                            userId = userId,
                            conversationId = conversation.conversationId,
                            labelId = label.labelId,
                            contextTime = label.contextTime,
                            contextSize = label.contextSize,
                            contextNumMessages = label.contextNumMessages,
                            contextNumUnread = label.contextNumUnread,
                            contextNumAttachments = label.contextNumAttachments
                        )
                    )
                }
            }
        }
    }

    private suspend fun Map<UserId, MutableList<Conversation>>.deleteLabels() {
        entries.forEach { (userId, conversations) ->
            conversationLabelDao.deleteAll(userId, conversations.map { it.conversationId })
        }
    }

    private fun List<ConversationLabel>.mapOnly(
        labelId: LabelId,
        updateAction: (ConversationLabel) -> ConversationLabel
    ): List<ConversationLabel> = this.map { label ->
        if (label.labelId == labelId) {
            return@map updateAction(label)
        }
        label
    }
}

private fun Int.incrementUpTo(max: Int) = (this + 1).coerceAtMost(max)

private fun Int.decrementCoercingZero() = (this - 1).coerceAtLeast(0)
