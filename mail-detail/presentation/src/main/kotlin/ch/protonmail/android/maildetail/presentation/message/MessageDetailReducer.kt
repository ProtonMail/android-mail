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

package ch.protonmail.android.maildetail.presentation.message

import ch.protonmail.android.maildetail.presentation.message.model.MessageDetailEvent
import ch.protonmail.android.maildetail.presentation.message.model.MessageDetailState
import ch.protonmail.android.maildetail.presentation.message.model.MessageUiModel
import javax.inject.Inject

class MessageDetailReducer @Inject constructor() {

    fun reduce(currentState: MessageDetailState, event: MessageDetailEvent): MessageDetailState {
        return when (event) {
            is MessageDetailEvent.NoPrimaryUser -> MessageDetailState.Error.NotLoggedIn
            is MessageDetailEvent.NoMessageIdProvided -> MessageDetailState.Error.NoMessageIdProvided

            is MessageDetailEvent.MessageMetadata -> {
                MessageDetailState.Data(
                    MessageUiModel(event.message.messageId)
                )
            }
            is MessageDetailEvent.NoCachedMetadata -> TODO(
                "This should never happen. Handle by following the 'load message body' flow (once implemented)"
            )
            is MessageDetailEvent.MessageBody -> TODO("Implement when adding message body flow")
        }
    }
}
