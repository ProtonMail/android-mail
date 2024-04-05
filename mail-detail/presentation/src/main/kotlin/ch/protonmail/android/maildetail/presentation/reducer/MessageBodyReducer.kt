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

import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.maildetail.presentation.model.MessageBodyState
import ch.protonmail.android.mailmessage.presentation.model.MessageBodyUiModel
import ch.protonmail.android.maildetail.presentation.model.MessageDetailEvent
import ch.protonmail.android.maildetail.presentation.model.MessageDetailOperation
import ch.protonmail.android.maildetail.presentation.model.MessageViewAction
import ch.protonmail.android.mailmessage.presentation.model.MessageBodyExpandCollapseMode
import ch.protonmail.android.mailmessage.presentation.model.MessageBodyWithType
import ch.protonmail.android.mailmessage.presentation.model.ViewModePreference
import ch.protonmail.android.mailmessage.presentation.usecase.InjectCssIntoDecryptedMessageBody
import javax.inject.Inject

class MessageBodyReducer @Inject constructor(
    private val injectCssIntoDecryptedMessageBody: InjectCssIntoDecryptedMessageBody
) {

    fun newStateFrom(
        messageBodyState: MessageBodyState,
        event: MessageDetailOperation.AffectingMessageBody
    ): MessageBodyState {
        return when (event) {
            is MessageViewAction.Reload -> MessageBodyState.Loading
            is MessageDetailEvent.MessageBodyEvent -> MessageBodyState.Data(event.messageBody, event.expandCollapseMode)
            is MessageDetailEvent.ErrorGettingMessageBody -> MessageBodyState.Error.Data(event.isNetworkError)
            is MessageDetailEvent.ErrorDecryptingMessageBody -> MessageBodyState.Error.Decryption(event.messageBody)
            is MessageDetailEvent.AttachmentStatusChanged ->
                messageBodyState.newMessageBodyStateFromAttachmentStatus(event)

            is MessageViewAction.ExpandOrCollapseMessageBody ->
                messageBodyState.newMessageBodyStateFromCollapseOrExpand()

            is MessageViewAction.LoadRemoteContent -> messageBodyState.newStateFromLoadRemoteContent()
            is MessageViewAction.ShowEmbeddedImages -> messageBodyState.newStateFromShowEmbeddedImages()
            is MessageViewAction.LoadRemoteAndEmbeddedContent ->
                messageBodyState.newStateFromLoadRemoteAndEmbeddedContent()
            is MessageViewAction.SwitchViewMode -> messageBodyState.newStateFromSwitchViewMode(event.viewModePreference)
            is MessageViewAction.PrintRequested -> messageBodyState.newStateFromPrintRequested()
        }
    }

    private fun MessageBodyState.newStateFromLoadRemoteAndEmbeddedContent(): MessageBodyState {
        return when (this) {
            is MessageBodyState.Data -> this.copy(
                messageBodyUiModel = messageBodyUiModel.copy(
                    shouldShowRemoteContent = true,
                    shouldShowEmbeddedImages = true,
                    shouldShowEmbeddedImagesBanner = false,
                    shouldShowRemoteContentBanner = false
                )
            )
            else -> this
        }
    }

    private fun MessageBodyState.newStateFromShowEmbeddedImages(): MessageBodyState {
        return when (this) {
            is MessageBodyState.Data -> this.copy(
                messageBodyUiModel = messageBodyUiModel.copy(
                    shouldShowEmbeddedImages = true,
                    shouldShowEmbeddedImagesBanner = false
                )
            )
            else -> this
        }
    }

    private fun MessageBodyState.newStateFromLoadRemoteContent(): MessageBodyState {
        return when (this) {
            is MessageBodyState.Data -> this.copy(
                messageBodyUiModel = messageBodyUiModel.copy(
                    shouldShowRemoteContent = true,
                    shouldShowRemoteContentBanner = false
                )
            )
            else -> this
        }
    }

    private fun MessageBodyState.newMessageBodyStateFromCollapseOrExpand(): MessageBodyState {
        return when (this) {
            is MessageBodyState.Data -> MessageBodyState.Data(
                messageBodyUiModel,
                when (expandCollapseMode) {
                    MessageBodyExpandCollapseMode.Collapsed -> MessageBodyExpandCollapseMode.Expanded
                    MessageBodyExpandCollapseMode.Expanded -> MessageBodyExpandCollapseMode.Collapsed
                    else -> expandCollapseMode
                }
            )

            else -> this
        }
    }

    private fun MessageBodyState.newMessageBodyStateFromAttachmentStatus(
        operation: MessageDetailEvent.AttachmentStatusChanged
    ): MessageBodyState {
        return when (this) {
            is MessageBodyState.Data -> createMessageBodyState(messageBodyUiModel, expandCollapseMode, operation)
            else -> this
        }
    }

    private fun createMessageBodyState(
        messageBodyUiModel: MessageBodyUiModel,
        expandCollapseMode: MessageBodyExpandCollapseMode,
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
            ),
            expandCollapseMode = expandCollapseMode
        )
    }

    private fun MessageBodyState.newStateFromSwitchViewMode(viewModePreference: ViewModePreference): MessageBodyState {
        return when (this) {
            is MessageBodyState.Data -> this.copy(
                messageBodyUiModel = messageBodyUiModel.copy(
                    messageBody = injectCssIntoDecryptedMessageBody(
                        MessageBodyWithType(messageBodyUiModel.messageBody, messageBodyUiModel.mimeType),
                        viewModePreference
                    ),
                    messageBodyWithoutQuote = injectCssIntoDecryptedMessageBody(
                        MessageBodyWithType(messageBodyUiModel.messageBodyWithoutQuote, messageBodyUiModel.mimeType),
                        viewModePreference
                    ),
                    viewModePreference = viewModePreference
                )
            )
            else -> this
        }
    }

    private fun MessageBodyState.newStateFromPrintRequested(): MessageBodyState {
        return when (this) {
            is MessageBodyState.Data -> this.copy(
                messageBodyUiModel = messageBodyUiModel.copy(
                    printEffect = Effect.of(Unit)
                )
            )
            else -> this
        }
    }
}
