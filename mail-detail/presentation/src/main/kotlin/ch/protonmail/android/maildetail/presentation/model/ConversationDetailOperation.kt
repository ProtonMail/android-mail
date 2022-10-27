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

sealed interface ConversationDetailOperation {

    sealed interface AffectingConversation : ConversationDetailOperation
    sealed interface AffectingMessages : ConversationDetailOperation
}

sealed interface ConversationDetailEvent : ConversationDetailOperation {

    data class ConversationBottomBarEvent(val bottomBarEvent: BottomBarEvent) : ConversationDetailEvent

    data class ConversationData(
        val conversationUiModel: ConversationDetailMetadataUiModel
    ) : ConversationDetailEvent, ConversationDetailOperation.AffectingConversation

    object ErrorLoadingConversation : ConversationDetailEvent, ConversationDetailOperation.AffectingConversation

    object ErrorLoadingMessages : ConversationDetailEvent, ConversationDetailOperation.AffectingMessages

    data class MessagesData(
        val messagesUiModels: List<ConversationDetailMessageUiModel>
    ) : ConversationDetailEvent, ConversationDetailOperation.AffectingMessages

    object NoPrimaryUser :
        ConversationDetailEvent,
        ConversationDetailOperation.AffectingConversation,
        ConversationDetailOperation.AffectingMessages
}

sealed interface ConversationDetailViewAction : ConversationDetailOperation {

    object Star : ConversationDetailViewAction, ConversationDetailOperation.AffectingConversation
    object UnStar : ConversationDetailViewAction, ConversationDetailOperation.AffectingConversation
}
