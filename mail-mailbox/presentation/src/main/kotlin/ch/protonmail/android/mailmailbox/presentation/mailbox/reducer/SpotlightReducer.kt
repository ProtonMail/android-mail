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
import ch.protonmail.android.mailmailbox.presentation.mailbox.model.SpotlightState
import javax.inject.Inject

class SpotlightReducer @Inject constructor() {

    internal fun newStateFrom(
        currentState: SpotlightState,
        operation: MailboxOperation.AffectingSpotlight
    ): SpotlightState {
        return when (operation) {
            is MailboxEvent.ShowSpotlight -> currentState.toNewStateForSpotlightShowChanged()
            is MailboxEvent.HideSpotlight -> currentState.toNewStateForSpotlightHideChanged()
            is MailboxViewAction.SpotlightClosed -> currentState.toNewStateForSpotlightClosedChanged()
        }
    }

    private fun SpotlightState.toNewStateForSpotlightShowChanged(): SpotlightState {
        return when (this) {
            is SpotlightState.Hidden -> SpotlightState.Shown
            is SpotlightState.Shown -> this
        }
    }

    private fun SpotlightState.toNewStateForSpotlightHideChanged(): SpotlightState {
        return when (this) {
            is SpotlightState.Hidden -> this
            is SpotlightState.Shown -> SpotlightState.Hidden
        }
    }

    private fun SpotlightState.toNewStateForSpotlightClosedChanged(): SpotlightState {
        return when (this) {
            is SpotlightState.Hidden -> this
            is SpotlightState.Shown -> SpotlightState.Hidden
        }
    }
}
