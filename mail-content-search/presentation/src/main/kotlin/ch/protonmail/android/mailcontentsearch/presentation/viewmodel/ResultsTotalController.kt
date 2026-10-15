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
package ch.protonmail.android.mailcontentsearch.presentation.viewmodel

import ch.protonmail.android.mailcontentsearch.presentation.model.ResultsTotalUiState
import ch.protonmail.android.mailmailbox.domain.model.MailboxFetchNewStatus
import ch.protonmail.android.mailmailbox.domain.model.ScrollerType
import ch.protonmail.android.mailmailbox.presentation.mailbox.MailboxLoadingBarStateController
import ch.protonmail.android.mailpagination.domain.model.PaginatorTotal
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Turns the totals Rust sends for a search into what the results header shows.
 *
 * A pending total starts the loading bar. The known total that follows stops it. The bar finishes
 * its current cycle before it hides, so a fast remote search does not flash it.
 *
 * The header shows the number only once the bar has hidden. A known total that arrives with no
 * pending one before it, as when offline, shows straight away.
 */
internal class ResultsTotalController(
    private val loadingBar: MailboxLoadingBarStateController,
    private val currentTimeMs: () -> Long = System::currentTimeMillis
) {

    private val latestCount = MutableStateFlow<Int?>(null)

    val state: Flow<ResultsTotalUiState> = combine(loadingBar.observeState(), latestCount, ::ResultsTotalUiState)
        .distinctUntilChanged()

    fun onTotal(total: PaginatorTotal) {
        when (total) {
            is PaginatorTotal.Pending -> loadingBar.onMailboxFetchNewStatus(
                MailboxFetchNewStatus.Started(currentTimeMs(), ScrollerType.Message)
            )

            is PaginatorTotal.Known -> {
                latestCount.value = total.count.toInt()
                loadingBar.onMailboxFetchNewStatus(MailboxFetchNewStatus.Ended(currentTimeMs(), ScrollerType.Message))
            }
        }
    }

    /** Forgets the last search's number, so a new search never shows it. */
    fun reset() {
        latestCount.value = null
        loadingBar.onMailboxFetchNewStatus(MailboxFetchNewStatus.Ended(currentTimeMs(), ScrollerType.Message))
    }
}
