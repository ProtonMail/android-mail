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
import ch.protonmail.android.mailpagination.domain.model.PaginatorTotal
import kotlinx.coroutines.flow.Flow
import me.proton.core.domain.entity.UserId
import javax.inject.Inject

/**
 * How many results the search for [keyword] has in all, sent again whenever Rust's number changes.
 *
 * It is [PaginatorTotal.Pending] while the remote search is still running and will change it.
 */
class ObserveSearchResultsTotal @Inject constructor(
    private val messageRepository: MessageRepository
) {

    operator fun invoke(userId: UserId, keyword: String): Flow<PaginatorTotal> =
        messageRepository.observeTotalItemsCount(userId, PageKey.PageKeyForSearch(keyword = keyword))
}
