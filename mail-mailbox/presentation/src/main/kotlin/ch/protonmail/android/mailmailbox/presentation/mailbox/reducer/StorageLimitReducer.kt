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

import ch.protonmail.android.mailmailbox.domain.model.isOverFirstLimit
import ch.protonmail.android.mailmailbox.domain.model.isOverQuota
import ch.protonmail.android.mailmailbox.domain.model.isOverSecondLimit
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxEvent
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxOperation
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.MailboxViewAction
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.StorageLimitState
import javax.inject.Inject

class StorageLimitReducer @Inject constructor() {

    internal fun newStateFrom(
        currentState: StorageLimitState,
        event: MailboxOperation.AffectingStorageLimit
    ): StorageLimitState {

        return when (event) {
            is MailboxEvent.StorageLimitStatusChanged -> newStateForStatusChange(currentState, event)
            is MailboxViewAction.StorageLimitDoNotRemind,
            is MailboxViewAction.StorageLimitConfirmed -> newStateForWarningConfirmed(currentState)
        }
    }

    private fun newStateForStatusChange(
        currentState: StorageLimitState,
        event: MailboxEvent.StorageLimitStatusChanged
    ): StorageLimitState {
        return when (currentState) {
            is StorageLimitState.None -> createInitialState(event)
            is StorageLimitState.Notifiable.QuotaOver -> handleStatusChangeWhenQuotaOver(currentState, event)
            is StorageLimitState.Notifiable.FirstLimitOver -> handleStatusChangeWhenFirstLimitOver(currentState, event)
            is StorageLimitState.Notifiable.SecondLimitOver ->
                handleStatusChangeWhenSecondLimitOver(currentState, event)
            is StorageLimitState.HasEnoughSpace -> handleStatusChangeWhenHasEnoughSpace(event)
        }
    }

    private fun createInitialState(event: MailboxEvent.StorageLimitStatusChanged): StorageLimitState {
        return if (event.userAccountStorageStatus.isOverQuota()) {
            StorageLimitState.Notifiable.QuotaOver(false)
        } else if (event.userAccountStorageStatus.isOverSecondLimit()) {
            StorageLimitState.Notifiable.SecondLimitOver(event.storageLimitPreference.secondLimitWarningConfirmed)
        } else if (event.userAccountStorageStatus.isOverFirstLimit()) {
            StorageLimitState.Notifiable.FirstLimitOver(event.storageLimitPreference.firstLimitWarningConfirmed)
        } else {
            StorageLimitState.HasEnoughSpace
        }
    }

    private fun handleStatusChangeWhenQuotaOver(
        currentState: StorageLimitState.Notifiable.QuotaOver,
        event: MailboxEvent.StorageLimitStatusChanged
    ): StorageLimitState {
        return if (event.userAccountStorageStatus.isOverQuota()) {
            currentState
        } else if (event.userAccountStorageStatus.isOverSecondLimit()) {
            StorageLimitState.Notifiable.SecondLimitOver(currentState.confirmed)
        } else if (event.userAccountStorageStatus.isOverFirstLimit()) {
            StorageLimitState.Notifiable.FirstLimitOver(currentState.confirmed)
        } else {
            StorageLimitState.HasEnoughSpace
        }
    }

    private fun handleStatusChangeWhenSecondLimitOver(
        currentState: StorageLimitState.Notifiable.SecondLimitOver,
        event: MailboxEvent.StorageLimitStatusChanged
    ): StorageLimitState {
        return if (event.userAccountStorageStatus.isOverQuota()) {
            StorageLimitState.Notifiable.QuotaOver(false)
        } else if (event.userAccountStorageStatus.isOverSecondLimit()) {
            currentState
        } else if (event.userAccountStorageStatus.isOverFirstLimit()) {
            StorageLimitState.Notifiable.FirstLimitOver(currentState.confirmed)
        } else {
            StorageLimitState.HasEnoughSpace
        }
    }

    private fun handleStatusChangeWhenFirstLimitOver(
        currentState: StorageLimitState.Notifiable.FirstLimitOver,
        event: MailboxEvent.StorageLimitStatusChanged
    ): StorageLimitState {
        return if (event.userAccountStorageStatus.isOverQuota()) {
            StorageLimitState.Notifiable.QuotaOver(false)
        } else if (event.userAccountStorageStatus.isOverSecondLimit()) {
            StorageLimitState.Notifiable.SecondLimitOver(false)
        } else if (event.userAccountStorageStatus.isOverFirstLimit()) {
            currentState
        } else {
            StorageLimitState.HasEnoughSpace
        }
    }

    private fun handleStatusChangeWhenHasEnoughSpace(event: MailboxEvent.StorageLimitStatusChanged): StorageLimitState {
        return if (event.userAccountStorageStatus.isOverQuota()) {
            StorageLimitState.Notifiable.QuotaOver(false)
        } else if (event.userAccountStorageStatus.isOverSecondLimit()) {
            StorageLimitState.Notifiable.SecondLimitOver(false)
        } else if (event.userAccountStorageStatus.isOverFirstLimit()) {
            StorageLimitState.Notifiable.FirstLimitOver(false)
        } else {
            StorageLimitState.HasEnoughSpace
        }
    }

    private fun newStateForWarningConfirmed(currentState: StorageLimitState): StorageLimitState {
        return when (currentState) {
            is StorageLimitState.Notifiable.QuotaOver -> StorageLimitState.Notifiable.QuotaOver(true)
            is StorageLimitState.Notifiable.FirstLimitOver -> StorageLimitState.Notifiable.FirstLimitOver(true)
            is StorageLimitState.Notifiable.SecondLimitOver -> StorageLimitState.Notifiable.SecondLimitOver(true)
            else -> currentState
        }
    }
}
