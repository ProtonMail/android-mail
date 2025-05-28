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

package ch.protonmail.android.mailmailbox.presentation.mailbox.previewdata

import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxSearchMode
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxSearchState

object MailboxSearchStateSampleData {
    const val QueryString = "Query"

    val NotSearching = MailboxSearchState(
        searchQuery = "",
        searchMode = MailboxSearchMode.None,
        showIncludeSpamTrashButton = false,
        isSearchingAllMail = false
    )

    val NewSearch = MailboxSearchState(
        searchQuery = "",
        searchMode = MailboxSearchMode.NewSearch,
        showIncludeSpamTrashButton = false,
        isSearchingAllMail = false
    )

    val NewSearchAllMail = MailboxSearchState(
        searchQuery = "",
        searchMode = MailboxSearchMode.NewSearch,
        showIncludeSpamTrashButton = false,
        isSearchingAllMail = true
    )

    val SearchLoading = MailboxSearchState(
        searchQuery = QueryString,
        searchMode = MailboxSearchMode.NewSearchLoading,
        showIncludeSpamTrashButton = false,
        isSearchingAllMail = false
    )

    val SearchData = MailboxSearchState(
        searchQuery = QueryString,
        searchMode = MailboxSearchMode.SearchData,
        showIncludeSpamTrashButton = true,
        isSearchingAllMail = false
    )

}
