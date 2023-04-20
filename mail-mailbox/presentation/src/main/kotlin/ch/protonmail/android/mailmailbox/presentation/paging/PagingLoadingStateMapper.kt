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
import ch.protonmail.android.mailmailbox.presentation.mailbox.MailboxScreenState
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxItemUiModel

@Suppress("ReturnCount")
fun LazyPagingItems<MailboxItemUiModel>.mapToUiStates(): MailboxScreenState {
    if (this.loadState.isLoading()) {
        return if (this.itemCount == 0) {
            MailboxScreenState.Loading
        } else {
            MailboxScreenState.LoadingWithData(this)
        }
    } else if (this.loadState.refresh is LoadState.Error) {
        return if (this.itemCount == 0) {
            MailboxScreenState.Error
        } else {
            MailboxScreenState.ErrorWithData(this)
        }
    } else if (this.itemCount == 0) {
        return MailboxScreenState.Empty
    } else {
        return MailboxScreenState.Data(this)
    }
}

private fun CombinedLoadStates.isLoading() =
    this.isSourceLoading() || this.isMediatorLoading() || this.isAppendLoading()

private fun CombinedLoadStates.isSourceLoading() = this.source.refresh is LoadState.Loading
private fun CombinedLoadStates.isMediatorLoading() = this.mediator?.refresh is LoadState.Loading
private fun CombinedLoadStates.isAppendLoading() = this.append is LoadState.Loading
