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

package ch.protonmail.android.mailmailbox.presentation.paging

import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import ch.protonmail.android.mailcommon.domain.model.isOfflineError
import ch.protonmail.android.mailmailbox.presentation.mailbox.MailboxScreenState
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxItemUiModel
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxListState
import ch.protonmail.android.mailmailbox.presentation.paging.exception.DataErrorException
import timber.log.Timber

/**
 * Maps the [MailboxListState] to the [MailboxScreenState]
 */
fun LazyPagingItems<MailboxItemUiModel>.mapToUiStates(
    listState: MailboxListState,
    refreshRequested: Boolean,
    userInitiatedAppendState: MailboxScreenState
): MailboxScreenState {
    return when (listState) {
        is MailboxListState.Data.ViewMode -> {
            this.mapToUiStateInViewMode(refreshRequested, userInitiatedAppendState)
        }

        is MailboxListState.Data.SelectionMode -> {
            MailboxScreenState.Data(this)
        }

        is MailboxListState.Loading -> {
            MailboxScreenState.Loading
        }
    }
}

/**
 * Maps the [MailboxListState] to the [MailboxScreenState] only when user scroll downs the bottom
 * of the list for Appending a new page
 *
 * We keep this function separate from [mapToUiState] because we do not want to access LazyPagingItems.loadState.append
 * in [mapToUiState] function. Because it will cause background page loading changes loadState.append then it
 * causes redundant recomposition of the whole MailboxList.
 *
 * This function need to be called only locally at the end of the LazyColumn scope. So
 * it will not cause redundant recompositions
 */
fun LazyPagingItems<MailboxItemUiModel>.mapAppendToUiStates(listState: MailboxListState): MailboxScreenState {
    return when (listState) {
        is MailboxListState.Data.ViewMode -> this.mapAppendToUiStateInViewMode()
        else -> MailboxScreenState.Data(this)
    }
}

private fun LazyPagingItems<MailboxItemUiModel>.mapToUiStateInViewMode(
    refreshRequested: Boolean,
    userInitiatedAppendState: MailboxScreenState
): MailboxScreenState {
    return when {
        this.loadState.refresh is LoadState.Loading -> {
            if (this.itemCount == 0) {
                MailboxScreenState.Loading
            } else {
                MailboxScreenState.LoadingWithData(refreshRequested)
            }
        }

        userInitiatedAppendState is MailboxScreenState.AppendLoading -> MailboxScreenState.AppendLoading
        this.loadState.refresh is LoadState.Error -> refreshErrorToUiState(this)
        this.itemCount == 0 -> MailboxScreenState.Empty
        else -> MailboxScreenState.Data(this)
    }

}

private fun LazyPagingItems<MailboxItemUiModel>.mapAppendToUiStateInViewMode(): MailboxScreenState {
    return when (this.loadState.append) {
        is LoadState.Loading -> MailboxScreenState.AppendLoading
        is LoadState.Error -> appendErrorToUiState(this)
        else -> MailboxScreenState.Data(this)
    }
}

private fun appendErrorToUiState(pagingItems: LazyPagingItems<MailboxItemUiModel>): MailboxScreenState {
    val exception = (pagingItems.loadState.append as? LoadState.Error)?.error
    if (exception !is DataErrorException) {
        return MailboxScreenState.UnexpectedError
    }

    return when {
        exception.error.isOfflineError() -> MailboxScreenState.AppendOfflineError
        else -> MailboxScreenState.AppendError
    }
}

private fun refreshErrorToUiState(pagingItems: LazyPagingItems<MailboxItemUiModel>): MailboxScreenState {
    val exception = (pagingItems.loadState.refresh as? LoadState.Error)?.error
    if (exception !is DataErrorException) {
        return MailboxScreenState.UnexpectedError
    }

    Timber.d("Refresh error being mapped to UI state: ${exception.error}")
    val listHasItems = pagingItems.itemCount > 0
    if (listHasItems) {
        return when {
            exception.error.isOfflineError() -> MailboxScreenState.OfflineWithData
            else -> MailboxScreenState.ErrorWithData
        }
    }

    return when {
        exception.error.isOfflineError() -> MailboxScreenState.Offline
        else -> MailboxScreenState.Error
    }
}
