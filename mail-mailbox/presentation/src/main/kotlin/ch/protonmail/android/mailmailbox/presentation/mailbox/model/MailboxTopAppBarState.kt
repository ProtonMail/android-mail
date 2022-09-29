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

import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.maillabel.domain.model.MailLabel
import ch.protonmail.android.maillabel.presentation.text
import me.proton.core.util.kotlin.EMPTY_STRING

sealed interface MailboxTopAppBarState {

    @Deprecated("Use with currentLabelName instead")
    fun with(currentMailLabel: MailLabel) = when (this) {
        Loading -> Data.DefaultMode(currentLabelName = currentMailLabel.text())
        is Data.DefaultMode -> copy(currentLabelName = currentLabelName)
        is Data.SearchMode -> copy(currentLabelName = currentLabelName)
        is Data.SelectionMode -> copy(currentLabelName = currentLabelName)
    }

    fun with(currentLabelName: TextUiModel) = when (this) {
        Loading -> Data.DefaultMode(currentLabelName = currentLabelName)
        is Data.DefaultMode -> copy(currentLabelName = currentLabelName)
        is Data.SearchMode -> copy(currentLabelName = currentLabelName)
        is Data.SelectionMode -> copy(currentLabelName = currentLabelName)
    }

    object Loading : MailboxTopAppBarState

    sealed interface Data : MailboxTopAppBarState {

        val currentLabelName: TextUiModel

        fun toDefaultMode() = DefaultMode(currentLabelName)
        fun toSelectionMode() = SelectionMode(currentLabelName, selectedCount = 0)
        fun toSearchMode() = SearchMode(currentLabelName, searchQuery = EMPTY_STRING)

        data class DefaultMode(
            override val currentLabelName: TextUiModel
        ) : Data

        data class SelectionMode(
            override val currentLabelName: TextUiModel,
            val selectedCount: Int
        ) : Data

        data class SearchMode(
            override val currentLabelName: TextUiModel,
            val searchQuery: String
        ) : Data
    }
}
