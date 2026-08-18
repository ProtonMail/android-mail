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

package ch.protonmail.android.mailcontentsearch.presentation.model

import ch.protonmail.android.mailcommon.domain.model.ConversationId
import ch.protonmail.android.mailmessage.domain.model.MessageId

sealed interface RecentSearchesViewAction {

    /** Re-reads the history. Safe to submit repeatedly: it never drops back to the loading state. */
    data object Refresh : RecentSearchesViewAction

    /** Used to filter out the recent search terms as the user types **/
    data class QueryChanged(val query: String) : RecentSearchesViewAction

    data class TermClicked(val query: String) : RecentSearchesViewAction

    data class TermDismissed(val query: String) : RecentSearchesViewAction

    data object ClearTerms : RecentSearchesViewAction

    data class FoundItemClicked(val item: RecentFoundItemUiModel) : RecentSearchesViewAction

    data class FoundItemStarClicked(val item: RecentFoundItemUiModel) : RecentSearchesViewAction

    /** A live search result was opened: records both the query and the item it was found by. */
    data class SearchResultOpened(
        val query: String,
        val messageId: MessageId,
        val conversationId: ConversationId
    ) : RecentSearchesViewAction
}
