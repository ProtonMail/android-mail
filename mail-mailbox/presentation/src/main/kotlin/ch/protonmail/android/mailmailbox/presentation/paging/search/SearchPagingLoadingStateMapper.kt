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

package ch.protonmail.android.mailmailbox.presentation.paging.search

import androidx.paging.CombinedLoadStates
import androidx.paging.LoadState
import androidx.paging.LoadStates
import androidx.paging.compose.LazyPagingItems
import ch.protonmail.android.mailmailbox.presentation.mailbox.MailboxScreenState
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxItemUiModel
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxSearchMode
import me.proton.core.util.kotlin.exhaustive

@Suppress("ComplexMethod", "ReturnCount")
fun LazyPagingItems<MailboxItemUiModel>.mapToUiStatesInSearch(
    searchMode: MailboxSearchMode,
    currentScreenState: MailboxScreenState
): MailboxScreenState {

    // Switch to initial search state
    if (searchMode == MailboxSearchMode.NewSearch) {
        return MailboxScreenState.NewSearch
    }

    // Current state should change in a transitional way. Otherwise it may jump from one state to another randomly
    return when (currentScreenState) {
        is MailboxScreenState.NewSearch -> NewSearchStateHandler.getNextState(this, searchMode)

        is MailboxScreenState.SearchLoading -> SearchLoadingStateHandler.getNextState(this)

        is MailboxScreenState.SearchNoData -> SearchNoDataStateHandler.getNextState(this)

        is MailboxScreenState.SearchData -> SearchDataStateHandler.getNextState(this)

        is MailboxScreenState.SearchLoadingWithData -> SearchLoadingWithDataStateHandler.getNextState(this)

        is MailboxScreenState.UnexpectedError,
        is MailboxScreenState.Error,
        is MailboxScreenState.Offline,
        is MailboxScreenState.SearchInputInvalidError,
        is MailboxScreenState.OfflineWithData -> SearchRefreshErrorStateHandler.getNextState(this, searchMode)

        is MailboxScreenState.AppendError,
        is MailboxScreenState.AppendOfflineError -> SearchAppendErrorStateHandler.getNextState(this, searchMode)

        else -> SearchStateFinder.getBestSearchState(this, searchMode)
    }.exhaustive
}

// In search mode, after we have some search data we will only check if loadState.refresh is loading,
// otherwise the loading indicator can be shown randomly at the top
fun LazyPagingItems<MailboxItemUiModel>.isPageLoadingWhenSearchData(): Boolean =
    this.loadState.refresh is LoadState.Loading

// When there is no search data, we will check mediator + source refresh loading state.
// This is to ensure that loading is completed after both source + mediator loading completed. Otherwise,
// we may get a situation where "No Results Found" appear for a short time, then results appear
fun LazyPagingItems<MailboxItemUiModel>.isPageLoadingWhenNoSearchData(): Boolean =
    this.loadState.isLoadingInAnyDirection()

private fun CombinedLoadStates.isLoadingInAnyDirection(): Boolean {
    return source.isLoadingInAnyDirection() ||
        mediator?.isLoadingInAnyDirection() == true
}

private fun LoadStates.isLoadingInAnyDirection() =
    refresh is LoadState.Loading || prepend is LoadState.Loading || append is LoadState.Loading
