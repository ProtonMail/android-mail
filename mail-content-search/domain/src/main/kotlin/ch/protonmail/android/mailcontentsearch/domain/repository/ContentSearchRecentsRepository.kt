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

package ch.protonmail.android.mailcontentsearch.domain.repository

import arrow.core.Either
import ch.protonmail.android.mailcommon.domain.model.ConversationId
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcontentsearch.domain.model.RecentFoundItem
import ch.protonmail.android.mailcontentsearch.domain.model.RecentSearchTerm
import ch.protonmail.android.mailmessage.domain.model.MessageId
import me.proton.core.domain.entity.UserId

/**
 * Durable search history owned by the Rust layer: the queries the user ran and the results they
 * opened. There is no change stream, so callers re-read after every mutation.
 */
interface ContentSearchRecentsRepository {

    suspend fun getRecentSearchTerms(
        userId: UserId,
        prefix: String?,
        limit: Int
    ): Either<DataError, List<RecentSearchTerm>>

    suspend fun getRecentFoundItems(userId: UserId, limit: Int): Either<DataError, List<RecentFoundItem>>

    suspend fun touchSearchTerm(userId: UserId, query: String): Either<DataError, Unit>

    suspend fun dismissSearchTerm(userId: UserId, query: String): Either<DataError, Unit>

    suspend fun touchFoundItem(userId: UserId, messageId: MessageId): Either<DataError, Unit>

    suspend fun dismissFoundItem(userId: UserId, messageId: MessageId): Either<DataError, Unit>

    suspend fun recordSearchOpen(
        userId: UserId,
        query: String,
        messageId: MessageId,
        conversationId: ConversationId
    ): Either<DataError, Unit>
}
