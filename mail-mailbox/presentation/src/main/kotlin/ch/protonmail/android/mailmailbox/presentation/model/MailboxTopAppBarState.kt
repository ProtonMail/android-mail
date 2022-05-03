/*
 * Copyright (c) 2021 Proton Technologies AG
 * This file is part of Proton Technologies AG and ProtonMail.
 *
 * ProtonMail is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ProtonMail is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with ProtonMail.  If not, see <https://www.gnu.org/licenses/>.
 */

package ch.protonmail.android.mailmailbox.presentation.model

import me.proton.core.util.kotlin.EMPTY_STRING

sealed interface MailboxTopAppBarState {

    object Loading : MailboxTopAppBarState

    sealed interface Data : MailboxTopAppBarState {

        val currentLabelName: String

        fun toDefaultMode() = DefaultMode(currentLabelName)
        fun toSelectionMode() = SelectionMode(currentLabelName, selectedCount = 0)
        fun toSearchMode() = SearchMode(currentLabelName, searchQuery = EMPTY_STRING)

        data class DefaultMode(
            override val currentLabelName: String
        ) : MailboxTopAppBarState.Data

        data class SelectionMode(
            override val currentLabelName: String,
            val selectedCount: Int
        ) : MailboxTopAppBarState.Data

        data class SearchMode(
            override val currentLabelName: String,
            val searchQuery: String
        ) : MailboxTopAppBarState.Data
    }
}
