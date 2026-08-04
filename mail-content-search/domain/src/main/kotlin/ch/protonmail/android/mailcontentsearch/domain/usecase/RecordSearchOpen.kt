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

package ch.protonmail.android.mailcontentsearch.domain.usecase

import arrow.core.Either
import arrow.core.right
import ch.protonmail.android.mailcommon.domain.model.ConversationId
import ch.protonmail.android.mailcommon.domain.model.DataError
import ch.protonmail.android.mailcontentsearch.domain.repository.ContentSearchRecentsRepository
import ch.protonmail.android.mailmessage.domain.model.MessageId
import me.proton.core.domain.entity.UserId
import javax.inject.Inject

/**
 * Records that the user opened a search result, which upserts both the query and the item.
 *
 * This is the only entry point that creates history: opening a result is what makes a query worth
 * remembering, so tapping a recent query only bumps it (see [TouchRecentSearchTerm]).
 */
class RecordSearchOpen @Inject constructor(
    private val repository: ContentSearchRecentsRepository
) {

    /**
     * @param query must be the same non-empty string that produced the result. A blank query is a
     * no-op, since the Rust layer rejects it and there would be no term to remember.
     */
    suspend operator fun invoke(
        userId: UserId,
        query: String,
        messageId: MessageId,
        conversationId: ConversationId
    ): Either<DataError, Unit> {
        if (query.isBlank()) return Unit.right()
        return repository.recordSearchOpen(userId, query, messageId, conversationId)
    }
}
