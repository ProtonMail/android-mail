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
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailMetadataState
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailOperation
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailViewAction
import javax.inject.Inject

class ConversationDetailMetadataReducer @Inject constructor() {

    fun newStateFrom(
        currentState: ConversationDetailMetadataState,
        event: ConversationDetailOperation.AffectingConversation
    ): ConversationDetailMetadataState = when (event) {
        is ConversationDetailEvent.ConversationData -> ConversationDetailMetadataState.Data(
            conversationUiModel = event.conversationUiModel
        )
        is ConversationDetailEvent.ErrorLoadingConversation -> currentState.toNewStateForErrorLoading()
        is ConversationDetailViewAction.Star -> currentState.toNewStateForStarredConversation()
        is ConversationDetailViewAction.UnStar -> currentState.toNewStateForUnStarredConversation()
    }

    private fun ConversationDetailMetadataState.toNewStateForStarredConversation() = when (this) {
        is ConversationDetailMetadataState.Data -> copy(conversationUiModel.copy(isStarred = true))
        is ConversationDetailMetadataState.Error -> this
        is ConversationDetailMetadataState.Loading -> this
    }

    private fun ConversationDetailMetadataState.toNewStateForUnStarredConversation() = when (this) {
        is ConversationDetailMetadataState.Data -> copy(conversationUiModel.copy(isStarred = false))
        is ConversationDetailMetadataState.Error -> this
        is ConversationDetailMetadataState.Loading -> this
    }

    private fun ConversationDetailMetadataState.toNewStateForErrorLoading() = when (this) {
        is ConversationDetailMetadataState.Data -> this
        is ConversationDetailMetadataState.Loading,
        is ConversationDetailMetadataState.Error -> ConversationDetailMetadataState.Error(
            message = TextUiModel(string.detail_error_loading_conversation)
        )
    }

}
