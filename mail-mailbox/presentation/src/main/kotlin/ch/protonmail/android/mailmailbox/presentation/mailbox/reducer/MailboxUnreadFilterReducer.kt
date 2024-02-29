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

import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxEvent
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxOperation
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxViewAction
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.UnreadFilterState
import javax.inject.Inject

class MailboxUnreadFilterReducer @Inject constructor() {

    internal fun newStateFrom(
        currentState: UnreadFilterState,
        operation: MailboxOperation.AffectingUnreadFilter
    ): UnreadFilterState {
        return when (operation) {
            is MailboxEvent.SelectedLabelCountChanged -> currentState.toNewStateForCountChanged(operation)
            is MailboxEvent.NewLabelSelected -> currentState.toNewStateForLabelSelected(operation)
            MailboxViewAction.DisableUnreadFilter -> currentState.toNewStateForFilterDisabled()
            MailboxViewAction.EnableUnreadFilter -> currentState.toNewStateForFilterEnabled()
        }
    }

    private fun UnreadFilterState.toNewStateForFilterEnabled() = when (this) {
        is UnreadFilterState.Loading -> this
        is UnreadFilterState.Data -> copy(isFilterEnabled = true)
    }

    private fun UnreadFilterState.toNewStateForFilterDisabled() = when (this) {
        is UnreadFilterState.Loading -> this
        is UnreadFilterState.Data -> copy(isFilterEnabled = false)
    }

    private fun UnreadFilterState.toNewStateForLabelSelected(operation: MailboxEvent.NewLabelSelected) = when (this) {
        is UnreadFilterState.Loading -> UnreadFilterState.Data(
            numUnread = operation.selectedLabelCount ?: 0,
            isFilterEnabled = false
        )
        is UnreadFilterState.Data -> copy(numUnread = operation.selectedLabelCount ?: 0)
    }

    private fun UnreadFilterState.toNewStateForCountChanged(
        operation: MailboxEvent.SelectedLabelCountChanged
    ): UnreadFilterState.Data {
        val currentLabelCount = operation.selectedLabelCount
        return when (this) {
            is UnreadFilterState.Loading -> UnreadFilterState.Data(currentLabelCount, false)
            is UnreadFilterState.Data -> copy(currentLabelCount)
        }
    }
}
