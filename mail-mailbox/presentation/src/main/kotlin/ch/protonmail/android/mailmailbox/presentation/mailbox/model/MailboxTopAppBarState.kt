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

package ch.protonmail.android.mailmailbox.presentation.mailbox.model

import ch.protonmail.android.maillabel.domain.model.MailLabel
import me.proton.core.util.kotlin.EMPTY_STRING

sealed interface MailboxTopAppBarState {

    fun with(currentMailLabel: MailLabel) = when (this) {
        Loading -> Data.DefaultMode(currentMailLabel = currentMailLabel)
        is Data.DefaultMode -> copy(currentMailLabel = currentMailLabel)
        is Data.SearchMode -> copy(currentMailLabel = currentMailLabel)
        is Data.SelectionMode -> copy(currentMailLabel = currentMailLabel)
    }

    object Loading : MailboxTopAppBarState

    sealed interface Data : MailboxTopAppBarState {

        val currentMailLabel: MailLabel

        fun toDefaultMode() = DefaultMode(currentMailLabel)
        fun toSelectionMode() = SelectionMode(currentMailLabel, selectedCount = 0)
        fun toSearchMode() = SearchMode(currentMailLabel, searchQuery = EMPTY_STRING)

        data class DefaultMode(
            override val currentMailLabel: MailLabel
        ) : Data

        data class SelectionMode(
            override val currentMailLabel: MailLabel,
            val selectedCount: Int
        ) : Data

        data class SearchMode(
            override val currentMailLabel: MailLabel,
            val searchQuery: String
        ) : Data
    }
}
