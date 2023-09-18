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

import ch.protonmail.android.maildetail.presentation.model.MessageBodyState
import ch.protonmail.android.mailmessage.presentation.model.MessageBodyUiModel
import ch.protonmail.android.maildetail.presentation.model.MessageDetailEvent
import ch.protonmail.android.maildetail.presentation.model.MessageDetailOperation
import ch.protonmail.android.maildetail.presentation.model.MessageViewAction
import javax.inject.Inject

class MessageBodyReducer @Inject constructor() {

    fun newStateFrom(
        messageBodyState: MessageBodyState,
        event: MessageDetailOperation.AffectingMessageBody
    ): MessageBodyState {
        return when (event) {
            is MessageViewAction.Reload -> MessageBodyState.Loading
            is MessageDetailEvent.MessageBodyEvent -> MessageBodyState.Data(event.messageBody)
            is MessageDetailEvent.ErrorGettingMessageBody -> MessageBodyState.Error.Data(event.isNetworkError)
            is MessageDetailEvent.ErrorDecryptingMessageBody -> MessageBodyState.Error.Decryption(event.messageBody)
            is MessageDetailEvent.AttachmentStatusChanged ->
                messageBodyState.newMessageBodyStateFromAttachmentStatus(event)
        }
    }

    private fun MessageBodyState.newMessageBodyStateFromAttachmentStatus(
        operation: MessageDetailEvent.AttachmentStatusChanged
    ): MessageBodyState {
        return when (this) {
            is MessageBodyState.Data -> createMessageBodyState(messageBodyUiModel, operation)
            else -> this
        }
    }

    private fun createMessageBodyState(
        messageBodyUiModel: MessageBodyUiModel,
        operation: MessageDetailEvent.AttachmentStatusChanged
    ): MessageBodyState.Data {
        val attachmentGroupUiModel = messageBodyUiModel.attachments
        return MessageBodyState.Data(
            messageBodyUiModel.copy(
                attachments = attachmentGroupUiModel?.copy(
                    attachments = attachmentGroupUiModel.attachments.map { attachment ->
                        if (attachment.attachmentId == operation.attachmentId.id) {
                            attachment.copy(status = operation.status)
                        } else {
                            attachment
                        }
                    }
                )
            )
        )
    }
}
