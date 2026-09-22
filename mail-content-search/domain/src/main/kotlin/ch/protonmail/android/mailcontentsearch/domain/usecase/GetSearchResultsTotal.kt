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

import ch.protonmail.android.mailmessage.domain.repository.MessageRepository
import ch.protonmail.android.mailpagination.domain.model.PageKey
import me.proton.core.domain.entity.UserId
import timber.log.Timber
import javax.inject.Inject

/**
 * How many results the search for [keyword] has in all, which the paging list cannot say: it only
 * counts the pages loaded so far.
 *
 * Null when there is no number to show — that search is not the one paging, or Rust could not
 * answer. The header keeps its previous count in that case rather than showing a wrong one.
 */
class GetSearchResultsTotal @Inject constructor(
    private val messageRepository: MessageRepository
) {

    suspend operator fun invoke(userId: UserId, keyword: String): Int? = messageRepository
        .getTotalItemsCount(userId, PageKey.PageKeyForSearch(keyword = keyword))
        ?.onLeft { Timber.w("content-search: failed to read the results total: $it") }
        ?.getOrNull()
        ?.toInt()
}
