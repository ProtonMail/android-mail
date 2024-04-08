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
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.maildetail.presentation.R.string
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailEvent
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailMessageUiModel
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailOperation
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailViewAction
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailsMessagesState
import ch.protonmail.android.maildetail.presentation.model.MessageIdUiModel
import ch.protonmail.android.mailmessage.domain.model.MessageId
import ch.protonmail.android.mailmessage.presentation.model.MessageBodyExpandCollapseMode
import ch.protonmail.android.mailmessage.presentation.model.MessageBodyUiModel
import ch.protonmail.android.mailmessage.presentation.model.MessageBodyWithType
import ch.protonmail.android.mailmessage.presentation.model.ViewModePreference
import ch.protonmail.android.mailmessage.presentation.usecase.InjectCssIntoDecryptedMessageBody
import kotlinx.collections.immutable.toImmutableList
import javax.inject.Inject

@Suppress("ComplexMethod")
class ConversationDetailMessagesReducer @Inject constructor(
    private val injectCssIntoDecryptedMessageBody: InjectCssIntoDecryptedMessageBody
) {

    fun newStateFrom(
        currentState: ConversationDetailsMessagesState,
        operation: ConversationDetailOperation.AffectingMessages
    ): ConversationDetailsMessagesState = when (operation) {
        ConversationDetailEvent.ErrorLoadingContacts -> ConversationDetailsMessagesState.Error(
            message = TextUiModel(string.detail_error_loading_contacts)
        )

        is ConversationDetailEvent.ErrorLoadingMessages -> ConversationDetailsMessagesState.Error(
            message = TextUiModel(string.detail_error_loading_messages)
        )

        is ConversationDetailEvent.MessagesData -> ConversationDetailsMessagesState.Data(
            messages = operation.messagesUiModels
        )

        is ConversationDetailEvent.NoNetworkError -> currentState.toNewStateForNoNetworkError()
        is ConversationDetailEvent.ErrorLoadingConversation -> currentState.toNewStateForErrorLoadingConversation()
        is ConversationDetailEvent.CollapseDecryptedMessage ->
            currentState.toNewExpandCollapseState(
                operation.messageId,
                operation.conversationDetailMessageUiModel
            )

        is ConversationDetailEvent.ExpandDecryptedMessage ->
            currentState.toNewExpandCollapseState(
                operation.messageId,
                operation.conversationDetailMessageUiModel
            )

        is ConversationDetailEvent.ExpandingMessage ->
            currentState.toNewExpandingState(
                operation.messageId,
                operation.conversationDetailMessageUiModel
            )

        is ConversationDetailEvent.ErrorExpandingRetrievingMessageOffline ->
            currentState.toCollapsedState(operation.messageId)

        is ConversationDetailEvent.ErrorExpandingRetrieveMessageError ->
            currentState.toCollapsedState(operation.messageId)

        is ConversationDetailEvent.ErrorExpandingDecryptMessageError ->
            currentState.toCollapsedState(operation.messageId)

        is ConversationDetailEvent.ShowAllAttachmentsForMessage ->
            currentState.toNewExpandCollapseState(
                operation.messageId,
                operation.conversationDetailMessageUiModel
            )

        is ConversationDetailEvent.AttachmentStatusChanged ->
            currentState.newStateFromMessageAttachmentStatus(operation)

        is ConversationDetailViewAction.ExpandOrCollapseMessageBody -> {
            currentState.toNewMessageBodyExpandCollapseState(operation)
        }

        is ConversationDetailViewAction.LoadRemoteContent -> {
            currentState.toNewMessageBodyLoadRemoteContentState(operation)
        }

        is ConversationDetailViewAction.ShowEmbeddedImages -> {
            currentState.toNewMessageBodyShowEmbeddedImagesState(operation)
        }

        is ConversationDetailViewAction.LoadRemoteAndEmbeddedContent -> {
            currentState.toNewStateLoadRemoteAndEmbeddedContent(operation)
        }

        is ConversationDetailViewAction.SwitchViewMode -> {
            currentState.toNewStateForSwitchViewMode(operation.messageId, operation.viewModePreference)
        }

        is ConversationDetailViewAction.PrintRequested -> {
            currentState.toNewStateForPrintRequested(operation.messageId)
        }
    }

    private fun ConversationDetailsMessagesState.toNewStateForNoNetworkError() = when (this) {
        is ConversationDetailsMessagesState.Data -> this
        is ConversationDetailsMessagesState.Offline,
        is ConversationDetailsMessagesState.Loading,
        is ConversationDetailsMessagesState.Error -> ConversationDetailsMessagesState.Offline
    }

    private fun ConversationDetailsMessagesState.toNewStateForErrorLoadingConversation() = when (this) {
        is ConversationDetailsMessagesState.Data -> this
        is ConversationDetailsMessagesState.Offline,
        is ConversationDetailsMessagesState.Loading,
        is ConversationDetailsMessagesState.Error -> ConversationDetailsMessagesState.Error(
            message = TextUiModel(string.detail_error_loading_messages)
        )
    }

    private fun ConversationDetailsMessagesState.toNewStateLoadRemoteAndEmbeddedContent(
        operation: ConversationDetailViewAction.LoadRemoteAndEmbeddedContent
    ): ConversationDetailsMessagesState = when (this) {
        is ConversationDetailsMessagesState.Data -> ConversationDetailsMessagesState.Data(
            messages = messages.map {
                if (it.messageId == operation.messageId && it is ConversationDetailMessageUiModel.Expanded) {
                    it.copy(
                        messageBodyUiModel = it.messageBodyUiModel.copy(
                            shouldShowEmbeddedImages = true,
                            shouldShowRemoteContent = true,
                            shouldShowEmbeddedImagesBanner = false,
                            shouldShowRemoteContentBanner = false
                        )
                    )
                } else {
                    it
                }
            }.toImmutableList()
        )

        else -> this
    }

    private fun ConversationDetailsMessagesState.toNewMessageBodyShowEmbeddedImagesState(
        operation: ConversationDetailViewAction.ShowEmbeddedImages
    ): ConversationDetailsMessagesState = when (this) {
        is ConversationDetailsMessagesState.Data -> ConversationDetailsMessagesState.Data(
            messages = messages.map {
                if (it.messageId == operation.messageId && it is ConversationDetailMessageUiModel.Expanded) {
                    it.copy(
                        messageBodyUiModel = it.messageBodyUiModel.copy(
                            shouldShowEmbeddedImages = true,
                            shouldShowEmbeddedImagesBanner = false
                        )
                    )
                } else {
                    it
                }
            }.toImmutableList()
        )

        else -> this
    }

    private fun ConversationDetailsMessagesState.toNewMessageBodyLoadRemoteContentState(
        operation: ConversationDetailViewAction.LoadRemoteContent
    ): ConversationDetailsMessagesState = when (this) {
        is ConversationDetailsMessagesState.Data -> ConversationDetailsMessagesState.Data(
            messages = messages.map {
                if (it.messageId == operation.messageId && it is ConversationDetailMessageUiModel.Expanded) {
                    it.copy(
                        messageBodyUiModel = it.messageBodyUiModel.copy(
                            shouldShowRemoteContent = true,
                            shouldShowRemoteContentBanner = false
                        )
                    )
                } else {
                    it
                }
            }.toImmutableList()
        )

        else -> this
    }

    private fun ConversationDetailsMessagesState.toNewMessageBodyExpandCollapseState(
        operation: ConversationDetailViewAction.ExpandOrCollapseMessageBody
    ): ConversationDetailsMessagesState = when (this) {
        is ConversationDetailsMessagesState.Data -> ConversationDetailsMessagesState.Data(
            messages = messages.map {
                if (it.messageId == operation.messageId && it is ConversationDetailMessageUiModel.Expanded) {
                    it.copy(
                        expandCollapseMode = when (it.expandCollapseMode) {
                            MessageBodyExpandCollapseMode.Collapsed -> MessageBodyExpandCollapseMode.Expanded
                            MessageBodyExpandCollapseMode.Expanded -> MessageBodyExpandCollapseMode.Collapsed
                            else -> it.expandCollapseMode
                        }
                    )
                } else {
                    it
                }
            }.toImmutableList()
        )

        else -> this
    }

    private fun ConversationDetailsMessagesState.toNewExpandCollapseState(
        messageId: MessageIdUiModel,
        conversationDetailMessageUiModel: ConversationDetailMessageUiModel
    ): ConversationDetailsMessagesState = when (this) {
        is ConversationDetailsMessagesState.Data -> ConversationDetailsMessagesState.Data(
            messages = messages.map {
                if (it.messageId == messageId) {
                    conversationDetailMessageUiModel
                } else {
                    it
                }
            }.toImmutableList()
        )

        else -> this
    }

    private fun ConversationDetailsMessagesState.toNewExpandingState(
        messageId: MessageIdUiModel,
        conversationDetailMessageUiModel: ConversationDetailMessageUiModel.Collapsed
    ): ConversationDetailsMessagesState = when (this) {
        is ConversationDetailsMessagesState.Data -> ConversationDetailsMessagesState.Data(
            messages = messages.map {
                if (it.messageId == messageId) {
                    ConversationDetailMessageUiModel.Expanding(
                        messageId = messageId,
                        collapsed = conversationDetailMessageUiModel
                    )
                } else {
                    it
                }
            }.toImmutableList()
        )

        else -> this
    }

    private fun ConversationDetailsMessagesState.toCollapsedState(
        messageId: MessageIdUiModel
    ): ConversationDetailsMessagesState = when (this) {
        is ConversationDetailsMessagesState.Data -> ConversationDetailsMessagesState.Data(
            messages = messages.map {
                if (it.messageId == messageId && it is ConversationDetailMessageUiModel.Expanding) {
                    it.collapsed
                } else {
                    it
                }
            }.toImmutableList()
        )

        else -> this
    }

    private fun ConversationDetailsMessagesState.newStateFromMessageAttachmentStatus(
        operation: ConversationDetailEvent.AttachmentStatusChanged
    ): ConversationDetailsMessagesState {
        return when (this) {
            is ConversationDetailsMessagesState.Data -> {
                this.copy(
                    messages = this.messages.map {
                        if (it.messageId == operation.messageId && it is ConversationDetailMessageUiModel.Expanded) {
                            it.copy(messageBodyUiModel = createMessageBodyState(it.messageBodyUiModel, operation))
                        } else
                            it
                    }.toImmutableList()
                )
            }

            else -> this
        }
    }

    private fun createMessageBodyState(
        messageBodyUiModel: MessageBodyUiModel,
        operation: ConversationDetailEvent.AttachmentStatusChanged
    ): MessageBodyUiModel {
        val attachmentGroupUiModel = messageBodyUiModel.attachments
        return messageBodyUiModel.copy(
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
    }

    private fun ConversationDetailsMessagesState.toNewStateForSwitchViewMode(
        messageId: MessageId,
        viewModePreference: ViewModePreference
    ): ConversationDetailsMessagesState {
        return when (this) {
            is ConversationDetailsMessagesState.Data -> this.copy(
                messages = messages.map {
                    if (it is ConversationDetailMessageUiModel.Expanded && it.messageId.id == messageId.id) {
                        it.copy(
                            messageBodyUiModel = it.messageBodyUiModel.copy(
                                messageBody = injectCssIntoDecryptedMessageBody(
                                    MessageBodyWithType(
                                        it.messageBodyUiModel.messageBody,
                                        it.messageBodyUiModel.mimeType
                                    ),
                                    viewModePreference
                                ),
                                messageBodyWithoutQuote = injectCssIntoDecryptedMessageBody(
                                    MessageBodyWithType(
                                        it.messageBodyUiModel.messageBodyWithoutQuote,
                                        it.messageBodyUiModel.mimeType
                                    ),
                                    viewModePreference
                                ),
                                viewModePreference = viewModePreference
                            )
                        )
                    } else {
                        it
                    }
                }.toImmutableList()
            )
            else -> this
        }
    }

    private fun ConversationDetailsMessagesState.toNewStateForPrintRequested(
        messageId: MessageId
    ): ConversationDetailsMessagesState {
        return when (this) {
            is ConversationDetailsMessagesState.Data -> this.copy(
                messages = messages.map { messageUiModel ->
                    if (
                        messageUiModel is ConversationDetailMessageUiModel.Expanded &&
                        messageUiModel.messageId.id == messageId.id
                    ) {
                        messageUiModel.copy(
                            messageBodyUiModel = messageUiModel.messageBodyUiModel.copy(
                                printEffect = Effect.of(Unit)
                            )
                        )
                    } else {
                        messageUiModel
                    }
                }.toImmutableList()
            )
            else -> this
        }
    }
}
