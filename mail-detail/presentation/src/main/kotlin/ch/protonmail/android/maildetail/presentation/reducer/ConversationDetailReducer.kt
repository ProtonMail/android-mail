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

import arrow.core.Option
import arrow.core.some
import ch.protonmail.android.mailcommon.presentation.Effect
import ch.protonmail.android.mailcommon.presentation.model.TextUiModel
import ch.protonmail.android.mailcommon.presentation.reducer.BottomBarReducer
import ch.protonmail.android.maildetail.presentation.R
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailEvent
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailOperation
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailState
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailViewAction
import me.proton.core.util.kotlin.exhaustive
import javax.inject.Inject

class ConversationDetailReducer @Inject constructor(
    private val bottomBarReducer: BottomBarReducer,
    private val metadataReducer: ConversationDetailMetadataReducer,
    private val messagesReducer: ConversationDetailMessagesReducer
) {

    fun newStateFrom(
        currentState: ConversationDetailState,
        operation: ConversationDetailOperation
    ): ConversationDetailState = currentState.copy(
        conversationState = currentState.toNewConversationState(operation),
        messagesState = currentState.toNewMessageState(operation),
        bottomBarState = currentState.toNewBottomBarState(operation),
        error = currentState.toErrorState(operation),
        exitScreenEffect = currentState.toDismissState(operation)
    )

    private fun ConversationDetailState.toNewConversationState(operation: ConversationDetailOperation) =
        if (operation is ConversationDetailOperation.AffectingConversation) {
            metadataReducer.newStateFrom(conversationState, operation)
        } else {
            conversationState
        }

    private fun ConversationDetailState.toNewMessageState(operation: ConversationDetailOperation) =
        if (operation is ConversationDetailOperation.AffectingMessages) {
            messagesReducer.newStateFrom(messagesState, operation)
        } else {
            messagesState
        }

    private fun ConversationDetailState.toNewBottomBarState(operation: ConversationDetailOperation) =
        if (operation is ConversationDetailEvent.ConversationBottomBarEvent) {
            bottomBarReducer.newStateFrom(bottomBarState, operation.bottomBarEvent)
        } else {
            bottomBarState
        }

    private fun ConversationDetailState.toErrorState(operation: ConversationDetailOperation) =
        if (operation is ConversationDetailOperation.AffectingErrorBar) {
            when (operation) {
                is ConversationDetailEvent.ErrorAddStar -> Effect.of(TextUiModel(R.string.error_star_operation_failed))
                is ConversationDetailEvent.ErrorRemoveStar -> Effect.of(
                    TextUiModel(R.string.error_unstar_operation_failed)
                )
                is ConversationDetailEvent.ErrorMovingToTrash -> Effect.of(
                    TextUiModel(R.string.error_move_to_trash_failed)
                )
            }.exhaustive
        } else {
            error
        }

    private fun ConversationDetailState.toDismissState(
        operation: ConversationDetailOperation
    ): Effect<Option<TextUiModel>> = when (operation) {
        is ConversationDetailViewAction.Trash -> Effect.of(TextUiModel(R.string.conversation_moved_to_trash).some())
        else -> exitScreenEffect
    }
}
