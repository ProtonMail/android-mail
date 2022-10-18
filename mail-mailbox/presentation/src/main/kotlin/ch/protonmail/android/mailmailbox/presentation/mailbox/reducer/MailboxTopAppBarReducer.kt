
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

package ch.protonmail.android.mailmailbox.presentation.mailbox.reducer

import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.maillabel.presentation.text
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxViewAction
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxEvent
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxOperation
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxTopAppBarState
import javax.inject.Inject

class MailboxTopAppBarReducer @Inject constructor() {

    internal fun newStateFrom(
        currentState: MailboxTopAppBarState,
        operation: MailboxOperation.AffectingTopAppBar
    ): MailboxTopAppBarState {
        return when (operation) {
            is MailboxEvent.SelectedLabelChanged -> {
                val currentMailLabel = operation.selectedLabel
                when (currentState) {
                    MailboxTopAppBarState.Loading -> MailboxTopAppBarState.Data.DefaultMode(currentMailLabel.text())
                    is MailboxTopAppBarState.Data -> currentState.with(currentMailLabel.text())
                }
            }
            is MailboxEvent.NewLabelSelected -> {
                val currentMailLabel = operation.selectedLabel
                when (currentState) {
                    MailboxTopAppBarState.Loading -> MailboxTopAppBarState.Data.DefaultMode(currentMailLabel.text())
                    is MailboxTopAppBarState.Data -> currentState.with(currentMailLabel.text())
                }
            }
            MailboxViewAction.EnterSelectionMode -> {
                when (currentState) {
                    is MailboxTopAppBarState.Loading -> currentState
                    is MailboxTopAppBarState.Data -> MailboxTopAppBarState.Data.SelectionMode(
                        currentState.currentLabelName,
                        selectedCount = 0
                    )
                }
            }
            MailboxViewAction.ExitSelectionMode -> {
                when (currentState) {
                    is MailboxTopAppBarState.Loading -> currentState
                    is MailboxTopAppBarState.Data ->
                        MailboxTopAppBarState.Data.DefaultMode(currentState.currentLabelName)
                }
            }
        }
    }

    fun MailboxTopAppBarState.with(currentLabelName: TextUiModel) = when (this) {
        MailboxTopAppBarState.Loading -> MailboxTopAppBarState.Data.DefaultMode(currentLabelName = currentLabelName)
        is MailboxTopAppBarState.Data.DefaultMode -> copy(currentLabelName = currentLabelName)
        is MailboxTopAppBarState.Data.SearchMode -> copy(currentLabelName = currentLabelName)
        is MailboxTopAppBarState.Data.SelectionMode -> copy(currentLabelName = currentLabelName)
    }
}
