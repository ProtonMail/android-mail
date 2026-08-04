/*
 * Copyright (c) 2026 Proton Technologies AG
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

package ch.protonmail.android.mailcontentsearch.data.repository

import arrow.core.Either
import arrow.core.flatten
import ch.protonmail.android.mailcommon.domain.model.ConversationId
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcontentsearch.data.mapper.toRecentFoundItem
import ch.protonmail.android.mailcontentsearch.data.mapper.toRecentSearchTerm
import ch.protonmail.android.mailcontentsearch.data.usecase.CreateSearchRecents
import ch.protonmail.android.mailcontentsearch.data.wrapper.SearchRecentsWrapper
import ch.protonmail.android.mailcontentsearch.domain.model.RecentFoundItem
import ch.protonmail.android.mailcontentsearch.domain.model.RecentSearchTerm
import ch.protonmail.android.mailcontentsearch.domain.repository.ContentSearchRecentsRepository
import ch.protonmail.android.mailmessage.data.mapper.toLocalConversationId
import ch.protonmail.android.mailmessage.data.mapper.toLocalMessageId
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailsession.data.usecase.ExecuteWithUserSession
import me.proton.core.domain.entity.UserId
import javax.inject.Inject

class ContentSearchRecentsRepositoryImpl @Inject constructor(
    private val executeWithUserSession: ExecuteWithUserSession,
    private val createSearchRecents: CreateSearchRecents
) : ContentSearchRecentsRepository {

    override suspend fun getRecentSearchTerms(
        userId: UserId,
        prefix: String?,
        limit: Int
    ): Either<DataError, List<RecentSearchTerm>> = withRecents(userId) { recents ->
        recents.recentSearchTerms(prefix, limit.coerceAtLeast(0).toUInt()).map { terms ->
            terms.map { it.toRecentSearchTerm() }
        }
    }

    override suspend fun getRecentFoundItems(userId: UserId, limit: Int): Either<DataError, List<RecentFoundItem>> =
        withRecents(userId) { recents ->
            recents.recentFoundItems(limit.coerceAtLeast(0).toUInt()).map { items ->
                items.map { it.toRecentFoundItem() }
            }
        }

    override suspend fun touchSearchTerm(userId: UserId, query: String): Either<DataError, Unit> =
        withRecents(userId) { it.touchRecentSearchTerm(query) }

    override suspend fun dismissSearchTerm(userId: UserId, query: String): Either<DataError, Unit> =
        withRecents(userId) { it.dismissRecentSearchTerm(query) }

    override suspend fun touchFoundItem(userId: UserId, messageId: MessageId): Either<DataError, Unit> =
        withRecents(userId) { it.touchRecentFoundItem(messageId.toLocalMessageId()) }

    override suspend fun dismissFoundItem(userId: UserId, messageId: MessageId): Either<DataError, Unit> =
        withRecents(userId) { it.dismissRecentFoundItem(messageId.toLocalMessageId()) }

    override suspend fun recordSearchOpen(
        userId: UserId,
        query: String,
        messageId: MessageId,
        conversationId: ConversationId
    ): Either<DataError, Unit> = withRecents(userId) { recents ->
        recents.recordSearchOpen(
            query = query,
            messageId = messageId.toLocalMessageId(),
            conversationId = conversationId.toLocalConversationId()
        )
    }

    // Flattens the "no session" error from ExecuteWithUserSession into the wrapper's own error.
    private suspend fun <T> withRecents(
        userId: UserId,
        block: suspend (SearchRecentsWrapper) -> Either<DataError, T>
    ): Either<DataError, T> = executeWithUserSession(userId) { session ->
        block(createSearchRecents(session))
    }.flatten()
}
