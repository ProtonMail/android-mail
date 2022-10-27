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

import ch.protonmail.android.mailcommon.presentation.reducer.BottomBarReducer
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailEvent
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailOperation
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailState
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailViewAction
import javax.inject.Inject

class ConversationDetailReducer @Inject constructor(
    private val bottomBarReducer: BottomBarReducer,
    private val metadataReducer: ConversationDetailMetadataReducer,
    private val messagesReducer: ConversationDetailMessagesReducer
) {

    fun newStateFrom(
        currentState: ConversationDetailState,
        operation: ConversationDetailOperation
    ): ConversationDetailState =
        when (operation) {
            is ConversationDetailEvent.ConversationData -> currentState.copy(
                conversationState = metadataReducer.newStateFrom(currentState.conversationState, operation)
            )
            is ConversationDetailEvent.ErrorLoadingConversation -> currentState.copy(
                conversationState = metadataReducer.newStateFrom(currentState.conversationState, operation)
            )
            is ConversationDetailEvent.ErrorLoadingMessages -> currentState.copy(
                messagesState = messagesReducer.newStateFrom(currentState.messagesState, operation)
            )
            is ConversationDetailEvent.MessagesData -> currentState.copy(
                messagesState = messagesReducer.newStateFrom(currentState.messagesState, operation)
            )
            is ConversationDetailEvent.NoPrimaryUser -> currentState.copy(
                conversationState = metadataReducer.newStateFrom(currentState.conversationState, operation)
            )
            is ConversationDetailViewAction.Star -> currentState.copy(
                conversationState = metadataReducer.newStateFrom(currentState.conversationState, operation)
            )
            is ConversationDetailViewAction.UnStar -> currentState.copy(
                conversationState = metadataReducer.newStateFrom(currentState.conversationState, operation)
            )
            is ConversationDetailEvent.ConversationBottomBarEvent -> currentState.copy(
                bottomBarState = bottomBarReducer.newStateFrom(currentState.bottomBarState, operation.bottomBarEvent)
            )
        }
}
