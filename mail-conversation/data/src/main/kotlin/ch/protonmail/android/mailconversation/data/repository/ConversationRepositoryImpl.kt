/*
 * Copyright (c) 2021 Proton Technologies AG
 * This file is part of Proton Technologies AG and ProtonMail.
 *
 * ProtonMail is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ProtonMail is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with ProtonMail.  If not, see <https://www.gnu.org/licenses/>.
 */

package ch.protonmail.android.mailconversation.data.repository

import ch.protonmail.android.mailpagination.domain.entity.PageKey
import ch.protonmail.android.mailconversation.data.remote.ConversationApi
import ch.protonmail.android.mailconversation.domain.entity.Conversation
import ch.protonmail.android.mailconversation.domain.repository.ConversationLocalDataSource
import ch.protonmail.android.mailconversation.domain.repository.ConversationRemoteDataSource
import ch.protonmail.android.mailconversation.domain.repository.ConversationRepository
import me.proton.core.domain.entity.UserId
import me.proton.core.label.domain.entity.LabelId
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min

@Singleton
class ConversationRepositoryImpl @Inject constructor(
    private val remoteDataSource: ConversationRemoteDataSource,
    private val localDataSource: ConversationLocalDataSource,
) : ConversationRepository {

    override suspend fun getConversations(
        userId: UserId,
        pageKey: PageKey,
    ): List<Conversation> = localDataSource.getConversations(
        userId = userId,
        pageKey = pageKey
    ).let { conversations ->
        if (localDataSource.isLocalPageValid(userId, pageKey, conversations)) conversations
        else runCatching { fetchConversations(userId, pageKey) }.getOrElse { conversations }
    }

    override suspend fun markAsStale(
        userId: UserId,
        labelId: LabelId,
    ) = localDataSource.markAsStale(userId, labelId)

    private suspend fun fetchConversations(
        userId: UserId,
        pageKey: PageKey,
    ) = localDataSource.getClippedPageKey(
        userId = userId,
        pageKey = pageKey.copy(size = min(ConversationApi.maxPageSize, pageKey.size))
    ).let { adaptedPageKey ->
        remoteDataSource.getConversations(
            userId = userId,
            pageKey = adaptedPageKey
        ).also { conversations -> insertConversations(userId, adaptedPageKey, conversations) }
    }

    private suspend fun insertConversations(
        userId: UserId,
        pageKey: PageKey,
        conversations: List<Conversation>,
    ) = localDataSource.upsertConversations(
        userId = userId,
        pageKey = pageKey,
        items = conversations
    )
}
