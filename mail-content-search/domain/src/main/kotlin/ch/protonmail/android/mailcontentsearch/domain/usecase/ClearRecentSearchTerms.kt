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

import arrow.core.getOrElse
import ch.protonmail.android.mailcontentsearch.domain.repository.ContentSearchRecentsRepository
import me.proton.core.domain.entity.UserId
import timber.log.Timber
import javax.inject.Inject

/**
 * Dismisses every remembered query, backing the "Clear" action on the recents header.
 *
 * The Rust layer has no bulk clear, so this reads the whole term history and dismisses each entry —
 * dismissing only the handful on screen would let the list immediately refill from the tail and look
 * like the action had failed. [GetRecentSearchTerms.DefaultLimit] is the plain-recents-list page size,
 * not a sweep size — [WholeHistoryLimit] is a generous ceiling instead: comfortably above anything a
 * user-typed query history could realistically reach, without the real cap being known here. Both the
 * read and each dismiss are logged rather than surfaced: this is a best-effort sweep, and re-reading
 * the list is how the UI finds out what is actually left.
 */
class ClearRecentSearchTerms @Inject constructor(
    private val repository: ContentSearchRecentsRepository
) {

    suspend operator fun invoke(userId: UserId) {
        repository.getRecentSearchTerms(userId, prefix = null, limit = WholeHistoryLimit)
            .getOrElse {
                Timber.d("ClearRecentSearchTerms: could not read the term history: $it")
                emptyList()
            }
            .forEach { term ->
                repository.dismissSearchTerm(userId, term.query).onLeft {
                    Timber.d("ClearRecentSearchTerms: could not dismiss '${term.query}': $it")
                }
            }
    }

    companion object {

        const val WholeHistoryLimit = 100
    }
}
