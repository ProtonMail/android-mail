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

package ch.protonmail.android.mailmessage.domain.usecase

import arrow.core.getOrElse
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailmessage.domain.model.searchMatchedTerms
import ch.protonmail.android.mailmessage.domain.repository.MessageRepository
import ch.protonmail.android.mailpagination.domain.model.PageKey
import me.proton.core.domain.entity.UserId
import timber.log.Timber
import javax.inject.Inject

/**
 * The text [searchQuery] matched in one message's subject and addresses, fuzzy matches included.
 *
 * Positions come from the active search, so this is empty once [searchQuery] is no longer being searched.
 * Highlighting is best effort, so a failure also returns no terms.
 */
class GetSearchMatchedTerms @Inject constructor(
    private val messageRepository: MessageRepository
) {

    suspend operator fun invoke(
        userId: UserId,
        searchQuery: String,
        messageId: MessageId
    ): List<String> {
        val positions = messageRepository
            .getSearchMatchPositions(userId, PageKey.PageKeyForSearch(searchQuery), listOf(messageId))
            .getOrElse {
                Timber.w("Failed to get search match positions for $messageId: $it")
                emptyMap()
            }[messageId]
            .orEmpty()
        if (positions.isEmpty()) return emptyList()

        return messageRepository.getMessage(userId, messageId).fold(
            ifLeft = {
                Timber.w("Failed to get message $messageId for search highlighting: $it")
                emptyList()
            },
            ifRight = { it.searchMatchedTerms(positions) }
        )
    }
}
