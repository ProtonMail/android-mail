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

import androidx.paging.compose.LazyPagingItems
import ch.protonmail.android.mailmailbox.presentation.mailbox.MailboxScreenState
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxItemUiModel
import ch.protonmail.android.mailmailbox.presentation.paging.appendErrorToUiState
import ch.protonmail.android.mailmailbox.presentation.paging.isPageEmpty
import ch.protonmail.android.mailmailbox.presentation.paging.isPageInError
import ch.protonmail.android.mailmailbox.presentation.paging.isPageRefreshFailed
import ch.protonmail.android.mailmailbox.presentation.paging.refreshErrorToUiState

object SearchNoDataStateHandler {

    fun getNextState(paging: LazyPagingItems<MailboxItemUiModel>): MailboxScreenState {
        return if (!paging.isPageInError()) {
            if (paging.isPageLoadingWhenNoSearchData()) {
                MailboxScreenState.SearchLoading
            } else if (!paging.isPageLoadingWhenNoSearchData() && !paging.isPageEmpty()) {
                MailboxScreenState.SearchData(paging)
            } else {
                MailboxScreenState.SearchNoData
            }
        } else {
            if (paging.isPageRefreshFailed()) {
                refreshErrorToUiState(paging)
            } else {
                appendErrorToUiState(paging)
            }
        }
    }
}
