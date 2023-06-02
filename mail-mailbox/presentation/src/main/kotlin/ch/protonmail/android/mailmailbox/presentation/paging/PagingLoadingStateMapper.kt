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

import androidx.paging.CombinedLoadStates
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import ch.protonmail.android.mailcommon.domain.model.isOfflineError
import ch.protonmail.android.mailmailbox.presentation.mailbox.MailboxScreenState
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxItemUiModel
import ch.protonmail.android.mailmailbox.presentation.paging.exception.DataErrorException
import timber.log.Timber

fun LazyPagingItems<MailboxItemUiModel>.mapToUiStates(): MailboxScreenState {
    return when {
        this.loadState.isLoading() -> {
            if (this.itemCount == 0) {
                MailboxScreenState.Loading
            } else {
                MailboxScreenState.LoadingWithData
            }
        }
        this.loadState.isAppendLoading() -> MailboxScreenState.AppendLoading
        this.loadState.append is LoadState.Error -> appendErrorToUiState(this)
        this.loadState.refresh is LoadState.Error -> refreshErrorToUiState(this)
        this.itemCount == 0 -> MailboxScreenState.Empty
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

private fun CombinedLoadStates.isLoading() = this.isSourceLoading() || this.isMediatorLoading()

private fun CombinedLoadStates.isSourceLoading() = this.source.refresh is LoadState.Loading
private fun CombinedLoadStates.isMediatorLoading() = this.mediator?.refresh is LoadState.Loading
private fun CombinedLoadStates.isAppendLoading() = this.append is LoadState.Loading
