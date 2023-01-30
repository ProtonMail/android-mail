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

package ch.protonmail.android.maildetail.presentation.model

import ch.protonmail.android.mailcommon.presentation.model.BottomBarEvent
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailOperation.AffectingBottomSheet
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailOperation.AffectingConversation
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailOperation.AffectingErrorBar
import ch.protonmail.android.maildetail.presentation.model.ConversationDetailOperation.AffectingMessages
import ch.protonmail.android.maillabel.domain.model.MailLabelId

sealed interface ConversationDetailOperation {

    sealed interface AffectingConversation : ConversationDetailOperation
    sealed interface AffectingMessages : ConversationDetailOperation
    sealed interface AffectingErrorBar
    sealed interface AffectingBottomSheet
}

sealed interface ConversationDetailEvent : ConversationDetailOperation {

    data class ConversationBottomBarEvent(val bottomBarEvent: BottomBarEvent) : ConversationDetailEvent

    data class ConversationData(
        val conversationUiModel: ConversationDetailMetadataUiModel
    ) : ConversationDetailEvent, AffectingConversation

    object ErrorLoadingContacts : ConversationDetailEvent, AffectingMessages
    object ErrorLoadingConversation : ConversationDetailEvent, AffectingConversation, AffectingMessages
    object ErrorLoadingMessages : ConversationDetailEvent, AffectingMessages
    object NoNetworkError : ConversationDetailEvent, AffectingMessages

    data class MessagesData(
        val messagesUiModels: List<ConversationDetailMessageUiModel>
    ) : ConversationDetailEvent, AffectingMessages

    data class ConversationBottomSheetEvent(
        val bottomSheetOperation: BottomSheetOperation
    ) : ConversationDetailEvent, AffectingBottomSheet

    object ErrorAddStar : ConversationDetailEvent, AffectingErrorBar
    object ErrorRemoveStar : ConversationDetailEvent, AffectingErrorBar
    object ErrorMarkingAsUnread : ConversationDetailEvent, AffectingErrorBar
    object ErrorMovingToTrash : ConversationDetailEvent, AffectingErrorBar
    object ErrorMovingConversation : ConversationDetailEvent, AffectingErrorBar
}

sealed interface ConversationDetailViewAction : ConversationDetailOperation {

    object Star : ConversationDetailViewAction, AffectingConversation
    object UnStar : ConversationDetailViewAction, AffectingConversation
    object MarkUnread : ConversationDetailViewAction
    object Trash : ConversationDetailViewAction
    object RequestMoveToBottomSheet : ConversationDetailViewAction, AffectingBottomSheet
    object DismissBottomSheet : ConversationDetailViewAction, AffectingBottomSheet
    data class MoveToDestinationSelected(
        val mailLabelId: MailLabelId
    ) : ConversationDetailViewAction, AffectingBottomSheet

    data class MoveToDestinationConfirmed(val mailLabelText: String) : ConversationDetailViewAction
}
