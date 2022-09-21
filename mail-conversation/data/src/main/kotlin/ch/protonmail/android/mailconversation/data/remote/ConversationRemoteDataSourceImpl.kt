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

import ch.protonmail.android.mailcommon.domain.model.ConversationId
import ch.protonmail.android.mailconversation.domain.entity.Conversation
import ch.protonmail.android.mailconversation.domain.entity.ConversationWithContext
import ch.protonmail.android.mailconversation.domain.repository.ConversationRemoteDataSource
import ch.protonmail.android.mailpagination.domain.entity.OrderBy
import ch.protonmail.android.mailpagination.domain.entity.OrderDirection
import ch.protonmail.android.mailpagination.domain.entity.PageKey
import ch.protonmail.android.mailpagination.domain.entity.ReadStatus
import me.proton.core.domain.entity.UserId
import me.proton.core.network.data.ApiProvider
import me.proton.core.util.kotlin.takeIfNotBlank
import javax.inject.Inject

class ConversationRemoteDataSourceImpl @Inject constructor(
    private val apiProvider: ApiProvider
) : ConversationRemoteDataSource {

    override suspend fun getConversations(
        userId: UserId,
        pageKey: PageKey
    ): List<ConversationWithContext> = apiProvider.get<ConversationApi>(userId).invoke {
        require(pageKey.size <= ConversationApi.maxPageSize)
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
    }.valueOrThrow

    override suspend fun getConversation(
        userId: UserId,
        conversationId: ConversationId
    ): Conversation = apiProvider.get<ConversationApi>(userId).invoke {
        getConversation(
            conversationId = conversationId.id
        ).conversation.toConversation(userId)
    }.valueOrThrow
}
