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

package ch.protonmail.android.maildetail.presentation.reducer

import ch.protonmail.android.maildetail.presentation.model.AffectingMessage
import ch.protonmail.android.maildetail.presentation.model.Event
import ch.protonmail.android.maildetail.presentation.model.MessageState
import ch.protonmail.android.maildetail.presentation.model.MessageViewAction
import javax.inject.Inject

class MessageStateReducer @Inject constructor() {

    @SuppressWarnings("UnusedPrivateMember", "NotImplementedDeclaration")
    fun newStateFrom(currentState: MessageState, event: AffectingMessage): MessageState {
        return when (event) {
            is Event.NoPrimaryUser -> MessageState.Error.NotLoggedIn

            is Event.MessageMetadata -> MessageState.Data(event.messageUiModel)

            is Event.NoCachedMetadata -> TODO(
                "This should never happen. Handle by following the 'load message body' flow (once implemented)"
            )
            is Event.MessageBody -> TODO("Implement when adding message body flow")
            is MessageViewAction.Star -> TODO()
            is MessageViewAction.UnStar -> TODO()
        }
    }
}
