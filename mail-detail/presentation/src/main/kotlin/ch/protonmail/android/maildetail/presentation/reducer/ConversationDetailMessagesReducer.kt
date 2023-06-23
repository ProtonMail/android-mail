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

import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.maildetail.presentation.R.string
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailEvent
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailMessageUiModel
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailOperation
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailsMessagesState
import ch.protonmail.android.mailmessage.domain.entity.MessageId
import javax.inject.Inject

class ConversationDetailMessagesReducer @Inject constructor() {

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

    private fun ConversationDetailsMessagesState.toNewExpandCollapseState(
        messageId: MessageId,
        conversationDetailMessageUiModel: ConversationDetailMessageUiModel
    ): ConversationDetailsMessagesState = when (this) {
        is ConversationDetailsMessagesState.Data -> ConversationDetailsMessagesState.Data(
            messages = messages.map {
                if (it.messageId == messageId) {
                    conversationDetailMessageUiModel
                } else {
                    it
                }
            }
        )

        else -> this
    }

    private fun ConversationDetailsMessagesState.toNewExpandingState(
        messageId: MessageId,
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
            }
        )

        else -> this
    }

    private fun ConversationDetailsMessagesState.toCollapsedState(
        messageId: MessageId
    ): ConversationDetailsMessagesState = when (this) {
        is ConversationDetailsMessagesState.Data -> ConversationDetailsMessagesState.Data(
            messages = messages.map {
                if (it.messageId == messageId && it is ConversationDetailMessageUiModel.Expanding) {
                    it.collapsed
                } else {
                    it
                }
            }
        )

        else -> this
    }
}
