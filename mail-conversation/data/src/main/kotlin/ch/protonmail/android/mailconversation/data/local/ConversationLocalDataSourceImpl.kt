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

import ch.protonmail.android.mailcommon.domain.model.ConversationId
import ch.protonmail.android.mailconversation.data.local.entity.ConversationLabelEntity
import ch.protonmail.android.mailconversation.domain.entity.Conversation
import ch.protonmail.android.mailconversation.domain.entity.ConversationWithContext
import ch.protonmail.android.mailconversation.domain.repository.ConversationLocalDataSource
import ch.protonmail.android.mailpagination.data.local.getClippedPageKey
import ch.protonmail.android.mailpagination.data.local.isLocalPageValid
import ch.protonmail.android.mailpagination.data.local.upsertPageInterval
import ch.protonmail.android.mailpagination.domain.entity.PageItemType
import ch.protonmail.android.mailpagination.domain.entity.PageKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapLatest
import me.proton.core.domain.entity.UserId
import me.proton.core.label.domain.entity.LabelId
import javax.inject.Inject

class ConversationLocalDataSourceImpl @Inject constructor(
    private val db: ConversationDatabase
) : ConversationLocalDataSource {

    private val conversationDao = db.conversationDao()
    private val conversationLabelDao = db.conversationLabelDao()
    private val pageIntervalDao = db.pageIntervalDao()

    override fun observeConversations(
        userId: UserId,
        pageKey: PageKey
    ): Flow<List<ConversationWithContext>> = conversationDao
        .observeAll(userId, pageKey)
        .mapLatest { list -> list.map { it.toConversationWithContext(pageKey.filter.labelId) } }

    override suspend fun getConversations(
        userId: UserId,
        pageKey: PageKey
    ): List<ConversationWithContext> = observeConversations(userId, pageKey).first()

    override suspend fun upsertConversations(
        userId: UserId,
        pageKey: PageKey,
        items: List<ConversationWithContext>
    ) = db.inTransaction {
        upsertConversations(items.map { it.conversation })
        upsertPageInterval(userId, pageKey, items)
    }

    override suspend fun upsertConversations(
        items: List<Conversation>
    ) = db.inTransaction {
        conversationDao.insertOrUpdate(*items.map { it.toEntity() }.toTypedArray())
        updateLabels(items)
    }

    override suspend fun deleteConversation(
        userId: UserId,
        ids: List<ConversationId>
    ) = conversationDao.delete(userId, ids.map { it.id })

    override suspend fun deleteAllConversations(
        userId: UserId
    ) = db.inTransaction {
        conversationDao.deleteAll(userId)
        pageIntervalDao.deleteAll(userId, PageItemType.Conversation)
    }

    override suspend fun markAsStale(
        userId: UserId,
        labelId: LabelId
    ) = pageIntervalDao.deleteAll(userId, PageItemType.Conversation, labelId)

    override suspend fun isLocalPageValid(
        userId: UserId,
        pageKey: PageKey,
        items: List<ConversationWithContext>
    ): Boolean = pageIntervalDao.isLocalPageValid(userId, PageItemType.Conversation, pageKey, items)

    override suspend fun getClippedPageKey(
        userId: UserId,
        pageKey: PageKey
    ): PageKey = pageIntervalDao.getClippedPageKey(userId, PageItemType.Conversation, pageKey)

    override fun observeConversation(userId: UserId, conversationId: ConversationId): Flow<Conversation?> =
        conversationDao
            .observe(userId, conversationId)
            .mapLatest { it?.toConversation() }

    override suspend fun upsertConversation(userId: UserId, conversation: Conversation) {
        conversationDao.insertOrUpdate(conversation.toEntity())
    }

    private suspend fun upsertPageInterval(
        userId: UserId,
        pageKey: PageKey,
        items: List<ConversationWithContext>
    ) = pageIntervalDao.upsertPageInterval(userId, PageItemType.Conversation, pageKey, items)

    private suspend fun updateLabels(
        items: List<Conversation>
    ) = with(groupByUserId(items)) {
        deleteLabels()
        insertLabels()
    }

    private fun groupByUserId(items: List<Conversation>) = items.fold(
        mutableMapOf<UserId, MutableList<Conversation>>()
    ) { acc, conversation ->
        acc.apply { getOrPut(conversation.userId) { mutableListOf() }.add(conversation) }
    }.toMap()

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
}
